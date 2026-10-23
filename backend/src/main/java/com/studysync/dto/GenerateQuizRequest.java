package com.studysync.dto;

import jakarta.validation.constraints.NotNull;

public class GenerateQuizRequest {

    @NotNull(message = "Question count must be provided")
    private Integer questionCount = 5;

    private String difficulty = "Medium";

    public GenerateQuizRequest() {}

    public GenerateQuizRequest(Integer questionCount, String difficulty) {
        this.questionCount = questionCount;
        this.difficulty = difficulty;
    }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
