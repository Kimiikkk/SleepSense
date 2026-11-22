package com.sleepsense.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** A simulated or measured sleep session stored in Cloud Firestore. */
public class SleepSession {
    private String id;
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
    private String userId;
    private transient User user;

    public SleepSession() {
        this.id = UUID.randomUUID().toString();
    }

    public SleepSession(User user, LocalDateTime startTime, LocalDateTime endTime,
                        int totalMinutes, int awakeMinutes, int lightMinutes,
                        int deepMinutes, int remMinutes, int averageHeartRate,
                        double averageMovement, double spo2, double skinTemperature,
                        double deltaPower, double thetaPower,
                        double alphaPower, double betaPower, int qualityScore) {
        this();
        setUser(user);
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

    public String getId() { return id; }
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
    public double getSpo2() { return spo2; }
    public double getSkinTemperature() { return skinTemperature; }
    public String getUserId() { return userId; }
    public User getUser() { return user; }

    public void setId(String id) { this.id = id; }
    public void setUser(User user) {
        this.user = user;
        this.userId = user == null ? null : user.getId();
    }
    public void setUserId(String userId) { this.userId = userId; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
    public void setAwakeMinutes(int awakeMinutes) { this.awakeMinutes = awakeMinutes; }
    public void setLightMinutes(int lightMinutes) { this.lightMinutes = lightMinutes; }
    public void setDeepMinutes(int deepMinutes) { this.deepMinutes = deepMinutes; }
    public void setRemMinutes(int remMinutes) { this.remMinutes = remMinutes; }
    public void setAverageHeartRate(int averageHeartRate) { this.averageHeartRate = averageHeartRate; }
    public void setAverageMovement(double averageMovement) { this.averageMovement = averageMovement; }
    public void setSpo2(double spo2) { this.spo2 = spo2; }
    public void setSkinTemperature(double skinTemperature) { this.skinTemperature = skinTemperature; }
    public void setDeltaPower(double deltaPower) { this.deltaPower = deltaPower; }
    public void setThetaPower(double thetaPower) { this.thetaPower = thetaPower; }
    public void setAlphaPower(double alphaPower) { this.alphaPower = alphaPower; }
    public void setBetaPower(double betaPower) { this.betaPower = betaPower; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
}
