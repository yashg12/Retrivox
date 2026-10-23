package com.studysync.service;

import java.util.List;

public interface LlmClient {
    String generateSummary(String extractedText);
    String generateMcqs(String extractedText, int count, String difficulty);
    String answerQuestion(String question, List<String> contextChunks);
}
