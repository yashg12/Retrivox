package com.studysync.dto;

import java.util.List;

public class QuestionResultDto {
    private int questionId;
    private String question;
    private List<String> options;
    private String selectedAnswer;
    private String correctAnswer;
    private boolean correct;
    private String explanation;

    public QuestionResultDto() {}

    public QuestionResultDto(int questionId, String question, List<String> options, String selectedAnswer, String correctAnswer, boolean correct, String explanation) {
        this.questionId = questionId;
        this.question = question;
        this.options = options;
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.explanation = explanation;
    }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
