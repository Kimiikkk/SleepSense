package com.sleepsense.service;

import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import com.sleepsense.repository.SleepSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Computes period summaries from a bounded list of the authenticated user's saved sessions. */
@Service
public class SleepAnalyticsService {
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM d");
    private final SleepSessionRepository sessions;
    private final SleepScoreCalculator scores;

    public SleepAnalyticsService(SleepSessionRepository sessions, SleepScoreCalculator scores) {
        this.sessions = sessions;
        this.scores = scores;
    }

    public Map<String, Object> analytics(User user, int days) {
        if (days != 1 && days != 7 && days != 30 && days != 90) throw new IllegalArgumentException("Choose 1, 7, 30, or 90 days.");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.toLocalDate().minusDays(days - 1L).atStartOfDay();
        LocalDateTime previousStart = start.minusDays(days);
        LocalDateTime previousEnd = start.minusNanos(1);
        List<SleepSession> recent = scoredRecent(user);
        List<SleepSession> current = recent.stream().filter(s -> between(s, start, now)).toList();
        List<SleepSession> previous = recent.stream().filter(s -> between(s, previousStart, previousEnd)).toList();
        List<Map<String, Object>> points = points(current, recent);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("sessionCount", current.size());
        result.put("metrics", metrics(current));
        result.put("previousMetrics", metrics(previous));
        result.put("comparisons", comparisons(metrics(current), metrics(previous)));
        result.put("sessions", points);
        result.put("stageDistribution", stageDistribution(current));
        result.put("consistencyBySession", consistencySeries(current));
        return result;
    }

    public Map<String, Object> scoreAverages(User user) {
        List<SleepSession> recent = scoredRecent(user);
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int days : List.of(7, 30)) {
            List<SleepSession> selected = recent.stream().filter(s -> between(s,
                    now.toLocalDate().minusDays(days - 1L).atStartOfDay(), now)).toList();
            List<SleepSession> available = selected.stream().filter(s -> s.getSmartSleepScore() != null).toList();
            result.put("last" + days, available.isEmpty() ? null
                    : average(available, s -> s.getSmartSleepScore().score()));
            result.put("last" + days + "Count", available.size());
        }
        return result;
    }

    public List<SleepSession> recent(User user, int limit) {
        List<SleepSession> recent = sessions.findRecentByUserOrderByStartTimeDesc(user, limit);
        for (int index = recent.size() - 1; index >= 0; index--) {
            SleepSession night = recent.get(index);
            if (night.getSmartSleepScore() == null) night.setSmartSleepScore(scores.calculate(night,
                    recent.subList(index + 1, Math.min(recent.size(), index + 7))));
        }
        return recent;
    }

    private List<SleepSession> scoredRecent(User user) {
        List<SleepSession> recent = sessions.findRecentByUserOrderByStartTimeDesc(user, 500);
        for (int index = recent.size() - 1; index >= 0; index--) {
            SleepSession night = recent.get(index);
            if (night.getSmartSleepScore() == null) {
                night.setSmartSleepScore(scores.calculate(night,
                        recent.subList(index + 1, Math.min(recent.size(), index + 7))));
            }
        }
        return recent;
    }

    private boolean between(SleepSession session, LocalDateTime from, LocalDateTime to) {
        return session.getEndTime() != null && !session.getEndTime().isBefore(from) && !session.getEndTime().isAfter(to);
    }

    private List<Map<String, Object>> points(List<SleepSession> period, List<SleepSession> history) {
        List<Map<String, Object>> output = new ArrayList<>();
        for (SleepSession session : period.stream().sorted(Comparator.comparing(SleepSession::getEndTime)).toList()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("id", session.getId());
            point.put("date", session.getEndTime() == null ? "" : session.getEndTime().format(LABEL));
            point.put("endDate", session.getEndTime() == null ? "" : session.getEndTime().toLocalDate().toString());
            point.put("totalMinutes", available(session, "totalMinutes") ? session.getTotalMinutes() : null);
            point.put("score", session.getSmartSleepScore() == null
                    ? scores.calculate(session, olderThan(session, history)).score()
                    : session.getSmartSleepScore().score());
            point.put("deepMinutes", available(session, "deepMinutes") ? session.getDeepMinutes() : null);
            point.put("remMinutes", available(session, "remMinutes") ? session.getRemMinutes() : null);
            point.put("awakeMinutes", available(session, "awakeMinutes") ? session.getAwakeMinutes() : null);
            point.put("heartRate", available(session, "averageHeartRate") ? session.getAverageHeartRate() : null);
            point.put("spo2", available(session, "spo2") ? session.getSpo2() : null);
            point.put("dataMode", session.getDataMode());
            output.add(point);
        }
        return output;
    }

    private List<SleepSession> olderThan(SleepSession current, List<SleepSession> history) {
        return history.stream().filter(s -> s.getEndTime() != null && current.getEndTime() != null
                        && !s.getEndTime().isAfter(current.getEndTime()) && !s.getId().equals(current.getId()))
                .sorted(Comparator.comparing(SleepSession::getEndTime).reversed()).limit(6).toList();
    }

    private Map<String, Object> metrics(List<SleepSession> list) {
        List<SleepSession> withScores = list.stream().filter(s -> s.getSmartSleepScore() != null).toList();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("avgSleepDurationMinutes", average(list, s -> s.getTotalMinutes(), "totalMinutes"));
        value.put("avgSleepScore", withScores.isEmpty() ? null : average(withScores,
                s -> s.getSmartSleepScore().score()));
        value.put("bestSession", withScores.stream().max(Comparator.comparingInt(s -> s.getSmartSleepScore().score()))
                .map(s -> sessionLabel(s, s.getSmartSleepScore().score())).orElse(null));
        value.put("lowestScore", withScores.stream().min(Comparator.comparingInt(s -> s.getSmartSleepScore().score()))
                .map(s -> sessionLabel(s, s.getSmartSleepScore().score())).orElse(null));
        value.put("avgDeepMinutes", average(list, SleepSession::getDeepMinutes, "deepMinutes"));
        value.put("avgRemMinutes", average(list, SleepSession::getRemMinutes, "remMinutes"));
        value.put("avgDeepPercentage", averagePercentage(list, "deepMinutes"));
        value.put("avgRemPercentage", averagePercentage(list, "remMinutes"));
        value.put("avgAwakeMinutes", average(list, s -> s.getAwakeMinutes(), "awakeMinutes"));
        value.put("avgHeartRate", average(list, s -> s.getAverageHeartRate(), "averageHeartRate"));
        value.put("avgSpo2", average(list, SleepSession::getSpo2, "spo2"));
        value.put("consistencyPercentage", consistency(list));
        return value;
    }

    private Map<String, Object> sessionLabel(SleepSession session, int score) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", session.getEndTime() == null ? "" : session.getEndTime().format(LABEL));
        result.put("score", score);
        return result;
    }

    private Map<String, Object> comparisons(Map<String, Object> current, Map<String, Object> previous) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("avgSleepDurationMinutes", "avgSleepScore", "avgDeepPercentage",
                "avgRemPercentage", "avgAwakeMinutes", "avgHeartRate", "avgSpo2", "consistencyPercentage")) {
            Object now = current.get(key), before = previous.get(key);
            Map<String, Object> item = new LinkedHashMap<>();
            if (now instanceof Number n && before instanceof Number p) {
                double difference = n.doubleValue() - p.doubleValue();
                item.put("difference", Math.round(difference * 10.0) / 10.0);
                item.put("direction", comparisonDirection(key, n.doubleValue(), p.doubleValue(), difference));
            } else {
                item.put("difference", null);
                item.put("direction", "unavailable");
            }
            result.put(key, item);
        }
        return result;
    }

    private String comparisonDirection(String metric, double current, double previous, double difference) {
        if (Math.abs(difference) < 0.05) return "steady";
        return switch (metric) {
            case "avgSleepScore", "consistencyPercentage" -> current > previous ? "improved" : "declined";
            case "avgAwakeMinutes" -> current < previous ? "improved" : "declined";
            case "avgSleepDurationMinutes" -> Math.abs(current - 480) < Math.abs(previous - 480) ? "improved" : "declined";
            case "avgDeepPercentage" -> Math.abs(current - 18) < Math.abs(previous - 18) ? "improved" : "declined";
            case "avgRemPercentage" -> Math.abs(current - 22) < Math.abs(previous - 22) ? "improved" : "declined";
            default -> "changed";
        };
    }

    private Map<String, Object> stageDistribution(List<SleepSession> list) {
        Map<String, Object> distribution = new LinkedHashMap<>();
        for (String stage : List.of("awake", "light", "deep", "rem")) {
            String field = switch (stage) {
                case "awake" -> "awakeMinutes";
                case "light" -> "lightMinutes";
                case "deep" -> "deepMinutes";
                default -> "remMinutes";
            };
            distribution.put(stage, averagePercentage(list, field));
        }
        return distribution;
    }

    private List<Map<String, Object>> consistencySeries(List<SleepSession> list) {
        List<SleepSession> ordered = list.stream().filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .sorted(Comparator.comparing(SleepSession::getEndTime)).toList();
        if (ordered.size() < 3) return List.of();
        double bedMean = meanClock(ordered, s -> s.getStartTime());
        double wakeMean = meanClock(ordered, s -> s.getEndTime());
        List<Map<String, Object>> result = new ArrayList<>();
        for (SleepSession session : ordered) {
            double distance = (clockDistance(session.getStartTime(), bedMean)
                    + clockDistance(session.getEndTime(), wakeMean)) / 2;
            double consistency = Math.max(0, Math.round((100 - Math.min(100, distance * 100 / 180)) * 10) / 10.0);
            result.add(Map.of("date", session.getEndTime().format(LABEL), "value", consistency));
        }
        return result;
    }

    private Double consistency(List<SleepSession> list) {
        List<Map<String, Object>> series = consistencySeries(list);
        return series.isEmpty() ? null : series.stream()
                .mapToDouble(point -> ((Number) point.get("value")).doubleValue()).average().orElse(Double.NaN);
    }

    private double meanClock(List<SleepSession> list, java.util.function.Function<SleepSession, LocalDateTime> getter) {
        double x = 0, y = 0;
        for (SleepSession session : list) {
            double angle = getter.apply(session).getHour() * 2 * Math.PI / 24
                    + getter.apply(session).getMinute() * 2 * Math.PI / 1440;
            x += Math.cos(angle); y += Math.sin(angle);
        }
        double angle = Math.atan2(y, x);
        if (angle < 0) angle += 2 * Math.PI;
        return angle * 1440 / (2 * Math.PI);
    }

    private double clockDistance(LocalDateTime time, double meanMinute) {
        double minute = time.getHour() * 60 + time.getMinute();
        double diff = Math.abs(minute - meanMinute);
        return Math.min(diff, 1440 - diff);
    }

    private Double averagePercentage(List<SleepSession> list, String metric) {
        List<SleepSession> available = list.stream().filter(s -> available(s, metric)
                && available(s, "totalMinutes") && s.getTotalMinutes() > 0).toList();
        if (available.isEmpty()) return null;
        return average(available, s -> {
            int value = switch (metric) {
                case "awakeMinutes" -> s.getAwakeMinutes();
                case "lightMinutes" -> s.getLightMinutes();
                case "deepMinutes" -> s.getDeepMinutes();
                default -> s.getRemMinutes();
            };
            return 100.0 * value / s.getTotalMinutes();
        });
    }

    private Double average(List<SleepSession> list, ToDoubleFunction<SleepSession> getter, String metric) {
        List<SleepSession> available = list.stream().filter(s -> available(s, metric)).toList();
        return available.isEmpty() ? null : average(available, getter);
    }

    private Double average(List<SleepSession> list, ToDoubleFunction<SleepSession> getter) {
        return list.isEmpty() ? null : list.stream().mapToDouble(getter).average().orElse(Double.NaN);
    }

    private boolean available(SleepSession session, String metric) {
        return session.isMetricAvailable(metric);
    }
}
