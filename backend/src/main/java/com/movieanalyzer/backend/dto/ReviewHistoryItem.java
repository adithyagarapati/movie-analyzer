package com.movieanalyzer.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistoryItem {
    private String movie_name;
    private String review_text;
    private String predicted_sentiment;
    private int pseudo_rating;
    private LocalDateTime created_at;
}