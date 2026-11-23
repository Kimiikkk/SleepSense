package com.sleepsense.service;

import com.sleepsense.model.SleepScoreBreakdown;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SleepScoreCalculatorTest {
    private final SleepScoreCalculator calculator = new SleepScoreCalculator();

    @Test
    void calculatesBoundedScoreAndExplainsEveryAvailableFactor() {
        SleepSession previous = session(LocalDateTime.of(2026, 11, 20, 23, 0), 455, 30, 76, 95, 59);
        SleepSession current = session(LocalDateTime.of(2026, 11, 21, 23, 0), 480, 20, 86, 106, 56);

        SleepScoreBreakdown result = calculator.calculate(current, List.of(previous));

        assertTrue(result.score() >= 0 && result.score() <= 100);
        assertNotNull(result.previousScore());
        assertNotNull(result.difference());
        assertTrue(result.factors().stream().anyMatch(factor -> factor.metric().equals("duration")));
        assertTrue(result.factors().stream().anyMatch(factor -> factor.metric().equals("efficiency")));
        assertTrue(result.factors().stream().allMatch(factor -> factor.explanation() != null
                && !factor.explanation().isBlank()));
    }

    @Test
    void missingCoreMetricIsOmittedAndPreventsPerfectScore() {
        SleepSession session = session(LocalDateTime.of(2026, 11, 21, 23, 0), 480, 20, 86, 106, 56);
        session.setMetricAvailable("deepMinutes", false);

        SleepScoreBreakdown result = calculator.calculate(session, List.of());

        assertTrue(result.provisional());
        assertTrue(result.score() <= 84);
        assertFalse(result.factors().stream().anyMatch(factor -> factor.metric().equals("deep")));
    }

    @Test
    void invalidStagesAreNotScoredAsZero() {
        SleepSession session = session(LocalDateTime.of(2026, 11, 21, 23, 0), 480, 20, 520, 106, 56);

        SleepScoreBreakdown result = calculator.calculate(session, List.of());

        assertTrue(result.provisional());
        assertTrue(result.score() <= 84);
        assertFalse(result.factors().stream().anyMatch(factor -> factor.metric().equals("deep")));
    }

    @Test
    void scheduleConsistencyIsReportedAsUnavailableUntilHistoryExists() {
        SleepSession session = session(LocalDateTime.of(2026, 11, 21, 23, 0), 480, 20, 86, 106, 56);

        SleepScoreBreakdown result = calculator.calculate(session, List.of());

        assertTrue(result.provisional());
        assertTrue(result.score() <= 92);
        assertFalse(result.factors().stream().anyMatch(factor -> factor.metric().equals("consistency")));
    }

    private SleepSession session(LocalDateTime end, int total, int awake, int deep, int rem, int heartRate) {
        return new SleepSession(new User("Test User", "test@example.com", null, 30),
                end.minusMinutes(total), end, total, awake, total - awake - deep - rem,
                deep, rem, heartRate, .15, 97.2, 36.4, .65, .22, .08, .05, 80);
    }
}
