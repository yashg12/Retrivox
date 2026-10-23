package com.studysync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quiz_attempt", indexes = {
    @Index(name = "idx_quiz_attempt_user_id", columnList = "user_id"),
    @Index(name = "idx_quiz_attempt_quiz_id", columnList = "quiz_id")
})
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quiz_id", nullable = false)
    private String quizId;

    @Column(name = "study_material_id")
    private Long studyMaterialId;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private double percentage;

    @Column(nullable = false, updatable = false)
    private Instant completedAt = Instant.now();

    public QuizAttempt() {
    }

    public QuizAttempt(User user, String quizId, Long studyMaterialId, int totalQuestions, int score, double percentage) {
        this.user = user;
        this.quizId = quizId;
        this.studyMaterialId = studyMaterialId;
        this.totalQuestions = totalQuestions;
        this.score = score;
        this.percentage = percentage;
        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public Long getStudyMaterialId() {
        return studyMaterialId;
    }

    public void setStudyMaterialId(Long studyMaterialId) {
        this.studyMaterialId = studyMaterialId;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
