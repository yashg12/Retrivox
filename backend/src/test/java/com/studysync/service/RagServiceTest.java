package com.studysync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.dto.AskQuestionRequest;
import com.studysync.dto.QuestionAnswerResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.model.StudyMaterialChunk;
import com.studysync.repository.StudyMaterialChunkRepository;
import com.studysync.repository.StudyMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RagServiceTest {

    @Mock
    private StudyMaterialRepository materialRepository;

    @Mock
    private StudyMaterialChunkRepository chunkRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private LlmClient llmClient;

    private RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ragService = new RagService(materialRepository, chunkRepository, embeddingClient, llmClient, 2);
    }

    @Test
    void askQuestion_SuccessWithGroundedAnswerAndSourceAttribution() throws Exception {
        Long materialId = 1L;
        StudyMaterial material = new StudyMaterial();
        material.setId(materialId);
        material.setTitle("Calculus Notes");
        material.setExtractedText("Derivatives represent the instantaneous rate of change. Integrals calculate the area under the curve.");

        StudyMaterialChunk chunk0 = new StudyMaterialChunk(
                material,
                0,
                "Derivatives represent the instantaneous rate of change.",
                0,
                55,
                objectMapper.writeValueAsString(List.of(1.0f, 0.0f))
        );
        StudyMaterialChunk chunk1 = new StudyMaterialChunk(
                material,
                1,
                "Integrals calculate the area under the curve.",
                56,
                101,
                objectMapper.writeValueAsString(List.of(0.0f, 1.0f))
        );

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        // Existing chunks already indexed in repository
        when(chunkRepository.findByStudyMaterialIdOrderByChunkIndexAsc(materialId)).thenReturn(List.of(chunk0, chunk1));
        // Query embedding aligns closely with chunk0 (derivatives)
        when(embeddingClient.generateEmbedding("What is a derivative?")).thenReturn(List.of(0.99f, 0.01f));
        when(llmClient.answerQuestion(eq("What is a derivative?"), anyList()))
                .thenReturn("A derivative represents the instantaneous rate of change of a function.");

        AskQuestionRequest request = new AskQuestionRequest("What is a derivative?");
        QuestionAnswerResponse response = ragService.askQuestion(materialId, request);

        assertNotNull(response);
        assertEquals("A derivative represents the instantaneous rate of change of a function.", response.getAnswer());
        assertNotNull(response.getSources());
        assertFalse(response.getSources().isEmpty());
        // Top source must be chunk 0 because dot product is higher
        assertEquals(0, response.getSources().get(0).getChunkIndex());
        assertTrue(response.getSources().get(0).getSnippet().contains("Derivatives"));
        assertTrue(response.getSources().get(0).getSimilarity() > 0.9);

        verify(llmClient).answerQuestion(eq("What is a derivative?"), anyList());
    }

    @Test
    void askQuestion_AutoIndexesChunksWhenNoneExist() throws Exception {
        Long materialId = 2L;
        StudyMaterial material = new StudyMaterial();
        material.setId(materialId);
        material.setTitle("Linear Algebra");
        material.setExtractedText("Matrix multiplication is associative but not commutative.");

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        // Initially empty
        when(chunkRepository.findByStudyMaterialIdOrderByChunkIndexAsc(materialId)).thenReturn(Collections.emptyList());
        when(embeddingClient.generateEmbeddings(anyList())).thenReturn(List.of(List.of(0.5f, 0.5f)));
        when(chunkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        when(embeddingClient.generateEmbedding("Is matrix multiplication commutative?")).thenReturn(List.of(0.5f, 0.5f));
        when(llmClient.answerQuestion(any(), anyList())).thenReturn("Matrix multiplication is not commutative.");

        AskQuestionRequest request = new AskQuestionRequest("Is matrix multiplication commutative?");
        QuestionAnswerResponse response = ragService.askQuestion(materialId, request);

        assertNotNull(response);
        assertEquals("Matrix multiplication is not commutative.", response.getAnswer());
        verify(chunkRepository).saveAll(anyList());
    }

    @Test
    void askQuestion_InsufficientEvidenceReturnsHonestAnswer() {
        Long materialId = 3L;
        StudyMaterial material = new StudyMaterial();
        material.setId(materialId);
        material.setExtractedText("Topic: Classical Mechanics and Newton's Laws.");

        StudyMaterialChunk chunk = new StudyMaterialChunk(
                material, 0, "Topic: Classical Mechanics and Newton's Laws.", 0, 44, "[0.1, 0.2]"
        );

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(chunkRepository.findByStudyMaterialIdOrderByChunkIndexAsc(materialId)).thenReturn(List.of(chunk));
        when(embeddingClient.generateEmbedding("What is quantum entanglement?")).thenReturn(List.of(0.9f, -0.4f));
        when(llmClient.answerQuestion(eq("What is quantum entanglement?"), anyList()))
                .thenReturn("The provided study material does not contain sufficient information to answer this question.");

        AskQuestionRequest request = new AskQuestionRequest("What is quantum entanglement?");
        QuestionAnswerResponse response = ragService.askQuestion(materialId, request);

        assertNotNull(response);
        assertEquals("The provided study material does not contain sufficient information to answer this question.", response.getAnswer());
    }

    @Test
    void askQuestion_MaterialNotFoundThrows404() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        AskQuestionRequest request = new AskQuestionRequest("Any question");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ragService.askQuestion(999L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void askQuestion_EmptyExtractedTextThrows400() {
        StudyMaterial material = new StudyMaterial();
        material.setId(5L);
        material.setExtractedText("");

        when(materialRepository.findById(5L)).thenReturn(Optional.of(material));

        AskQuestionRequest request = new AskQuestionRequest("What is this?");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ragService.askQuestion(5L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void askQuestion_BlankQuestionThrows400() {
        AskQuestionRequest request = new AskQuestionRequest("   ");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ragService.askQuestion(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void askQuestion_EmbeddingServiceUnavailableThrows503() {
        StudyMaterial material = new StudyMaterial();
        material.setId(1L);
        material.setExtractedText("Some content for indexing");

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(chunkRepository.findByStudyMaterialIdOrderByChunkIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(embeddingClient.generateEmbeddings(anyList())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Missing API key"));

        AskQuestionRequest request = new AskQuestionRequest("What is this?");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ragService.askQuestion(1L, request));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void askQuestion_LlmProviderErrorPropagates502() {
        StudyMaterial material = new StudyMaterial();
        material.setId(1L);
        material.setExtractedText("Some content for indexing");

        StudyMaterialChunk chunk = new StudyMaterialChunk(
                material, 0, "Some content for indexing", 0, 25, "[0.1, 0.2]"
        );

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(chunkRepository.findByStudyMaterialIdOrderByChunkIndexAsc(1L)).thenReturn(List.of(chunk));
        when(embeddingClient.generateEmbedding("Question")).thenReturn(List.of(0.1f, 0.2f));
        when(llmClient.answerQuestion(any(), anyList())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM timeout"));

        AskQuestionRequest request = new AskQuestionRequest("Question");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ragService.askQuestion(1L, request));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    @Test
    void computeCosineSimilarity_CalculatesCorrectSimilarity() {
        List<Float> v1 = List.of(1.0f, 0.0f);
        List<Float> v2 = List.of(1.0f, 0.0f);
        List<Float> v3 = List.of(0.0f, 1.0f);
        List<Float> v4 = List.of(-1.0f, 0.0f);

        assertEquals(1.0, ragService.computeCosineSimilarity(v1, v2), 0.0001);
        assertEquals(0.0, ragService.computeCosineSimilarity(v1, v3), 0.0001);
        assertEquals(-1.0, ragService.computeCosineSimilarity(v1, v4), 0.0001);
        assertEquals(0.0, ragService.computeCosineSimilarity(Collections.emptyList(), v1));
    }

    @Test
    void askQuestion_CrossUser_ThrowsForbidden() {
        com.studysync.model.User owner = new com.studysync.model.User("owner@test.edu", "hash", "Owner");
        owner.setId(100L);

        StudyMaterial material = new StudyMaterial();
        material.setId(1L);
        material.setUser(owner);
        material.setExtractedText("Some extracted text.");

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        AskQuestionRequest request = new AskQuestionRequest("What is this?");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ragService.askQuestion(1L, request, 200L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(embeddingClient, never()).generateEmbedding(any());
    }
}
