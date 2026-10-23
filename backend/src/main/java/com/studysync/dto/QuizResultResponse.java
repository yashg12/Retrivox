package com.studysync.dto;

import java.util.List;

public class QuizResultResponse {
    private String quizId;
    private int totalQuestions;
    private int score;
    private double percentage;
    private List<QuestionResultDto> details;

    public QuizResultResponse() {}

    public QuizResultResponse(String quizId, int totalQuestions, int score, double percentage, List<QuestionResultDto> details) {
        this.quizId = quizId;
        this.totalQuestions = totalQuestions;
        this.score = score;
        this.percentage = percentage;
        this.details = details;
    }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public List<QuestionResultDto> getDetails() { return details; }
    public void setDetails(List<QuestionResultDto> details) { this.details = details; }
}
