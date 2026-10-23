package com.studysync.service;

import com.studysync.dto.SummaryResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.repository.StudyMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudySummaryServiceTest {

    @Mock
    private StudyMaterialRepository repository;

    @Mock
    private LlmClient llmClient;

    private StudySummaryService service;

    @BeforeEach
    void setUp() {
        service = new StudySummaryService(repository, llmClient, 500);
    }

    @Test
    void generateSummary_Success() {
        StudyMaterial material = new StudyMaterial();
        material.setId(1L);
        material.setTitle("Operating Systems");
        material.setExtractedText("Processes and threads are fundamental concepts in operating systems.");

        when(repository.findById(1L)).thenReturn(Optional.of(material));
        when(llmClient.generateSummary(material.getExtractedText())).thenReturn("Structured Summary: Key concepts on processes and threads.");
        when(repository.save(any(StudyMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SummaryResponse response = service.generateAndSaveSummary(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Operating Systems", response.getTitle());
        assertEquals("Structured Summary: Key concepts on processes and threads.", response.getSummary());
        assertEquals("Structured Summary: Key concepts on processes and threads.", material.getSummary());

        verify(repository).save(material);
    }

    @Test
    void generateSummary_PassesExactPersistedExtractedTextToLlmClient() {
        StudyMaterial material = new StudyMaterial();
        material.setId(20L);
        material.setTitle("Discrete Math");
        String actualExtractedText = "Graph theory: vertices, edges, connectivity, and Euler paths.";
        material.setExtractedText(actualExtractedText);

        when(repository.findById(20L)).thenReturn(Optional.of(material));
        when(llmClient.generateSummary(actualExtractedText)).thenReturn("Summary of Graph Theory");
        when(repository.save(any(StudyMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generateAndSaveSummary(20L);

        // Verify that the LLM client received the exact extracted text from the entity
        verify(llmClient, times(1)).generateSummary(actualExtractedText);
        // Verify it was NOT invoked with title or any generic fallback
        verify(llmClient, never()).generateSummary("Discrete Math");
    }

    @Test
    void generateSummary_MaterialNotFound_Throws404() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.generateAndSaveSummary(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateSummary_EmptyExtractedText_Throws400() {
        StudyMaterial material = new StudyMaterial();
        material.setId(2L);
        material.setTitle("Empty Material");
        material.setExtractedText("   ");

        when(repository.findById(2L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.generateAndSaveSummary(2L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateSummary_NullExtractedText_Throws400() {
        StudyMaterial material = new StudyMaterial();
        material.setId(3L);
        material.setTitle("Null Material");
        material.setExtractedText(null);

        when(repository.findById(3L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.generateAndSaveSummary(3L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateSummary_ExceedsMaxInputChars_Throws413() {
        StudyMaterial material = new StudyMaterial();
        material.setId(4L);
        material.setTitle("Huge Material");
        material.setExtractedText("A".repeat(501));

        when(repository.findById(4L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.generateAndSaveSummary(4L));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getStatusCode());
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateSummary_UpstreamLlmFailure_PropagatesException() {
        StudyMaterial material = new StudyMaterial();
        material.setId(5L);
        material.setTitle("Math");
        material.setExtractedText("Calculus and linear algebra fundamentals.");

        when(repository.findById(5L)).thenReturn(Optional.of(material));
        when(llmClient.generateSummary(anyString())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM failed"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.generateAndSaveSummary(5L));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void getSummary_Success() {
        StudyMaterial material = new StudyMaterial();
        material.setId(10L);
        material.setTitle("Physics");
        material.setSummary("Newtonian mechanics summary");

        when(repository.findById(10L)).thenReturn(Optional.of(material));

        SummaryResponse response = service.getSummary(10L);
        assertNotNull(response);
        assertEquals("Newtonian mechanics summary", response.getSummary());
    }

    @Test
    void getSummary_NotGeneratedYet_Throws404() {
        StudyMaterial material = new StudyMaterial();
        material.setId(11L);
        material.setTitle("Chemistry");
        material.setSummary(null);

        when(repository.findById(11L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.getSummary(11L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void generateSummary_CrossUser_ThrowsForbidden() {
        com.studysync.model.User owner = new com.studysync.model.User("owner@test.edu", "hash", "Owner");
        owner.setId(100L);

        StudyMaterial material = new StudyMaterial();
        material.setId(12L);
        material.setUser(owner);
        material.setExtractedText("Valid extracted text.");

        when(repository.findById(12L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.generateAndSaveSummary(12L, 200L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(llmClient, never()).generateSummary(any());
    }

    @Test
    void getSummary_CrossUser_ThrowsForbidden() {
        com.studysync.model.User owner = new com.studysync.model.User("owner@test.edu", "hash", "Owner");
        owner.setId(100L);

        StudyMaterial material = new StudyMaterial();
        material.setId(12L);
        material.setUser(owner);
        material.setSummary("Some saved summary");

        when(repository.findById(12L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.getSummary(12L, 200L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
