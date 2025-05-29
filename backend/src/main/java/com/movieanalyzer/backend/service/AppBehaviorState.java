package com.movieanalyzer.backend.service;

// Simple in-memory state holder for debug/demo purposes
public class AppBehaviorState {
    // --- Database Interaction Toggle ---
    private static boolean databaseInteractionEnabled = true;

    public static synchronized boolean isDatabaseInteractionEnabled() {
        return databaseInteractionEnabled;
    }
    public static synchronized void setDatabaseInteractionEnabled(boolean enabled) {
        databaseInteractionEnabled = enabled;
    }


    // --- CPU Load per Request Toggle ---
    private static boolean triggerCpuLoadPerRequest = false;

    public static synchronized boolean isTriggerCpuLoadPerRequest() {
        return triggerCpuLoadPerRequest;
    }
    public static synchronized void setTriggerCpuLoadPerRequest(boolean enabled) {
        triggerCpuLoadPerRequest = enabled;
    }
}