package com.moviereview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MovieReviewApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Movie Review Backend Service - Lambda Integration");
        System.out.println("📋 This service is designed to be resilient to downstream failures");
        System.out.println("🗄️ Database connection will be tested at runtime, not startup");
        System.out.println("🔗 Sentiment analysis powered by AWS Lambda");
        
        SpringApplication.run(MovieReviewApplication.class, args);
    }
} 