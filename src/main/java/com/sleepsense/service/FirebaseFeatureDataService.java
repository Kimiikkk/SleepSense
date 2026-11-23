package com.sleepsense.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.sleepsense.model.SleepAchievement;
import com.sleepsense.model.SleepGoal;
import com.sleepsense.model.SleepJournalEntry;
import com.sleepsense.model.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Firestore operations for per-profile journal, goals, and achievements. */
@Service
public class FirebaseFeatureDataService {
    private final ObjectProvider<Firestore> firestoreProvider;

    public FirebaseFeatureDataService(ObjectProvider<Firestore> firestoreProvider) {
        this.firestoreProvider = firestoreProvider;
    }

    public List<SleepJournalEntry> listJournal(User user, LocalDate from, LocalDate through) {
        if (from == null || through == null || from.isAfter(through)) return List.of();
        QuerySnapshot snapshot = await(journal(user).whereGreaterThanOrEqualTo("date", from.toString())
                .whereLessThanOrEqualTo("date", through.toString()).orderBy("date")
                .limit(400).get(), "Firestore could not load this sleep journal.");
        List<SleepJournalEntry> entries = new ArrayList<>();
        snapshot.getDocuments().forEach(document -> entries.add(journalEntry(document)));
        entries.sort(Comparator.comparing(SleepJournalEntry::date).reversed());
        return entries;
    }

    public SleepJournalEntry saveJournal(User user, SleepJournalEntry entry) {
        SleepJournalEntry saved = new SleepJournalEntry(entry.date(), entry.moodBefore(), entry.moodAfter(),
                entry.stressLevel(), entry.caffeineCups(), entry.exercised(), entry.screenTimeMinutes(),
                entry.eveningMealTime(), entry.sleepEnvironment(), entry.notes(), Instant.now());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("firebaseUid", user.getFirebaseUid());
        data.put("date", saved.date().toString());
        data.put("moodBefore", saved.moodBefore());
        data.put("moodAfter", saved.moodAfter());
        data.put("stressLevel", saved.stressLevel());
        data.put("caffeineCups", saved.caffeineCups());
        data.put("exercised", saved.exercised());
        data.put("screenTimeMinutes", saved.screenTimeMinutes());
        data.put("eveningMealTime", saved.eveningMealTime());
        data.put("sleepEnvironment", saved.sleepEnvironment());
        data.put("notes", saved.notes());
        data.put("updatedAt", saved.updatedAt().toString());
        await(journal(user).document(saved.date().toString()).set(data), "Firestore could not save this journal entry.");
        return saved;
    }

    public boolean deleteJournal(User user, LocalDate date) {
        DocumentReference reference = journal(user).document(date.toString());
        DocumentSnapshot current = await(reference.get(), "Firestore could not find this journal entry.");
        if (!current.exists()) return false;
        await(reference.delete(), "Firestore could not delete this journal entry.");
        return true;
    }

    public SleepGoal getGoal(User user) {
        DocumentSnapshot document = await(userDocument(user).collection("sleepGoals").document("current").get(),
                "Firestore could not load your sleep goal.");
        if (!document.exists()) return SleepGoal.defaults();
        return new SleepGoal(integer(document, "targetSleepMinutes", 480),
                string(document, "targetBedtime", "23:00"), string(document, "targetWakeTime", "07:00"),
                integer(document, "weeklyConsistencyNights", 5), bool(document, "bedtimeReminderEnabled"),
                string(document, "reminderTime", "22:30"), instant(document, "updatedAt"));
    }

    public SleepGoal saveGoal(User user, SleepGoal goal) {
        SleepGoal saved = new SleepGoal(goal.targetSleepMinutes(), goal.targetBedtime(), goal.targetWakeTime(),
                goal.weeklyConsistencyNights(), goal.bedtimeReminderEnabled(), goal.reminderTime(), Instant.now());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("firebaseUid", user.getFirebaseUid());
        data.put("targetSleepMinutes", saved.targetSleepMinutes());
        data.put("targetBedtime", saved.targetBedtime());
        data.put("targetWakeTime", saved.targetWakeTime());
        data.put("weeklyConsistencyNights", saved.weeklyConsistencyNights());
        data.put("bedtimeReminderEnabled", saved.bedtimeReminderEnabled());
        data.put("reminderTime", saved.reminderTime());
        data.put("updatedAt", saved.updatedAt().toString());
        await(userDocument(user).collection("sleepGoals").document("current").set(data),
                "Firestore could not save your sleep goal.");
        return saved;
    }

    public List<SleepAchievement> listAchievements(User user) {
        QuerySnapshot snapshot = await(userDocument(user).collection("achievements").limit(100).get(),
                "Firestore could not load your achievements.");
        List<SleepAchievement> entries = new ArrayList<>();
        snapshot.getDocuments().forEach(document -> {
            String date = string(document, "earnedOn", "");
            entries.add(new SleepAchievement(string(document, "code", document.getId()),
                    string(document, "title", "Achievement"), string(document, "description", ""),
                    date.isBlank() ? null : LocalDate.parse(date), instant(document, "earnedAt")));
        });
        entries.sort(Comparator.comparing(SleepAchievement::earnedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

    /** A stable document ID and transaction make awards idempotent under concurrent requests. */
    public boolean awardAchievement(User user, SleepAchievement achievement) {
        DocumentReference reference = userDocument(user).collection("achievements").document(achievement.code());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("firebaseUid", user.getFirebaseUid());
        data.put("code", achievement.code());
        data.put("title", achievement.title());
        data.put("description", achievement.description());
        data.put("earnedOn", achievement.earnedOn().toString());
        data.put("earnedAt", Instant.now().toString());
        return await(firestore().runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(reference).get();
            if (existing.exists()) return false;
            transaction.create(reference, data);
            return true;
        }), "Firestore could not record this achievement.");
    }

    private SleepJournalEntry journalEntry(DocumentSnapshot document) {
        return new SleepJournalEntry(LocalDate.parse(string(document, "date", document.getId())),
                string(document, "moodBefore", ""), string(document, "moodAfter", ""),
                integer(document, "stressLevel", 0), integer(document, "caffeineCups", 0),
                bool(document, "exercised"), integer(document, "screenTimeMinutes", 0),
                string(document, "eveningMealTime", ""), string(document, "sleepEnvironment", ""),
                string(document, "notes", ""), instant(document, "updatedAt"));
    }

    private CollectionReference journal(User user) {
        return userDocument(user).collection("sleepJournal");
    }

    private DocumentReference userDocument(User user) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw new IllegalStateException("The signed-in profile has no Firestore document ID.");
        }
        return firestore().collection("users").document(user.getId());
    }

    private Firestore firestore() {
        Firestore firestore = firestoreProvider.getIfAvailable();
        if (firestore == null) throw new IllegalStateException("Firebase Firestore is not configured.");
        return firestore;
    }

    private String string(DocumentSnapshot document, String field, String fallback) {
        Object value = document.get(field);
        return value instanceof String text ? text : fallback;
    }
    private int integer(DocumentSnapshot document, String field, int fallback) {
        Object value = document.get(field);
        return value instanceof Number number ? number.intValue() : fallback;
    }
    private boolean bool(DocumentSnapshot document, String field) { return Boolean.TRUE.equals(document.get(field)); }
    private Instant instant(DocumentSnapshot document, String field) {
        String value = string(document, field, "");
        try { return value.isBlank() ? null : Instant.parse(value); }
        catch (RuntimeException ignored) { return null; }
    }
    private <T> T await(ApiFuture<T> future, String failure) {
        try { return future.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure, e); }
        catch (ExecutionException e) { throw new IllegalStateException(failure, e.getCause()); }
    }
}
