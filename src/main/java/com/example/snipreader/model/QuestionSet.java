package com.example.snipreader.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of questions from a single image upload.
 * This entity is used to store and retrieve question sets from the database.
 */
@Entity
@Table(name = "question_sets")
public class QuestionSet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "title")
    private String title;
    
    @OneToMany(mappedBy = "questionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
    
    /**
     * Default constructor required by JPA
     */
    public QuestionSet() {
    }
    
    /**
     * Constructor with title
     */
    public QuestionSet(String title) {
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Add a question to this question set
     */
    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuestionSet(this);
    }
    
    /**
     * Remove a question from this question set
     */
    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setQuestionSet(null);
    }
    
    // Getters and setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<Question> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}