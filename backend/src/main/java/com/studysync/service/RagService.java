package com.studysync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.dto.AskQuestionRequest;
import com.studysync.dto.ChunkSourceDto;
import com.studysync.dto.QuestionAnswerResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.model.StudyMaterialChunk;
import com.studysync.repository.StudyMaterialChunkRepository;
import com.studysync.repository.StudyMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final StudyMaterialRepository materialRepository;
    private final StudyMaterialChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final TextChunker textChunker;
    private final int topK;

    public RagService(
            StudyMaterialRepository materialRepository,
            StudyMaterialChunkRepository chunkRepository,
            EmbeddingClient embeddingClient,
            LlmClient llmClient,
            @Value("${studysync.rag.top-k:3}") int topK) {
        this.materialRepository = materialRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
        this.textChunker = new TextChunker(1000, 200);
        this.topK = topK > 0 ? topK : 3;
    }

    @Transactional
    public void clearChunksForMaterial(Long materialId) {
        chunkRepository.deleteByStudyMaterialId(materialId);
    }

    @Transactional
    public List<StudyMaterialChunk> ensureChunksIndexed(StudyMaterial material) {
        List<StudyMaterialChunk> existingChunks = chunkRepository.findByStudyMaterialIdOrderByChunkIndexAsc(material.getId());
        if (!existingChunks.isEmpty()) {
            return existingChunks;
        }

        String text = material.getExtractedText();
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Study material has no extracted text. Please upload a valid PDF first.");
        }

        List<TextChunker.ChunkSpan> spans = textChunker.chunk(text.trim());
        if (spans.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extracted text contains no indexable content.");
        }

        List<String> chunkTexts = spans.stream().map(TextChunker.ChunkSpan::content).collect(Collectors.toList());
        List<List<Float>> embeddings = embeddingClient.generateEmbeddings(chunkTexts);

        if (embeddings.size() != spans.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding count mismatch from provider.");
        }

        List<StudyMaterialChunk> entitiesToSave = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            TextChunker.ChunkSpan span = spans.get(i);
            List<Float> embedding = embeddings.get(i);
            String embeddingJson;
            try {
                embeddingJson = objectMapper.writeValueAsString(embedding);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize embedding vector.");
            }

            StudyMaterialChunk chunk = new StudyMaterialChunk(
                    material,
                    span.chunkIndex(),
                    span.content(),
                    span.charStartIndex(),
                    span.charEndIndex(),
                    embeddingJson
            );
            entitiesToSave.add(chunk);
        }

        return chunkRepository.saveAll(entitiesToSave);
    }

    @Transactional
    public QuestionAnswerResponse askQuestion(Long materialId, AskQuestionRequest request) {
        return askQuestion(materialId, request, null);
    }

    @Transactional
    public QuestionAnswerResponse askQuestion(Long materialId, AskQuestionRequest request, Long userId) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be blank.");
        }
        String cleanQuestion = request.getQuestion().trim();

        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found with id: " + materialId));

        if (userId != null && material.getUser() != null && !userId.equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }

        String extractedText = material.getExtractedText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Study material has no extracted text. Please upload a valid PDF first.");
        }

        // Ensure chunks and embeddings exist
        List<StudyMaterialChunk> chunks = ensureChunksIndexed(material);
        if (chunks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document chunks available for this material.");
        }

        // Generate embedding for query
        List<Float> queryEmbedding = embeddingClient.generateEmbedding(cleanQuestion);
        validateVector(queryEmbedding);

        // Rank chunks by cosine similarity
        record ScoredChunk(StudyMaterialChunk chunk, double similarity) {}
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (StudyMaterialChunk chunk : chunks) {
            List<Float> chunkEmbedding = parseAndValidateEmbedding(chunk.getEmbeddingJson());
            if (chunkEmbedding.size() != queryEmbedding.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Vector dimension mismatch between query (" + queryEmbedding.size() + ") and chunk (" + chunkEmbedding.size() + ").");
            }
            double similarity = computeCosineSimilarity(queryEmbedding, chunkEmbedding);
            scoredChunks.add(new ScoredChunk(chunk, similarity));
        }

        scoredChunks.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        List<ScoredChunk> topResults = scoredChunks.stream().limit(topK).toList();

        List<String> contextChunks = topResults.stream()
                .map(sc -> sc.chunk.getContent())
                .collect(Collectors.toList());

        String answer = llmClient.answerQuestion(cleanQuestion, contextChunks);

        List<ChunkSourceDto> sources = topResults.stream()
                .map(sc -> new ChunkSourceDto(
                        sc.chunk.getChunkIndex(),
                        buildSnippet(sc.chunk.getContent(), 150),
                        Math.round(sc.similarity * 1000.0) / 1000.0
                ))
                .collect(Collectors.toList());

        return new QuestionAnswerResponse(answer, sources);
    }

    private String buildSnippet(String content, int maxLength) {
        if (content == null) return "";
        String trimmed = content.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    public double computeCosineSimilarity(List<Float> vecA, List<Float> vecB) {
        if (vecA == null || vecB == null || vecA.size() != vecB.size() || vecA.isEmpty()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.size(); i++) {
            float a = vecA.get(i);
            float b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private void validateVector(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding vector is empty or null.");
        }
        for (Float val : vector) {
            if (val == null || Float.isNaN(val) || Float.isInfinite(val)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding contains invalid numeric values.");
            }
        }
    }

    private List<Float> parseAndValidateEmbedding(String json) {
        if (json == null || json.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stored chunk embedding is missing or empty.");
        }
        try {
            List<Float> list = objectMapper.readValue(json, new TypeReference<List<Float>>() {});
            validateVector(list);
            return list;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse stored chunk embedding JSON.");
        }
    }
}
