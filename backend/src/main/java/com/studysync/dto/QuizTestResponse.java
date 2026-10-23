package com.studysync.dto;

import java.time.Instant;
import java.util.List;

public class QuizTestResponse {
    private String quizId;
    private Long studyMaterialId;
    private int questionCount;
    private String difficulty;
    private List<QuizTestQuestionDto> questions;
    private Instant createdAt;

    public QuizTestResponse() {}

    public QuizTestResponse(String quizId, Long studyMaterialId, int questionCount, String difficulty, List<QuizTestQuestionDto> questions, Instant createdAt) {
        this.quizId = quizId;
        this.studyMaterialId = studyMaterialId;
        this.questionCount = questionCount;
        this.difficulty = difficulty;
        this.questions = questions;
        this.createdAt = createdAt;
    }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public Long getStudyMaterialId() { return studyMaterialId; }
    public void setStudyMaterialId(Long studyMaterialId) { this.studyMaterialId = studyMaterialId; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<QuizTestQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizTestQuestionDto> questions) { this.questions = questions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
