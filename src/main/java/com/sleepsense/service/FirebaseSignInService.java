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
import java.util.Optional;

@Service
public class FirebaseSignInService {
    private static final String GOOGLE_PROVIDER = "google.com";
    private static final String PHONE_PROVIDER = "phone";
    private static final String EMAIL_PASSWORD_PROVIDER = "password";

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

    /** Verifies Firebase's signed ID token and accepts only the enabled Google, phone, and password providers. */
    public FirebaseSignInResult signInWithFirebaseToken(String idToken) {
        return signInWithFirebaseToken(idToken, null, null);
    }

    /** Verifies a Firebase ID token and optionally uses validated profile details from email signup. */
    public FirebaseSignInResult signInWithFirebaseToken(String idToken, String signupName, Integer signupAge) {
        FirebaseAuth auth = authProvider.getIfAvailable();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase sign-in is not configured yet.");
        }

        FirebaseToken token;
        try {
            token = auth.verifyIdToken(idToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase sign-in could not be verified.");
        }

        String uid = token.getUid();
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase did not provide a valid account ID.");
        }

        Map<String, Object> claims = token.getClaims();
        String provider = signInProvider(claims);
        if (GOOGLE_PROVIDER.equals(provider)) {
            return signInWithGoogleClaims(uid, claims);
        }
        if (PHONE_PROVIDER.equals(provider)) {
            return signInWithPhoneClaims(uid, claims);
        }
        if (EMAIL_PASSWORD_PROVIDER.equals(provider)) {
            return signInWithEmailPasswordClaims(uid, claims, signupName, signupAge);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Use a supported Firebase sign-in method.");
    }

    private FirebaseSignInResult signInWithGoogleClaims(String uid, Map<String, Object> claims) {
        if (!Boolean.TRUE.equals(claims.get("email_verified"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Use a verified Google account to sign in.");
        }
        Object rawEmail = claims.get("email");
        if (!(rawEmail instanceof String emailValue) || emailValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your Google account did not provide an email address.");
        }
        String email = emailValue.toLowerCase(Locale.ROOT).trim();

        Optional<User> existing = users.findByFirebaseUid(uid)
                .or(() -> users.findByEmailIgnoreCase(email));
        User user = existing.orElseGet(() -> new User(
                displayName(claims, email), email, passwordEncoder.encode(randomPassword()), 0));
        ensureNotLinkedToDifferentFirebaseUser(user, uid);
        if (existing.isEmpty()) {
            user.setId(uid);
            user.setOnboardingComplete(false);
        }
        user.setFirebaseUid(uid);
        user.setAuthProvider(GOOGLE_PROVIDER);
        if (user.getId() == null || user.getId().isBlank()) user.setId(uid);
        save(user);
        return new FirebaseSignInResult(user, !user.isOnboardingComplete(), needsName(user));
    }

    private FirebaseSignInResult signInWithPhoneClaims(String uid, Map<String, Object> claims) {
        Object rawPhoneNumber = claims.get("phone_number");
        if (!(rawPhoneNumber instanceof String phoneValue) || phoneValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase did not provide a verified phone number.");
        }
        String phoneNumber = phoneValue.trim();

        Optional<User> existing = users.findByFirebaseUid(uid)
                .or(() -> users.findByPhoneNumber(phoneNumber));
        User user = existing.orElseGet(() -> new User("", null, null, 0));
        ensureNotLinkedToDifferentFirebaseUser(user, uid);
        if (existing.isEmpty()) {
            user.setId(uid);
            user.setOnboardingComplete(false);
        }
        user.setFirebaseUid(uid);
        user.setPhoneNumber(phoneNumber);
        user.setAuthProvider(PHONE_PROVIDER);
        if (user.getId() == null || user.getId().isBlank()) user.setId(uid);
        save(user);
        return new FirebaseSignInResult(user, !user.isOnboardingComplete(), needsName(user));
    }

    private FirebaseSignInResult signInWithEmailPasswordClaims(
            String uid, Map<String, Object> claims, String signupName, Integer signupAge) {
        if (!Boolean.TRUE.equals(claims.get("email_verified"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Verify your email address before continuing. Check your inbox for Firebase's verification email.");
        }
        Object rawEmail = claims.get("email");
        if (!(rawEmail instanceof String emailValue) || emailValue.isBlank() || emailValue.length() > 254) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your Firebase account did not provide an email address.");
        }
        String email = emailValue.toLowerCase(Locale.ROOT).trim();

        boolean hasName = signupName != null && !signupName.isBlank();
        boolean hasAge = signupAge != null;
        if (hasName != hasAge) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter both your name and age to create your profile.");
        }
        if (hasName && (signupName.trim().length() > 255 || signupAge < 1 || signupAge > 120)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a name and an age between 1 and 120.");
        }

        Optional<User> existing = users.findByFirebaseUid(uid)
                .or(() -> users.findByEmailIgnoreCase(email));
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            ensureNotLinkedToDifferentFirebaseUser(user, uid);
            if (!user.isOnboardingComplete() && hasName) {
                user.setFullName(signupName.trim());
                user.setAge(signupAge);
                user.setOnboardingComplete(true);
            }
        } else if (hasName) {
            user = new User(signupName.trim(), email, null, signupAge);
            user.setId(uid);
        } else {
            user = new User("", email, null, 0);
            user.setId(uid);
            user.setOnboardingComplete(false);
        }

        user.setEmail(email);
        user.setFirebaseUid(uid);
        user.setAuthProvider(EMAIL_PASSWORD_PROVIDER);
        user.setPasswordHash(null);
        if (user.getId() == null || user.getId().isBlank()) user.setId(uid);
        save(user);
        return new FirebaseSignInResult(user, !user.isOnboardingComplete(), needsName(user));
    }

    private String signInProvider(Map<String, Object> claims) {
        Object providerClaim = claims.get("firebase");
        if (providerClaim instanceof Map<?, ?> provider) {
            Object signInProvider = provider.get("sign_in_provider");
            return signInProvider instanceof String value ? value : "";
        }
        return "";
    }

    private boolean needsName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank();
    }

    private void ensureNotLinkedToDifferentFirebaseUser(User user, String uid) {
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This account is already linked to another Firebase sign-in.");
        }
    }

    private void save(User user) {
        try {
            users.save(user);
        } catch (Exception e) {
            throw new IllegalStateException("FIRESTORE_USER_SAVE", e);
        }
    }

    public record FirebaseSignInResult(User user, boolean needsProfile, boolean needsName) {}

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
