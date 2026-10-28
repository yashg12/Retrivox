package com.studysync.dto;

import java.util.List;

public class FlashcardsResponse {
    private Long studyMaterialId;
    private int count;
    private List<FlashcardDto> flashcards;

    public FlashcardsResponse() {
    }

    public FlashcardsResponse(Long studyMaterialId, int count, List<FlashcardDto> flashcards) {
        this.studyMaterialId = studyMaterialId;
        this.count = count;
        this.flashcards = flashcards;
    }

    public Long getStudyMaterialId() {
        return studyMaterialId;
    }

    public void setStudyMaterialId(Long studyMaterialId) {
        this.studyMaterialId = studyMaterialId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<FlashcardDto> getFlashcards() {
        return flashcards;
    }

    public void setFlashcards(List<FlashcardDto> flashcards) {
        this.flashcards = flashcards;
    }
}
