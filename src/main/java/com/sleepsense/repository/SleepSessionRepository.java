package com.sleepsense.repository;

import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import com.sleepsense.service.FirebaseDataService;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Firestore-backed sleep-session store. */
@Repository
public class SleepSessionRepository {
    private final FirebaseDataService data;

    public SleepSessionRepository(FirebaseDataService data) {
        this.data = data;
    }

    public SleepSession save(SleepSession session) {
        if (session.getUser() == null) {
            throw new IllegalArgumentException("A sleep session must belong to a user.");
        }
        return data.saveSleepSession(session.getUser(), session);
    }

    public List<SleepSession> findByUserOrderByStartTimeDesc(User user) {
        return data.findSleepSessions(user);
    }

    public List<SleepSession> findRecentByUserOrderByStartTimeDesc(User user, int limit) {
        return data.findRecentSleepSessions(user, limit);
    }

    public Optional<SleepSession> findFirstByUserOrderByIdDesc(User user) {
        return data.findSleepSessions(user).stream().findFirst();
    }
}
