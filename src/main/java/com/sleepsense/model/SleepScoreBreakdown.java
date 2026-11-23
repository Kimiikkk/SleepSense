package com.sleepsense.model;

import java.util.List;

/** Explainable score and its component contributions for one sleep session. */
public record SleepScoreBreakdown(
        int score,
        String category,
        Integer previousScore,
        Integer difference,
        boolean provisional,
        List<Factor> factors) {

    public SleepScoreBreakdown {
        score = Math.max(0, Math.min(100, score));
        factors = factors == null ? List.of() : List.copyOf(factors);
    }

    public record Factor(
            String metric,
            String label,
            int points,
            int weight,
            String direction,
            String explanation) {}
}
