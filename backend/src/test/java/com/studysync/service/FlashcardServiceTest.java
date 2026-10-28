package com.studysync.service;

import com.studysync.dto.FlashcardsResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.model.User;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlashcardServiceTest {

    @Mock
    private StudyMaterialRepository materialRepository;

    @Mock
    private LlmClient llmClient;

    private FlashcardService flashcardService;
    private User owner;
    private User otherUser;
    private StudyMaterial material;

    @BeforeEach
    void setUp() {
        flashcardService = new FlashcardService(materialRepository, llmClient, 500);

        owner = new User("owner@test.edu", "hash", "Owner");
        owner.setId(10L);

        otherUser = new User("other@test.edu", "hash", "Other");
        otherUser.setId(20L);

        material = new StudyMaterial();
        material.setId(1L);
        material.setTitle("Photosynthesis Notes");
        material.setExtractedText("Photosynthesis converts light into chemical energy.");
        material.setUser(owner);
    }

    @Test
    void generateFlashcards_Success() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        String mockJson = "[\n" +
                "  {\"id\": 1, \"front\": \"What is photosynthesis?\", \"back\": \"Process converting light to energy.\"},\n" +
                "  {\"id\": 2, \"front\": \"What is chlorophyll?\", \"back\": \"Green pigment in chloroplasts.\"}\n" +
                "]";
        when(llmClient.generateFlashcards(anyString(), eq(5))).thenReturn(mockJson);

        FlashcardsResponse response = flashcardService.generateFlashcards(1L, 5, owner);

        assertNotNull(response);
        assertEquals(1L, response.getStudyMaterialId());
        assertEquals(2, response.getCount());
        assertEquals(2, response.getFlashcards().size());
        assertEquals("What is photosynthesis?", response.getFlashcards().get(0).getFront());
        assertEquals("Process converting light to energy.", response.getFlashcards().get(0).getBack());
    }

    @Test
    void generateFlashcards_CrossUser_ThrowsForbidden() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                flashcardService.generateFlashcards(1L, 5, otherUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(llmClient, never()).generateFlashcards(anyString(), anyInt());
    }

    @Test
    void generateFlashcards_MissingMaterial_ThrowsNotFound() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                flashcardService.generateFlashcards(999L, 5, owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void generateFlashcards_EmptyExtractedText_ThrowsBadRequest() {
        material.setExtractedText("");
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                flashcardService.generateFlashcards(1L, 5, owner));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void generateFlashcards_MarkdownCodeFences_ParsedCleanly() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        String fencedJson = "```json\n" +
                "[\n" +
                "  {\"id\": 1, \"front\": \"Concept A\", \"back\": \"Definition A\"}\n" +
                "]\n" +
                "```";
        when(llmClient.generateFlashcards(anyString(), eq(5))).thenReturn(fencedJson);

        FlashcardsResponse response = flashcardService.generateFlashcards(1L, 5, owner);
        assertEquals(1, response.getCount());
        assertEquals("Concept A", response.getFlashcards().get(0).getFront());
    }
}
