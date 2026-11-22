package com.sleepsense.model;

import java.util.UUID;

/** A SleepSense profile stored in Cloud Firestore. */
public class User {
    private String id;
    private String fullName;
    private String email;
    private String emailLowercase;
    private String firebaseUid;
    private String passwordHash;
    private int age;

    public User() {
        this.id = UUID.randomUUID().toString();
    }

    public User(String fullName, String email, String passwordHash, int age) {
        this();
        this.fullName = fullName;
        this.email = email;
        this.emailLowercase = email == null ? null : email.toLowerCase(java.util.Locale.ROOT).trim();
        this.passwordHash = passwordHash;
        this.age = age;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getEmailLowercase() { return emailLowercase; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getPasswordHash() { return passwordHash; }
    public int getAge() { return age; }

    public void setId(String id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) {
        this.email = email;
        this.emailLowercase = email == null ? null : email.toLowerCase(java.util.Locale.ROOT).trim();
    }
    public void setEmailLowercase(String emailLowercase) { this.emailLowercase = emailLowercase; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setAge(int age) { this.age = age; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }
}
