package com.moviereview.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ModelServerService modelServerService;

    private boolean backendHealthy = true;
    
    // Backend overload simulation
    private boolean backendOverloaded = false;
    private ExecutorService overloadExecutor;
    private List<Future<?>> overloadTasks = new ArrayList<>();

    /**
     * Get comprehensive health status
     */
    public Map<String, Object> getHealthStatus() {
        boolean databaseStatus = reviewService.isDatabaseAvailable();
        boolean modelServerStatus = modelServerService.isModelServerAvailable();

        String overallStatus;
        if (!backendHealthy) {
            overallStatus = "unhealthy";
        } else if (databaseStatus && modelServerStatus) {
            overallStatus = "healthy";
        } else {
            overallStatus = "degraded";
        }

        return Map.of(
            "status", overallStatus,
            "timestamp", java.time.Instant.now().toString(),
            "service", "backend",
            "database", databaseStatus,
            "modelServer", modelServerStatus,
            "backendHealthy", backendHealthy,
            "backendOverloaded", backendOverloaded
        );
    }

    /**
     * Toggle backend health status - ALWAYS respond to allow re-enabling
     */
    public Map<String, Object> toggleBackendHealth() {
        backendHealthy = !backendHealthy;
        System.out.println("🏥 Backend health toggled: " + (backendHealthy ? "HEALTHY" : "UNHEALTHY"));
        
        return Map.of(
            "message", "Backend health " + (backendHealthy ? "enabled" : "disabled"),
            "healthy", backendHealthy,
            "timestamp", java.time.Instant.now().toString()
        );
    }

    /**
     * Toggle backend overload simulation
     */
    public Map<String, Object> toggleBackendOverload() {
        if (backendOverloaded) {
            stopBackendOverload();
        } else {
            startBackendOverload();
        }
        
        return Map.of(
            "message", "Backend overload " + (backendOverloaded ? "started" : "stopped"),
            "overloaded", backendOverloaded,
            "timestamp", java.time.Instant.now().toString()
        );
    }

    /**
     * Start backend overload simulation
     */
    private void startBackendOverload() {
        if (backendOverloaded) {
            return; // Already running
        }
        
        backendOverloaded = true;
        overloadExecutor = Executors.newFixedThreadPool(4);
        
        System.out.println("🔥 Starting backend overload simulation...");
        
        // CPU intensive tasks
        for (int i = 0; i < 3; i++) {
            Future<?> task = overloadExecutor.submit(() -> {
                Thread.currentThread().setName("BackendOverload-CPU-" + Thread.currentThread().getId());
                while (backendOverloaded && !Thread.currentThread().isInterrupted()) {
                    try {
                        // CPU intensive calculation
                        double result = 0;
                        for (int j = 0; j < 1000000; j++) {
                            result += Math.sqrt(j) * Math.sin(j);
                        }
                        Thread.sleep(10); // Brief pause
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            overloadTasks.add(task);
        }
        
        // Memory intensive task
        Future<?> memoryTask = overloadExecutor.submit(() -> {
            Thread.currentThread().setName("BackendOverload-Memory");
            List<byte[]> memoryEater = new ArrayList<>();
            
            while (backendOverloaded && !Thread.currentThread().isInterrupted()) {
                try {
                    // Allocate memory in chunks
                    byte[] chunk = new byte[1024 * 1024]; // 1MB chunks
                    memoryEater.add(chunk);
                    
                    // Keep only last 50MB to avoid OutOfMemoryError
                    if (memoryEater.size() > 50) {
                        memoryEater.remove(0);
                    }
                    
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (OutOfMemoryError e) {
                    System.err.println("⚠️ Backend overload hit memory limit - reducing load");
                    memoryEater.clear();
                    System.gc();
                }
            }
        });
        overloadTasks.add(memoryTask);
        
        System.out.println("🔥 Backend overload simulation started with " + overloadTasks.size() + " tasks");
    }

    /**
     * Stop backend overload simulation
     */
    private void stopBackendOverload() {
        if (!backendOverloaded) {
            return; // Already stopped
        }
        
        backendOverloaded = false;
        
        System.out.println("🛑 Stopping backend overload simulation...");
        
        // Cancel all running tasks
        for (Future<?> task : overloadTasks) {
            task.cancel(true);
        }
        overloadTasks.clear();
        
        // Shutdown executor
        if (overloadExecutor != null) {
            overloadExecutor.shutdownNow();
            overloadExecutor = null;
        }
        
        // Force garbage collection to clean up
        System.gc();
        
        System.out.println("✅ Backend overload simulation stopped");
    }

    /**
     * Toggle database connection
     */
    public Map<String, Object> toggleDatabaseConnection() {
        reviewService.toggleDatabaseConnection();
        boolean newStatus = reviewService.isDatabaseConnectionEnabled();
        
        return Map.of(
            "message", "Database connection " + (newStatus ? "enabled" : "disabled"),
            "connected", newStatus,
            "timestamp", java.time.Instant.now().toString()
        );
    }

    /**
     * Toggle model server connection
     */
    public Map<String, Object> toggleModelServerConnection() {
        modelServerService.toggleModelConnection();
        boolean newStatus = modelServerService.isModelConnectionEnabled();
        
        return Map.of(
            "message", "Model server connection " + (newStatus ? "enabled" : "disabled"),
            "connected", newStatus,
            "timestamp", java.time.Instant.now().toString()
        );
    }

    /**
     * Get comprehensive admin status including Lambda configuration
     */
    public Map<String, Object> getAdminStatus() {
        Map<String, Object> modelConfig = modelServerService.getServerConfig();
        
        return Map.of(
            "backendHealthy", backendHealthy,
            "backendOverloaded", backendOverloaded,
            "databaseConnected", reviewService.isDatabaseConnectionEnabled(),
            "modelServerConnected", modelServerService.isModelConnectionEnabled(),
            "modelServerConfig", modelConfig,
            "actualDatabaseStatus", reviewService.isDatabaseAvailable(),
            "actualModelServerStatus", modelServerService.isModelServerAvailable(),
            "timestamp", java.time.Instant.now().toString(),
            "reviewStats", reviewService.getReviewStats()
        );
    }

    /**
     * Check if backend is healthy
     */
    public boolean isBackendHealthy() {
        return backendHealthy;
    }

    /**
     * Check if backend is overloaded
     */
    public boolean isBackendOverloaded() {
        return backendOverloaded;
    }
} 