package com.sleepsense.model;

import java.time.Instant;

public record SleepGoal(
        int targetSleepMinutes,
        String targetBedtime,
        String targetWakeTime,
        int weeklyConsistencyNights,
        boolean bedtimeReminderEnabled,
        String reminderTime,
        Instant updatedAt) {

    public static SleepGoal defaults() {
        return new SleepGoal(480, "23:00", "07:00", 5, false, "22:30", null);
    }
}
