package com.sleepsense.service;

import com.sleepsense.model.SleepJournalEntry;
import com.sleepsense.model.SleepSession;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reports descriptive associations only; this intentionally makes no causal claims. */
@Service
public class JournalCorrelationAnalyzer {
    public Map<String, Object> analyze(List<SleepJournalEntry> journal, List<SleepSession> sessions) {
        Map<LocalDate, SleepSession> byDate = new LinkedHashMap<>();
        sessions.stream().filter(s -> s.getEndTime() != null)
                .sorted(java.util.Comparator.comparing(SleepSession::getEndTime).reversed())
                .forEach(s -> byDate.putIfAbsent(s.getEndTime().toLocalDate(), s));
        List<Pair> pairs = new ArrayList<>();
        for (SleepJournalEntry entry : journal) {
            SleepSession session = byDate.get(entry.date());
            if (session != null && session.getSmartSleepScore() != null) {
                pairs.add(new Pair(entry, session.getSmartSleepScore().score()));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampleSize", pairs.size());
        result.put("minimumSampleSize", 5);
        result.put("disclaimer", "These are descriptive associations in your saved entries. Correlation does not show that a habit caused a sleep change.");
        result.put("simulated", sessions.stream().anyMatch(s -> "simulated".equalsIgnoreCase(s.getDataMode())));
        List<Map<String, Object>> correlations = new ArrayList<>();
        if (pairs.size() >= 5) {
            correlations.add(correlation("Stress", pairs, p -> p.entry.stressLevel()));
            correlations.add(correlation("Caffeine", pairs, p -> p.entry.caffeineCups()));
            correlations.add(correlation("Pre-bed screen time", pairs, p -> p.entry.screenTimeMinutes()));
        }
        result.put("correlations", correlations);
        result.put("ready", pairs.size() >= 5);
        return result;
    }

    private Map<String, Object> correlation(String label, List<Pair> pairs, java.util.function.ToDoubleFunction<Pair> x) {
        double meanX = pairs.stream().mapToDouble(x).average().orElse(0);
        double meanY = pairs.stream().mapToDouble(Pair::score).average().orElse(0);
        double covariance = 0, varianceX = 0, varianceY = 0;
        for (Pair pair : pairs) {
            double dx = x.applyAsDouble(pair) - meanX;
            double dy = pair.score - meanY;
            covariance += dx * dy;
            varianceX += dx * dx;
            varianceY += dy * dy;
        }
        Double coefficient = varianceX == 0 || varianceY == 0 ? null
                : Math.round(covariance / Math.sqrt(varianceX * varianceY) * 100.0) / 100.0;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("habit", label);
        item.put("sleepMetric", "Sleep score");
        item.put("coefficient", coefficient);
        item.put("note", coefficient == null ? "Not enough variation to compare."
                : Math.abs(coefficient) < .2 ? "Little linear association in this sample."
                : coefficient > 0 ? "Higher values moved with higher scores in this sample."
                : "Higher values moved with lower scores in this sample.");
        return item;
    }

    private record Pair(SleepJournalEntry entry, double score) {}
}
