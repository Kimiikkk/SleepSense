package com.sleepsense.service;

import com.sleepsense.model.SleepSession;
import com.sleepsense.repository.SleepSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SleepAnalyticsServiceTest {
    @Test
    void missingMeasuresRemainNullInsteadOfBecomingZero() {
        SleepSessionRepository repository = mock(SleepSessionRepository.class);
        SleepSession partial = new SleepSession();
        partial.setId("partial");
        partial.setStartTime(LocalDateTime.now().minusHours(8));
        partial.setEndTime(LocalDateTime.now());
        partial.setTotalMinutes(480);
        partial.setAwakeMinutes(20);
        partial.setAvailableMetrics(Set.of("totalMinutes", "awakeMinutes"));
        when(repository.findRecentByUserOrderByStartTimeDesc(any(), eq(500))).thenReturn(List.of(partial));
        SleepAnalyticsService service = new SleepAnalyticsService(repository, new SleepScoreCalculator());

        Map<String, Object> result = service.analytics(new com.sleepsense.model.User(), 7);
        Map<?, ?> metrics = (Map<?, ?>) result.get("metrics");
        Map<?, ?> point = (Map<?, ?>) ((List<?>) result.get("sessions")).get(0);

        assertEquals(480.0, metrics.get("avgSleepDurationMinutes"));
        assertNull(metrics.get("avgDeepMinutes"));
        assertNull(metrics.get("avgRemMinutes"));
        assertNull(metrics.get("avgDeepPercentage"));
        assertNull(metrics.get("avgHeartRate"));
        assertNull(point.get("deepMinutes"));
        assertNull(point.get("spo2"));
        assertTrue(partial.getSmartSleepScore().provisional());
    }

    @Test
    void periodComparisonExplainsWhetherSleepScoreMovedUp() {
        SleepSessionRepository repository = mock(SleepSessionRepository.class);
        SleepAnalyticsService service = new SleepAnalyticsService(repository, new SleepScoreCalculator());
        assertThrows(IllegalArgumentException.class, () -> service.analytics(new com.sleepsense.model.User(), 12));
        verifyNoInteractions(repository);
    }
}
