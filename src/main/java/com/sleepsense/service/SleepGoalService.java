package com.sleepsense.service;

import com.sleepsense.model.SleepAchievement;
import com.sleepsense.model.SleepGoal;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import com.sleepsense.repository.SleepSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SleepGoalService {
    private final FirebaseFeatureDataService data;
    private final SleepSessionRepository sessions;

    public SleepGoalService(FirebaseFeatureDataService data, SleepSessionRepository sessions) {
        this.data = data;
        this.sessions = sessions;
    }

    public SleepGoal get(User user) { return data.getGoal(user); }

    public SleepGoal save(User user, SleepGoal goal) {
        validate(goal);
        SleepGoal saved = data.saveGoal(user, goal);
        evaluateAndAward(user, saved);
        return saved;
    }

    public Map<String, Object> progress(User user) {
        SleepGoal goal = get(user);
        evaluateAndAward(user, goal);
        List<SleepSession> all = sessions.findRecentByUserOrderByStartTimeDesc(user, 500);
        List<SleepSession> measured = all.stream().filter(this::measured).toList();
        LocalDate today = LocalDate.now();
        Set<LocalDate> completed = completedDates(measured, goal);
        List<Map<String, Object>> days = new ArrayList<>();
        int weekCount = 0;
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            boolean done = completed.contains(date);
            if (done) weekCount++;
            days.add(Map.of("date", date.toString(), "label", date.getDayOfWeek().toString().substring(0, 3), "complete", done));
        }
        List<Map<String, Object>> history = new ArrayList<>();
        for (int offset = 27; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            history.add(Map.of("date", date.toString(), "label", Integer.toString(date.getDayOfMonth()),
                    "complete", completed.contains(date)));
        }
        int streak = streak(completed, today);
        int targetMetDays = measured.stream().filter(s -> s.getEndTime() != null
                        && !s.getEndTime().toLocalDate().isBefore(today.minusDays(6))
                        && !s.getEndTime().toLocalDate().isAfter(today))
                .map(s -> s.getEndTime().toLocalDate()).distinct().toList().size();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goal", goal);
        result.put("days", days);
        result.put("history", history);
        result.put("measuredSessionCount", measured.size());
        result.put("simulatedSessionCount", all.size() - measured.size());
        result.put("weeklyConsistencyCount", weekCount);
        result.put("weeklyConsistencyTarget", goal.weeklyConsistencyNights());
        result.put("weeklyComplete", weekCount >= goal.weeklyConsistencyNights());
        result.put("streakDays", streak);
        result.put("recordedNightsThisWeek", targetMetDays);
        result.put("achievements", data.listAchievements(user));
        result.put("eligibleAchievementDefinitions", List.of(
                Map.of("code", "first-seven-hour", "title", "First 7-hour sleep", "description", "A measured session reaches your 7-hour milestone."),
                Map.of("code", "3-day-consistency", "title", "3-night consistency", "description", "Meet your sleep schedule for three consecutive nights."),
                Map.of("code", "7-day-consistency", "title", "7-night consistency", "description", "Meet your sleep schedule for seven consecutive nights."),
                Map.of("code", "weekly-goal", "title", "Weekly goal", "description", "Reach your weekly sleep consistency target."),
                Map.of("code", "30-day-tracking", "title", "30-night milestone", "description", "Record measured sleep on 30 distinct nights.")));
        result.put("achievementNote", "Measurement-based achievements only use measured sessions. Simulated nights remain visible as demo data and do not earn badges.");
        return result;
    }

    /** Call after a user-owned session is saved. Synthetic sessions never qualify for awards. */
    public void evaluateAndAward(User user, SleepGoal goal) {
        List<SleepSession> measured = sessions.findRecentByUserOrderByStartTimeDesc(user, 500).stream()
                .filter(this::measured).filter(s -> s.getEndTime() != null).toList();
        if (measured.isEmpty()) return;
        LocalDate today = LocalDate.now();
        Set<LocalDate> completeDates = completedDates(measured, goal);
        LocalDate firstSevenHourNight = measured.stream()
                .filter(s -> s.isMetricAvailable("totalMinutes") && s.getTotalMinutes() >= 420)
                .map(s -> s.getEndTime().toLocalDate()).min(LocalDate::compareTo).orElse(null);
        if (firstSevenHourNight != null) award(user, "first-seven-hour", "First 7-hour sleep",
                "A measured sleep session reached seven hours.", firstSevenHourNight);
        int streak = streak(completeDates, today);
        if (streak >= 3) award(user, "3-day-consistency", "3-night consistency",
                "Met your configured sleep schedule for three consecutive nights.", today);
        if (streak >= 7) award(user, "7-day-consistency", "7-night consistency",
                "Met your configured sleep schedule for seven consecutive nights.", today);
        long uniqueMeasuredNights = measured.stream().map(s -> s.getEndTime().toLocalDate()).distinct().count();
        if (uniqueMeasuredNights >= 30) award(user, "30-day-tracking", "30-night milestone",
                "Recorded measured sleep on at least thirty distinct nights.", today);
        long thisWeek = completeDates.stream().filter(date -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today)).count();
        if (thisWeek >= goal.weeklyConsistencyNights()) award(user, "weekly-goal", "Weekly goal",
                "Reached the configured weekly sleep consistency goal.", today);
    }

    private void award(User user, String code, String title, String description, LocalDate date) {
        data.awardAchievement(user, new SleepAchievement(code, title, description, date, Instant.now()));
    }

    private Set<LocalDate> completedDates(List<SleepSession> source, SleepGoal goal) {
        LocalTime bed = parseTime(goal.targetBedtime(), LocalTime.of(23, 0));
        LocalTime wake = parseTime(goal.targetWakeTime(), LocalTime.of(7, 0));
        Set<LocalDate> result = new LinkedHashSet<>();
        for (SleepSession session : source) {
            if (session.getEndTime() == null || !session.isMetricAvailable("totalMinutes")
                    || session.getTotalMinutes() < goal.targetSleepMinutes()
                    || session.getStartTime() == null
                    || clockDistance(session.getStartTime().toLocalTime(), bed) > 60
                    || clockDistance(session.getEndTime().toLocalTime(), wake) > 60) continue;
            result.add(session.getEndTime().toLocalDate());
        }
        return result;
    }

    private int streak(Set<LocalDate> dates, LocalDate today) {
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (dates.contains(cursor)) { count++; cursor = cursor.minusDays(1); }
        return count;
    }

    private boolean measured(SleepSession session) {
        return "measured".equalsIgnoreCase(session.getDataMode());
    }

    private int clockDistance(LocalTime left, LocalTime right) {
        int diff = Math.abs(left.toSecondOfDay() / 60 - right.toSecondOfDay() / 60);
        return Math.min(diff, 1440 - diff);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        try { return value == null ? fallback : LocalTime.parse(value); }
        catch (DateTimeParseException ignored) { return fallback; }
    }

    private void validate(SleepGoal goal) {
        if (goal == null || goal.targetSleepMinutes() < 180 || goal.targetSleepMinutes() > 900) {
            throw new IllegalArgumentException("Target sleep must be from 3 to 15 hours.");
        }
        if (goal.weeklyConsistencyNights() < 1 || goal.weeklyConsistencyNights() > 7) {
            throw new IllegalArgumentException("Weekly consistency must be from 1 to 7 nights.");
        }
        try {
            LocalTime.parse(goal.targetBedtime());
            LocalTime.parse(goal.targetWakeTime());
            if (goal.bedtimeReminderEnabled()) LocalTime.parse(goal.reminderTime());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Enter bedtime, wake-up, and reminder times as HH:mm.");
        }
    }
}
