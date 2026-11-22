package com.sleepsense.controller;

import com.sleepsense.model.User;
import com.sleepsense.model.SleepSession;
import com.sleepsense.repository.UserRepository;
import com.sleepsense.repository.SleepSessionRepository;
import com.sleepsense.service.DummyDataService;
import com.sleepsense.service.AIChatService;
import com.sleepsense.service.FirebaseSignInService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageController {
    private static final Logger log = LoggerFactory.getLogger(PageController.class);
    private final UserRepository users;
    private final SleepSessionRepository sessions;
    private final DummyDataService dummy;
    private final AIChatService ai;
    private final PasswordEncoder encoder;
    private final FirebaseSignInService firebaseSignIn;

    @Value("${firebase.web.api-key:}")
    private String firebaseApiKey;
    @Value("${firebase.web.auth-domain:}")
    private String firebaseAuthDomain;
    @Value("${firebase.project-id:}")
    private String firebaseProjectId;
    @Value("${firebase.web.app-id:}")
    private String firebaseAppId;
    @Value("${firebase.enabled:false}")
    private boolean firebaseAdminEnabled;

    public PageController(UserRepository users, SleepSessionRepository sessions,
                          DummyDataService dummy, AIChatService ai, PasswordEncoder encoder,
                          FirebaseSignInService firebaseSignIn) {
        this.users = users;
        this.sessions = sessions;
        this.dummy = dummy;
        this.ai = ai;
        this.encoder = encoder;
        this.firebaseSignIn = firebaseSignIn;
    }

    @GetMapping("/")
    public String home() { return "redirect:/login"; }

    @GetMapping("/login")
    public String login(Model model) {
        boolean firebaseReady = firebaseAdminEnabled && hasValue(firebaseApiKey) && hasValue(firebaseAuthDomain)
                && hasValue(firebaseProjectId) && hasValue(firebaseAppId);
        model.addAttribute("firebaseEnabled", firebaseReady);
        model.addAttribute("firebaseApiKey", firebaseApiKey);
        model.addAttribute("firebaseAuthDomain", firebaseAuthDomain);
        model.addAttribute("firebaseProjectId", firebaseProjectId);
        model.addAttribute("firebaseAppId", firebaseAppId);
        return "login";
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank() && !value.startsWith("replace-");
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("error", "");
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String fullName,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam int age,
                          Model model) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 255) {
            model.addAttribute("error", "Enter a name between 1 and 255 characters.");
            return "signup";
        }
        if (email == null || email.isBlank() || email.trim().length() > 254
                || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            model.addAttribute("error", "Enter a valid email address.");
            return "signup";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "signup";
        }
        if (password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "signup";
        }
        if (age < 1 || age > 120) {
            model.addAttribute("error", "Please enter a valid age.");
            return "signup";
        }
        String normalizedEmail = email.toLowerCase(java.util.Locale.ROOT).trim();
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            model.addAttribute("error", "An account with this email already exists.");
            return "signup";
        }
        try {
            users.save(new User(fullName.trim(), normalizedEmail, encoder.encode(password), age));
        } catch (RuntimeException e) {
            log.error("Signup could not save a Firestore user profile; cause type={}",
                    e.getClass().getSimpleName());
            model.addAttribute("error", "The account could not be saved to Firebase. Check the Firebase configuration and try again.");
            return "signup";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        var history = sessions.findByUserOrderByStartTimeDesc(user);
        var latest = history.isEmpty() ? dummy.generate(user) : history.get(0);
        if (history.isEmpty()) {
            history = sessions.findByUserOrderByStartTimeDesc(user);
        }
        model.addAttribute("user", user);
        model.addAttribute("sleepSession", latest);
        model.addAttribute("sleepSessions", history);
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {

            User user = users
                .findByEmailIgnoreCase(auth.getName())
                .orElseThrow();

            model.addAttribute("user", user);

            return "profile";
}

    @PostMapping(value = "/api/demo/generate-session", produces = "application/json")
    @ResponseBody
    public Map<String, Object> generate(Authentication auth) {
        User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        SleepSession generated = dummy.generate(user);

        var history = sessions.findByUserOrderByStartTimeDesc(user);
        var result = new LinkedHashMap<String, Object>();
        result.put("session", sessionPayload(history.get(0)));
        result.put("history", history.stream().map(this::sessionPayload).toList());
        return result;
    }

    private Map<String, Object> sessionPayload(SleepSession session) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("id", session.getId());
        payload.put("startTime", session.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        payload.put("endTime", session.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        payload.put("date", session.getEndTime().format(DateTimeFormatter.ofPattern("MMM d · h:mm a")));
        payload.put("totalMinutes", session.getTotalMinutes());
        payload.put("awakeMinutes", session.getAwakeMinutes());
        payload.put("lightMinutes", session.getLightMinutes());
        payload.put("deepMinutes", session.getDeepMinutes());
        payload.put("remMinutes", session.getRemMinutes());
        payload.put("averageHeartRate", session.getAverageHeartRate());
        payload.put("averageMovement", session.getAverageMovement());
        payload.put("spo2", session.getSpo2());
        payload.put("skinTemperature", session.getSkinTemperature());
        payload.put("deltaPower", session.getDeltaPower());
        payload.put("thetaPower", session.getThetaPower());
        payload.put("alphaPower", session.getAlphaPower());
        payload.put("betaPower", session.getBetaPower());
        payload.put("qualityScore", session.getQualityScore());
        return payload;
    }

    @GetMapping("/api/chat")
    @ResponseBody
    public String chat(@RequestParam String message, Authentication auth) {
        User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        var latest = sessions.findFirstByUserOrderByIdDesc(user).orElse(null);
        if (latest == null) {
            latest = dummy.generate(user);
        }
        return ai.reply(message, latest);
    }

    @PostMapping(value = "/api/auth/firebase", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, String>> firebaseSignIn(
            @RequestBody FirebaseSignInRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (body == null || body.idToken() == null || body.idToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A Firebase sign-in token is required."));
        }

        try {
            User user = firebaseSignIn.signInWithGoogle(body.idToken());
            var authenticated = UsernamePasswordAuthenticationToken.authenticated(
                    user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
            if (request.getSession(false) != null) request.changeSessionId();
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            return ResponseEntity.ok(Map.of("redirect", "/dashboard"));
        } catch (ResponseStatusException e) {
            String message = e.getReason() == null ? "Google sign-in could not be verified." : e.getReason();
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", message));
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            log.error("Google sign-in persistence failed; root cause type={}", root.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", firebasePersistenceFailure(e)));
        }
    }

    private String firebasePersistenceFailure(Throwable failure) {
        StringBuilder causes = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null) causes.append(' ').append(current.getMessage());
        }
        String detail = causes.toString().toUpperCase(java.util.Locale.ROOT);

        if (detail.contains("FIRESTORE_USER_SAVE")) {
            return firestoreFailure(detail, "account profile");
        }
        return firestoreFailure(detail, "account or sleep data");
    }

    private String firestoreFailure(String detail, String dataType) {
        if (detail.contains("SERVICE_DISABLED") || detail.contains("API HAS NOT BEEN USED")
                || detail.contains("CLOUD FIRESTORE API IS DISABLED")) {
            return "Google was verified, but the Cloud Firestore API is disabled for this Firebase project. Enable the API in Google Cloud, then retry.";
        }
        if (detail.contains("PERMISSION_DENIED") || detail.contains("PERMISSION DENIED")) {
            return "Google was verified, but Firestore denied saving your " + dataType + ". Check that the backend service account has Firestore write access, then retry.";
        }
        if (detail.contains("NOT_FOUND") || detail.contains("DATABASE (DEFAULT) DOES NOT EXIST")) {
            return "Google was verified, but no Firestore database was found in this project. Create the project's default Firestore database, then retry.";
        }
        if (detail.contains("UNAUTHENTICATED") || detail.contains("INVALID CREDENTIALS")) {
            return "Google was verified, but Firebase rejected the backend credentials. Check that the service-account key belongs to the configured Firebase project.";
        }
        if (detail.contains("UNAVAILABLE") || detail.contains("DEADLINE_EXCEEDED")) {
            return "Google was verified, but Firestore is temporarily unreachable. Check the server's network access and try again.";
        }
        return "Google was verified, but SleepSense could not save your " + dataType + ". Check the server log for the Firebase error.";
    }

    public record FirebaseSignInRequest(String idToken) {}

    @GetMapping("/api/health")
    @ResponseBody
    public String health() {
        return "{\"status\":\"UP\",\"version\":\"1.0.0\"}";
    }
}
