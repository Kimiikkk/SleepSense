package com.sleepsense.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SleepSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalMinutes;
    private int awakeMinutes;
    private int lightMinutes;
    private int deepMinutes;
    private int remMinutes;
    private int averageHeartRate;
    private double averageMovement;
    private double spo2;
    private double skinTemperature;
    private double deltaPower;
    private double thetaPower;
    private double alphaPower;
    private double betaPower;
    private int qualityScore;

    @ManyToOne(optional = false)
    private User user;

    public SleepSession() {}

    public SleepSession(User user, LocalDateTime startTime, LocalDateTime endTime,
                         int totalMinutes, int awakeMinutes, int lightMinutes,
                         int deepMinutes, int remMinutes, int averageHeartRate,
                         double averageMovement, double spo2, double skinTemperature,
                         double deltaPower, double thetaPower,
                         double alphaPower, double betaPower, int qualityScore) {
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalMinutes = totalMinutes;
        this.awakeMinutes = awakeMinutes;
        this.lightMinutes = lightMinutes;
        this.deepMinutes = deepMinutes;
        this.remMinutes = remMinutes;       
        this.averageHeartRate = averageHeartRate;
        this.averageMovement = averageMovement;
        this.spo2 = spo2;
        this.skinTemperature = skinTemperature;
        this.deltaPower = deltaPower;
        this.thetaPower = thetaPower;
        this.alphaPower = alphaPower;
        this.betaPower = betaPower;
        this.qualityScore = qualityScore;
    }

    public Long getId() { return id; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public int getTotalMinutes() { return totalMinutes; }
    public int getAwakeMinutes() { return awakeMinutes; }
    public int getLightMinutes() { return lightMinutes; }
    public int getDeepMinutes() { return deepMinutes; }
    public int getRemMinutes() { return remMinutes; }
    public int getAverageHeartRate() { return averageHeartRate; }
    public double getAverageMovement() { return averageMovement; }
    public double getDeltaPower() { return deltaPower; }
    public double getThetaPower() { return thetaPower; }
    public double getAlphaPower() { return alphaPower; }
    public double getBetaPower() { return betaPower; }
    public int getQualityScore() { return qualityScore; }
    public double getSpo2() {return spo2;}
    public double getSkinTemperature(){return skinTemperature;}
}
