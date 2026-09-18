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
- H2 database for easy local running.
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

Create an account and log in.

The dashboard generates a sample sleep session automatically. Use **Generate new night** to create another simulated session.

## OOP structure
The backend is intentionally organized around domain classes and services so the team can demonstrate:
classes/objects, encapsulation, constructors, abstraction, inheritance, overriding, polymorphism, composition, enum, collections, exception handling, validation and modular design.

## Next build stage
Add the full sensor hierarchy (`Sensor`, `EEGSensor`, `HeartRateSensor`, `MotionSensor`), dedicated reading classes, richer session history/report pages, formal REST DTOs, automated tests, and a real AI provider behind the existing AI service abstraction.
