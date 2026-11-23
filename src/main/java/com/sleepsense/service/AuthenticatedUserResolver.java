package com.sleepsense.service;

import com.sleepsense.model.User;
import com.sleepsense.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/** Resolves the server-authenticated principal to its preserved Firestore profile document. */
@Service
public class AuthenticatedUserResolver {
    private final UserRepository users;

    public AuthenticatedUserResolver(UserRepository users) { this.users = users; }

    public User require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Sign in to access your SleepSense data.");
        }
        String principal = authentication.getName();
        var user = principal.startsWith("firebase:")
                ? users.findByFirebaseUid(principal.substring("firebase:".length()))
                : users.findByEmailIgnoreCase(principal);
        return user.orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                "The signed-in SleepSense profile could not be found."));
    }
}
