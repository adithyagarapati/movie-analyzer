package com.movieanalyzer.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "movie_reviews")
@Data
@NoArgsConstructor
public class MovieReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String movieName;

    @Lob // For longer text
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reviewText;

    @Column(nullable = false)
    private String predictedSentiment;

    @Column(nullable = false)
    private int pseudoRating;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MovieReview(String movieName, String reviewText, String predictedSentiment, int pseudoRating) {
        this.movieName = movieName;
        this.reviewText = reviewText;
        this.predictedSentiment = predictedSentiment;
        this.pseudoRating = pseudoRating;
    }
}