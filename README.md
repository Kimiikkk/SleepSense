# SleepSense — Wearable Sleep & EEG Monitoring

Java/Spring Boot software-only prototype for the university OOP project.

## Current scope
- No physical wearable hardware required.
- Uses simulated EEG, heart-rate and motion data.
- Login/signup with BCrypt.
- Minimalist dashboard.
- Sleep stage summary and charts.
- Prototype sleep report data.
- Rule-based sleep analyzer.
- AI Sleep Assistant with a local mock/fallback service.
- Firebase Firestore storage for user profiles and sleep sessions.
- Google sign-in through Firebase Authentication, with BCrypt-protected email/password accounts stored in Firestore.
- Version 1.0.0.

## Run
Requirements:
- Java 17+
- Maven 3.9+

From the project folder:

```bash
mvn spring-boot:run
```

Open:
http://localhost:8080

Before running the app, configure the Firebase project and backend service account below. Create an account and log in after Firebase is configured.

The dashboard generates a sample sleep session automatically. Use **Generate new night** to create another simulated session.

## Firebase Google sign-in and Firestore

Firebase is required. SleepSense stores profiles and sleep sessions only in Cloud Firestore; it does not use a local database. Email/password accounts are stored with BCrypt password hashes, and Google accounts are verified with Firebase Authentication.

1. Create a Firebase project, add a Web app, enable **Authentication → Google**, and create a **Cloud Firestore** database.
2. Add `localhost` and your deployed host to Firebase Authentication's authorized domains.
3. Copy `.env.example` to `.env`, set `FIREBASE_ENABLED=true`, and fill in the Web app's project ID, API key, auth domain, and app ID.
4. Create a service account for the backend. Save its JSON key outside this repository and set `FIREBASE_SERVICE_ACCOUNT_PATH` to that existing file. On Windows, use forward slashes (`C:/Users/.../service-account.json`) or double each backslash in `.env`. On Google Cloud, use Application Default Credentials instead and leave this path empty. Never commit or paste the JSON key into source control.
5. Deploy `firestore.rules`. Direct browser reads and writes are denied; the backend uses the service account and Firebase Admin SDK to read/write profile documents in `users/{userId}` and generated nights in `users/{userId}/sleepSessions/{sessionId}`. Email/password accounts also use this Firestore collection.
6. Restart the app and use **Continue with Google** on the sign-in page.

The Firebase Web API key identifies the browser app and is not a service-account secret. Protect the backend service-account key, keep `.env` out of source control, and restrict the service account's IAM permissions to the Firestore access this app needs. All account and sleep-session data lives in Firestore.

## OOP structure
The backend is intentionally organized around domain classes and services so the team can demonstrate:
classes/objects, encapsulation, constructors, abstraction, inheritance, overriding, polymorphism, composition, enum, collections, exception handling, validation and modular design.

## Next build stage
Add the full sensor hierarchy (`Sensor`, `EEGSensor`, `HeartRateSensor`, `MotionSensor`), dedicated reading classes, richer session history/report pages, formal REST DTOs, automated tests, and a real AI provider behind the existing AI service abstraction.
