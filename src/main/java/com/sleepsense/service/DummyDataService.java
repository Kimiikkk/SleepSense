package com.sleepsense.service;

import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import com.sleepsense.repository.SleepSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class DummyDataService {

    private final SleepSessionRepository repository;
    private final SleepDataAnalyzer analyzer;
    private final Random random = new Random();

    public DummyDataService(
            SleepSessionRepository repository,
            SleepDataAnalyzer analyzer) {

        this.repository = repository;
        this.analyzer = analyzer;
    }

    public SleepSession generate(User user) {

        /*
         * Randomly select a sleep pattern.
         *
         * 0 = Good night
         * 1 = Average night
         * 2 = Restless night
         */

        int nightType = random.nextInt(3);

        int awake;
        int light;
        int deep;
        int rem;

        double movement;
        int hr;

        double delta;
        double theta;
        double alpha;
        double beta;


        // =====================================================
        // GOOD NIGHT
        // =====================================================

        if (nightType == 0) {

            awake = 15 + random.nextInt(20);

            light = 210 + random.nextInt(50);

            deep = 90 + random.nextInt(40);

            rem = 85 + random.nextInt(40);

            movement = 0.08 + random.nextDouble() * 0.12;

            hr = 52 + random.nextInt(7);

            delta = 0.65 + random.nextDouble() * 0.15;

            theta = 0.20 + random.nextDouble() * 0.10;

            alpha = 0.07 + random.nextDouble() * 0.08;

            beta = 0.04 + random.nextDouble() * 0.06;
        }


        // =====================================================
        // AVERAGE NIGHT
        // =====================================================

        else if (nightType == 1) {

            awake = 25 + random.nextInt(30);

            light = 230 + random.nextInt(55);

            deep = 65 + random.nextInt(35);

            rem = 65 + random.nextInt(35);

            movement = 0.15 + random.nextDouble() * 0.20;

            hr = 56 + random.nextInt(9);

            delta = 0.50 + random.nextDouble() * 0.18;

            theta = 0.18 + random.nextDouble() * 0.14;

            alpha = 0.08 + random.nextDouble() * 0.12;

            beta = 0.05 + random.nextDouble() * 0.10;
        }


        // =====================================================
        // RESTLESS NIGHT
        // =====================================================

        else {

            awake = 55 + random.nextInt(45);

            light = 250 + random.nextInt(50);

            deep = 35 + random.nextInt(35);

            rem = 45 + random.nextInt(35);

            movement = 0.30 + random.nextDouble() * 0.30;

            hr = 62 + random.nextInt(11);

            delta = 0.35 + random.nextDouble() * 0.15;

            theta = 0.25 + random.nextDouble() * 0.15;

            alpha = 0.12 + random.nextDouble() * 0.12;

            beta = 0.08 + random.nextDouble() * 0.12;
        }


        // =====================================================
        // TOTAL SESSION
        // =====================================================

        int total = awake + light + deep + rem;


        // =====================================================
        // QUALITY SCORE
        // =====================================================

        int quality =
                analyzer.calculateQuality(
                        total,
                        awake,
                        deep,
                        rem
                );


        // Keep the score within 0–100.
        quality = Math.max(0, Math.min(100, quality));


        // =====================================================
        // SESSION TIME
        // =====================================================

        LocalDateTime end =
                LocalDateTime.now()
                        .withSecond(0)
                        .withNano(0);

        LocalDateTime start =
                end.minusMinutes(total);


        // =====================================================
        // SAVE SESSION
        // =====================================================

        return repository.save(
                new SleepSession(
                        user,
                        start,
                        end,
                        total,
                        awake,
                        light,
                        deep,
                        rem,
                        hr,
                        movement,
                        delta,
                        theta,
                        alpha,
                        beta,
                        quality
                )
        );
    }
}