package com.sleepsense.service;

import com.sleepsense.model.User;
import com.sleepsense.repository.SleepSessionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SleepReportServiceTest {
    @Test
    void createsARealDailyPdfWithBrandingAndPrototypeDisclosure() throws Exception {
        SleepSessionRepository repository = mock(SleepSessionRepository.class);
        when(repository.findRecentByUserOrderByStartTimeDesc(any(), anyInt())).thenReturn(List.of());
        SleepAnalyticsService analytics = new SleepAnalyticsService(repository, new SleepScoreCalculator());
        SleepReportService reports = new SleepReportService(analytics, mock(AIChatService.class));

        byte[] pdfBytes = reports.create(new User("Sleep Tester", "test@example.com", null, 22), "daily");

        assertTrue(new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
        try (var document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("SleepSense"));
            assertTrue(text.contains("Sleep report"));
            assertTrue(text.contains("software prototype"));
            assertTrue(text.contains("not a medical diagnosis"));
        }
    }

    @Test
    void rejectsUnknownReportPeriod() {
        SleepReportService reports = new SleepReportService(mock(SleepAnalyticsService.class), mock(AIChatService.class));
        assertThrows(IllegalArgumentException.class, () -> reports.create(new User(), "yearly"));
    }
}
