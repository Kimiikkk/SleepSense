package com.sleepsense.service;

import com.sleepsense.model.SleepJournalEntry;
import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SleepJournalServiceTest {
    @Test
    void validatesAndSavesJournalEntry() {
        FirebaseFeatureDataService data = mock(FirebaseFeatureDataService.class);
        SleepJournalService service = new SleepJournalService(data);
        User user = new User("A", "a@example.com", null, 20);
        SleepJournalEntry entry = new SleepJournalEntry(LocalDate.now(), "steady", "good", 4, 1,
                true, 30, "19:30", "Cool, dark room", "A short note", null);
        when(data.saveJournal(user, entry)).thenReturn(entry);

        assertSame(entry, service.save(user, entry));
        verify(data).saveJournal(user, entry);
    }

    @Test
    void rejectsOutOfRangeJournalInputsBeforeWriting() {
        FirebaseFeatureDataService data = mock(FirebaseFeatureDataService.class);
        SleepJournalService service = new SleepJournalService(data);
        SleepJournalEntry invalid = new SleepJournalEntry(LocalDate.now(), "steady", "good", 11, 1,
                false, 20, "20:00", "", "", null);

        assertThrows(IllegalArgumentException.class, () -> service.save(new User(), invalid));
        verifyNoInteractions(data);
    }

    @Test
    void correlationsWaitForFivePairedNightsAndDiscloseLimitations() {
        JournalCorrelationAnalyzer analyzer = new JournalCorrelationAnalyzer();
        List<SleepJournalEntry> entries = new ArrayList<>();
        List<SleepSession> sessions = new ArrayList<>();
        for (int day = 0; day < 5; day++) {
            LocalDate date = LocalDate.now().minusDays(day);
            entries.add(new SleepJournalEntry(date, "steady", "good", day + 1, day, false,
                    day * 10, "19:00", "", "", Instant.now()));
            SleepSession session = new SleepSession();
            session.setId("session-" + day);
            session.setStartTime(date.atTime(23, 0));
            session.setEndTime(date.atTime(7, 0));
            session.setSmartSleepScore(new com.sleepsense.model.SleepScoreBreakdown(80 - day, "Good", null, null, false, List.of()));
            session.setDataMode("simulated");
            sessions.add(session);
        }

        var result = analyzer.analyze(entries, sessions);

        assertEquals(true, result.get("ready"));
        assertEquals(5, result.get("sampleSize"));
        assertEquals(true, result.get("simulated"));
        assertTrue(result.get("disclaimer").toString().contains("does not show"));
        assertEquals(3, ((List<?>) result.get("correlations")).size());
    }
}
