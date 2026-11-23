# SleepSense — Wearable Sleep & EEG Monitoring

Java/Spring Boot software-only prototype for the university OOP project.

## Current scope
- No physical wearable hardware required.
- Uses simulated EEG, heart-rate and motion data.
- Email/password login and signup with Firebase Authentication.
- Minimalist dashboard.
- Sleep stage summary and charts.
- Prototype sleep report data.
- Rule-based sleep analyzer.
- AI Sleep Assistant with a local mock/fallback service.
- Firebase Firestore storage for user profiles and sleep sessions.
- Email/password, Google, and phone-number sign-in through Firebase Authentication; profile and sleep data are stored in Firestore.
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

## Firebase sign-in and Firestore

Firebase is required. SleepSense stores profiles and sleep sessions only in Cloud Firestore; it does not use a local database. Firebase Authentication verifies email/password, Google, and phone sign-ins. Email/password users must verify their email address before SleepSense accepts the Firebase token. The backend verifies Firebase ID tokens and stores application profiles and sleep sessions in Firestore. New email and phone accounts are asked for a name and age when they first sign in.

1. Create a Firebase project, add a Web app, enable **Authentication → Email/Password**, **Google**, and **Phone Number**, and create a **Cloud Firestore** database.
2. Add `localhost` and your deployed app domain under Firebase Authentication → Settings → Authorized domains so email-verification links can return to SleepSense. Configure the SMS region policy too; new Firebase projects may deny all SMS regions until a policy is set. Phone sign-in also requires an authorized app domain and reCAPTCHA; Firebase does not accept `localhost` as a hosted domain for phone authentication, so use an authorized hosted domain to test phone SMS. Firebase's first 10 real SMS messages per day are not billed, but a Cloud Billing account still must be linked before real SMS can be sent.
3. Copy `.env.example` to `.env`, set `FIREBASE_ENABLED=true`, and fill in the Web app's project ID, API key, auth domain, and app ID.
4. Create a service account for the backend. Save its JSON key outside this repository and set `FIREBASE_SERVICE_ACCOUNT_PATH` to that existing file. On Windows, use forward slashes (`C:/Users/.../service-account.json`) or double each backslash in `.env`. On Google Cloud, use Application Default Credentials instead and leave this path empty. Never commit or paste the JSON key into source control.
5. Deploy `firestore.rules`. Direct browser reads and writes are denied; the backend uses the service account and Firebase Admin SDK to read/write profile documents in `users/{userId}` and generated nights in `users/{userId}/sleepSessions/{sessionId}`. Email/password accounts also use this Firestore collection.
6. Restart the app and use email/password, **Continue with Google**, or the phone number form on the sign-in page. To connect a pre-existing SleepSense profile, create Firebase email/password credentials with the same email, verify that address, then sign in. For development without billing, add fictional phone numbers and fixed codes under **Authentication → Sign-in method → Phone numbers for testing**. Firebase sends no SMS for those numbers; enter the configured code in SleepSense.

The Firebase Web API key identifies the browser app and is not a service-account secret. Protect the backend service-account key, keep `.env` out of source control, and restrict the service account's IAM permissions to the Firestore access this app needs. All account and sleep-session data lives in Firestore.

## OOP structure
The backend is intentionally organized around domain classes and services so the team can demonstrate:
classes/objects, encapsulation, constructors, abstraction, inheritance, overriding, polymorphism, composition, enum, collections, exception handling, validation and modular design.

## Next build stage
Add the full sensor hierarchy (`Sensor`, `EEGSensor`, `HeartRateSensor`, `MotionSensor`), dedicated reading classes, richer session history/report pages, formal REST DTOs, automated tests, and a real AI provider behind the existing AI service abstraction.
