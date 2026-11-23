package com.sleepsense.service;

import com.sleepsense.model.SleepScoreBreakdown;
import com.sleepsense.model.SleepSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

/** Pure, explainable scoring rules. Missing inputs are omitted and never treated as zero. */
@Service
public class SleepScoreCalculator {
    @Value("${sleepsense.score.weight.duration:25}") private int durationWeight = 25;
    @Value("${sleepsense.score.weight.deep:15}") private int deepWeight = 15;
    @Value("${sleepsense.score.weight.rem:15}") private int remWeight = 15;
    @Value("${sleepsense.score.weight.awake:10}") private int awakeWeight = 10;
    @Value("${sleepsense.score.weight.efficiency:15}") private int efficiencyWeight = 15;
    @Value("${sleepsense.score.weight.consistency:10}") private int consistencyWeight = 10;
    @Value("${sleepsense.score.weight.heart-rate:10}") private int heartRateWeight = 10;
    @Value("${sleepsense.score.max-with-missing-core:84}") private int maxWithMissingCoreMetric = 84;
    @Value("${sleepsense.score.max-without-consistency:92}") private int maxWithoutConsistency = 92;

    public SleepScoreBreakdown calculate(SleepSession session, List<SleepSession> history) {
        if (session == null) throw new IllegalArgumentException("A sleep session is required.");
        List<SleepSession> previous = history == null ? List.of() : history.stream()
                .filter(candidate -> candidate != null && candidate != session)
                .filter(candidate -> candidate.getEndTime() != null
                        && (session.getEndTime() == null || !candidate.getEndTime().isAfter(session.getEndTime())))
                .filter(candidate -> session.getId() == null || !session.getId().equals(candidate.getId()))
                .sorted(Comparator.comparing(SleepSession::getEndTime).reversed())
                .toList();

        SleepScoreBreakdown current = calculateWithoutComparison(session, previous);
        Integer priorScore = null;
        if (!previous.isEmpty()) {
            SleepSession mostRecent = previous.get(0);
            List<SleepSession> earlier = previous.stream().skip(1).toList();
            priorScore = calculateWithoutComparison(mostRecent, earlier).score();
        }
        return new SleepScoreBreakdown(current.score(), current.category(), priorScore,
                priorScore == null ? null : current.score() - priorScore,
                current.provisional(), current.factors());
    }

    private SleepScoreBreakdown calculateWithoutComparison(SleepSession session, List<SleepSession> previous) {
        List<SleepScoreBreakdown.Factor> factors = new ArrayList<>();
        int weightedPoints = 0;
        int availableWeight = 0;

        if (validDuration(session)) {
            add(factors, "duration", "Sleep duration", durationScore(session.getTotalMinutes()),
                    durationWeight, "Closer to 8 hours receives a stronger contribution.");
        }
        if (validStage(session, "deepMinutes")) {
            double share = 100.0 * session.getDeepMinutes() / session.getTotalMinutes();
            add(factors, "deep", "Deep sleep", targetScore(share, 18, 5), deepWeight,
                    "Compared with an 18% reference share; stages are estimates.");
        }
        if (validStage(session, "remMinutes")) {
            double share = 100.0 * session.getRemMinutes() / session.getTotalMinutes();
            add(factors, "rem", "REM sleep", targetScore(share, 22, 4.5), remWeight,
                    "Compared with a 22% reference share; stages are estimates.");
        }
        if (validDuration(session) && session.isMetricAvailable("awakeMinutes")
                && session.getAwakeMinutes() >= 0 && session.getAwakeMinutes() <= session.getTotalMinutes()) {
            double score = clamp(100 - session.getAwakeMinutes() * 1.2);
            add(factors, "awake", "Awake time", score, awakeWeight,
                    "Lower awake time contributes more to this prototype score.");
            double efficiency = 100.0 * (session.getTotalMinutes() - session.getAwakeMinutes())
                    / session.getTotalMinutes();
            add(factors, "efficiency", "Sleep efficiency", clamp((efficiency - 65) / 25 * 100),
                    efficiencyWeight, "Estimated as time asleep divided by session duration.");
        }
        OptionalDouble consistency = consistencyScore(session, previous);
        consistency.ifPresent(score -> add(factors, "consistency", "Schedule consistency", score,
                consistencyWeight, "Compared with the start and end times of earlier sessions."));
        if (session.isMetricAvailable("averageHeartRate")
                && session.getAverageHeartRate() >= 30 && session.getAverageHeartRate() <= 110) {
            int bpm = session.getAverageHeartRate();
            double score = bpm < 50 ? 100 - (50 - bpm) * 3.0
                    : bpm > 65 ? 100 - (bpm - 65) * 3.0 : 100;
            add(factors, "heartRate", "Resting heart rate", clamp(score), heartRateWeight,
                    "A supporting input, compared with a broad 50–65 BPM prototype band.");
        }

        for (SleepScoreBreakdown.Factor factor : factors) {
            weightedPoints += factor.points();
            availableWeight += factor.weight();
        }
        int score = availableWeight == 0 ? 0 : (int) Math.round(100.0 * weightedPoints / availableWeight);
        boolean missingCore = !validDuration(session)
                || !validStage(session, "deepMinutes")
                || !validStage(session, "remMinutes")
                || !session.isMetricAvailable("awakeMinutes");
        if (missingCore) score = Math.min(score, maxWithMissingCoreMetric);
        else if (consistency.isEmpty()) score = Math.min(score, maxWithoutConsistency);
        return new SleepScoreBreakdown(score, category(score), null, null,
                missingCore || consistency.isEmpty(), factors);
    }

    private void add(List<SleepScoreBreakdown.Factor> factors, String metric, String label,
                     double rawScore, int weight, String explanation) {
        int component = (int) Math.round(clamp(rawScore));
        String direction = component >= 80 ? "positive" : component < 60 ? "attention" : "steady";
        factors.add(new SleepScoreBreakdown.Factor(metric, label,
                (int) Math.round(component * weight / 100.0), weight, direction, explanation));
    }

    private boolean validDuration(SleepSession session) {
        return session.isMetricAvailable("totalMinutes")
                && session.getTotalMinutes() >= 120 && session.getTotalMinutes() <= 960;
    }

    private boolean validStage(SleepSession session, String metric) {
        if (!validDuration(session)) return false;
        int value = "deepMinutes".equals(metric) ? session.getDeepMinutes() : session.getRemMinutes();
        return session.isMetricAvailable(metric) && value >= 0 && value <= session.getTotalMinutes();
    }

    private OptionalDouble consistencyScore(SleepSession current, List<SleepSession> previous) {
        if (current.getStartTime() == null || current.getEndTime() == null) return OptionalDouble.empty();
        List<SleepSession> comparable = previous.stream().filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .limit(6).toList();
        if (comparable.isEmpty()) return OptionalDouble.empty();
        double totalDifference = 0;
        for (SleepSession candidate : comparable) {
            totalDifference += clockDistance(current.getStartTime(), candidate.getStartTime());
            totalDifference += clockDistance(current.getEndTime(), candidate.getEndTime());
        }
        double averageMinutes = totalDifference / (2 * comparable.size());
        return OptionalDouble.of(clamp(100 - averageMinutes * 0.7));
    }

    private double clockDistance(LocalDateTime left, LocalDateTime right) {
        int leftMinute = left.getHour() * 60 + left.getMinute();
        int rightMinute = right.getHour() * 60 + right.getMinute();
        int difference = Math.abs(leftMinute - rightMinute);
        return Math.min(difference, 1440 - difference);
    }

    private double durationScore(int minutes) {
        return clamp(100 - Math.abs(minutes - 480) * 0.32);
    }

    private double targetScore(double value, double target, double penaltyPerPoint) {
        return clamp(100 - Math.abs(value - target) * penaltyPerPoint);
    }

    private double clamp(double value) { return Math.max(0, Math.min(100, value)); }

    private String category(int score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 55) return "Fair";
        return "Needs improvement";
    }
}
