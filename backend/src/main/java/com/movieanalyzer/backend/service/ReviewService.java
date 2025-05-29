package com.movieanalyzer.backend.service;

import com.movieanalyzer.backend.dto.AnalysisResponse;
import com.movieanalyzer.backend.dto.ReviewHistoryItem;
import com.movieanalyzer.backend.dto.ReviewRequest;
import com.movieanalyzer.backend.entity.MovieReview;
import com.movieanalyzer.backend.exception.SimulatedDbUnavailableException; // Ensure this import is present
import com.movieanalyzer.backend.repository.MovieReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException; // Ensure this import is present
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections; // Ensure this import is present
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

    @Transactional
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
                reviewRepository.save(movieReview);
                logger.info("Review saved to database successfully for movie: {}", reviewRequest.getMovie_name());
            } catch (DataAccessException e) {
                logger.warn("DATABASE WARN: Failed to save review to DB for movie: {}. Error: {}. Analysis result will still be returned.",
                        reviewRequest.getMovie_name(), e.getMessage());
                // Do not re-throw; allow the analysis to be returned to the user
            }
        } else {
            logger.warn("DATABASE INFO: Database interaction is disabled by API toggle. Skipping save for movie: {}", reviewRequest.getMovie_name());
            // This also means the analysis result will be returned without saving.
        }

        return modelAnalysis; // Return the analysis from the model service
    }

    public List<ReviewHistoryItem> getReviewHistory(String movieName) {
        logger.info("Fetching review history for movie: {}", movieName);

        // Check the debug toggle first
        if (!AppBehaviorState.isDatabaseInteractionEnabled()) {
            logger.warn("DATABASE INFO: Database interaction is disabled by API toggle. Throwing error for /reviews_history/{}", movieName);
            // This will make the endpoint return an error (handled by GlobalExceptionHandler),
            // and helps demonstrate the readiness probe failing if it relies on this state.
            throw new SimulatedDbUnavailableException("Database interaction is disabled via API for fetching history.");
        }

        // If debug toggle allows DB interaction, proceed to try and fetch from DB
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
            // This catches actual DB connection failures if the debug toggle was true
            // but the database is genuinely down.
            logger.error("DATABASE ERROR: Failed to fetch review history from DB for movie: {}. Error: {}", movieName, e.getMessage());
            // Gracefully return empty list to the user in case of actual DB error,
            // but the readiness probe (checking dataSource.getConnection()) would have already caught this.
            return Collections.emptyList();
        }
    }

    // Method for CPU intensive task
    public double performCpuIntensiveTask(int iterations) {
        logger.info("Performing CPU intensive task with iterations: {}", iterations);
        double result = 0;
        for (int i = 0; i < iterations; i++) {
            // A slightly more complex calculation to ensure CPU usage
            result += Math.log(Math.abs(Math.sin(i) * Math.cos(i / (Math.PI / 2)) + Math.exp(Math.sin(i/(double)(iterations > 0 ? iterations : 1 ))) ) +1) * Math.sqrt(i + 1.0);
            if ( (i > 0 && iterations > 10 && i % (iterations / 10) == 0) || (iterations <=10 && i > 0) ) {
                 logger.debug("CPU task progress: {}% complete", String.format("%.0f", (double)i*100/iterations) );
            }
        }
        logger.info("CPU intensive task completed. Result: {}", result);
        return result;
    }
}