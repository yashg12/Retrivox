package com.studysync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.dto.FlashcardDto;
import com.studysync.dto.FlashcardsResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.model.User;
import com.studysync.repository.StudyMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class FlashcardService {

    private final StudyMaterialRepository materialRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final int maxInputChars;

    public FlashcardService(
            StudyMaterialRepository materialRepository,
            LlmClient llmClient,
            @Value("${studysync.llm.max-input-chars:${llm.max-input-chars:8000}}") int maxInputChars) {
        this.materialRepository = materialRepository;
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
        this.maxInputChars = maxInputChars;
    }

    public FlashcardsResponse generateFlashcards(Long materialId, int count, User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }

        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found."));

        if (material.getUser() != null && !currentUser.getId().equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }

        String extractedText = material.getExtractedText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extracted text is empty. Upload a valid document first.");
        }

        String textToSend = extractedText.trim();
        if (textToSend.length() > maxInputChars) {
            textToSend = textToSend.substring(0, maxInputChars);
        }

        int targetCount = count > 0 ? Math.min(count, 30) : 5;
        String rawOutput = llmClient.generateFlashcards(textToSend, targetCount);
        List<FlashcardDto> flashcards = parseAndValidateFlashcards(rawOutput, targetCount);

        return new FlashcardsResponse(materialId, flashcards.size(), flashcards);
    }

    private List<FlashcardDto> parseAndValidateFlashcards(String rawJson, int expectedCount) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned empty flashcard content.");
        }

        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM output is not a JSON array.");
            }

            List<FlashcardDto> flashcards = new ArrayList<>();
            int idCounter = 1;
            for (JsonNode node : root) {
                String front = node.path("front").asText("").trim();
                String back = node.path("back").asText("").trim();

                if (!front.isEmpty() && !back.isEmpty()) {
                    flashcards.add(new FlashcardDto(idCounter++, front, back));
                }
            }

            if (flashcards.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No valid flashcards could be parsed from LLM output.");
            }

            return flashcards;
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse flashcards JSON from LLM: " + e.getMessage());
        }
    }
}
