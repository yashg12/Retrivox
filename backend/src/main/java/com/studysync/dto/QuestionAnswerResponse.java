package com.studysync.dto;

import java.util.List;

public class QuestionAnswerResponse {
    private String answer;
    private List<ChunkSourceDto> sources;

    public QuestionAnswerResponse() {}

    public QuestionAnswerResponse(String answer, List<ChunkSourceDto> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<ChunkSourceDto> getSources() {
        return sources;
    }

    public void setSources(List<ChunkSourceDto> sources) {
        this.sources = sources;
    }
}
