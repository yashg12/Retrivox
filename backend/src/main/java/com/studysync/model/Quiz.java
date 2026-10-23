package com.studysync.model;

import java.time.Instant;
import java.util.List;

public class Quiz {
    private String id;
    private Long studyMaterialId;
    private Long userId;
    private int questionCount;
    private String difficulty;
    private List<QuizQuestion> questions;
    private Instant createdAt = Instant.now();

    public Quiz() {}

    public Quiz(String id, Long studyMaterialId, Long userId, int questionCount, String difficulty, List<QuizQuestion> questions) {
        this.id = id;
        this.studyMaterialId = studyMaterialId;
        this.userId = userId;
        this.questionCount = questionCount;
        this.difficulty = difficulty;
        this.questions = questions;
    }

    public Quiz(String id, Long studyMaterialId, int questionCount, String difficulty, List<QuizQuestion> questions) {
        this(id, studyMaterialId, null, questionCount, difficulty, questions);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getStudyMaterialId() { return studyMaterialId; }
    public void setStudyMaterialId(Long studyMaterialId) { this.studyMaterialId = studyMaterialId; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
