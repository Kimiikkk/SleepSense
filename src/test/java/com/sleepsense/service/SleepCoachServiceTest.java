package com.sleepsense.service;

import com.google.firebase.auth.FirebaseAuth;
import com.sleepsense.model.SleepScoreBreakdown;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SleepCoachServiceTest {
    @Test
    void ruleBasedFallbackSummarizesRecentHistoryWhenProviderIsNotConfigured() {
        SleepAnalyticsService analytics = mock(SleepAnalyticsService.class);
        SleepJournalService journal = mock(SleepJournalService.class);
        ObjectProvider<SleepCoachProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        when(analytics.recent(any(), eq(14))).thenReturn(List.of(session("a", 82), session("b", 76)));

        var result = new SleepCoachService(analytics, journal, mock(AIChatService.class), provider)
                .ask(new User("Test", "test@example.com", null, 20), "Compare this week");

        assertTrue(result.answer().contains("2 most recent saved sessions"));
        assertTrue(result.answer().contains("simulated data"));
        assertEquals("SleepSense guide", result.source());
        verifyNoInteractions(journal);
    }

    @Test
    void externalProviderErrorsFallBackToExistingRuleService() {
        SleepAnalyticsService analytics = mock(SleepAnalyticsService.class);
        SleepJournalService journal = mock(SleepJournalService.class);
        ObjectProvider<SleepCoachProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn((instruction, input) -> { throw new IllegalStateException("offline"); });
        when(analytics.recent(any(), eq(14))).thenReturn(List.of(session("a", 82)));
        AIChatService rules = mock(AIChatService.class);
        when(rules.reply(eq("What is REM?"), any(SleepSession.class))).thenReturn("REM is a sleep stage.");

        var result = new SleepCoachService(analytics, journal, rules, provider)
                .ask(new User("Test", "test@example.com", null, 20), "What is REM?");

        assertEquals("REM is a sleep stage.", result.answer());
        assertTrue(result.source().contains("local fallback"));
    }

    @Test
    void rejectsOversizedQuestionsBeforeReadingHistory() {
        SleepAnalyticsService analytics = mock(SleepAnalyticsService.class);
        SleepCoachService service = new SleepCoachService(analytics, mock(SleepJournalService.class),
                mock(AIChatService.class), mock(ObjectProvider.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.ask(new User(), "x".repeat(501)));
        verifyNoInteractions(analytics);
    }

    private SleepSession session(String id, int score) {
        SleepSession session = new SleepSession();
        session.setId(id);
        session.setStartTime(LocalDateTime.now().minusDays(1).withHour(23));
        session.setEndTime(LocalDateTime.now().withHour(7));
        session.setTotalMinutes(470);
        session.setDeepMinutes(80);
        session.setRemMinutes(95);
        session.setAwakeMinutes(25);
        session.setAverageHeartRate(58);
        session.setAvailableMetrics(java.util.Set.of("totalMinutes", "deepMinutes", "remMinutes", "awakeMinutes", "averageHeartRate"));
        session.setSmartSleepScore(new SleepScoreBreakdown(score, "Good", null, null, false, List.of()));
        session.setDataMode("simulated");
        return session;
    }
}
