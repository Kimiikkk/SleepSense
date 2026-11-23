package com.sleepsense.service;

/** Provider boundary for an optional external language model. */
public interface SleepCoachProvider {
    String generate(String instruction, String userInput) throws Exception;
}
