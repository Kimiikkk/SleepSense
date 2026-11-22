
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseAdminConfig {

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(
            @Value("${firebase.project-id}") String projectId,
            @Value("${firebase.service-account-path:}") String serviceAccountPath,
            @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceAccountJson)
            throws IOException {

        if (projectId.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_PROJECT_ID is required when Firebase is enabled.");
        }

        GoogleCredentials credentials;

        if (!serviceAccountJson.isBlank()) {
            try (InputStream key = new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
                credentials = GoogleCredentials.fromStream(key);
            }
        } else if (!serviceAccountPath.isBlank()) {
            Path keyFile = Path.of(serviceAccountPath).toAbsolutePath().normalize();

            if (!Files.isRegularFile(keyFile)) {
                throw new IllegalStateException(
                        "Firebase service-account JSON file was not found.");
            }

            try (InputStream key = Files.newInputStream(keyFile)) {
                credentials = GoogleCredentials.fromStream(key);
            }
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
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
