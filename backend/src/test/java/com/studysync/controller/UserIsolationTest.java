package com.studysync.controller;

import com.studysync.dto.AskQuestionRequest;
import com.studysync.dto.CreateStudyMaterialRequest;
import com.studysync.dto.GenerateQuizRequest;
import com.studysync.dto.SubmitQuizRequest;
import com.studysync.model.Quiz;
import com.studysync.model.StudyMaterial;
import com.studysync.model.User;
import com.studysync.repository.QuizAttemptRepository;
import com.studysync.repository.QuizRepository;
import com.studysync.repository.StudyMaterialRepository;
import com.studysync.service.AuthService;
import com.studysync.service.QuizService;
import com.studysync.service.RagService;
import com.studysync.service.StudySummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserIsolationTest {

    @Mock
    private StudyMaterialRepository materialRepository;

    @Mock
    private StudySummaryService summaryService;

    @Mock
    private RagService ragService;

    @Mock
    private com.studysync.service.FlashcardService flashcardService;

    @Mock
    private AuthService authService;

    @Mock
    private QuizService quizService;

    private StudyMaterialController materialController;
    private QuizController quizController;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        materialController = new StudyMaterialController(materialRepository, summaryService, ragService, flashcardService, authService);
        quizController = new QuizController(quizService, authService);

        userA = new User("userA@test.edu", "hashA", "User A");
        userA.setId(101L);

        userB = new User("userB@test.edu", "hashB", "User B");
        userB.setId(202L);
    }

    @Test
    void createMaterial_AssignsCurrentAuthenticatedUser() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userA);
        when(materialRepository.save(any(StudyMaterial.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateStudyMaterialRequest request = new CreateStudyMaterialRequest();
        request.setTitle("User A Physics");
        request.setSubject("Physics");
        request.setFileName("physics.pdf");

        ResponseEntity<StudyMaterial> response = materialController.create(request);

        assertNotNull(response.getBody());
        assertEquals(userA, response.getBody().getUser());
        assertEquals(userA.getId(), response.getBody().getUserId());
    }

    @Test
    void listAll_OnlyReturnsCurrentUsersMaterials() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userA);

        StudyMaterial matA = new StudyMaterial();
        matA.setId(1L);
        matA.setTitle("User A Material");
        matA.setUser(userA);

        when(materialRepository.findAllByUser_IdOrderByCreatedAtDesc(101L)).thenReturn(List.of(matA));

        List<StudyMaterial> result = materialController.listAll();

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getUserId());
        verify(materialRepository).findAllByUser_IdOrderByCreatedAtDesc(101L);
        verify(materialRepository, never()).findAll();
    }

    @Test
    void getById_CrossUserAccess_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userB);

        StudyMaterial matA = new StudyMaterial();
        matA.setId(1L);
        matA.setUser(userA);

        when(materialRepository.findById(1L)).thenReturn(Optional.of(matA));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> materialController.getById(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteById_CrossUserAccess_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userB);

        StudyMaterial matA = new StudyMaterial();
        matA.setId(1L);
        matA.setUser(userA);

        when(materialRepository.findById(1L)).thenReturn(Optional.of(matA));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> materialController.deleteById(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(materialRepository, never()).delete(any());
    }

    @Test
    void uploadPdf_CrossUserAccess_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userB);

        StudyMaterial matA = new StudyMaterial();
        matA.setId(1L);
        matA.setUser(userA);

        when(materialRepository.findById(1L)).thenReturn(Optional.of(matA));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> materialController.uploadDocument(1L, file));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(materialRepository, never()).save(any());
    }

    @Test
    void generateSummary_CallsServiceWithCurrentUserId() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userA);

        materialController.generateSummary(1L);

        verify(summaryService).generateAndSaveSummary(1L, 101L);
    }

    @Test
    void askQuestion_CallsServiceWithCurrentUserId() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userA);
        AskQuestionRequest request = new AskQuestionRequest();
        request.setQuestion("What is velocity?");

        materialController.askQuestion(1L, request);

        verify(ragService).askQuestion(1L, request, 101L);
    }

    @Test
    void quizOperations_CrossUserAccess_EnforcesOwnership() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userB);

        // When User B calls generateQuiz on material owned by User A, quizService throws 403
        when(quizService.generateQuiz(eq(1L), any(GenerateQuizRequest.class), eq(userB)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material."));

        ResponseStatusException exGen = assertThrows(ResponseStatusException.class,
                () -> quizController.generateQuiz(1L, new GenerateQuizRequest()));
        assertEquals(HttpStatus.FORBIDDEN, exGen.getStatusCode());

        // When User B tries to view User A's quiz
        when(quizService.getQuizTest(eq("quiz-of-user-a"), eq(userB)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this quiz."));

        ResponseStatusException exGet = assertThrows(ResponseStatusException.class,
                () -> quizController.getQuizTest("quiz-of-user-a"));
        assertEquals(HttpStatus.FORBIDDEN, exGet.getStatusCode());

        // When User B tries to submit answers to User A's quiz
        when(quizService.submitQuiz(eq("quiz-of-user-a"), any(SubmitQuizRequest.class), eq(userB)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this quiz."));

        ResponseStatusException exSub = assertThrows(ResponseStatusException.class,
                () -> quizController.submitQuiz("quiz-of-user-a", new SubmitQuizRequest()));
        assertEquals(HttpStatus.FORBIDDEN, exSub.getStatusCode());
    }

    @Test
    void flashcardOperations_CrossUserAccess_EnforcesOwnership() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(userB);
        when(flashcardService.generateFlashcards(eq(1L), eq(10), eq(userB)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> materialController.generateFlashcards(1L, 10));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}

