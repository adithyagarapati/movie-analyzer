package com.movieanalyzer.backend.exception;

public class SimulatedDbUnavailableException extends RuntimeException {
    public SimulatedDbUnavailableException(String message) {
        super(message);
    }
}