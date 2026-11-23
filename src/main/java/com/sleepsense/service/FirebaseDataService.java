package com.sleepsense.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.SetOptions;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.SleepScoreBreakdown;
import com.sleepsense.model.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/** Firestore is the sole persistent store for SleepSense profiles and sleep sessions. */
@Service
public class FirebaseDataService {
    private final ObjectProvider<Firestore> firestoreProvider;

    public FirebaseDataService(ObjectProvider<Firestore> firestoreProvider) {
        this.firestoreProvider = firestoreProvider;
    }

    public User saveUser(User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId(user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()
                    ? UUID.randomUUID().toString() : user.getFirebaseUid());
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("uid", user.getFirebaseUid()); // Keep the original field for existing Google profiles.
        profile.put("firebaseUid", user.getFirebaseUid());
        profile.put("email", user.getEmail());
        profile.put("emailLowercase", normalizedEmail(user.getEmail()));
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("fullName", user.getFullName());
        profile.put("age", user.getAge());
        profile.put("passwordHash", user.getPasswordHash());
        profile.put("onboardingComplete", user.isOnboardingComplete());
        profile.put("authProvider", user.getAuthProvider() == null
                ? (user.getFirebaseUid() == null ? "password" : "google.com")
                : user.getAuthProvider());
        profile.put("updatedAt", Instant.now().toString());
        await(userDocument(user.getId()).set(profile, SetOptions.merge()), "Firestore could not save this account.");
        return user;
    }

    public Optional<User> findUserByEmail(String email) {
        String normalized = normalizedEmail(email);
        if (normalized == null || normalized.isBlank()) return Optional.empty();

        QuerySnapshot byIndex = await(firestore().collection("users")
                .whereEqualTo("emailLowercase", normalized).limit(1).get(),
                "Firestore could not look up this account.");
        if (!byIndex.isEmpty()) return Optional.of(toUser(byIndex.getDocuments().get(0)));

        // Older profiles predate emailLowercase; keep them accessible by their stored email.
        QuerySnapshot legacy = await(firestore().collection("users")
                .whereEqualTo("email", normalized).limit(1).get(),
                "Firestore could not look up this account.");
        return legacy.isEmpty() ? Optional.empty() : Optional.of(toUser(legacy.getDocuments().get(0)));
    }

    public Optional<User> findUserByFirebaseUid(String uid) {
        if (uid == null || uid.isBlank()) return Optional.empty();

        QuerySnapshot byField = await(firestore().collection("users")
                .whereEqualTo("firebaseUid", uid).limit(1).get(),
                "Firestore could not look up this Firebase account.");
        if (!byField.isEmpty()) return Optional.of(toUser(byField.getDocuments().get(0)));

        QuerySnapshot legacyField = await(firestore().collection("users")
                .whereEqualTo("uid", uid).limit(1).get(),
                "Firestore could not look up this Firebase account.");
        if (!legacyField.isEmpty()) return Optional.of(toUser(legacyField.getDocuments().get(0)));

        DocumentSnapshot legacyDocument = await(userDocument(uid).get(),
                "Firestore could not look up this Firebase account.");
        return legacyDocument.exists() ? Optional.of(toUser(legacyDocument)) : Optional.empty();
    }

    public Optional<User> findUserByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return Optional.empty();

        QuerySnapshot snapshot = await(firestore().collection("users")
                .whereEqualTo("phoneNumber", phoneNumber.trim()).limit(1).get(),
                "Firestore could not look up this phone account.");
        return snapshot.isEmpty() ? Optional.empty() : Optional.of(toUser(snapshot.getDocuments().get(0)));
    }

    public boolean userExistsByEmail(String email) {
        return findUserByEmail(email).isPresent();
    }

    public SleepSession saveSleepSession(User user, SleepSession session) {
        if (user.getId() == null || user.getId().isBlank()) {
            throw new IllegalStateException("The Firestore user profile must be saved before its sleep data.");
        }
        if (session.getId() == null || session.getId().isBlank()) {
            session.setId(UUID.randomUUID().toString());
        }
        session.setUser(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("userId", user.getId());
        data.put("startTime", session.getStartTime().toString());
        data.put("endTime", session.getEndTime().toString());
        data.put("totalMinutes", session.getTotalMinutes());
        data.put("awakeMinutes", session.getAwakeMinutes());
        data.put("lightMinutes", session.getLightMinutes());
        data.put("deepMinutes", session.getDeepMinutes());
        data.put("remMinutes", session.getRemMinutes());
        data.put("averageHeartRate", session.getAverageHeartRate());
        data.put("averageMovement", session.getAverageMovement());
        data.put("spo2", session.getSpo2());
        data.put("skinTemperature", session.getSkinTemperature());
        data.put("deltaPower", session.getDeltaPower());
        data.put("thetaPower", session.getThetaPower());
        data.put("alphaPower", session.getAlphaPower());
        data.put("betaPower", session.getBetaPower());
        data.put("qualityScore", session.getQualityScore());
        data.put("dataMode", session.getDataMode());
        data.put("availableMetrics", new ArrayList<>(session.getAvailableMetrics()));
        if (session.getSmartSleepScore() != null) {
            data.put("smartSleepScore", scoreData(session.getSmartSleepScore()));
        }
        data.put("updatedAt", Instant.now().toString());

        await(sleepSessions(user.getId()).document(session.getId()).set(data, SetOptions.merge()),
                "Firestore could not save this sleep session.");
        return session;
    }

    public List<SleepSession> findSleepSessions(User user) {
        if (user.getId() == null || user.getId().isBlank()) return List.of();
        QuerySnapshot snapshot = await(sleepSessions(user.getId()).get(),
                "Firestore could not load this user's sleep history.");
        List<SleepSession> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            result.add(toSleepSession(document, user));
        }
        result.sort(Comparator.comparing(SleepSession::getStartTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public List<SleepSession> findRecentSleepSessions(User user, int requestedLimit) {
        if (user.getId() == null || user.getId().isBlank()) return List.of();
        int limit = Math.max(1, Math.min(requestedLimit, 500));
        QuerySnapshot snapshot = await(sleepSessions(user.getId()).orderBy("endTime", Query.Direction.DESCENDING)
                .limit(limit).get(), "Firestore could not load this user's recent sleep history.");
        List<SleepSession> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) result.add(toSleepSession(document, user));
        result.sort(Comparator.comparing(SleepSession::getStartTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        // Old documents may use a different timestamp key; preserve their access if the indexed query is empty.
        return result.isEmpty() ? findSleepSessions(user) : result;
    }

    private User toUser(DocumentSnapshot document) {
        User user = new User();
        user.setId(value(document, "id", document.getId()));
        user.setFullName(value(document, "fullName", "SleepSense user"));
        user.setEmail(value(document, "email", ""));
        user.setEmailLowercase(value(document, "emailLowercase", normalizedEmail(user.getEmail())));
        user.setFirebaseUid(value(document, "firebaseUid", value(document, "uid", null)));
        user.setPhoneNumber(value(document, "phoneNumber", null));
        user.setAuthProvider(value(document, "authProvider",
                user.getFirebaseUid() == null ? "password" : "google.com"));
        user.setPasswordHash(value(document, "passwordHash", ""));
        Object age = document.get("age");
        user.setAge(age instanceof Number number ? number.intValue() : 0);
        Object onboardingComplete = document.get("onboardingComplete");
        // Profiles created before age onboarding existed count as established accounts.
        user.setOnboardingComplete(!(onboardingComplete instanceof Boolean complete) || complete);
        return user;
    }

    private SleepSession toSleepSession(DocumentSnapshot document, User user) {
        SleepSession session = new SleepSession();
        session.setId(value(document, "sessionId", document.getId()));
        session.setUser(user);
        session.setStartTime(localDateTime(value(document, "startTime", value(document, "startedAt", null))));
        session.setEndTime(localDateTime(value(document, "endTime", value(document, "endedAt", null))));
        session.setTotalMinutes(number(document, "totalMinutes").intValue());
        session.setAwakeMinutes(number(document, "awakeMinutes").intValue());
        session.setLightMinutes(number(document, "lightMinutes").intValue());
        session.setDeepMinutes(number(document, "deepMinutes").intValue());
        session.setRemMinutes(number(document, "remMinutes").intValue());
        session.setAverageHeartRate(number(document, "averageHeartRate").intValue());
        session.setAverageMovement(number(document, "averageMovement").doubleValue());
        session.setSpo2(number(document, "spo2").doubleValue());
        session.setSkinTemperature(number(document, "skinTemperature").doubleValue());
        session.setDeltaPower(number(document, "deltaPower").doubleValue());
        session.setThetaPower(number(document, "thetaPower").doubleValue());
        session.setAlphaPower(number(document, "alphaPower").doubleValue());
        session.setBetaPower(number(document, "betaPower").doubleValue());
        session.setQualityScore(number(document, "qualityScore").intValue());
        session.setDataMode(value(document, "dataMode", "simulated"));
        session.setAvailableMetrics(new java.util.LinkedHashSet<>(availableMetrics(document)));
        Object score = document.get("smartSleepScore");
        if (score instanceof Map<?, ?> values) session.setSmartSleepScore(scoreFromData(values));
        return session;
    }

    private List<String> availableMetrics(DocumentSnapshot document) {
        Object stored = document.get("availableMetrics");
        if (stored instanceof List<?> values) {
            return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        // Compatibility for existing sessions: a field is available only when it was actually stored.
        return List.of("totalMinutes", "awakeMinutes", "lightMinutes", "deepMinutes", "remMinutes",
                        "averageHeartRate", "averageMovement", "spo2", "skinTemperature", "deltaPower",
                        "thetaPower", "alphaPower", "betaPower", "qualityScore")
                .stream().filter(field -> document.get(field) instanceof Number).toList();
    }

    private Map<String, Object> scoreData(SleepScoreBreakdown score) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("score", score.score());
        data.put("category", score.category());
        data.put("previousScore", score.previousScore());
        data.put("difference", score.difference());
        data.put("provisional", score.provisional());
        data.put("factors", score.factors().stream().map(factor -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("metric", factor.metric());
            value.put("label", factor.label());
            value.put("points", factor.points());
            value.put("weight", factor.weight());
            value.put("direction", factor.direction());
            value.put("explanation", factor.explanation());
            return value;
        }).toList());
        return data;
    }

    private SleepScoreBreakdown scoreFromData(Map<?, ?> data) {
        int score = integer(data.get("score"), 0);
        Integer previous = nullableInteger(data.get("previousScore"));
        Integer difference = nullableInteger(data.get("difference"));
        List<SleepScoreBreakdown.Factor> factors = new ArrayList<>();
        Object rawFactors = data.get("factors");
        if (rawFactors instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> factor) {
                    factors.add(new SleepScoreBreakdown.Factor(
                            string(factor.get("metric")), string(factor.get("label")),
                            integer(factor.get("points"), 0), integer(factor.get("weight"), 0),
                            string(factor.get("direction")), string(factor.get("explanation"))));
                }
            }
        }
        return new SleepScoreBreakdown(score, string(data.get("category")), previous, difference,
                Boolean.TRUE.equals(data.get("provisional")), factors);
    }

    private Integer nullableInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String string(Object value) { return value instanceof String text ? text : ""; }

    private Number number(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value instanceof Number number ? number : 0;
    }

    private String value(DocumentSnapshot document, String field, String fallback) {
        Object value = document.get(field);
        return value instanceof String string ? string : fallback;
    }

    private LocalDateTime localDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private String normalizedEmail(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT).trim();
    }

    private DocumentReference userDocument(String id) {
        return firestore().collection("users").document(id);
    }

    private CollectionReference sleepSessions(String userId) {
        return userDocument(userId).collection("sleepSessions");
    }

    private Firestore firestore() {
        Firestore firestore = firestoreProvider.getIfAvailable();
        if (firestore == null) {
            throw new IllegalStateException("Firebase Firestore is not configured. Set FIREBASE_ENABLED=true and configure Firebase credentials.");
        }
        return firestore;
    }

    private <T> T await(ApiFuture<T> future, String failureMessage) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failureMessage, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(failureMessage, e.getCause());
        }
    }
}
