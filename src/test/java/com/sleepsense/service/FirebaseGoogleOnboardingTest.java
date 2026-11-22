package com.sleepsense.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.sleepsense.controller.PageController;
import com.sleepsense.model.User;
import com.sleepsense.repository.SleepSessionRepository;
import com.sleepsense.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FirebaseGoogleOnboardingTest {
    private static final String UID = "google-user-123";
    private static final String EMAIL = "new.sleep.user@example.com";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newGoogleProfileRequiresAgeBeforeLogin() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ObjectProvider<FirebaseAuth> authProvider = mock(ObjectProvider.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(authProvider.getIfAvailable()).thenReturn(auth);
        when(auth.verifyIdToken("verified-token")).thenReturn(token);
        when(token.getUid()).thenReturn(UID);
        when(token.getClaims()).thenReturn(googleClaims());
        when(users.findByFirebaseUid(UID)).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FirebaseSignInService signIn = new FirebaseSignInService(
                authProvider, users, mock(PasswordEncoder.class));
        FirebaseSignInService.GoogleSignInResult result = signIn.signInWithGoogle("verified-token");

        assertTrue(result.needsAge());
        assertFalse(result.user().isOnboardingComplete());
        assertEquals(0, result.user().getAge());

        FirebaseSignInService controllerSignIn = mock(FirebaseSignInService.class);
        when(controllerSignIn.signInWithGoogle("verified-token")).thenReturn(result);
        PageController controller = controller(users, controllerSignIn);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var endpointResult = controller.firebaseSignIn(
                new PageController.FirebaseSignInRequest("verified-token"), request, response);

        assertEquals("/complete-profile", endpointResult.getBody().get("redirect"));
        assertEquals(UID, request.getSession(false).getAttribute("pendingGoogleUid"));
        assertNull(request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void existingGoogleProfileGoesDirectlyToDashboard() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ObjectProvider<FirebaseAuth> authProvider = mock(ObjectProvider.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        User existing = new User("Sleep User", EMAIL, "hashed-password", 28);
        existing.setFirebaseUid(UID);
        when(authProvider.getIfAvailable()).thenReturn(auth);
        when(auth.verifyIdToken("verified-token")).thenReturn(token);
        when(token.getUid()).thenReturn(UID);
        when(token.getClaims()).thenReturn(googleClaims());
        when(users.findByFirebaseUid(UID)).thenReturn(Optional.of(existing));
        when(users.save(existing)).thenReturn(existing);

        FirebaseSignInService signIn = new FirebaseSignInService(
                authProvider, users, mock(PasswordEncoder.class));
        FirebaseSignInService.GoogleSignInResult result = signIn.signInWithGoogle("verified-token");

        assertFalse(result.needsAge());
        assertTrue(result.user().isOnboardingComplete());

        PageController controller = controller(users, signIn);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var endpointResult = controller.firebaseSignIn(
                new PageController.FirebaseSignInRequest("verified-token"), request, response);

        assertEquals("/dashboard", endpointResult.getBody().get("redirect"));
        assertNotNull(request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void completedProfileSavesAgeAndAuthenticatesPendingGoogleUser() {
        UserRepository users = mock(UserRepository.class);
        User pending = new User("Sleep User", EMAIL, "hashed-password", 0);
        pending.setFirebaseUid(UID);
        pending.setOnboardingComplete(false);
        when(users.findByFirebaseUid(UID)).thenReturn(Optional.of(pending));
        when(users.save(pending)).thenReturn(pending);

        PageController controller = controller(users, mock(FirebaseSignInService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("pendingGoogleUid", UID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.saveCompletedProfile(29, request, response, new ExtendedModelMap());

        assertEquals("redirect:/dashboard", view);
        assertEquals(29, pending.getAge());
        assertTrue(pending.isOnboardingComplete());
        assertNull(request.getSession(false).getAttribute("pendingGoogleUid"));
        assertNotNull(request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    private PageController controller(UserRepository users, FirebaseSignInService signIn) {
        return new PageController(users, mock(SleepSessionRepository.class),
                mock(DummyDataService.class), mock(AIChatService.class),
                mock(PasswordEncoder.class), signIn);
    }

    private Map<String, Object> googleClaims() {
        return Map.of(
                "email", EMAIL,
                "email_verified", true,
                "name", "Sleep User",
                "firebase", Map.of("sign_in_provider", "google.com"));
    }
}
