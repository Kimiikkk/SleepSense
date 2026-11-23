package com.sleepsense.model;

import java.time.Instant;
import java.time.LocalDate;

public record SleepJournalEntry(
        LocalDate date,
        String moodBefore,
        String moodAfter,
        int stressLevel,
        int caffeineCups,
        boolean exercised,
        int screenTimeMinutes,
        String eveningMealTime,
        String sleepEnvironment,
        String notes,
        Instant updatedAt) {}
