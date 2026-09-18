package com.sleepsense.controller;

import com.sleepsense.model.User;
import com.sleepsense.repository.UserRepository;
import com.sleepsense.repository.SleepSessionRepository;
import com.sleepsense.service.DummyDataService;
import com.sleepsense.service.AIChatService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PageController {
    private final UserRepository users;
    private final SleepSessionRepository sessions;
    private final DummyDataService dummy;
    private final AIChatService ai;
    private final PasswordEncoder encoder;

    public PageController(UserRepository users, SleepSessionRepository sessions,
                          DummyDataService dummy, AIChatService ai, PasswordEncoder encoder) {
        this.users = users;
        this.sessions = sessions;
        this.dummy = dummy;
        this.ai = ai;
        this.encoder = encoder;
    }

    @GetMapping("/")
    public String home() { return "redirect:/login"; }

    @GetMapping("/login")
    public String login() { return "login"; }

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
        if (users.existsByEmail(email.toLowerCase().trim())) {
            model.addAttribute("error", "An account with this email already exists.");
            return "signup";
        }
        users.save(new User(fullName.trim(), email.toLowerCase().trim(), encoder.encode(password), age));
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = users.findByEmail(auth.getName()).orElseThrow();
        var latest = sessions
        .findFirstByUserOrderByIdDesc(user)
        .orElseGet(() -> dummy.generate(user));
        model.addAttribute("user", user);
        model.addAttribute("sleepSession", latest);
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {

            User user = users
                .findByEmail(auth.getName())
                .orElseThrow();

            model.addAttribute("user", user);

            return "profile";
}

    @PostMapping("/api/demo/generate")
    public String generate(Authentication auth) {

        User user = users
                .findByEmail(auth.getName())
                .orElseThrow();

        dummy.generate(user);

        return "redirect:/dashboard";
    }

    @GetMapping("/api/chat")
    @ResponseBody
    public String chat(@RequestParam String message, Authentication auth) {
        User user = users.findByEmail(auth.getName()).orElseThrow();
        var latest = sessions
        .findFirstByUserOrderByIdDesc(user)
        .orElseGet(() -> dummy.generate(user));
        return ai.reply(message, latest);
    }

    @GetMapping("/api/health")
    @ResponseBody
    public String health() {
        return "{\"status\":\"UP\",\"version\":\"1.0.0\"}";
    }
}
