package com.movieanalyzer.backend.service;

import com.movieanalyzer.backend.dto.AnalysisResponse;
import com.movieanalyzer.backend.exception.ModelServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
public class ModelServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ModelServiceClient.class);

    private final RestTemplate restTemplate;
    private final String modelServiceUrl;

    public ModelServiceClient(RestTemplate restTemplate, @Value("${model.service.url}") String modelServiceUrl) {
        this.restTemplate = restTemplate;
        this.modelServiceUrl = modelServiceUrl + (modelServiceUrl.endsWith("/") ? "predict" : "/predict");
    }

    public AnalysisResponse getAnalysis(String reviewText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // The Model service expects: {"review_text": "some review"}
        // But our Python model service expects: {"sex": int, "age": int, "fare": int, "familysize": int}
        // THIS IS A MISMATCH FROM THE ORIGINAL TITANIC MODEL.
        // For the MOVIE REVIEW app, the model service should expect a review text.
        // Let's assume the model service for reviews expects: {"review_text": "some text"}
        // and returns {"sentiment": "Positive", "pseudo_rating": 8}

        // CORRECTED PAYLOAD FOR A HYPOTHETICAL MOVIE REVIEW MODEL:
        Map<String, String> requestBody = Collections.singletonMap("review_text", reviewText);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        logger.info("Calling Model Service at URL: {} with text: {}", modelServiceUrl, reviewText.substring(0, Math.min(reviewText.length(), 50)) + "...");

        try {
            ResponseEntity<AnalysisResponse> response = restTemplate.postForEntity(modelServiceUrl, request, AnalysisResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Model Service call successful. Sentiment: {}, Rating: {}",
                        response.getBody().getSentiment(), response.getBody().getPseudo_rating());
                return response.getBody();
            } else {
                logger.error("Model Service returned non-2xx status: {} or empty body", response.getStatusCode());
                throw new ModelServiceUnavailableException("Model service returned an invalid response: " + response.getStatusCode());
            }
        } catch (ResourceAccessException e) { // Handles connection refused, timeouts
            logger.error("Error accessing Model Service (ResourceAccessException): {}", modelServiceUrl, e);
            throw new ModelServiceUnavailableException("Could not connect to the Model Service. Please try again later.", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) { // Handles 4xx and 5xx errors from model service
            logger.error("Error from Model Service (HTTP {}): {}. Response: {}", e.getStatusCode(), modelServiceUrl, e.getResponseBodyAsString(), e);
            throw new ModelServiceUnavailableException("Model Service returned an error: " + e.getStatusCode() + ". " + e.getResponseBodyAsString(), e);
        } catch (Exception e) { // Catch any other unexpected exceptions
            logger.error("Unexpected error calling Model Service: {}", modelServiceUrl, e);
            throw new ModelServiceUnavailableException("An unexpected error occurred while communicating with the Model Service.", e);
        }
    }
}