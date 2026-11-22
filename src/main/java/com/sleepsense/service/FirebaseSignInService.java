package com.sleepsense.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.sleepsense.model.User;
import com.sleepsense.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
public class FirebaseSignInService {
    private final ObjectProvider<FirebaseAuth> authProvider;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public FirebaseSignInService(
            ObjectProvider<FirebaseAuth> authProvider,
            UserRepository users,
            PasswordEncoder passwordEncoder) {
        this.authProvider = authProvider;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User signInWithGoogle(String idToken) {
        FirebaseAuth auth = authProvider.getIfAvailable();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured yet.");
        }

        FirebaseToken token;
        try {
            token = auth.verifyIdToken(idToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google sign-in could not be verified.");
        }

        Map<String, Object> claims = token.getClaims();
        Object providerClaim = claims.get("firebase");
        boolean fromGoogle = providerClaim instanceof Map<?, ?> provider
                && "google.com".equals(provider.get("sign_in_provider"));
        if (!fromGoogle || !Boolean.TRUE.equals(claims.get("email_verified"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Use a verified Google account to sign in.");
        }

        Object rawEmail = claims.get("email");
        if (!(rawEmail instanceof String emailValue) || emailValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your Google account did not provide an email address.");
        }
        String email = emailValue.toLowerCase(Locale.ROOT).trim();

        User user = users.findByFirebaseUid(token.getUid())
                .or(() -> users.findByEmailIgnoreCase(email))
                .orElseGet(() -> new User(
                        displayName(claims, email),
                        email,
                        passwordEncoder.encode(randomPassword()),
                        0));

        if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(token.getUid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already linked to another Google account.");
        }
        user.setFirebaseUid(token.getUid());
        if (user.getId() == null || user.getId().isBlank()) user.setId(token.getUid());
        try {
            users.save(user);
        } catch (Exception e) {
            throw new IllegalStateException("FIRESTORE_USER_SAVE", e);
        }
        return user;
    }

    private String displayName(Map<String, Object> claims, String email) {
        Object name = claims.get("name");
        if (name instanceof String value && !value.isBlank()) return value.trim();
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : "SleepSense user";
    }

    private String randomPassword() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
