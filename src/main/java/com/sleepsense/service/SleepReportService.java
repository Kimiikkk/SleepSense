package com.sleepsense.service;

import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Creates a compact, server-generated PDF from the authenticated user's stored sessions. */
@Service
public class SleepReportService {
    private static final Color INK = new Color(20, 29, 43);
    private static final Color MUTED = new Color(105, 118, 135);
    private static final Color CYAN = new Color(31, 154, 170);
    private static final Color ROSE = new Color(191, 88, 166);
    private final SleepAnalyticsService analytics;
    private final AIChatService rules;

    public SleepReportService(SleepAnalyticsService analytics, AIChatService rules) {
        this.analytics = analytics;
        this.rules = rules;
    }

    public byte[] create(User user, String period) throws IOException {
        int days = switch (period == null ? "" : period.toLowerCase(java.util.Locale.ROOT)) {
            case "daily" -> 1;
            case "weekly" -> 7;
            case "monthly" -> 30;
            default -> throw new IllegalArgumentException("Choose daily, weekly, or monthly.");
        };
        Map<String, Object> report = analytics.analytics(user, days);
        List<SleepSession> history = analytics.recent(user, 500);
        List<SleepSession> inPeriod = history.stream().filter(s -> s.getEndTime() != null
                && !s.getEndTime().toLocalDate().isBefore(LocalDateTime.now().toLocalDate().minusDays(days - 1L))
                && !s.getEndTime().isAfter(LocalDateTime.now())).sorted(Comparator.comparing(SleepSession::getEndTime)).toList();
        Map<?, ?> metrics = (Map<?, ?>) report.get("metrics");
        Map<?, ?> stages = (Map<?, ?>) report.get("stageDistribution");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream canvas = new PDPageContentStream(document, page)) {
                text(canvas, 42, 795, 21, true, "SleepSense", CYAN);
                text(canvas, 42, 766, 16, true, "Sleep report", INK);
                text(canvas, 42, 747, 9, false, periodLabel(period) + " · generated "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")), MUTED);
                line(canvas, 42, 733, 553, 733, new Color(215, 222, 230), 0.8f);

                text(canvas, 42, 712, 11, true, safe(user.getFullName()), INK);
                text(canvas, 42, 696, 9, false, user.getAge() > 0 ? "Age " + user.getAge() : "Age not provided", MUTED);
                text(canvas, 42, 668, 10, true, "Period summary", INK);
                metric(canvas, 42, 638, "Average sleep", duration(metrics.get("avgSleepDurationMinutes")));
                metric(canvas, 228, 638, "Smart Sleep Score", number(metrics.get("avgSleepScore"), "/100"));
                metric(canvas, 414, 638, "Average deep sleep", duration(metrics.get("avgDeepMinutes")));
                metric(canvas, 42, 590, "Average REM sleep", duration(metrics.get("avgRemMinutes")));
                metric(canvas, 228, 590, "Awake time", number(metrics.get("avgAwakeMinutes"), " min"));
                metric(canvas, 414, 590, "Average heart rate", number(metrics.get("avgHeartRate"), " BPM"));

                metric(canvas, 42, 542, "Average SpO2", number(metrics.get("avgSpo2"), "%"));
                metric(canvas, 228, 542, "Saved sessions", Integer.toString(inPeriod.size()));
                metric(canvas, 414, 542, "Sleep consistency", number(metrics.get("consistencyPercentage"), "%"));

                text(canvas, 42, 502, 10, true, "Sleep score trend · per saved night", INK);
                drawScoreChart(canvas, inPeriod, 42, 374, 510, 108);
                text(canvas, 42, 349, 10, true, "Sleep-stage distribution · estimated", INK);
                stageBar(canvas, 42, 326, "Awake", stages.get("awake"), new Color(115, 125, 139));
                stageBar(canvas, 42, 304, "Light", stages.get("light"), new Color(141, 169, 199));
                stageBar(canvas, 42, 282, "Deep", stages.get("deep"), CYAN);
                stageBar(canvas, 42, 260, "REM", stages.get("rem"), ROSE);

                text(canvas, 42, 228, 10, true, "Personalized suggestion", INK);
                SleepSession latest = inPeriod.isEmpty() ? null : inPeriod.get(inPeriod.size() - 1);
                String advice = latest == null
                        ? "Save a sleep session to receive suggestions based on your sleep history."
                        : rules.reply("How can I improve my sleep?", latest);
                paragraph(canvas, 42, 209, 9, advice, 520, 13, MUTED, 5);

                line(canvas, 42, 92, 553, 92, new Color(215, 222, 230), 0.8f);
                text(canvas, 42, 75, 8, false, "SleepSense software prototype · readings may be simulated.", MUTED);
                text(canvas, 42, 61, 8, false, "General wellness information only. This report is not a medical diagnosis.", MUTED);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void metric(PDPageContentStream canvas, float x, float y, String title, String value) throws IOException {
        text(canvas, x, y, 8, false, title, MUTED);
        text(canvas, x, y - 18, 14, true, value, INK);
    }

    private void drawScoreChart(PDPageContentStream canvas, List<SleepSession> nights,
                                float x, float y, float width, float height) throws IOException {
        line(canvas, x, y, x + width, y, new Color(220, 226, 233), .6f);
        line(canvas, x, y + height, x + width, y + height, new Color(220, 226, 233), .6f);
        text(canvas, x, y - 14, 7, false, "0", MUTED);
        text(canvas, x, y + height + 5, 7, false, "100", MUTED);
        List<SleepSession> scored = nights.stream().filter(s -> s.getSmartSleepScore() != null).toList();
        if (scored.isEmpty()) {
            text(canvas, x + 6, y + height / 2, 9, false, "No scored sessions in this period.", MUTED);
            return;
        }
        canvas.setStrokingColor(ROSE);
        canvas.setLineWidth(1.7f);
        for (int index = 0; index < scored.size(); index++) {
            float pointX = x + (scored.size() == 1 ? width / 2 : (float) index / (scored.size() - 1) * width);
            float pointY = y + (float) scored.get(index).getSmartSleepScore().score() / 100 * height;
            if (index == 0) canvas.moveTo(pointX, pointY); else canvas.lineTo(pointX, pointY);
        }
        canvas.stroke();
    }

    private void stageBar(PDPageContentStream canvas, float x, float y, String label,
                          Object value, Color color) throws IOException {
        text(canvas, x, y, 8, false, label, MUTED);
        if (!(value instanceof Number number)) {
            text(canvas, x + 70, y, 8, false, "Not available", MUTED);
            return;
        }
        double percent = number.doubleValue();
        float maxWidth = 390, barWidth = (float) Math.max(0, Math.min(maxWidth, percent / 100 * maxWidth));
        canvas.setNonStrokingColor(new Color(235, 239, 243)); canvas.addRect(x + 70, y - 2, maxWidth, 8); canvas.fill();
        canvas.setNonStrokingColor(color); canvas.addRect(x + 70, y - 2, barWidth, 8); canvas.fill();
        text(canvas, x + 470, y, 8, false, String.format(java.util.Locale.ROOT, "%.1f%%", percent), MUTED);
    }

    private void paragraph(PDPageContentStream canvas, float x, float y, float size, String value,
                           float maxWidth, float lineHeight, Color color, int maximumLines) throws IOException {
        String[] words = safe(value).split("\\s+");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(candidate) / 100 * size > maxWidth) {
                text(canvas, x, y - lines * lineHeight, size, false, line.toString(), color);
                if (++lines >= maximumLines) return;
                line = new StringBuilder(word);
            } else line = new StringBuilder(candidate);
        }
        if (!line.isEmpty() && lines < maximumLines) text(canvas, x, y - lines * lineHeight, size, false, line.toString(), color);
    }

    private void text(PDPageContentStream canvas, float x, float y, float size, boolean bold,
                      String value, Color color) throws IOException {
        canvas.beginText();
        canvas.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), size);
        canvas.setNonStrokingColor(color);
        canvas.newLineAtOffset(x, y);
        canvas.showText(safe(value));
        canvas.endText();
    }

    private void line(PDPageContentStream canvas, float x1, float y1, float x2, float y2,
                      Color color, float width) throws IOException {
        canvas.setStrokingColor(color); canvas.setLineWidth(width);
        canvas.moveTo(x1, y1); canvas.lineTo(x2, y2); canvas.stroke();
    }

    private String duration(Object value) {
        if (!(value instanceof Number number)) return "Not available";
        int minutes = (int) Math.round(number.doubleValue());
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
    private String number(Object value, String suffix) {
        return value instanceof Number number ? String.format(java.util.Locale.ROOT, "%.1f%s", number.doubleValue(), suffix) : "Not available";
    }
    private String periodLabel(String period) {
        return switch (period.toLowerCase(java.util.Locale.ROOT)) {
            case "daily" -> "Daily report";
            case "weekly" -> "Weekly report · last 7 days";
            default -> "Monthly report · last 30 days";
        };
    }
    private String safe(String value) {
        return value == null ? "Not provided" : value.replaceAll("[^\\x20-\\x7E]", " ").replaceAll("\\s+", " ").trim();
    }
}
