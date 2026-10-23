package com.studysync.service;

import com.studysync.dto.SummaryResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.repository.StudyMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudySummaryService {

    private final StudyMaterialRepository repository;
    private final LlmClient llmClient;
    private final int maxInputChars;

    public StudySummaryService(
            StudyMaterialRepository repository,
            LlmClient llmClient,
            @Value("${studysync.llm.max-input-chars:120000}") int maxInputChars) {
        this.repository = repository;
        this.llmClient = llmClient;
        this.maxInputChars = maxInputChars > 0 ? maxInputChars : 120000;
    }

    @Transactional
    public SummaryResponse generateAndSaveSummary(Long materialId) {
        return generateAndSaveSummary(materialId, null);
    }

    @Transactional
    public SummaryResponse generateAndSaveSummary(Long materialId, Long userId) {
        StudyMaterial material = repository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found"));

        if (userId != null && material.getUser() != null && !userId.equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }

        String extractedText = material.getExtractedText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Study material has no extracted text. Please upload a valid PDF first.");
        }

        if (extractedText.length() > maxInputChars) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Extracted text exceeds the maximum character limit of " + maxInputChars + " characters.");
        }

        String generatedSummary = llmClient.generateSummary(extractedText.trim());
        material.setSummary(generatedSummary);
        repository.save(material);

        return new SummaryResponse(
                material.getId(),
                material.getTitle(),
                material.getSummary(),
                material.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long materialId) {
        return getSummary(materialId, null);
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long materialId, Long userId) {
        StudyMaterial material = repository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found"));

        if (userId != null && material.getUser() != null && !userId.equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }

        if (material.getSummary() == null || material.getSummary().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Summary not generated yet for this study material.");
        }

        return new SummaryResponse(
                material.getId(),
                material.getTitle(),
                material.getSummary(),
                material.getCreatedAt()
        );
    }
}
