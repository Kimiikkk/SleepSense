package com.sleepsense.controller;

import com.sleepsense.model.SleepGoal;
import com.sleepsense.model.SleepJournalEntry;
import com.sleepsense.service.AuthenticatedUserResolver;
import com.sleepsense.service.JournalCorrelationAnalyzer;
import com.sleepsense.service.SleepAnalyticsService;
import com.sleepsense.service.SleepGoalService;
import com.sleepsense.service.SleepJournalService;
import com.sleepsense.service.SleepCoachService;
import com.sleepsense.service.SleepReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SleepFeatureController {
    private final AuthenticatedUserResolver userResolver;
    private final SleepJournalService journal;
    private final SleepGoalService goals;
    private final SleepAnalyticsService analytics;
    private final JournalCorrelationAnalyzer correlations;
    private final SleepCoachService coach;
    private final SleepReportService reports;

    public SleepFeatureController(AuthenticatedUserResolver userResolver, SleepJournalService journal,
                                  SleepGoalService goals, SleepAnalyticsService analytics,
                                  JournalCorrelationAnalyzer correlations, SleepCoachService coach,
                                  SleepReportService reports) {
        this.userResolver = userResolver;
        this.journal = journal;
        this.goals = goals;
        this.analytics = analytics;
        this.correlations = correlations;
        this.coach = coach;
        this.reports = reports;
    }

    @GetMapping("/api/journal")
    public Map<String, Object> journal(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate through,
                                       Authentication authentication) {
        LocalDate end = through == null ? LocalDate.now() : through;
        LocalDate start = from == null ? end.minusDays(29) : from;
        return Map.of("entries", journal.list(userResolver.require(authentication), start, end),
                "from", start.toString(), "through", end.toString());
    }

    @PutMapping("/api/journal")
    public Map<String, Object> saveJournal(@RequestBody JournalRequest request, Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("Enter journal details before saving.");
        SleepJournalEntry entry = new SleepJournalEntry(request.date(), clean(request.moodBefore()), clean(request.moodAfter()),
                number(request.stressLevel()), number(request.caffeineCups()), Boolean.TRUE.equals(request.exercised()),
                number(request.screenTimeMinutes()), clean(request.eveningMealTime()), clean(request.sleepEnvironment()),
                clean(request.notes()), null);
        return Map.of("entry", journal.save(userResolver.require(authentication), entry), "message", "Journal entry saved.");
    }

    @DeleteMapping("/api/journal/{date}")
    public ResponseEntity<Map<String, String>> deleteJournal(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                             Authentication authentication) {
        boolean deleted = journal.delete(userResolver.require(authentication), date);
        return deleted ? ResponseEntity.ok(Map.of("message", "Journal entry deleted."))
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/api/journal/correlations")
    public Map<String, Object> correlations(@RequestParam(defaultValue = "30") int days, Authentication authentication) {
        if (days != 7 && days != 30 && days != 90) throw new IllegalArgumentException("Choose 7, 30, or 90 days.");
        var user = userResolver.require(authentication);
        LocalDate through = LocalDate.now();
        LocalDate from = through.minusDays(days - 1L);
        var entries = journal.list(user, from, through);
        var history = analytics.recent(user, 500).stream()
                .filter(session -> session.getEndTime() != null
                        && !session.getEndTime().toLocalDate().isBefore(from)
                        && !session.getEndTime().toLocalDate().isAfter(through)).toList();
        return correlations.analyze(entries, history);
    }

    @GetMapping("/api/goals")
    public Map<String, Object> goalProgress(Authentication authentication) {
        return goals.progress(userResolver.require(authentication));
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/chat")
    public SleepCoachService.CoachReply chat(@RequestBody CoachRequest request, Authentication authentication) {
        return coach.ask(userResolver.require(authentication), request == null ? null : request.question());
    }

    @GetMapping(value = "/api/reports/{period}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> report(@PathVariable String period, Authentication authentication) {
        try {
            byte[] pdf = reports.create(userResolver.require(authentication), period);
            String filename = "sleepsense-" + period.toLowerCase(java.util.Locale.ROOT) + "-report.pdf";
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdf);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The sleep report could not be generated.");
        }
    }

    @PutMapping("/api/goals")
    public Map<String, Object> saveGoal(@RequestBody GoalRequest request, Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("Enter your sleep goal first.");
        SleepGoal goal = new SleepGoal(number(request.targetSleepMinutes()), clean(request.targetBedtime()),
                clean(request.targetWakeTime()), number(request.weeklyConsistencyNights()),
                Boolean.TRUE.equals(request.bedtimeReminderEnabled()), clean(request.reminderTime()), null);
        return Map.of("goal", goals.save(userResolver.require(authentication), goal), "message", "Sleep goal saved.");
    }

    private int number(Integer value) { return value == null ? 0 : value; }
    private String clean(String value) { return value == null ? "" : value.trim(); }

    public record JournalRequest(LocalDate date, String moodBefore, String moodAfter, Integer stressLevel,
                                 Integer caffeineCups, Boolean exercised, Integer screenTimeMinutes,
                                 String eveningMealTime, String sleepEnvironment, String notes) {}
    public record GoalRequest(Integer targetSleepMinutes, String targetBedtime, String targetWakeTime,
                              Integer weeklyConsistencyNights, Boolean bedtimeReminderEnabled, String reminderTime) {}
    public record CoachRequest(String question) {}
}

@RestControllerAdvice(assignableTypes = SleepFeatureController.class)
class SleepFeatureErrorHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
