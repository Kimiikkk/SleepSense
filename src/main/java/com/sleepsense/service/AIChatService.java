package com.sleepsense.service;

import com.sleepsense.model.SleepSession;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AIChatService {

    public String reply(String question, SleepSession s) {

        if (question == null || question.trim().isEmpty()) {
            return "Please ask me something about your sleep session.";
        }

        String q = question.toLowerCase(Locale.ROOT).trim();

        // ============================================================
        // 1. GREETINGS
        // ============================================================

        if (containsAny(q,
                "hello", "hi", "hey", "hii", "hiii",
                "good morning", "good evening", "good night",
                "how are you", "what's up", "whats up")) {

            return "Hello! I'm your SleepSense sleep assistant. I can analyze your current sleep session, explain sleep stages and EEG bands, discuss heart rate, movement, SpO₂, and skin temperature, calculate percentages, explain your quality score, and give personalized suggestions.";
        }

        // ============================================================
        // 2. COMPLETE SLEEP REPORT
        // ============================================================

        if (containsAny(q,
                "sleep report",
                "full report",
                "complete report",
                "entire report",
                "whole report",
                "sleep summary",
                "complete summary",
                "summarize my sleep",
                "summary of my sleep",
                "how did i sleep",
                "how was my sleep",
                "tell me about my sleep",
                "analyze my sleep",
                "analyse my sleep",
                "analyze my sleep data",
                "analyse my sleep data",
                "overall sleep",
                "overall analysis",
                "overall report")) {

            return buildCompleteReport(s);
        }

        // ============================================================
        // 3. TOTAL SLEEP DURATION
        // ============================================================

        if (containsAny(q,
                "how many hours did i sleep",
                "how many hours have i slept",
                "how long did i sleep",
                "how long have i slept",
                "how much did i sleep",
                "how much sleep did i get",
                "total sleep",
                "total sleep time",
                "sleep duration",
                "duration of sleep",
                "sleeping duration",
                "hours of sleep",
                "minutes of sleep",
                "my sleep time",
                "my sleeping time")) {

            return buildDurationResponse(s);
        }

        // ============================================================
        // 4. SLEEP QUALITY
        // ============================================================

        if (containsAny(q,
                "sleep quality",
                "quality of my sleep",
                "quality score",
                "sleep score",
                "what is my score",
                "my sleep score",
                "how good was my sleep",
                "was my sleep good",
                "is my sleep good",
                "is my sleep healthy",
                "how healthy was my sleep",
                "rate my sleep",
                "evaluate my sleep",
                "sleep performance")) {

            return buildQualityResponse(s);
        }

        // ============================================================
        // 5. QUALITY SCORE WHY
        // ============================================================

        if (containsAny(q,
                "why is my score",
                "why did i get this score",
                "why is my sleep score",
                "why is the score",
                "how is the score calculated",
                "how did you calculate my score",
                "what affects my score",
                "what determines my score",
                "what lowered my score",
                "what improved my score")) {

            return buildScoreExplanation(s);
        }

        // ============================================================
        // 6. DEEP SLEEP
        // ============================================================

        if (containsAny(q,
                "deep sleep",
                "deep stage",
                "deep sleep stage",
                "slow wave sleep",
                "slow-wave sleep",
                "how much deep",
                "how long deep",
                "deep sleep duration",
                "deep sleep percentage",
                "deep sleep percent",
                "deep sleep amount",
                "my deep sleep",
                "explain deep sleep",
                "what is deep sleep")) {

            return buildDeepSleep(s);
        }

        // ============================================================
        // 7. REM SLEEP
        // ============================================================

        if (containsAny(q,
                "rem sleep",
                "rem stage",
                "rem sleep stage",
                "dream sleep",
                "how much rem",
                "how long rem",
                "rem duration",
                "rem percentage",
                "rem percent",
                "rem amount",
                "my rem",
                "explain rem",
                "what is rem")) {

            return buildRemSleep(s);
        }

        // ============================================================
        // 8. LIGHT SLEEP
        // ============================================================

        if (containsAny(q,
                "light sleep",
                "light stage",
                "light sleep stage",
                "how much light sleep",
                "how long light sleep",
                "light sleep duration",
                "light sleep percentage",
                "light sleep percent",
                "my light sleep",
                "explain light sleep",
                "what is light sleep")) {

            return buildLightSleep(s);
        }

        // ============================================================
        // 9. AWAKE TIME
        // ============================================================

        if (containsAny(q,
                "awake time",
                "awake duration",
                "how long was i awake",
                "how much was i awake",
                "how long awake",
                "time awake",
                "minutes awake",
                "awake minutes",
                "wake time",
                "how much awake",
                "was i awake a lot",
                "did i wake up a lot",
                "night awakenings",
                "awakenings")) {

            return buildAwakeResponse(s);
        }

        // ============================================================
        // 10. SLEEP STAGE BREAKDOWN
        // ============================================================

        if (containsAny(q,
                "sleep stages",
                "sleep stage breakdown",
                "breakdown of my sleep",
                "break down my sleep",
                "stages of sleep",
                "what stages did i have",
                "show my sleep stages",
                "explain my sleep stages",
                "how was my sleep divided",
                "sleep distribution",
                "sleep stage distribution")) {

            return buildStageBreakdown(s);
        }

        // ============================================================
        // 11. HEART RATE
        // ============================================================

        if (containsAny(q,
                "heart rate",
                "average heart rate",
                "average hr",
                "my heart rate",
                "my hr",
                "heartbeat",
                "heart beat",
                "pulse",
                "average pulse",
                "how fast was my heart",
                "what was my heart rate",
                "tell me my heart rate")) {

            return buildHeartRate(s);
        }

        // ============================================================
        // 12. MOVEMENT
        // ============================================================

        if (containsAny(q,
                "movement",
                "movement level",
                "motion",
                "motion level",
                "average movement",
                "my movement",
                "body movement",
                "restlessness",
                "restless",
                "was i restless",
                "did i move a lot",
                "how much did i move",
                "movement during sleep")) {

            return buildMovement(s);
        }

        // ============================================================
        // 13. SPO2 / BLOOD OXYGEN
        // ============================================================

        if (containsAny(q,
                "spo2",
                "sp02",
                "oxygen level",
                "blood oxygen",
                "oxygen saturation",
                "oxygen percentage",
                "oxygen reading",
                "my oxygen",
                "my spo2",
                "what was my spo2",
                "tell me my spo2",
                "show my spo2")) {

            return buildSpO2(s);
        }


        // ============================================================
        // 14. SKIN TEMPERATURE
        // ============================================================

        if (containsAny(q,
                "skin temperature",
                "skin temp",
                "skin temperature reading",
                "skin temperature data",
                "body surface temperature",
                "my skin temperature",
                "what was my skin temperature",
                "tell me my temperature",
                "temperature reading")) {

            return buildSkinTemperature(s);
        }


        // ============================================================
        // 15. ALL SENSOR DATA
        // ============================================================

        if (containsAny(q,
                "all sensor data",
                "all my sensor data",
                "all sensor readings",
                "all my sensor readings",
                "show all sensors",
                "show all my sensors",
                "show my sensor data",
                "show my sensor readings",
                "sensor data",
                "sensor readings",
                "health sensor data",
                "health sensor readings")) {

            return buildSensorSummary(s);
        }


        // ============================================================
        // 16. VITALS / BODY METRICS
        // ============================================================

        if (containsAny(q,
                "body metrics",
                "body readings",
                "vital readings",
                "vitals",
                "my vitals",
                "health readings",
                "health metrics",
                "physical metrics",
                "body sensor data")) {

            return buildVitalsAnalysis(s);
        }


        // ============================================================
        // 17. SENSOR COMPARISON
        // ============================================================

        if (containsAny(q,
                "compare my sensors",
                "compare sensor data",
                "compare sensor readings",
                "compare my readings",
                "compare my health metrics",
                "compare the sensors",
                "sensor comparison")) {

            return buildSensorComparison(s);
        }


        // ============================================================
        // 18. SPO2 EXPLANATION
        // ============================================================

        if (containsAny(q,
                "what is spo2",
                "what does spo2 mean",
                "explain spo2",
                "what is oxygen saturation",
                "what does oxygen saturation mean",
                "what is blood oxygen",
                "explain blood oxygen")) {

            return "SpO₂ refers to oxygen saturation, represented as a percentage. In SleepSense, SpO₂ is a simulated value generated by the prototype to demonstrate how an oxygen sensor could be integrated into the sleep-monitoring system. It is not a medical measurement or diagnostic result.";
        }


        // ============================================================
        // 19. SKIN TEMPERATURE EXPLANATION
        // ============================================================

        if (containsAny(q,
                "what is skin temperature",
                "what does skin temperature mean",
                "explain skin temperature",
                "what is skin temp",
                "why measure skin temperature",
                "why track skin temperature")) {

            return "Skin temperature is the temperature measured at the body surface. In SleepSense, it is simulated to demonstrate how a temperature sensor could contribute another signal to the sleep-monitoring dashboard. It should not be interpreted as a clinical body-temperature measurement.";
        }


        // ============================================================
        // 20. SENSOR PURPOSE
        // ============================================================

        if (containsAny(q,
                "what do the sensors measure",
                "what does each sensor measure",
                "what does each sensor do",
                "explain the sensors",
                "explain my sensors",
                "sensor types",
                "types of sensors",
                "what sensors are used")) {

            return "SleepSense uses simulated sensor data representing EEG activity, heart rate, movement, SpO₂, and skin temperature. EEG provides Delta, Theta, Alpha, and Beta values; heart rate provides an average BPM value; movement represents activity or restlessness; SpO₂ represents simulated blood oxygen saturation; and skin temperature represents simulated body-surface temperature.";
        }


        // ============================================================
        // 21. SENSOR + SLEEP ANALYSIS
        // ============================================================

        if (containsAny(q,
                "sensors and sleep",
                "sensor data and sleep",
                "how do sensors help sleep",
                "how sensors help sleep",
                "how does sensor data help",
                "sensor sleep analysis",
                "analyze my sensor data",
                "analyse my sensor data",
                "interpret my sensor data")) {

            return buildSensorSleepAnalysis(s);
        }


        // ============================================================
        // 22. SENSOR SAFETY / SIMULATION
        // ============================================================

        if (containsAny(q,
                "is my spo2 normal",
                "is my oxygen normal",
                "is my temperature normal",
                "is my skin temperature normal",
                "are these sensor readings normal",
                "are my sensor readings normal",
                "is this medical data",
                "is this a medical reading")) {

            return "These values are simulated prototype data, so they should not be used to decide whether a real SpO₂ or temperature reading is normal or abnormal. SleepSense demonstrates the software pipeline and dashboard behavior rather than providing medical diagnosis.";
        }


        // ============================================================
        // 23. EEG GENERAL
        // ============================================================

        if (containsAny(q,
                "what is eeg",
                "what does eeg mean",
                "explain eeg",
                "eeg meaning",
                "tell me about eeg",
                "my eeg",
                "eeg data",
                "eeg reading",
                "brain activity",
                "brain waves",
                "brain wave data")) {

            return buildEEG(s);
        }

        // ============================================================
        // 14. DELTA
        // ============================================================

        if (containsAny(q,
                "delta",
                "delta wave",
                "delta waves",
                "delta band",
                "delta activity",
                "delta power",
                "my delta",
                "what is delta",
                "what does delta mean",
                "explain delta")) {

            return buildDelta(s);
        }

        // ============================================================
        // 15. THETA
        // ============================================================

        if (containsAny(q,
                "theta",
                "theta wave",
                "theta waves",
                "theta band",
                "theta activity",
                "theta power",
                "my theta",
                "what is theta",
                "what does theta mean",
                "explain theta")) {

            return buildTheta(s);
        }

        // ============================================================
        // 16. ALPHA
        // ============================================================

        if (containsAny(q,
                "alpha",
                "alpha wave",
                "alpha waves",
                "alpha band",
                "alpha activity",
                "alpha power",
                "my alpha",
                "what is alpha",
                "what does alpha mean",
                "explain alpha")) {

            return buildAlpha(s);
        }

        // ============================================================
        // 17. BETA
        // ============================================================

        if (containsAny(q,
                "beta",
                "beta wave",
                "beta waves",
                "beta band",
                "beta activity",
                "beta power",
                "my beta",
                "what is beta",
                "what does beta mean",
                "explain beta")) {

            return buildBeta(s);
        }

        // ============================================================
        // 18. EEG COMPARISON
        // ============================================================

        if (containsAny(q,
                "compare eeg",
                "compare the eeg",
                "compare brain waves",
                "compare delta theta",
                "delta theta alpha beta",
                "difference between eeg bands",
                "difference between brain waves",
                "difference between delta theta alpha beta",
                "all eeg bands",
                "all brain waves",
                "which eeg is highest",
                "highest eeg",
                "highest brain wave",
                "lowest eeg",
                "lowest brain wave")) {

            return buildEEGComparison(s);
        }

        // ============================================================
        // 19. SLEEP STAGE EDUCATION
        // ============================================================

        if (containsAny(q,
                "why do we sleep",
                "why is sleep important",
                "why sleep is important",
                "importance of sleep",
                "benefits of sleep",
                "why do humans sleep",
                "why should i sleep",
                "what happens during sleep",
                "what happens when we sleep",
                "sleep benefits")) {

            return "Sleep supports several important functions, including physical recovery, memory and learning, emotional regulation, and normal daily functioning. Your SleepSense system looks at sleep stages together with EEG, heart-rate, and movement data to create a broader picture of a sleep session.";
        }

        // ============================================================
        // 20. IMPROVEMENT
        // ============================================================

        if (containsAny(q,
                "improve",
                "improvement",
                "how can i sleep better",
                "how to sleep better",
                "what should i improve",
                "what can i improve",
                "how can i improve my sleep",
                "give me advice",
                "give me suggestions",
                "recommendations",
                "sleep recommendations",
                "sleep advice",
                "tips for better sleep",
                "tips to sleep better",
                "what should i do",
                "help me sleep better")) {

            return buildImprovementAdvice(s);
        }

        // ============================================================
        // 21. SLEEP SCHEDULE
        // ============================================================

        if (containsAny(q,
                "sleep schedule",
                "sleep routine",
                "bedtime",
                "wake up time",
                "wake time",
                "consistent sleep",
                "consistent schedule",
                "sleep timing",
                "when should i sleep",
                "when should i go to bed",
                "regular sleep",
                "sleep pattern")) {

            return "A consistent sleep and wake schedule can help maintain a stable sleep routine. Try to keep your bedtime and wake-up time reasonably consistent, including on weekends. Your dashboard can then be used to observe whether your sleep metrics change over time.";
        }

        // ============================================================
        // 22. SCREEN / PHONE
        // ============================================================

        if (containsAny(q,
                "phone",
                "mobile",
                "screen",
                "screens",
                "laptop",
                "computer before bed",
                "screen time",
                "phone before sleep",
                "phone before bed")) {

            return "Stimulating screen use close to bedtime can make it harder for some people to wind down. Consider reducing phone, laptop, and other stimulating activities before bed and keeping your sleeping environment comfortable and dark.";
        }

        // ============================================================
        // 23. CAFFEINE
        // ============================================================

        if (containsAny(q,
                "caffeine",
                "coffee",
                "tea",
                "energy drink",
                "caffeinated",
                "coffee before bed",
                "caffeine before sleep")) {

            return "Caffeine can interfere with sleep for some people, especially when consumed later in the day. If you notice a connection between caffeine and poorer sleep, consider limiting caffeine several hours before bedtime.";
        }

        // ============================================================
        // 24. SLEEP ENVIRONMENT
        // ============================================================

        if (containsAny(q,
                "sleep environment",
                "room",
                "bedroom",
                "dark room",
                "temperature",
                "noise",
                "sleeping environment",
                "room temperature",
                "quiet room")) {

            return "A comfortable sleep environment can support better rest. A dark, quiet, and comfortable room is generally helpful. If noise, light, or temperature regularly interrupts your sleep, adjusting the environment may reduce disturbances.";
        }

        // ============================================================
        // 25. SLEEP CONSISTENCY
        // ============================================================

        if (containsAny(q,
                "consistent sleep",
                "sleep consistency",
                "regular bedtime",
                "regular sleep",
                "same bedtime",
                "same wake time",
                "sleep discipline")) {

            return "Consistency is an important part of a healthy sleep routine. Keeping sleep and wake times reasonably stable helps your body maintain a predictable sleep schedule.";
        }

        // ============================================================
        // 26. PERSONALIZED ANALYSIS
        // ============================================================

        if (containsAny(q,
                "personal analysis",
                "personalized analysis",
                "personalised analysis",
                "analyze my data",
                "analyse my data",
                "interpret my data",
                "interpret my sleep",
                "what does my data mean",
                "what does my sleep data mean",
                "tell me what is wrong",
                "what is wrong with my sleep",
                "what is affecting my sleep",
                "analyze this session",
                "analyse this session")) {

            return buildPersonalizedAnalysis(s);
        }

        // ============================================================
        // 27. WHAT IS GOOD
        // ============================================================

        if (containsAny(q,
                "what is good about my sleep",
                "what did i do well",
                "what looks good",
                "good things about my sleep",
                "positive things about my sleep",
                "strengths of my sleep",
                "what are my strengths")) {

            return buildPositiveAnalysis(s);
        }

        // ============================================================
        // 28. WHAT NEEDS IMPROVEMENT
        // ============================================================

        if (containsAny(q,
                "what is bad about my sleep",
                "what needs improvement",
                "what should i work on",
                "weaknesses",
                "problem with my sleep",
                "problems with my sleep",
                "what is poor",
                "what is low",
                "what needs to improve")) {

            return buildWeaknessAnalysis(s);
        }

        // ============================================================
        // 29. PERCENTAGES
        // ============================================================

        if (containsAny(q,
                "percentage",
                "percent",
                "percentages",
                "what percentage",
                "sleep percentage",
                "how much percent",
                "distribution percentage")) {

            return buildPercentages(s);
        }

        // ============================================================
        // 30. LONGEST STAGE
        // ============================================================

        if (containsAny(q,
                "longest stage",
                "most sleep",
                "which stage was longest",
                "which stage was the longest",
                "stage with most time",
                "most time in sleep")) {

            return buildLongestStage(s);
        }

        // ============================================================
        // 31. SHORTEST STAGE
        // ============================================================

        if (containsAny(q,
                "shortest stage",
                "least sleep",
                "which stage was shortest",
                "which stage was the shortest",
                "stage with least time",
                "least time in sleep")) {

            return buildShortestStage(s);
        }

        // ============================================================
        // 32. DEEP VS REM
        // ============================================================

        if (containsAny(q,
                "deep vs rem",
                "deep versus rem",
                "compare deep and rem",
                "compare deep sleep and rem",
                "more deep or rem",
                "which is more deep or rem")) {

            return buildDeepVsRem(s);
        }

        // ============================================================
        // 33. AWAKE VS SLEEP
        // ============================================================

        if (containsAny(q,
                "awake compared",
                "awake vs sleep",
                "awake versus sleep",
                "sleep compared to awake",
                "how much was i awake compared")) {

            return buildAwakeComparison(s);
        }

        // ============================================================
        // 34. MOVEMENT + AWAKE
        // ============================================================

        if (containsAny(q,
                "movement and awake",
                "movement affect",
                "movement affecting",
                "restlessness and awake",
                "movement related to waking",
                "does movement affect sleep")) {

            return buildMovementAwakeAnalysis(s);
        }

        // ============================================================
        // 35. HEART RATE + SLEEP
        // ============================================================

        if (containsAny(q,
                "heart rate and sleep",
                "heart rate affect sleep",
                "heart rate during sleep",
                "heart rate analysis",
                "analyze my heart rate",
                "analyse my heart rate")) {

            return buildHeartSleepAnalysis(s);
        }

        // ============================================================
        // 36. EEG + SLEEP
        // ============================================================

        if (containsAny(q,
                "eeg and sleep",
                "eeg affect sleep",
                "brain waves and sleep",
                "eeg sleep analysis",
                "analyze eeg",
                "analyse eeg")) {

            return buildEEGSleepAnalysis(s);
        }

        // ============================================================
        // 37. DATA EXPLANATION
        // ============================================================

        if (containsAny(q,
                "explain my data",
                "explain the dashboard",
                "explain these numbers",
                "explain the numbers",
                "what do these numbers mean",
                "what do these metrics mean",
                "explain the metrics",
                "explain my metrics")) {

            return buildDataExplanation(s);
        }

        // ============================================================
        // 38. DASHBOARD
        // ============================================================

        if (containsAny(q,
                "dashboard",
                "what does dashboard show",
                "what does this dashboard do",
                "how does dashboard work",
                "explain dashboard",
                "dashboard metrics")) {

            return "The SleepSense dashboard presents your current sleep session using several measurements: total sleep duration, sleep-stage distribution, quality score, average heart rate, movement level, simulated SpO₂, simulated skin temperature, and EEG frequency bands. The purpose is to combine these measurements into an understandable sleep summary.";
        }

        // ============================================================
        // 39. WEARABLE
        // ============================================================

        if (containsAny(q,
                "wearable",
                "wearable device",
                "how does wearable work",
                "how does the wearable work",
                "sleep wearable",
                "wearable sensor",
                "sensors")) {

            return "A wearable sleep-monitoring system can collect signals such as EEG-related activity, heart rate, movement, SpO₂, and skin temperature. In this prototype, the data is simulated so the Java backend and analysis pipeline can be demonstrated without requiring physical hardware.";
        }

        // ============================================================
        // 40. PROTOTYPE
        // ============================================================

        if (containsAny(q,
                "prototype",
                "is this real data",
                "real data",
                "dummy data",
                "simulated data",
                "simulation",
                "is this simulated")) {

            return "This SleepSense prototype currently uses simulated sleep-session data. The Java backend processes that data through the same general analysis flow that can later receive readings from wearable sensors.";
        }

        // ============================================================
        // 41. DATA PRIVACY
        // ============================================================

        if (containsAny(q,
                "privacy",
                "private data",
                "my data safe",
                "data security",
                "secure",
                "security")) {

            return "Sleep and health-related information should be handled carefully because it can be sensitive. In this prototype, the application is designed as a local demonstration system. A production version would require stronger privacy, authentication, encryption, access control, and data-protection measures.";
        }

        // ============================================================
        // 42. FUTURE HARDWARE
        // ============================================================

        if (containsAny(q,
                "real wearable",
                "real sensor",
                "real eeg",
                "future hardware",
                "hardware integration",
                "connect sensor",
                "connect wearable")) {

            return "The current prototype uses simulated data. A future version could connect actual EEG, heart-rate, and motion sensors, after which the Java backend could process incoming measurements using the same analysis architecture.";
        }

        // ============================================================
        // 43. HEALTH GENERAL
        // ============================================================

        if (containsAny(q,
                "healthy sleep",
                "healthy sleeping",
                "good sleep habits",
                "sleep habits",
                "sleep hygiene",
                "healthy sleep habits")) {

            return "Healthy sleep habits generally include a consistent sleep schedule, a comfortable and dark sleeping environment, limiting stimulating activity close to bedtime, and allowing enough time for sleep. Your SleepSense dashboard can help you observe your sleep metrics and identify patterns.";
        }

        // ============================================================
        // 44. STUDENT SLEEP
        // ============================================================

        if (containsAny(q,
                "student sleep",
                "college sleep",
                "college student sleep",
                "student sleeping",
                "study and sleep",
                "sleep while studying",
                "sleep for students")) {

            return "Students often have changing schedules, academic workload, screen use, and irregular sleep timing. Maintaining a reasonably consistent sleep schedule and allowing enough time for sleep can help support concentration, learning, and daily functioning.";
        }

        // ============================================================
        // 45. MEMORY AND LEARNING
        // ============================================================

        if (containsAny(q,
                "sleep and memory",
                "sleep and learning",
                "sleep helps memory",
                "does sleep improve memory",
                "sleep for studying",
                "sleep and concentration",
                "sleep and focus")) {

            return "Sleep plays an important role in memory and learning. Adequate sleep can support processes involved in retaining and organizing information, which is particularly relevant for students.";
        }

        // ============================================================
        // 46. EXERCISE
        // ============================================================

        if (containsAny(q,
                "exercise and sleep",
                "exercise affect sleep",
                "workout and sleep",
                "exercise before bed",
                "physical activity and sleep")) {

            return "Regular physical activity can support healthy sleep for many people. However, intense exercise immediately before bedtime may not feel comfortable for everyone, so timing can be adjusted based on individual response.";
        }

        // ============================================================
        // 47. NAP
        // ============================================================

        if (containsAny(q,
                "nap",
                "napping",
                "daytime sleep",
                "afternoon sleep",
                "should i nap")) {

            return "Short naps can be useful for some people, but long or late naps may make it harder to sleep at night. If nighttime sleep is already difficult, paying attention to nap timing may be useful.";
        }

        // ============================================================
        // 48. WEEKEND SLEEP
        // ============================================================

        if (containsAny(q,
                "weekend sleep",
                "sleep on weekends",
                "weekend schedule",
                "sleep late on weekends")) {

            return "Large changes in bedtime and wake time between weekdays and weekends can make a sleep routine less consistent. Keeping the schedule reasonably stable can make it easier to maintain a predictable routine.";
        }

        // ============================================================
        // 49. STRESS
        // ============================================================

        if (containsAny(q,
                "stress and sleep",
                "stress affect sleep",
                "stress affecting sleep",
                "sleep and stress",
                "study stress and sleep")) {

            return "Stress can make it harder for some people to relax and fall asleep. A consistent bedtime routine, reducing stimulating activity before bed, and using relaxing wind-down activities may help.";
        }

        // ============================================================
        // 50. RELAXATION
        // ============================================================

        if (containsAny(q,
                "relax before bed",
                "how to relax",
                "relaxation",
                "relax before sleep",
                "wind down",
                "wind-down")) {

            return "A calm wind-down routine can help prepare for sleep. Examples include reducing screen use, keeping lighting comfortable, reading something relaxing, and maintaining a consistent bedtime routine.";
        }

        // ============================================================
        // 51. NIGHT ROUTINE
        // ============================================================

        if (containsAny(q,
                "night routine",
                "nighttime routine",
                "before bed routine",
                "bedtime routine",
                "sleep routine")) {

            return "A simple night routine could include finishing stimulating work, reducing screen activity, preparing a comfortable sleeping environment, and going to bed at a reasonably consistent time.";
        }

        // ============================================================
        // 52. BEST METRIC
        // ============================================================

        if (containsAny(q,
                "most important metric",
                "most important sleep metric",
                "which metric matters most",
                "important metric",
                "best sleep metric")) {

            return "No single metric completely describes sleep quality. Sleep duration, sleep stages, awake time, movement, heart rate, and EEG-related information can provide different pieces of the picture. SleepSense combines them rather than relying on only one value.";
        }

        // ============================================================
        // 53. WHICH METRIC IS AFFECTING SCORE
        // ============================================================

        if (containsAny(q,
                "which metric affects my score",
                "which metric lowered my score",
                "metric affecting my score",
                "what affects my quality score",
                "why is my quality score")) {

            return buildScoreExplanation(s);
        }

        // ============================================================
        // 54. CURRENT SESSION
        // ============================================================

        if (containsAny(q,
                "current session",
                "this session",
                "current sleep",
                "latest sleep",
                "latest session",
                "last sleep")) {

            return buildCompleteReport(s);
        }

        // ============================================================
        // 55. DATA REFRESH / NEW NIGHT
        // ============================================================

        if (containsAny(q,
                "new night",
                "new session",
                "generate data",
                "generate new data",
                "new sleep data",
                "refresh sleep")) {

            return "Use the \"Generate new night\" button on the dashboard to create a new simulated sleep session. After the dashboard refreshes, I will analyze the newly generated session.";
        }

        // ============================================================
        // 56. HELP
        // ============================================================

        if (containsAny(q,
                "help",
                "what can you do",
                "what can i ask",
                "questions i can ask",
                "what questions can i ask",
                "commands",
                "features")) {

            return "You can ask me about your sleep report, sleep duration, quality score, deep sleep, REM, light sleep, awake time, sleep stages, heart rate, movement, SpO₂, skin temperature, EEG, Delta, Theta, Alpha, Beta, sensor readings, percentages, comparisons, personalized analysis, sleep habits, screens, caffeine, sleep schedule, and ways to improve your sleep.";
        }

        // ============================================================
        // 57. THANKS
        // ============================================================

        if (containsAny(q,
                "thanks",
                "thank you",
                "thankyou",
                "thx",
                "great thanks",
                "thank")) {

            return "You're welcome! If you want, you can ask me another question about your current SleepSense session.";
        }

        // ============================================================
        // FALLBACK
        // ============================================================

        return "I can help analyze your current sleep session. Try asking something like: \"Explain my sleep report\", \"Why is my sleep score this way?\", \"How much deep sleep did I get?\", \"What does Delta mean?\", \"Compare my deep and REM sleep\", \"What is affecting my score?\", or \"What should I improve?\"";
    }


    // ================================================================
    // COMPLETE REPORT
    // ================================================================

    private String buildCompleteReport(SleepSession s) {

        double hours = s.getTotalMinutes() / 60.0;

        double awakePercent = percent(s.getAwakeMinutes(), s.getTotalMinutes());
        double lightPercent = percent(s.getLightMinutes(), s.getTotalMinutes());
        double deepPercent = percent(s.getDeepMinutes(), s.getTotalMinutes());
        double remPercent = percent(s.getRemMinutes(), s.getTotalMinutes());

        return String.format(
                Locale.ROOT,
                "Here is your current SleepSense report: You recorded %.1f hours of sleep with a quality score of %d/100. Your session contained %d minutes awake (%.1f%%), %d minutes of light sleep (%.1f%%), %d minutes of deep sleep (%.1f%%), and %d minutes of REM sleep (%.1f%%). Your average heart rate was %d BPM and your average movement level was %.2f. Your simulated SpO₂ was %.1f%% and skin temperature was %.1f °C. Your EEG values were Delta %.2f, Theta %.2f, Alpha %.2f, and Beta %.2f.",
                hours,
                s.getQualityScore(),
                s.getAwakeMinutes(),
                awakePercent,
                s.getLightMinutes(),
                lightPercent,
                s.getDeepMinutes(),
                deepPercent,
                s.getRemMinutes(),
                remPercent,
                s.getAverageHeartRate(),
                s.getAverageMovement(),
                s.getSpo2(),
                s.getSkinTemperature(),
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower()
        );
    }


    // ================================================================
    // DURATION
    // ================================================================

    private String buildDurationResponse(SleepSession s) {

        double hours = s.getTotalMinutes() / 60.0;
        int hoursPart = s.getTotalMinutes() / 60;
        int minutesPart = s.getTotalMinutes() % 60;

        return String.format(
                Locale.ROOT,
                "Your recorded sleep duration was %.1f hours, or %d hours and %d minutes. The total session duration is %d minutes.",
                hours,
                hoursPart,
                minutesPart,
                s.getTotalMinutes()
        );
    }


    // ================================================================
    // QUALITY
    // ================================================================

    private String buildQualityResponse(SleepSession s) {

        int score = s.getQualityScore();

        if (score >= 80) {
            return String.format(
                    Locale.ROOT,
                    "Your SleepSense quality score is %d/100. The prototype classifies this as a strong session. You recorded %d minutes of deep sleep, %d minutes of REM sleep, and %d minutes awake.",
                    score,
                    s.getDeepMinutes(),
                    s.getRemMinutes(),
                    s.getAwakeMinutes()
            );
        }

        if (score >= 60) {
            return String.format(
                    Locale.ROOT,
                    "Your SleepSense quality score is %d/100. This is a moderate result in the prototype's scoring system. Your session contained %d minutes awake, %d minutes of deep sleep, and %d minutes of REM sleep.",
                    score,
                    s.getAwakeMinutes(),
                    s.getDeepMinutes(),
                    s.getRemMinutes()
            );
        }

        return String.format(
                Locale.ROOT,
                "Your SleepSense quality score is %d/100. The prototype's calculation suggests there are some areas to improve. Your session recorded %d minutes awake and %d minutes of deep sleep.",
                score,
                s.getAwakeMinutes(),
                s.getDeepMinutes()
        );
    }


    // ================================================================
    // SCORE EXPLANATION
    // ================================================================

    private String buildScoreExplanation(SleepSession s) {

        StringBuilder response = new StringBuilder();

        response.append("Your quality score is ")
                .append(s.getQualityScore())
                .append("/100. ");

        response.append("The prototype's scoring calculation considers the recorded sleep duration, awake time, deep sleep, and REM sleep. ");

        if (s.getAwakeMinutes() > 60) {
            response.append("Your awake time is relatively high at ")
                    .append(s.getAwakeMinutes())
                    .append(" minutes, which can reduce the quality score. ");
        }

        if (s.getDeepMinutes() < 60) {
            response.append("Your deep sleep is ")
                    .append(s.getDeepMinutes())
                    .append(" minutes, which is another area the prototype considers. ");
        }

        if (s.getRemMinutes() < 60) {
            response.append("Your REM sleep is ")
                    .append(s.getRemMinutes())
                    .append(" minutes. ");
        }

        if (s.getTotalMinutes() >= 420) {
            response.append("Your total session duration is at least seven hours, which contributes positively to the duration component.");
        }

        return response.toString();
    }


    // ================================================================
    // DEEP SLEEP
    // ================================================================

    private String buildDeepSleep(SleepSession s) {

        double percentage = percent(
                s.getDeepMinutes(),
                s.getTotalMinutes()
        );

        return String.format(
                Locale.ROOT,
                "You recorded %d minutes of deep sleep, which is %.1f%% of your session. Deep sleep is a slow-wave NREM sleep stage associated with physical restoration. Your prototype also records a Delta value of %.2f, which represents the slow-frequency EEG activity shown in the dashboard.",
                s.getDeepMinutes(),
                percentage,
                s.getDeltaPower()
        );
    }


    // ================================================================
    // REM
    // ================================================================

    private String buildRemSleep(SleepSession s) {

        double percentage = percent(
                s.getRemMinutes(),
                s.getTotalMinutes()
        );

        return String.format(
                Locale.ROOT,
                "You recorded %d minutes of REM sleep, which is %.1f%% of the session. REM is associated with vivid dreaming and processes related to learning and memory. SleepSense tracks REM as one of the main sleep-stage categories.",
                s.getRemMinutes(),
                percentage
        );
    }


    // ================================================================
    // LIGHT SLEEP
    // ================================================================

    private String buildLightSleep(SleepSession s) {

        double percentage = percent(
                s.getLightMinutes(),
                s.getTotalMinutes()
        );

        return String.format(
                Locale.ROOT,
                "You recorded %d minutes of light sleep, which is %.1f%% of the session. Light sleep is a relatively lighter stage of sleep and makes up a substantial portion of many sleep periods.",
                s.getLightMinutes(),
                percentage
        );
    }


    // ================================================================
    // AWAKE
    // ================================================================

    private String buildAwakeResponse(SleepSession s) {

        double percentage = percent(
                s.getAwakeMinutes(),
                s.getTotalMinutes()
        );

        String interpretation;

        if (percentage < 8) {
            interpretation = "That represents a relatively small portion of the recorded session.";
        } else if (percentage < 15) {
            interpretation = "That represents a moderate portion of the recorded session.";
        } else {
            interpretation = "That represents a relatively large portion of the recorded session and may indicate a more fragmented night.";
        }

        return String.format(
                Locale.ROOT,
                "You were recorded as awake for %d minutes, or %.1f%% of the session. %s",
                s.getAwakeMinutes(),
                percentage,
                interpretation
        );
    }


    // ================================================================
    // STAGE BREAKDOWN
    // ================================================================

    private String buildStageBreakdown(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your sleep-stage breakdown is: Awake %d minutes, Light %d minutes, Deep %d minutes, and REM %d minutes. Together these values describe how the recorded session was distributed across the main categories used by the SleepSense prototype.",
                s.getAwakeMinutes(),
                s.getLightMinutes(),
                s.getDeepMinutes(),
                s.getRemMinutes()
        );
    }


    // ================================================================
    // HEART RATE
    // ================================================================

    private String buildHeartRate(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your average recorded heart rate was %d BPM. Heart rate can naturally vary throughout sleep and between individuals. SleepSense displays this value alongside sleep stages, movement, and EEG information rather than treating heart rate alone as a complete measure of sleep quality.",
                s.getAverageHeartRate()
        );
    }


    // ================================================================
    // MOVEMENT
    // ================================================================

    private String buildMovement(SleepSession s) {

        double movement = s.getAverageMovement();

        String interpretation;

        if (movement < 0.20) {
            interpretation = "The recorded movement level is relatively low, suggesting a comparatively settled session.";
        } else if (movement < 0.40) {
            interpretation = "The recorded movement level is moderate.";
        } else {
            interpretation = "The recorded movement level is relatively high and may indicate greater restlessness.";
        }

        return String.format(
                Locale.ROOT,
                "Your average movement value was %.2f. %s Movement is best interpreted together with awake time and the sleep-stage distribution.",
                movement,
                interpretation
        );
    }


    // ================================================================
    // EEG
    // ================================================================

    private String buildEEG(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "EEG stands for Electroencephalography and refers to measuring electrical activity of the brain. Your SleepSense prototype represents EEG information using four frequency bands: Delta %.2f, Theta %.2f, Alpha %.2f, and Beta %.2f. These values are visualized on the dashboard.",
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower()
        );
    }


    // ================================================================
    // DELTA
    // ================================================================

    private String buildDelta(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your Delta value is %.2f. Delta represents slow-frequency EEG activity and is strongly associated with deep slow-wave sleep. Your dashboard can be interpreted by considering this value together with your recorded %d minutes of deep sleep.",
                s.getDeltaPower(),
                s.getDeepMinutes()
        );
    }


    // ================================================================
    // THETA
    // ================================================================

    private String buildTheta(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your Theta value is %.2f. Theta represents slower-frequency brain activity commonly associated with lighter sleep and transitions between sleep states. In your prototype, it is one of the four EEG values displayed.",
                s.getThetaPower()
        );
    }


    // ================================================================
    // ALPHA
    // ================================================================

    private String buildAlpha(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your Alpha value is %.2f. Alpha activity is commonly associated with relaxed wakefulness and can also appear around transitions into sleep. SleepSense displays it as part of your overall EEG profile.",
                s.getAlphaPower()
        );
    }


    // ================================================================
    // BETA
    // ================================================================

    private String buildBeta(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your Beta value is %.2f. Beta represents relatively faster EEG activity and is commonly associated with alert mental activity. In SleepSense, it is displayed alongside Delta, Theta, and Alpha.",
                s.getBetaPower()
        );
    }


    // ================================================================
    // EEG COMPARISON
    // ================================================================

    private String buildEEGComparison(SleepSession s) {

        String highest = "Delta";
        double highestValue = s.getDeltaPower();

        if (s.getThetaPower() > highestValue) {
            highest = "Theta";
            highestValue = s.getThetaPower();
        }

        if (s.getAlphaPower() > highestValue) {
            highest = "Alpha";
            highestValue = s.getAlphaPower();
        }

        if (s.getBetaPower() > highestValue) {
            highest = "Beta";
            highestValue = s.getBetaPower();
        }

        String lowest = "Delta";
        double lowestValue = s.getDeltaPower();

        if (s.getThetaPower() < lowestValue) {
            lowest = "Theta";
            lowestValue = s.getThetaPower();
        }

        if (s.getAlphaPower() < lowestValue) {
            lowest = "Alpha";
            lowestValue = s.getAlphaPower();
        }

        if (s.getBetaPower() < lowestValue) {
            lowest = "Beta";
            lowestValue = s.getBetaPower();
        }

        return String.format(
                Locale.ROOT,
                "Your EEG values are Delta %.2f, Theta %.2f, Alpha %.2f, and Beta %.2f. In this session, %s has the highest recorded value at %.2f, while %s has the lowest at %.2f. Delta is associated with slower activity and deep sleep, Theta with lighter sleep and transitions, Alpha with relaxed wakefulness, and Beta with faster activity.",
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower(),
                highest,
                highestValue,
                lowest,
                lowestValue
        );
    }


    // ================================================================
    // PERSONALIZED ANALYSIS
    // ================================================================

    private String buildPersonalizedAnalysis(SleepSession s) {

        StringBuilder result = new StringBuilder();

        result.append("Based on your current SleepSense session: ");

        result.append("your quality score is ")
                .append(s.getQualityScore())
                .append("/100. ");

        result.append("You recorded ")
                .append(s.getTotalMinutes())
                .append(" minutes in the session, including ")
                .append(s.getDeepMinutes())
                .append(" minutes of deep sleep and ")
                .append(s.getRemMinutes())
                .append(" minutes of REM sleep. ");

        if (s.getAwakeMinutes() > 60) {
            result.append("Your awake time is relatively high at ")
                    .append(s.getAwakeMinutes())
                    .append(" minutes, so sleep continuity is an area to watch. ");
        } else {
            result.append("Your awake time is relatively limited in this session. ");
        }

        if (s.getAverageMovement() > 0.35) {
            result.append("Movement is relatively elevated, suggesting a more restless session. ");
        } else {
            result.append("Movement is relatively low or moderate, suggesting a more settled session. ");
        }

        if (s.getDeepMinutes() < 60) {
            result.append("Your recorded deep sleep is below 60 minutes, so maintaining a consistent sleep routine may be useful.");
        } else {
            result.append("Your recorded deep sleep is at least 60 minutes and forms a meaningful part of the session.");
        }

        result.append(" The additional simulated sensor readings are SpO₂ ")
                .append(String.format(Locale.ROOT, "%.1f", s.getSpo2()))
                .append("% and skin temperature ")
                .append(String.format(Locale.ROOT, "%.1f", s.getSkinTemperature()))
                .append(" °C. These values are prototype sensor data and should be interpreted together with the other measurements.");

        return result.toString();
    }


    // ================================================================
    // POSITIVE ANALYSIS
    // ================================================================

    private String buildPositiveAnalysis(SleepSession s) {

        StringBuilder result = new StringBuilder();

        result.append("Some positive aspects of this session are: ");

        boolean added = false;

        if (s.getTotalMinutes() >= 420) {
            result.append("your total recorded duration was at least seven hours");
            added = true;
        }

        if (s.getAwakeMinutes() < 60) {
            if (added) result.append("; ");
            result.append("your awake time was below 60 minutes");
            added = true;
        }

        if (s.getDeepMinutes() >= 60) {
            if (added) result.append("; ");
            result.append("you recorded at least 60 minutes of deep sleep");
            added = true;
        }

        if (s.getAverageMovement() < 0.35) {
            if (added) result.append("; ");
            result.append("your movement level was relatively controlled");
            added = true;
        }

        if (!added) {
            result.append("the dashboard provides several useful measurements that can be tracked over multiple sessions.");
        }

        return result.toString() + ".";
    }


    // ================================================================
    // WEAKNESS ANALYSIS
    // ================================================================

    private String buildWeaknessAnalysis(SleepSession s) {

        StringBuilder result = new StringBuilder();

        result.append("Areas to pay attention to in this session: ");

        boolean added = false;

        if (s.getAwakeMinutes() > 60) {
            result.append("awake time is ")
                    .append(s.getAwakeMinutes())
                    .append(" minutes");
            added = true;
        }

        if (s.getDeepMinutes() < 60) {
            if (added) result.append("; ");
            result.append("deep sleep is ")
                    .append(s.getDeepMinutes())
                    .append(" minutes");
            added = true;
        }

        if (s.getAverageMovement() > 0.35) {
            if (added) result.append("; ");
            result.append("movement is relatively high at ")
                    .append(String.format(Locale.ROOT, "%.2f", s.getAverageMovement()));
            added = true;
        }

        if (s.getTotalMinutes() < 420) {
            if (added) result.append("; ");
            result.append("total sleep duration is below seven hours");
            added = true;
        }

        if (!added) {
            result.append("no major issue is flagged by the simple prototype rules for this session");
        }

        return result.toString() + ".";
    }


    // ================================================================
    // IMPROVEMENT ADVICE
    // ================================================================

    private String buildImprovementAdvice(SleepSession s) {

        StringBuilder advice = new StringBuilder();

        advice.append("Based on your current session, I would focus on: ");

        boolean added = false;

        if (s.getTotalMinutes() < 420) {
            advice.append("allowing enough time for a full night's sleep");
            added = true;
        }

        if (s.getAwakeMinutes() > 60) {
            if (added) advice.append("; ");
            advice.append("reducing factors that may interrupt your sleep because awake time was ")
                    .append(s.getAwakeMinutes())
                    .append(" minutes");
            added = true;
        }

        if (s.getDeepMinutes() < 60) {
            if (added) advice.append("; ");
            advice.append("maintaining a consistent sleep schedule and a relaxing bedtime routine");
            added = true;
        }

        if (s.getAverageMovement() > 0.35) {
            if (added) advice.append("; ");
            advice.append("checking your sleep environment and factors that may contribute to restlessness");
            added = true;
        }

        if (!added) {
            advice.append("continue maintaining a consistent sleep schedule and monitor your results across multiple nights");
        }

        advice.append(". General habits such as reducing stimulating screen use before bed and avoiding late caffeine may also help.");

        return advice.toString();
    }


    // ================================================================
    // PERCENTAGES
    // ================================================================

    private String buildPercentages(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your session distribution is: Awake %.1f%%, Light Sleep %.1f%%, Deep Sleep %.1f%%, and REM Sleep %.1f%%.",
                percent(s.getAwakeMinutes(), s.getTotalMinutes()),
                percent(s.getLightMinutes(), s.getTotalMinutes()),
                percent(s.getDeepMinutes(), s.getTotalMinutes()),
                percent(s.getRemMinutes(), s.getTotalMinutes())
        );
    }


    // ================================================================
    // LONGEST STAGE
    // ================================================================

    private String buildLongestStage(SleepSession s) {

        String stage = "Awake";
        int value = s.getAwakeMinutes();

        if (s.getLightMinutes() > value) {
            stage = "Light Sleep";
            value = s.getLightMinutes();
        }

        if (s.getDeepMinutes() > value) {
            stage = "Deep Sleep";
            value = s.getDeepMinutes();
        }

        if (s.getRemMinutes() > value) {
            stage = "REM Sleep";
            value = s.getRemMinutes();
        }

        return "The longest recorded category in your current session was " +
                stage + " at " + value + " minutes.";
    }


    // ================================================================
    // SHORTEST STAGE
    // ================================================================

    private String buildShortestStage(SleepSession s) {

        String stage = "Awake";
        int value = s.getAwakeMinutes();

        if (s.getLightMinutes() < value) {
            stage = "Light Sleep";
            value = s.getLightMinutes();
        }

        if (s.getDeepMinutes() < value) {
            stage = "Deep Sleep";
            value = s.getDeepMinutes();
        }

        if (s.getRemMinutes() < value) {
            stage = "REM Sleep";
            value = s.getRemMinutes();
        }

        return "The shortest recorded category in your current session was " +
                stage + " at " + value + " minutes.";
    }


    // ================================================================
    // DEEP VS REM
    // ================================================================

    private String buildDeepVsRem(SleepSession s) {

        int difference = Math.abs(
                s.getDeepMinutes() - s.getRemMinutes()
        );

        if (s.getDeepMinutes() > s.getRemMinutes()) {
            return "You recorded more deep sleep than REM sleep. Deep sleep was " +
                    s.getDeepMinutes() + " minutes, while REM sleep was " +
                    s.getRemMinutes() + " minutes, a difference of " +
                    difference + " minutes.";
        }

        if (s.getRemMinutes() > s.getDeepMinutes()) {
            return "You recorded more REM sleep than deep sleep. REM sleep was " +
                    s.getRemMinutes() + " minutes, while deep sleep was " +
                    s.getDeepMinutes() + " minutes, a difference of " +
                    difference + " minutes.";
        }

        return "Your deep sleep and REM sleep durations were equal at " +
                s.getDeepMinutes() + " minutes.";
    }


    // ================================================================
    // AWAKE COMPARISON
    // ================================================================

    private String buildAwakeComparison(SleepSession s) {

        int sleepingMinutes =
                s.getLightMinutes() +
                s.getDeepMinutes() +
                s.getRemMinutes();

        return String.format(
                Locale.ROOT,
                "You recorded %d minutes awake and %d minutes across Light, Deep, and REM sleep combined. That means approximately %.1f%% of your recorded session was spent in sleep stages.",
                s.getAwakeMinutes(),
                sleepingMinutes,
                percent(sleepingMinutes, s.getTotalMinutes())
        );
    }


    // ================================================================
    // MOVEMENT + AWAKE
    // ================================================================

    private String buildMovementAwakeAnalysis(SleepSession s) {

        String interpretation;

        if (s.getAverageMovement() > 0.35 &&
                s.getAwakeMinutes() > 60) {

            interpretation = "Both movement and awake time are relatively high in this session, so the data suggests a more restless and fragmented night.";
        } else if (s.getAverageMovement() > 0.35) {

            interpretation = "Movement is relatively high, although your awake time is not extremely high.";
        } else if (s.getAwakeMinutes() > 60) {

            interpretation = "Awake time is relatively high, while movement is not particularly high.";
        } else {

            interpretation = "Both movement and awake time are relatively controlled in this session.";
        }

        return String.format(
                Locale.ROOT,
                "Your movement value is %.2f and your awake time is %d minutes. %s",
                s.getAverageMovement(),
                s.getAwakeMinutes(),
                interpretation
        );
    }


    // ================================================================
    // HEART RATE + SLEEP
    // ================================================================

    private String buildHeartSleepAnalysis(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your average heart rate was %d BPM. SleepSense treats heart rate as a supporting metric rather than using it alone to determine sleep quality. It should be considered alongside your sleep duration, stages, movement, and EEG information.",
                s.getAverageHeartRate()
        );
    }


    // ================================================================
    // EEG + SLEEP
    // ================================================================

    private String buildEEGSleepAnalysis(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Your EEG profile is Delta %.2f, Theta %.2f, Alpha %.2f, and Beta %.2f. Your session also contains %d minutes of deep sleep and %d minutes of REM sleep. Delta is particularly relevant when discussing slow-wave/deep sleep, while the other bands provide additional information about brain activity.",
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower(),
                s.getDeepMinutes(),
                s.getRemMinutes()
        );
    }


    // ================================================================
    // SPO2 RESPONSE
    // ================================================================

    private String buildSpO2(SleepSession s) {

        double spo2 = s.getSpo2();

        String interpretation;

        if (spo2 >= 97.0) {
            interpretation =
                    "This simulated value is in the higher part of the prototype's generated range.";
        } else if (spo2 >= 95.0) {
            interpretation =
                    "This simulated value is within the prototype's generated range.";
        } else {
            interpretation =
                    "This is a simulated prototype value and should not be treated as a medical measurement.";
        }

        return String.format(
                Locale.ROOT,
                "Your simulated SpO₂ was %.1f%%. %s SleepSense currently uses simulated values to demonstrate how an SpO₂ sensor could be integrated into the software.",
                spo2,
                interpretation
        );
    }


    // ================================================================
    // SKIN TEMPERATURE RESPONSE
    // ================================================================

    private String buildSkinTemperature(SleepSession s) {

        double temperature = s.getSkinTemperature();

        return String.format(
                Locale.ROOT,
                "Your simulated skin temperature for this sleep session was %.1f °C. This represents body-surface temperature in the prototype and should not be interpreted as a clinical body-temperature measurement.",
                temperature
        );
    }


    // ================================================================
    // ALL SENSOR DATA
    // ================================================================

    private String buildSensorSummary(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "Here are your simulated sensor readings for this sleep session: Heart Rate %d BPM, Movement %.2f, SpO₂ %.1f%%, Skin Temperature %.1f °C, EEG Delta %.2f, Theta %.2f, Alpha %.2f, and Beta %.2f.",
                s.getAverageHeartRate(),
                s.getAverageMovement(),
                s.getSpo2(),
                s.getSkinTemperature(),
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower()
        );
    }


    // ================================================================
    // VITALS / BODY METRICS
    // ================================================================

    private String buildVitalsAnalysis(SleepSession s) {

        StringBuilder response = new StringBuilder();

        response.append("Your current simulated body-related readings are: ")
                .append("heart rate ")
                .append(s.getAverageHeartRate())
                .append(" BPM, SpO₂ ")
                .append(String.format(Locale.ROOT, "%.1f", s.getSpo2()))
                .append("%, skin temperature ")
                .append(String.format(Locale.ROOT, "%.1f", s.getSkinTemperature()))
                .append(" °C, and movement ")
                .append(String.format(Locale.ROOT, "%.2f", s.getAverageMovement()))
                .append(". ");

        response.append("These values are prototype sensor data and are intended to demonstrate how multiple signals can be displayed and analyzed together.");

        return response.toString();
    }


    // ================================================================
    // SENSOR COMPARISON
    // ================================================================

    private String buildSensorComparison(SleepSession s) {

        String oxygenDescription;

        if (s.getSpo2() >= 97.0) {
            oxygenDescription = "SpO₂ is in the higher part of the simulated range";
        } else if (s.getSpo2() >= 95.0) {
            oxygenDescription = "SpO₂ is in the middle of the simulated range";
        } else {
            oxygenDescription = "SpO₂ is toward the lower end of the simulated range";
        }

        String movementDescription;

        if (s.getAverageMovement() < 0.20) {
            movementDescription = "movement is relatively low";
        } else if (s.getAverageMovement() < 0.40) {
            movementDescription = "movement is moderate";
        } else {
            movementDescription = "movement is relatively high";
        }

        return String.format(
                Locale.ROOT,
                "Comparing your simulated sensor readings: heart rate is %d BPM, %s at %.1f%%, skin temperature is %.1f °C, and %s at %.2f. The EEG profile is Delta %.2f, Theta %.2f, Alpha %.2f, and Beta %.2f.",
                s.getAverageHeartRate(),
                oxygenDescription,
                s.getSpo2(),
                s.getSkinTemperature(),
                movementDescription,
                s.getAverageMovement(),
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower()
        );
    }


    // ================================================================
    // SENSOR + SLEEP ANALYSIS
    // ================================================================

    private String buildSensorSleepAnalysis(SleepSession s) {

        StringBuilder result = new StringBuilder();

        result.append("Looking at your simulated sensor data together with the sleep session: ");

        result.append("your average heart rate was ")
                .append(s.getAverageHeartRate())
                .append(" BPM, movement was ")
                .append(String.format(Locale.ROOT, "%.2f", s.getAverageMovement()))
                .append(", SpO₂ was ")
                .append(String.format(Locale.ROOT, "%.1f", s.getSpo2()))
                .append("%, and skin temperature was ")
                .append(String.format(Locale.ROOT, "%.1f", s.getSkinTemperature()))
                .append(" °C. ");

        result.append("The EEG profile was Delta ")
                .append(String.format(Locale.ROOT, "%.2f", s.getDeltaPower()))
                .append(", Theta ")
                .append(String.format(Locale.ROOT, "%.2f", s.getThetaPower()))
                .append(", Alpha ")
                .append(String.format(Locale.ROOT, "%.2f", s.getAlphaPower()))
                .append(", and Beta ")
                .append(String.format(Locale.ROOT, "%.2f", s.getBetaPower()))
                .append(". ");

        result.append("These measurements provide different types of information, so the prototype combines them rather than relying on one sensor alone.");

        return result.toString();
    }


    // ================================================================
    // DATA EXPLANATION
    // ================================================================

    private String buildDataExplanation(SleepSession s) {

        return String.format(
                Locale.ROOT,
                "The dashboard is showing several types of information. Sleep duration: %d minutes. Sleep stages: Awake %d, Light %d, Deep %d, REM %d minutes. Average heart rate: %d BPM. Movement: %.2f. SpO₂: %.1f%%. Skin temperature: %.1f °C. EEG: Delta %.2f, Theta %.2f, Alpha %.2f, Beta %.2f. Quality score: %d/100.",
                s.getTotalMinutes(),
                s.getAwakeMinutes(),
                s.getLightMinutes(),
                s.getDeepMinutes(),
                s.getRemMinutes(),
                s.getAverageHeartRate(),
                s.getAverageMovement(),
                s.getSpo2(),
                s.getSkinTemperature(),
                s.getDeltaPower(),
                s.getThetaPower(),
                s.getAlphaPower(),
                s.getBetaPower(),
                s.getQualityScore()
        );
    }


    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private double percent(int value, int total) {

        if (total <= 0) {
            return 0.0;
        }

        return value * 100.0 / total;
    }


    private boolean containsAny(String text, String... words) {

        for (String word : words) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }
}
