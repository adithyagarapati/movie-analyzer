package com.moviereview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.core.SdkBytes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class LambdaModelService {

    private String functionName;
    private String region;
    private int timeoutMs;
    private String authMethod; // "iam" or "keys"

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private boolean lambdaConnected = true; // For admin simulation

    public LambdaModelService() {
        // Read directly from environment variables 
        this.functionName = System.getenv("LAMBDA_FUNCTION_NAME");
        if (this.functionName == null) this.functionName = "movie-analyzer-sentiment";
        
        this.region = System.getenv("LAMBDA_REGION");
        if (this.region == null) this.region = "ap-south-1";
        
        String timeoutStr = System.getenv("LAMBDA_TIMEOUT");
        this.timeoutMs = timeoutStr != null ? Integer.parseInt(timeoutStr) : 30000;
        
        this.authMethod = System.getenv("LAMBDA_AUTH_METHOD");
        if (this.authMethod == null) this.authMethod = "iam";
        
        this.objectMapper = new ObjectMapper();
        this.lambdaClient = createLambdaClient(this.region);
        System.out.println("🔐 Lambda authentication method: " + authMethod);
    }

    private LambdaClient createLambdaClient(String region) {
        try {
            var clientBuilder = LambdaClient.builder().region(Region.of(region));
            
            // Configure credentials based on auth method
            switch (authMethod.toLowerCase()) {
                case "keys":
                    System.out.println("🔑 Using AWS access keys authentication");
                    clientBuilder.credentialsProvider(
                        software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider.create()
                    );
                    break;
                    
                case "iam":
                default:
                    System.out.println("🔑 Using IAM role authentication (IRSA/Instance Profile)");
                    clientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
                    break;
            }
            
            LambdaClient client = clientBuilder.build();
            
            // Test credentials by checking if we can access AWS STS
            try {
                software.amazon.awssdk.services.sts.StsClient stsClient = software.amazon.awssdk.services.sts.StsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(client.serviceClientConfiguration().credentialsProvider())
                    .build();
                    
                var identity = stsClient.getCallerIdentity();
                System.out.println("✅ AWS authentication successful - Account: " + identity.account() + ", User/Role: " + identity.arn());
                stsClient.close();
            } catch (Exception e) {
                System.err.println("⚠️ Could not verify AWS credentials: " + e.getMessage());
            }
            
            return client;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to create Lambda client: " + e.getMessage());
            throw new RuntimeException("Failed to initialize AWS Lambda client", e);
        }
    }

    /**
     * Analyze sentiment of review text using Lambda function
     */
    public SentimentResult analyzeSentiment(String reviewText) {
        if (!lambdaConnected) {
            throw new ModelServerException("Lambda function connection is disabled (admin simulation)");
        }

        try {
            System.out.println("🔗 Invoking Lambda function for sentiment analysis: " + reviewText.substring(0, Math.min(50, reviewText.length())) + "...");
            
            // Prepare Lambda payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "analyze");
            payload.put("text", reviewText);
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            // Invoke Lambda function
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromString(payloadJson, StandardCharsets.UTF_8))
                    .build();

            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            
            // Parse response
            String responseBody = response.payload().asString(StandardCharsets.UTF_8);
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            // Check if Lambda returned an error
            if (response.statusCode() != 200) {
                System.err.println("❌ Lambda function returned error status: " + response.statusCode());
                throw new ModelServerException("Lambda function returned error status: " + response.statusCode());
            }
            
            // Parse Lambda response body (it's wrapped in statusCode/body format)
            if (responseMap.containsKey("body")) {
                String bodyJson = (String) responseMap.get("body");
                Map<String, Object> bodyMap = objectMapper.readValue(bodyJson, new TypeReference<Map<String, Object>>() {});
                
                if (bodyMap.containsKey("sentiment")) {
                    String sentiment = (String) bodyMap.get("sentiment");
                    Double score = bodyMap.get("score") != null ? 
                        Double.valueOf(bodyMap.get("score").toString()) : 0.0;
                    Double rating = bodyMap.get("rating") != null ? 
                        Double.valueOf(bodyMap.get("rating").toString()) : 3.0;
                    
                    System.out.println("✅ Lambda function response: " + sentiment + " (score: " + score + ", rating: " + rating + " stars)");
                    return new SentimentResult(sentiment, score, rating);
                } else {
                    System.err.println("❌ Invalid response from Lambda function: " + bodyJson);
                    throw new ModelServerException("Invalid response from Lambda function");
                }
            } else {
                System.err.println("❌ Lambda function response missing body: " + responseBody);
                throw new ModelServerException("Invalid Lambda function response format");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to invoke Lambda function: " + e.getMessage());
            throw new ModelServerException("Lambda function is down - analysis cannot be done at this moment: " + e.getMessage());
        }
    }

    /**
     * Check if Lambda function is available
     */
    public boolean isLambdaAvailable() {
        if (!lambdaConnected) {
            return false;
        }

        try {
            System.out.println("🔍 Checking Lambda function health...");
            
            // Prepare health check payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "health");
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            // Invoke Lambda function
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromString(payloadJson, StandardCharsets.UTF_8))
                    .build();

            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            
            if (response.statusCode() != 200) {
                System.err.println("❌ Lambda function health check failed with status: " + response.statusCode());
                return false;
            }
            
            // Parse response
            String responseBody = response.payload().asString(StandardCharsets.UTF_8);
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            if (responseMap.containsKey("body")) {
                String bodyJson = (String) responseMap.get("body");
                Map<String, Object> bodyMap = objectMapper.readValue(bodyJson, new TypeReference<Map<String, Object>>() {});
                
                boolean available = bodyMap != null && "healthy".equals(bodyMap.get("status"));
                System.out.println(available ? "✅ Lambda function is healthy" : "❌ Lambda function is unhealthy");
                return available;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Lambda function health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Admin function to toggle Lambda connection simulation
     */
    public void toggleLambdaConnection() {
        lambdaConnected = !lambdaConnected;
        System.out.println("🔧 Lambda function connection toggled: " + (lambdaConnected ? "ENABLED" : "DISABLED"));
    }

    /**
     * Get current Lambda connection status
     */
    public boolean isLambdaConnectionEnabled() {
        return lambdaConnected;
    }

    /**
     * Get Lambda function configuration
     */
    public Map<String, Object> getLambdaConfig() {
        return Map.of(
            "functionName", functionName,
            "region", region,
            "timeout", timeoutMs,
            "connected", lambdaConnected
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
     * Exception for Lambda function errors
     */
    public static class ModelServerException extends RuntimeException {
        public ModelServerException(String message) {
            super(message);
        }
    }
} 