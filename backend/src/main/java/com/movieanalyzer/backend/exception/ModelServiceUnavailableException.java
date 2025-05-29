package com.movieanalyzer.backend.exception;

public class ModelServiceUnavailableException extends RuntimeException {
    public ModelServiceUnavailableException(String message) {
        super(message);
    }

    public ModelServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}