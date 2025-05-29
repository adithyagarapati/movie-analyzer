package com.movieanalyzer.backend.service;

import com.movieanalyzer.backend.dto.AnalysisResponse;
import com.movieanalyzer.backend.dto.ReviewHistoryItem;
import com.movieanalyzer.backend.dto.ReviewRequest;
import com.movieanalyzer.backend.entity.MovieReview;
import com.movieanalyzer.backend.exception.SimulatedDbUnavailableException;
import com.movieanalyzer.backend.repository.MovieReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional; // REMOVE THIS

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final MovieReviewRepository reviewRepository;
    private final ModelServiceClient modelServiceClient;

    @Autowired
    public ReviewService(MovieReviewRepository reviewRepository, ModelServiceClient modelServiceClient) {
        this.reviewRepository = reviewRepository;
        this.modelServiceClient = modelServiceClient;
    }

    // REMOVED @Transactional from here
    public AnalysisResponse analyzeAndSaveReview(ReviewRequest reviewRequest) {
        logger.info("Received review for analysis: {}", reviewRequest.getMovie_name());

        // 1. Call Model Service (This will throw ModelServiceUnavailableException if it fails)
        AnalysisResponse modelAnalysis = modelServiceClient.getAnalysis(reviewRequest.getReview_text());
        logger.info("Model service analysis complete. Sentiment: {}, Rating: {}", modelAnalysis.getSentiment(), modelAnalysis.getPseudo_rating());

        // 2. Attempt to save to DB, but don't let DB failure stop the response if model call succeeded
        if (AppBehaviorState.isDatabaseInteractionEnabled()) {
            try {
                MovieReview movieReview = new MovieReview(
                        reviewRequest.getMovie_name(),
                        reviewRequest.getReview_text(),
                        modelAnalysis.getSentiment(),
                        modelAnalysis.getPseudo_rating()
                );
                // For this simple save, Spring Data JPA might handle transactionality implicitly
                // or it will simply try to get a connection.
                reviewRepository.save(movieReview);
                logger.info("Review saved to database successfully for movie: {}", reviewRequest.getMovie_name());
            } catch (DataAccessException e) { // This will catch errors if save() tries to get a connection and fails
                logger.warn("DATABASE WARN: Failed to save review to DB for movie: {}. Error: {}. Analysis result will still be returned.",
                        reviewRequest.getMovie_name(), e.getMessage());
                // Do not re-throw; allow the analysis to be returned to the user
            } catch (Exception e) { // Catch any other unexpected error during the save attempt
                 logger.error("UNEXPECTED ERROR during DB save attempt for movie: {}. Error: {}. Analysis result will still be returned.",
                        reviewRequest.getMovie_name(), e.getMessage(), e);
            }
        } else {
            logger.warn("DATABASE INFO: Database interaction is disabled by API toggle. Skipping save for movie: {}", reviewRequest.getMovie_name());
        }

        return modelAnalysis; // Return the analysis from the model service
    }

    public List<ReviewHistoryItem> getReviewHistory(String movieName) {
        // ... (this method remains the same as the last correct version) ...
        logger.info("Fetching review history for movie: {}", movieName);
        if (!AppBehaviorState.isDatabaseInteractionEnabled()) {
            logger.warn("DATABASE INFO: Database interaction is disabled by API toggle. Throwing error for /reviews_history/{}", movieName);
            throw new SimulatedDbUnavailableException("Database interaction is disabled via API for fetching history.");
        }
        try {
            List<MovieReview> reviews = reviewRepository.findTop10ByMovieNameOrderByCreatedAtDesc(movieName);
            return reviews.stream()
                    .map(review -> new ReviewHistoryItem(
                            review.getMovieName(),
                            review.getReviewText(),
                            review.getPredictedSentiment(),
                            review.getPseudoRating(),
                            review.getCreatedAt()))
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("DATABASE ERROR: Failed to fetch review history from DB for movie: {}. Error: {}", movieName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public double performCpuIntensiveTask(int iterations) {
        // ... (this method remains the same) ...
        logger.info("Performing CPU intensive task with iterations: {}", iterations);
        double result = 0;
        for (int i = 0; i < iterations; i++) {
            result += Math.log(Math.abs(Math.sin(i) * Math.cos(i / (Math.PI / 2)) + Math.exp(Math.sin(i/(double)(iterations > 0 ? iterations : 1 ))) ) +1) * Math.sqrt(i + 1.0);
            if ( (i > 0 && iterations > 10 && i % (iterations / 10) == 0) || (iterations <=10 && i > 0) ) {
                 logger.debug("CPU task progress: {}% complete", String.format("%.0f", (double)i*100/iterations) );
            }
        }
        logger.info("CPU intensive task completed. Result: {}", result);
        return result;
    }
}