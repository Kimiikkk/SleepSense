package com.sleepsense.service;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.sleepsense.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseFeatureDataServiceTest {
    @Test
    void legacyProfileFeaturesStayUnderResolvedOwnerDocument() {
        ObjectProvider<Firestore> provider = mock(ObjectProvider.class);
        Firestore firestore = mock(Firestore.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference ownerDocument = mock(DocumentReference.class);
        CollectionReference goalCollection = mock(CollectionReference.class);
        DocumentReference goalDocument = mock(DocumentReference.class);
        DocumentSnapshot missingGoal = mock(DocumentSnapshot.class);
        when(provider.getIfAvailable()).thenReturn(firestore);
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("legacy-profile-doc")).thenReturn(ownerDocument);
        when(ownerDocument.collection("sleepGoals")).thenReturn(goalCollection);
        when(goalCollection.document("current")).thenReturn(goalDocument);
        when(goalDocument.get()).thenReturn(ApiFutures.immediateFuture(missingGoal));
        when(missingGoal.exists()).thenReturn(false);

        User owner = new User("Owner", "owner@example.com", null, 30);
        owner.setId("legacy-profile-doc");
        owner.setFirebaseUid("verified-owner-uid");
        SleepGoalService service = new SleepGoalService(new FirebaseFeatureDataService(provider),
                mock(com.sleepsense.repository.SleepSessionRepository.class));

        assertEquals(480, service.get(owner).targetSleepMinutes());
        verify(usersCollection).document("legacy-profile-doc");
        verify(usersCollection, never()).document("verified-owner-uid");
    }
}
