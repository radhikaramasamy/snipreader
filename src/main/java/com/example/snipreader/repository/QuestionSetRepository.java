package com.example.snipreader.repository;

import com.example.snipreader.model.QuestionSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for QuestionSet entities.
 */
@Repository
public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {
    
    /**
     * Find all question sets ordered by creation date (newest first).
     */
    List<QuestionSet> findAllByOrderByCreatedAtDesc();
}