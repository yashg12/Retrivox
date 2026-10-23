package com.studysync.service;

import com.studysync.dto.GenerateQuizRequest;
import com.studysync.dto.QuizResultResponse;
import com.studysync.dto.QuizTestResponse;
import com.studysync.dto.SubmitQuizRequest;
import com.studysync.model.Quiz;
import com.studysync.model.QuizQuestion;
import com.studysync.model.StudyMaterial;
import com.studysync.repository.QuizRepository;
import com.studysync.repository.StudyMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuizServiceTest {

    @Mock
    private StudyMaterialRepository materialRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private com.studysync.repository.QuizAttemptRepository attemptRepository;

    @Mock
    private LlmClient llmClient;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(materialRepository, quizRepository, attemptRepository, llmClient, 500);
    }

    private String createValidMockLlmOutput(int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i <= count; i++) {
            if (i > 1) sb.append(",");
            sb.append(String.format("{\n" +
                    "  \"question\": \"Question %d\",\n" +
                    "  \"options\": [\"Option A%d\", \"Option B%d\", \"Option C%d\", \"Option D%d\"],\n" +
                    "  \"correctAnswer\": \"Option B%d\",\n" +
                    "  \"explanation\": \"Explanation for question %d\"\n" +
                    "}", i, i, i, i, i, i, i));
        }
        sb.append("]");
        return sb.toString();
    }

    @Test
    void generateQuiz_Success_ReturnsTestWithoutAnswers() {
        StudyMaterial material = new StudyMaterial();
        material.setId(100L);
        material.setTitle("Operating Systems");
        material.setExtractedText("Operating system processes manage computer resources and memory.");

        when(materialRepository.findById(100L)).thenReturn(Optional.of(material));
        when(llmClient.generateMcqs(material.getExtractedText(), 5, "Medium"))
                .thenReturn(createValidMockLlmOutput(5));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateQuizRequest request = new GenerateQuizRequest(5, "Medium");
        QuizTestResponse response = quizService.generateQuiz(100L, request);

        assertNotNull(response);
        assertNotNull(response.getQuizId());
        assertEquals(5, response.getQuestions().size());
        assertEquals("Medium", response.getDifficulty());

        // Crucial security requirement: verify test-taking response does not expose answers or explanations
        assertEquals("Question 1", response.getQuestions().get(0).getQuestion());
        assertEquals(4, response.getQuestions().get(0).getOptions().size());

        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void generateQuiz_InvalidCount_Throws400() {
        GenerateQuizRequest request = new GenerateQuizRequest(7, "Medium");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("5, 10, or 20"));
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateQuiz_InvalidDifficulty_Throws400() {
        GenerateQuizRequest request = new GenerateQuizRequest(5, "Impossible");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Easy, Medium, or Hard"));
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateQuiz_MissingMaterial_Throws404() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        GenerateQuizRequest request = new GenerateQuizRequest(5, "Easy");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(999L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateQuiz_EmptyExtractedText_Throws400() {
        StudyMaterial material = new StudyMaterial();
        material.setId(101L);
        material.setExtractedText("   ");

        when(materialRepository.findById(101L)).thenReturn(Optional.of(material));

        GenerateQuizRequest request = new GenerateQuizRequest(5, "Easy");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(101L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(llmClient);
    }

    @Test
    void generateQuiz_MalformedLlmOutput_Throws502() {
        StudyMaterial material = new StudyMaterial();
        material.setId(102L);
        material.setExtractedText("Valid text");

        when(materialRepository.findById(102L)).thenReturn(Optional.of(material));
        when(llmClient.generateMcqs(any(), eq(5), any())).thenReturn("not a json array");

        GenerateQuizRequest request = new GenerateQuizRequest(5, "Medium");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(102L, request));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    @Test
    void generateQuiz_OptionsCountNotFour_Throws502() {
        StudyMaterial material = new StudyMaterial();
        material.setId(103L);
        material.setExtractedText("Valid text");

        String invalidOptionsJson = "[{\"question\":\"Q1\",\"options\":[\"A\",\"B\",\"C\"],\"correctAnswer\":\"A\",\"explanation\":\"Exp\"}]";
        when(materialRepository.findById(103L)).thenReturn(Optional.of(material));
        when(llmClient.generateMcqs(any(), eq(5), any())).thenReturn(invalidOptionsJson);

        GenerateQuizRequest request = new GenerateQuizRequest(5, "Medium");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(103L, request));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    @Test
    void submitQuiz_CalculatesScoreCorrectlyOnServer() {
        QuizQuestion q1 = new QuizQuestion(1, "Q1", List.of("A", "B", "C", "D"), "B", "Exp 1");
        QuizQuestion q2 = new QuizQuestion(2, "Q2", List.of("A", "B", "C", "D"), "C", "Exp 2");
        Quiz quiz = new Quiz("quiz-123", 100L, 2, "Medium", List.of(q1, q2));

        when(quizRepository.findById("quiz-123")).thenReturn(Optional.of(quiz));

        // Submit answer 1 as correct ("B"), answer 2 as wrong ("A")
        SubmitQuizRequest request = new SubmitQuizRequest(Map.of(1, "B", 2, "A"));
        QuizResultResponse result = quizService.submitQuiz("quiz-123", request);

        assertNotNull(result);
        assertEquals("quiz-123", result.getQuizId());
        assertEquals(2, result.getTotalQuestions());
        assertEquals(1, result.getScore());
        assertEquals(50.0, result.getPercentage());
        assertEquals(2, result.getDetails().size());

        assertTrue(result.getDetails().get(0).isCorrect());
        assertEquals("B", result.getDetails().get(0).getCorrectAnswer());

        assertFalse(result.getDetails().get(1).isCorrect());
        assertEquals("A", result.getDetails().get(1).getSelectedAnswer());
        assertEquals("C", result.getDetails().get(1).getCorrectAnswer());
        assertEquals("Exp 2", result.getDetails().get(1).getExplanation());
    }

    @Test
    void submitQuiz_QuizNotFound_Throws404() {
        when(quizRepository.findById("missing-id")).thenReturn(Optional.empty());

        SubmitQuizRequest request = new SubmitQuizRequest(Map.of());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.submitQuiz("missing-id", request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void generateQuiz_CrossUser_ThrowsForbidden() {
        com.studysync.model.User owner = new com.studysync.model.User("owner@test.edu", "hash", "Owner");
        owner.setId(10L);
        com.studysync.model.User attacker = new com.studysync.model.User("attacker@test.edu", "hash", "Attacker");
        attacker.setId(20L);

        StudyMaterial material = new StudyMaterial();
        material.setId(1L);
        material.setUser(owner);
        material.setExtractedText("Valid text content for quiz generation testing.");

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        GenerateQuizRequest request = new GenerateQuizRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz(1L, request, attacker));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(llmClient, never()).generateMcqs(any(), anyInt(), any());
    }

    @Test
    void getQuizTest_CrossUser_ThrowsForbidden() {
        Quiz quiz = new Quiz("quiz-1", 1L, 10L, 5, "Medium", List.of());
        when(quizRepository.findById("quiz-1")).thenReturn(Optional.of(quiz));

        com.studysync.model.User attacker = new com.studysync.model.User("attacker@test.edu", "hash", "Attacker");
        attacker.setId(20L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.getQuizTest("quiz-1", attacker));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void submitQuiz_CrossUser_ThrowsForbidden() {
        Quiz quiz = new Quiz("quiz-1", 1L, 10L, 5, "Medium", List.of());
        when(quizRepository.findById("quiz-1")).thenReturn(Optional.of(quiz));

        com.studysync.model.User attacker = new com.studysync.model.User("attacker@test.edu", "hash", "Attacker");
        attacker.setId(20L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.submitQuiz("quiz-1", new SubmitQuizRequest(), attacker));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submitQuiz_Owner_SavesQuizAttempt() {
        com.studysync.model.User owner = new com.studysync.model.User("owner@test.edu", "hash", "Owner");
        owner.setId(10L);

        QuizQuestion q = new QuizQuestion(1, "Q?", List.of("A", "B", "C", "D"), "A", "Explanation");
        Quiz quiz = new Quiz("quiz-1", 1L, 10L, 1, "Medium", List.of(q));
        when(quizRepository.findById("quiz-1")).thenReturn(Optional.of(quiz));

        SubmitQuizRequest request = new SubmitQuizRequest(Map.of(1, "A"));
        QuizResultResponse response = quizService.submitQuiz("quiz-1", request, owner);

        assertNotNull(response);
        assertEquals(1, response.getScore());
        verify(attemptRepository).save(argThat(attempt ->
                attempt.getUser().getId().equals(10L) &&
                "quiz-1".equals(attempt.getQuizId()) &&
                attempt.getScore() == 1
        ));
    }
}
