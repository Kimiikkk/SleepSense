package com.sleepsense.model;

import java.time.Instant;
import java.time.LocalDate;

public record SleepAchievement(String code, String title, String description, LocalDate earnedOn, Instant earnedAt) {}
