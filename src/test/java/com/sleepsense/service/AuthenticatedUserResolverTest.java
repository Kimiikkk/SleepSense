package com.sleepsense.service;

import com.sleepsense.model.User;
import com.sleepsense.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedUserResolverTest {
    @Test
    void firebasePrincipalResolvesOnlyItsServerVerifiedUid() {
        UserRepository users = mock(UserRepository.class);
        Authentication authentication = mock(Authentication.class);
        User owner = new User("Owner", "owner@example.com", null, 30);
        owner.setId("legacy-profile-document");
        owner.setFirebaseUid("verified-uid");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("firebase:verified-uid");
        when(users.findByFirebaseUid("verified-uid")).thenReturn(Optional.of(owner));

        assertSame(owner, new AuthenticatedUserResolver(users).require(authentication));
        verify(users).findByFirebaseUid("verified-uid");
        verify(users, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void unauthenticatedRequestsCannotResolveAProfile() {
        UserRepository users = mock(UserRepository.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> new AuthenticatedUserResolver(users).require(authentication));
        verifyNoInteractions(users);
    }
}
