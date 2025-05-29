package com.movieanalyzer.backend.controller;

import com.movieanalyzer.backend.dto.AnalysisResponse;
import com.movieanalyzer.backend.dto.ReviewHistoryItem;
import com.movieanalyzer.backend.dto.ReviewRequest;
import com.movieanalyzer.backend.service.AppBehaviorState;
import com.movieanalyzer.backend.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Combined imports

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final DataSource dataSource;

    @Autowired
    public ReviewController(ReviewService reviewService, DataSource dataSource) {
        this.reviewService = reviewService;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        logger.info("ReviewController initialized.");
        logger.info("Initial AppBehaviorState.databaseInteractionEnabled: {}", AppBehaviorState.isDatabaseInteractionEnabled());
        logger.info("Initial AppBehaviorState.triggerCpuLoadPerRequest: {}", AppBehaviorState.isTriggerCpuLoadPerRequest());
    }

    private void applyCpuLoadIfNeeded() {
        if (AppBehaviorState.isTriggerCpuLoadPerRequest()) {
            logger.info("CPU load per request is enabled. Triggering CPU burn...");
            reviewService.performCpuIntensiveTask(500_000); // Smaller iteration for per-request load
        }
    }

    @PostMapping("/analyze_review")
    public ResponseEntity<AnalysisResponse> analyzeReview(@RequestBody ReviewRequest reviewRequest) {
        logger.info("Received POST /analyze_review for movie: {}", reviewRequest.getMovie_name());
        applyCpuLoadIfNeeded();
        AnalysisResponse response = reviewService.analyzeAndSaveReview(reviewRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews_history/{movie_name}")
    public ResponseEntity<List<ReviewHistoryItem>> getReviewHistory(@PathVariable("movie_name") String movieName) {
        logger.info("Received GET /reviews_history for movie: {}", movieName);
        applyCpuLoadIfNeeded();
        List<ReviewHistoryItem> history = reviewService.getReviewHistory(movieName);
        return ResponseEntity.ok(history);
    }

    // --- Debug Endpoints ---
    @PostMapping("/debug/backend/database/enable")
    public ResponseEntity<Map<String, String>> enableDatabase() {
        AppBehaviorState.setDatabaseInteractionEnabled(true);
        logger.info("DEBUG: Database interaction ENABLED via API.");
        return ResponseEntity.ok(Map.of("status", "Backend database interaction enabled"));
    }

    @PostMapping("/debug/backend/database/disable")
    public ResponseEntity<Map<String, String>> disableDatabase() {
        AppBehaviorState.setDatabaseInteractionEnabled(false);
        logger.info("DEBUG: Database interaction DISABLED via API.");
        return ResponseEntity.ok(Map.of("status", "Backend database interaction disabled"));
    }

    @PostMapping("/debug/backend/cpuload/enable")
    public ResponseEntity<Map<String, String>> enableCpuLoadPerRequest() {
        AppBehaviorState.setTriggerCpuLoadPerRequest(true);
        logger.info("DEBUG: CPU load per request ENABLED via API.");
        return ResponseEntity.ok(Map.of("status", "CPU load per request enabled"));
    }

    @PostMapping("/debug/backend/cpuload/disable")
    public ResponseEntity<Map<String, String>> disableCpuLoadPerRequest() {
        AppBehaviorState.setTriggerCpuLoadPerRequest(false);
        logger.info("DEBUG: CPU load per request DISABLED via API.");
        return ResponseEntity.ok(Map.of("status", "CPU load per request disabled"));
    }

    @GetMapping("/load-cpu")
    public ResponseEntity<Map<String, String>> generateCpuLoad(@RequestParam(defaultValue = "1000000") int iterations) {
        logger.info("Received GET /load-cpu with iterations: {}", iterations);
        long startTime = System.nanoTime();
        double result = reviewService.performCpuIntensiveTask(iterations);
        long duration = (System.nanoTime() - startTime) / 1_000_000; // Milliseconds
        String message = "CPU load task completed in " + duration + " ms. Result checksum: " + result;
        logger.info(message);
        return ResponseEntity.ok(Map.of("message", message));
    }

    // --- Kubernetes Health Probe Endpoints ---
    @GetMapping("/livez")
    public ResponseEntity<String> livenessProbe() {
        logger.trace("Liveness probe received");
        return ResponseEntity.ok("Backend is live");
    }

    @GetMapping("/readyz")
    public ResponseEntity<String> readinessProbe() {
        logger.trace("Readiness probe received");
        if (!AppBehaviorState.isDatabaseInteractionEnabled()) {
            logger.warn("READINESS PROBE: Backend NOT READY (DB interaction disabled via API)");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Backend not ready: DB interaction disabled via API");
        }

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) { // Timeout of 1 second
                logger.trace("READINESS PROBE: Backend IS READY (DB connected)");
                return ResponseEntity.ok("Backend is ready");
            } else {
                logger.warn("READINESS PROBE: Backend NOT READY (DB connection isValid returned false)");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Backend not ready: DB connection invalid");
            }
        } catch (SQLException e) {
            logger.error("READINESS PROBE: Backend NOT READY (DB connection failed with SQLException: {})", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Backend not ready: Database connectivity issue (SQLException)");
        } catch (Exception e) {
            logger.error("READINESS PROBE: Backend NOT READY (Unexpected error during DB check: {})", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Backend not ready: Unexpected error during DB check");
        }
    }
}