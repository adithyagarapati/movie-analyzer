package com.movieanalyzer.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String sentiment;      // e.g., "Positive", "Negative", "Neutral"
    private int pseudo_rating;  // e.g., 1-10
}