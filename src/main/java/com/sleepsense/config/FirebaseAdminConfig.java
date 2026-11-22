package com.sleepsense.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseAdminConfig {

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(
            @Value("${firebase.project-id}") String projectId,
            @Value("${firebase.service-account-path:}") String serviceAccountPath) throws IOException {
        if (projectId.isBlank()) {
            throw new IllegalStateException("FIREBASE_PROJECT_ID is required when Firebase is enabled.");
        }

        GoogleCredentials credentials;
        try {
            if (serviceAccountPath.isBlank()) {
                credentials = GoogleCredentials.getApplicationDefault();
            } else {
                Path keyFile = Path.of(serviceAccountPath).toAbsolutePath().normalize();
                if (!Files.isRegularFile(keyFile)) {
                    throw new IllegalStateException(
                            "Firebase service-account JSON was not found at " + keyFile
                                    + ". Set FIREBASE_SERVICE_ACCOUNT_PATH to the existing key file. "
                                    + "In a Windows .env file, use forward slashes (C:/Users/...) "
                                    + "or escape each backslash (C:\\\\Users\\\\...).");
                }
                try (InputStream key = Files.newInputStream(keyFile)) {
                    credentials = GoogleCredentials.fromStream(key);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Firebase credentials could not be loaded. Set a valid FIREBASE_SERVICE_ACCOUNT_PATH "
                            + "or configure Application Default Credentials.", e);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        return FirebaseApp.initializeApp(options, "sleepsense");
    }

    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean(destroyMethod = "close")
    Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
