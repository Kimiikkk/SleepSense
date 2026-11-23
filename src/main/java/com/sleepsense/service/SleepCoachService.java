package com.sleepsense.service;

import com.sleepsense.model.SleepJournalEntry;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SleepCoachService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String INSTRUCTIONS = "You are SleepSense, a general wellness sleep coach. "
            + "Use only the supplied user's data; do not invent readings or infer diagnoses. "
            + "Separate observed values from interpretations, state when values are simulated or missing, "
            + "keep suggestions practical and non-medical, and encourage professional care for medical concerns.";
    private final SleepAnalyticsService analytics;
    private final SleepJournalService journal;
    private final AIChatService rules;
    private final ObjectProvider<SleepCoachProvider> provider;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> requestsByUser = new ConcurrentHashMap<>();

    public SleepCoachService(SleepAnalyticsService analytics, SleepJournalService journal,
                             AIChatService rules, ObjectProvider<SleepCoachProvider> provider) {
        this.analytics = analytics;
        this.journal = journal;
        this.rules = rules;
        this.provider = provider;
    }

    public CoachReply ask(User user, String question) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("Ask a question about your sleep.");
        if (question.length() > 500) throw new IllegalArgumentException("Keep your question under 500 characters.");
        List<SleepSession> history = analytics.recent(user, 14);
        SleepSession latest = history.isEmpty() ? null : history.get(0);
        String fallback = ruleFallback(question, history, latest);
        SleepCoachProvider activeProvider = provider.getIfAvailable();
        if (activeProvider == null || !allowRequest(user.getId())) {
            return new CoachReply(fallback, "SleepSense guide", "General wellness information; not a diagnosis.");
        }
        try {
            String input = "Recent saved SleepSense sessions (maximum 14):\n" + sessionContext(history)
                    + "\nRecent sleep journal entries (maximum 7):\n" + journalContext(user)
                    + "\nQuestion: " + question.trim();
            String answer = activeProvider.generate(INSTRUCTIONS, input);
            return new CoachReply(answer, "AI interpretation based on saved context",
                    "AI-generated general wellness information; not a diagnosis. Readings marked simulated are not measured data.");
        } catch (Exception ignored) {
            return new CoachReply(fallback, "SleepSense guide · local fallback",
                    "The external coach is unavailable. General wellness information only; not a diagnosis.");
        }
    }

    private boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        if (requestsByUser.size() > 5000) requestsByUser.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().isEmpty() || now - entry.getValue().peekLast() > 120_000;
            }
        });
        ArrayDeque<Long> calls = requestsByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (calls) {
            while (!calls.isEmpty() && now - calls.peekFirst() > 60_000) calls.removeFirst();
            if (calls.size() >= 8) return false;
            calls.addLast(now);
            return true;
        }
    }

    private String sessionContext(List<SleepSession> history) {
        if (history.isEmpty()) return "No sleep sessions have been saved yet.";
        StringBuilder result = new StringBuilder();
        for (SleepSession session : history) {
            result.append("- ").append(session.getEndTime() == null ? "unknown date" : session.getEndTime().format(DATE))
                    .append(" (" ).append(session.getDataMode()).append("): ");
            append(result, session, "totalMinutes", "duration", session.getTotalMinutes() + " min");
            append(result, session, "deepMinutes", "deep", session.getDeepMinutes() + " min");
            append(result, session, "remMinutes", "REM", session.getRemMinutes() + " min");
            append(result, session, "awakeMinutes", "awake", session.getAwakeMinutes() + " min");
            if (session.getSmartSleepScore() != null) result.append("score ").append(session.getSmartSleepScore().score()).append("/100; ");
            append(result, session, "averageHeartRate", "average heart rate", session.getAverageHeartRate() + " BPM");
            append(result, session, "spo2", "SpO2", String.format(java.util.Locale.ROOT, "%.1f%%", session.getSpo2()));
            append(result, session, "skinTemperature", "skin temperature", String.format(java.util.Locale.ROOT, "%.1f C", session.getSkinTemperature()));
            if (session.isMetricAvailable("deltaPower") || session.isMetricAvailable("thetaPower")
                    || session.isMetricAvailable("alphaPower") || session.isMetricAvailable("betaPower")) {
                result.append("EEG relative bands are simulated estimates; ");
            }
            result.append('\n');
        }
        return result.toString();
    }

    private void append(StringBuilder result, SleepSession session, String metric, String label, String value) {
        if (session.isMetricAvailable(metric)) result.append(label).append(' ').append(value).append("; ");
    }

    private String journalContext(User user) {
        try {
            LocalDate through = LocalDate.now();
            List<SleepJournalEntry> entries = journal.list(user, through.minusDays(6), through);
            if (entries.isEmpty()) return "No recent journal entries.";
            return entries.stream().map(entry -> "- " + entry.date() + ": stress " + entry.stressLevel()
                    + "/10, caffeine " + entry.caffeineCups() + " cups, pre-bed screen "
                    + entry.screenTimeMinutes() + " min, exercised " + entry.exercised()
                    + (entry.moodBefore().isBlank() ? "" : ", mood before " + entry.moodBefore())
                    + (entry.moodAfter().isBlank() ? "" : ", mood after " + entry.moodAfter()))
                    .reduce((left, right) -> left + "\n" + right).orElse("No recent journal entries.");
        } catch (RuntimeException ignored) { return "Recent journal context is unavailable."; }
    }

    private String ruleFallback(String question, List<SleepSession> history, SleepSession latest) {
        if (history.isEmpty()) return "Save a sleep session first, and I can help you review it. I can also explain sleep stages and share general sleep-hygiene suggestions.";
        String normalized = question.toLowerCase(java.util.Locale.ROOT);
        if (history.size() > 1 && (normalized.contains("week") || normalized.contains("trend")
                || normalized.contains("compare") || normalized.contains("pattern"))) {
            List<SleepSession> lastSeven = history.stream().limit(7).toList();
            double avgMinutes = lastSeven.stream().filter(s -> s.isMetricAvailable("totalMinutes"))
                    .mapToInt(SleepSession::getTotalMinutes).average().orElse(0);
            double avgScore = lastSeven.stream().filter(s -> s.getSmartSleepScore() != null)
                    .mapToInt(s -> s.getSmartSleepScore().score()).average().orElse(0);
            double avgDeep = lastSeven.stream().filter(s -> s.isMetricAvailable("deepMinutes"))
                    .mapToInt(SleepSession::getDeepMinutes).average().orElse(0);
            return String.format(java.util.Locale.ROOT,
                    "Across your %d most recent saved sessions, average duration was %.1f hours, average Sleep Score was %.0f/100, and average deep sleep was %.0f minutes. These are descriptive summaries of %s data, not medical findings. For steadier patterns, consider keeping wake time similar and review the next few nights.",
                    lastSeven.size(), avgMinutes / 60, avgScore, avgDeep,
                    history.stream().allMatch(s -> "simulated".equalsIgnoreCase(s.getDataMode())) ? "simulated" : "saved");
        }
        return rules.reply(question, latest);
    }

    public record CoachReply(String answer, String source, String disclaimer) {}
}
