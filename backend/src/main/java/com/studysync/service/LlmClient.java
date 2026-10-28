package com.studysync.service;

import java.util.List;

public interface LlmClient {
    String generateSummary(String extractedText);
    String generateMcqs(String extractedText, int count, String difficulty);
    String generateFlashcards(String extractedText, int count);
    String answerQuestion(String question, List<String> contextChunks);
}

