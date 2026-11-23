package com.sleepsense.repository;

import com.sleepsense.model.User;
import com.sleepsense.service.FirebaseDataService;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Firestore-backed user store. */
@Repository
public class UserRepository {
    private final FirebaseDataService data;

    public UserRepository(FirebaseDataService data) {
        this.data = data;
    }

    public User save(User user) {
        return data.saveUser(user);
    }

    public Optional<User> findByEmailIgnoreCase(String email) {
        return data.findUserByEmail(email);
    }

    public Optional<User> findByFirebaseUid(String firebaseUid) {
        return data.findUserByFirebaseUid(firebaseUid);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return data.findUserByPhoneNumber(phoneNumber);
    }

    public boolean existsByEmailIgnoreCase(String email) {
        return data.userExistsByEmail(email);
    }
}
