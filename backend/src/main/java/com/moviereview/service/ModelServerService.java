package com.moviereview.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ModelServerService {

    @Autowired
    private LambdaModelService lambdaModelService;

    private boolean modelServerConnected = true; // For admin simulation

    /**
     * Analyze sentiment of review text using AWS Lambda
     */
    public SentimentResult analyzeSentiment(String reviewText) {
        if (!modelServerConnected) {
            throw new ModelServerException("Model server connection is disabled (admin simulation)");
        }

        try {
            LambdaModelService.SentimentResult lambdaResult = lambdaModelService.analyzeSentiment(reviewText);
            return new SentimentResult(lambdaResult.getSentiment(), lambdaResult.getScore(), lambdaResult.getRating());
        } catch (LambdaModelService.ModelServerException e) {
            throw new ModelServerException(e.getMessage());
        }
    }

    /**
     * Check if Lambda function is available
     */
    public boolean isModelServerAvailable() {
        if (!modelServerConnected) {
            return false;
        }
        return lambdaModelService.isLambdaAvailable();
    }

    /**
     * Admin function to toggle model server connection simulation
     */
    public void toggleModelConnection() {
        modelServerConnected = !modelServerConnected;
        System.out.println("🔧 Model server connection toggled: " + (modelServerConnected ? "ENABLED" : "DISABLED"));
        
        // Also toggle Lambda connection
        lambdaModelService.toggleLambdaConnection();
    }

    /**
     * Get current model server connection status
     */
    public boolean isModelConnectionEnabled() {
        return modelServerConnected;
    }

    /**
     * Get current server configuration (Lambda only)
     */
    public Map<String, Object> getServerConfig() {
        Map<String, Object> lambdaConfig = lambdaModelService.getLambdaConfig();
        return Map.of(
            "mode", "lambda",
            "connected", modelServerConnected,
            "lambda", lambdaConfig
        );
    }

    /**
     * Sentiment analysis result with rating
     */
    public static class SentimentResult {
        private final String sentiment;
        private final Double score;
        private final Double rating;

        public SentimentResult(String sentiment, Double score, Double rating) {
            this.sentiment = sentiment;
            this.score = score;
            this.rating = rating;
        }

        public String getSentiment() {
            return sentiment;
        }

        public Double getScore() {
            return score;
        }

        public Double getRating() {
            return rating;
        }
    }

    /**
     * Exception for model server errors
     */
    public static class ModelServerException extends RuntimeException {
        public ModelServerException(String message) {
            super(message);
        }
    }
} 