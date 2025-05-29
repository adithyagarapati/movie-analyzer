package com.movieanalyzer.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Configure timeouts for the RestTemplate
        return builder
                .setConnectTimeout(Duration.ofSeconds(5)) // Connection timeout
                .setReadTimeout(Duration.ofSeconds(10))    // Read timeout
                .build();
    }
}