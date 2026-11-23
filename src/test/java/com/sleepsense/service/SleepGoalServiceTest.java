package com.sleepsense.service;

import com.sleepsense.model.SleepGoal;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import com.sleepsense.repository.SleepSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SleepGoalServiceTest {
    @Test
    void simulatedSessionsNeverCountTowardAchievementsOrStreaks() {
        FirebaseFeatureDataService data = mock(FirebaseFeatureDataService.class);
        SleepSessionRepository repository = mock(SleepSessionRepository.class);
        User user = new User("Test", "test@example.com", null, 20);
        SleepSession simulated = session(LocalDate.now(), "simulated");
        when(data.getGoal(user)).thenReturn(SleepGoal.defaults());
        when(data.listAchievements(user)).thenReturn(List.of());
        when(repository.findRecentByUserOrderByStartTimeDesc(user, 500)).thenReturn(List.of(simulated));

        var result = new SleepGoalService(data, repository).progress(user);

        assertEquals(0, result.get("measuredSessionCount"));
        assertEquals(1, result.get("simulatedSessionCount"));
        assertEquals(0, result.get("streakDays"));
        assertEquals(28, ((List<?>) result.get("history")).size());
        assertFalse((Boolean) ((java.util.Map<?, ?>) ((List<?>) result.get("history")).get(27)).get("complete"));
        verify(data, never()).awardAchievement(any(), any());
    }

    @Test
    void savesAndAwardsAConsistentMeasuredStreakIdempotently() {
        FirebaseFeatureDataService data = mock(FirebaseFeatureDataService.class);
        SleepSessionRepository repository = mock(SleepSessionRepository.class);
        User user = new User("Test", "test@example.com", null, 20);
        SleepGoal goal = new SleepGoal(420, "23:00", "07:00", 3, false, "22:30", null);
        when(data.saveGoal(user, goal)).thenReturn(goal);
        when(repository.findRecentByUserOrderByStartTimeDesc(user, 500)).thenReturn(List.of(
                session(LocalDate.now(), "measured"), session(LocalDate.now().minusDays(1), "measured"),
                session(LocalDate.now().minusDays(2), "measured")));
        when(data.awardAchievement(any(), any())).thenReturn(true);

        new SleepGoalService(data, repository).save(user, goal);

        verify(data).awardAchievement(eq(user), argThat(award -> award.code().equals("3-day-consistency")));
        verify(data).awardAchievement(eq(user), argThat(award -> award.code().equals("weekly-goal")));
        verify(data, never()).awardAchievement(eq(user), argThat(award -> award.code().equals("7-day-consistency")));
    }

    private SleepSession session(LocalDate endDate, String mode) {
        SleepSession session = new SleepSession();
        session.setId(mode + endDate);
        session.setStartTime(endDate.minusDays(1).atTime(23, 0));
        session.setEndTime(endDate.atTime(7, 0));
        session.setTotalMinutes(480);
        session.setAvailableMetrics(java.util.Set.of("totalMinutes"));
        session.setDataMode(mode);
        return session;
    }
}
