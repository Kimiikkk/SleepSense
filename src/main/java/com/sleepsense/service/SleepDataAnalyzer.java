package com.sleepsense.service;

import com.sleepsense.model.SleepStage;
import org.springframework.stereotype.Service;

@Service
public class SleepDataAnalyzer {

    public SleepStage classifyStage(double delta, double theta, double alpha, double beta, double movement) {
        if (movement > 0.70 || beta > 0.45) return SleepStage.AWAKE;
        if (delta > 0.55 && movement < 0.30) return SleepStage.DEEP;
        if (theta > 0.30) return SleepStage.LIGHT;
        if (alpha > 0.25 && beta < 0.30) return SleepStage.REM;
        return SleepStage.LIGHT;
    }

    public int calculateQuality(int totalMinutes, int awakeMinutes, int deepMinutes, int remMinutes) {
        double efficiency = totalMinutes == 0 ? 0 : (double)(totalMinutes - awakeMinutes) / totalMinutes;
        double score = 55 + efficiency * 25;
        if (totalMinutes >= 420 && totalMinutes <= 540) score += 8;
        if (deepMinutes >= 60) score += 6;
        if (remMinutes >= 60) score += 6;
        return (int)Math.max(0, Math.min(100, Math.round(score)));
    }
}
