package com.studysync.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class SubmitQuizRequest {

    @NotNull(message = "Answers must be provided")
    private Map<Integer, String> answers; // questionId -> selectedOption

    public SubmitQuizRequest() {}

    public SubmitQuizRequest(Map<Integer, String> answers) {
        this.answers = answers;
    }

    public Map<Integer, String> getAnswers() { return answers; }
    public void setAnswers(Map<Integer, String> answers) { this.answers = answers; }
}
