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
import org.springframework.security.core.context.SecurityContext;
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
        FirebaseSignInService.FirebaseSignInResult result = signIn.signInWithFirebaseToken("verified-token");

        assertTrue(result.needsProfile());
        assertFalse(result.needsName());
        assertFalse(result.user().isOnboardingComplete());
        assertEquals(0, result.user().getAge());

        FirebaseSignInService controllerSignIn = mock(FirebaseSignInService.class);
        when(controllerSignIn.signInWithFirebaseToken("verified-token", null, null)).thenReturn(result);
        PageController controller = controller(users, controllerSignIn);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var endpointResult = controller.firebaseSignIn(
                new PageController.FirebaseSignInRequest("verified-token"), request, response);

        assertEquals("/complete-profile", endpointResult.getBody().get("redirect"));
        assertEquals(UID, request.getSession(false).getAttribute("pendingFirebaseUid"));
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
        FirebaseSignInService.FirebaseSignInResult result = signIn.signInWithFirebaseToken("verified-token");

        assertFalse(result.needsProfile());
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
        request.getSession(true).setAttribute("pendingFirebaseUid", UID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.saveCompletedProfile("", 29, request, response, new ExtendedModelMap());

        assertEquals("redirect:/dashboard", view);
        assertEquals(29, pending.getAge());
        assertTrue(pending.isOnboardingComplete());
        assertNull(request.getSession(false).getAttribute("pendingFirebaseUid"));
        assertNotNull(request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void newPhoneProfileRequiresNameAndAgeThenAuthenticatesByFirebaseUid() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ObjectProvider<FirebaseAuth> authProvider = mock(ObjectProvider.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        String phoneUid = "phone-user-456";
        String phoneNumber = "+14155550123";
        when(authProvider.getIfAvailable()).thenReturn(auth);
        when(auth.verifyIdToken("phone-token")).thenReturn(token);
        when(token.getUid()).thenReturn(phoneUid);
        when(token.getClaims()).thenReturn(Map.of(
                "phone_number", phoneNumber,
                "firebase", Map.of("sign_in_provider", "phone")));
        when(users.findByFirebaseUid(phoneUid)).thenReturn(Optional.empty());
        when(users.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FirebaseSignInService signIn = new FirebaseSignInService(
                authProvider, users, mock(PasswordEncoder.class));
        FirebaseSignInService.FirebaseSignInResult result = signIn.signInWithFirebaseToken("phone-token");
        assertTrue(result.needsProfile());
        assertTrue(result.needsName());
        assertEquals(phoneUid, result.user().getId());
        assertEquals(phoneNumber, result.user().getPhoneNumber());
        assertEquals("phone", result.user().getAuthProvider());

        when(users.findByFirebaseUid(phoneUid)).thenReturn(Optional.of(result.user()));
        PageController controller = controller(users, signIn);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var endpointResult = controller.firebaseSignIn(
                new PageController.FirebaseSignInRequest("phone-token"), request, response);
        assertEquals("/complete-profile", endpointResult.getBody().get("redirect"));
        assertEquals(phoneUid, request.getSession(false).getAttribute("pendingFirebaseUid"));
        assertEquals(Boolean.TRUE, request.getSession(false).getAttribute("pendingProfileNeedsName"));

        ExtendedModelMap invalidProfile = new ExtendedModelMap();
        assertEquals("complete-profile", controller.saveCompletedProfile("", 31, request, response, invalidProfile));
        assertEquals("Enter a name between 1 and 255 characters.", invalidProfile.get("error"));
        assertFalse(result.user().isOnboardingComplete());

        String view = controller.saveCompletedProfile("Taylor Night", 31, request, response, new ExtendedModelMap());
        assertEquals("redirect:/dashboard", view);
        assertEquals("Taylor Night", result.user().getFullName());
        assertEquals(31, result.user().getAge());
        assertTrue(result.user().isOnboardingComplete());
        SecurityContext context = (SecurityContext) request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertEquals("firebase:" + phoneUid, context.getAuthentication().getName());
    }

    @Test
    void firebaseEmailPasswordSignupCreatesProfileWithoutStoringPasswordHash() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ObjectProvider<FirebaseAuth> authProvider = mock(ObjectProvider.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        String emailUid = "email-user-789";
        String email = "new.account@example.com";
        when(authProvider.getIfAvailable()).thenReturn(auth);
        when(auth.verifyIdToken("email-token")).thenReturn(token);
        when(token.getUid()).thenReturn(emailUid);
        when(token.getClaims()).thenReturn(Map.of(
                "email", email,
                "email_verified", true,
                "firebase", Map.of("sign_in_provider", "password")));
        when(users.findByFirebaseUid(emailUid)).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FirebaseSignInService signIn = new FirebaseSignInService(
                authProvider, users, mock(PasswordEncoder.class));
        FirebaseSignInService.FirebaseSignInResult result =
                signIn.signInWithFirebaseToken("email-token", "Morgan Sleep", 26);

        assertFalse(result.needsProfile());
        assertEquals(emailUid, result.user().getId());
        assertEquals(emailUid, result.user().getFirebaseUid());
        assertEquals(email, result.user().getEmail());
        assertEquals("Morgan Sleep", result.user().getFullName());
        assertEquals(26, result.user().getAge());
        assertEquals("password", result.user().getAuthProvider());
        assertNull(result.user().getPasswordHash());
        verify(users).save(result.user());
    }

    @Test
    void firebaseEmailPasswordLoginRejectsUnverifiedEmail() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ObjectProvider<FirebaseAuth> authProvider = mock(ObjectProvider.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(authProvider.getIfAvailable()).thenReturn(auth);
        when(auth.verifyIdToken("unverified-email-token")).thenReturn(token);
        when(token.getUid()).thenReturn("unverified-email-user");
        when(token.getClaims()).thenReturn(Map.of(
                "email", "unverified@example.com",
                "email_verified", false,
                "firebase", Map.of("sign_in_provider", "password")));

        FirebaseSignInService signIn = new FirebaseSignInService(
                authProvider, users, mock(PasswordEncoder.class));
        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> signIn.signInWithFirebaseToken("unverified-email-token"));

        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(users);
    }

    @Test
    void firebaseEmailPasswordSignupLinksAnExistingSleepSenseProfileByEmail() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ObjectProvider<FirebaseAuth> authProvider = mock(ObjectProvider.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        String emailUid = "email-user-legacy";
        User legacy = new User("Existing Sleep User", EMAIL, "legacy-bcrypt-hash", 34);
        when(authProvider.getIfAvailable()).thenReturn(auth);
        when(auth.verifyIdToken("email-token")).thenReturn(token);
        when(token.getUid()).thenReturn(emailUid);
        when(token.getClaims()).thenReturn(Map.of(
                "email", EMAIL,
                "email_verified", true,
                "firebase", Map.of("sign_in_provider", "password")));
        when(users.findByFirebaseUid(emailUid)).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(legacy));
        when(users.save(legacy)).thenReturn(legacy);

        FirebaseSignInService signIn = new FirebaseSignInService(
                authProvider, users, mock(PasswordEncoder.class));
        FirebaseSignInService.FirebaseSignInResult result =
                signIn.signInWithFirebaseToken("email-token", "Different Signup Name", 18);

        assertFalse(result.needsProfile());
        assertEquals(emailUid, legacy.getFirebaseUid());
        assertEquals("password", legacy.getAuthProvider());
        assertEquals("Existing Sleep User", legacy.getFullName());
        assertEquals(34, legacy.getAge());
        assertNull(legacy.getPasswordHash());
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
