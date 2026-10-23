package com.studysync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AskQuestionRequest {

    @NotBlank(message = "Question must not be blank")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;

    public AskQuestionRequest() {}

    public AskQuestionRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
