package com.movieanalyzer.backend.repository;

import com.movieanalyzer.backend.entity.MovieReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovieReviewRepository extends JpaRepository<MovieReview, Long> {
    List<MovieReview> findTop10ByMovieNameOrderByCreatedAtDesc(String movieName);
}