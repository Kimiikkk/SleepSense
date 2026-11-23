package com.sleepsense.service;

import com.sleepsense.model.SleepJournalEntry;
import com.sleepsense.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class SleepJournalService {
    private final FirebaseFeatureDataService data;

    public SleepJournalService(FirebaseFeatureDataService data) { this.data = data; }

    public List<SleepJournalEntry> list(User user, LocalDate from, LocalDate through) {
        LocalDate today = LocalDate.now();
        LocalDate end = through == null ? today : through;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (end.isAfter(today) || start.isAfter(end) || start.isBefore(today.minusYears(2))) {
            throw new IllegalArgumentException("Choose a date range within the last two years.");
        }
        return data.listJournal(user, start, end);
    }

    public SleepJournalEntry save(User user, SleepJournalEntry entry) {
        validate(entry);
        return data.saveJournal(user, entry);
    }

    public boolean delete(User user, LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.now().minusYears(2))) {
            throw new IllegalArgumentException("Choose a valid journal date.");
        }
        return data.deleteJournal(user, date);
    }

    private void validate(SleepJournalEntry entry) {
        if (entry == null || entry.date() == null || entry.date().isAfter(LocalDate.now())
                || entry.date().isBefore(LocalDate.now().minusYears(2))) {
            throw new IllegalArgumentException("Choose a journal date within the last two years.");
        }
        if (entry.stressLevel() < 1 || entry.stressLevel() > 10) throw new IllegalArgumentException("Stress must be from 1 to 10.");
        if (entry.caffeineCups() < 0 || entry.caffeineCups() > 20) throw new IllegalArgumentException("Caffeine must be from 0 to 20 cups.");
        if (entry.screenTimeMinutes() < 0 || entry.screenTimeMinutes() > 1440) throw new IllegalArgumentException("Screen time must be from 0 to 1440 minutes.");
        if (!mood(entry.moodBefore()) || !mood(entry.moodAfter())) throw new IllegalArgumentException("Choose a valid mood.");
        if (length(entry.sleepEnvironment()) > 160 || length(entry.notes()) > 2000) {
            throw new IllegalArgumentException("Environment must be at most 160 characters and notes at most 2,000.");
        }
        if (length(entry.eveningMealTime()) > 5) throw new IllegalArgumentException("Enter a meal time as HH:mm.");
        if (entry.eveningMealTime() != null && !entry.eveningMealTime().isBlank()) {
            try { LocalTime.parse(entry.eveningMealTime()); }
            catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter a meal time as HH:mm."); }
        }
    }

    private boolean mood(String value) {
        return value == null || value.isBlank() || List.of("very low", "low", "steady", "good", "great").contains(value);
    }
    private int length(String value) { return value == null ? 0 : value.length(); }
}
