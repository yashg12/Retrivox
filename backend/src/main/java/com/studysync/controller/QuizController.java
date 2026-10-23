package com.studysync.controller;

import com.studysync.dto.GenerateQuizRequest;
import com.studysync.dto.QuizResultResponse;
import com.studysync.dto.QuizTestResponse;
import com.studysync.dto.SubmitQuizRequest;
import com.studysync.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class QuizController {

    private final QuizService quizService;
    private final com.studysync.service.AuthService authService;

    public QuizController(QuizService quizService, com.studysync.service.AuthService authService) {
        this.quizService = quizService;
        this.authService = authService;
    }

    @PostMapping("/study-materials/{id}/quizzes")
    public ResponseEntity<QuizTestResponse> generateQuiz(
            @PathVariable Long id,
            @RequestBody(required = false) GenerateQuizRequest request) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        GenerateQuizRequest actualRequest = request != null ? request : new GenerateQuizRequest();
        QuizTestResponse testResponse = quizService.generateQuiz(id, actualRequest, currentUser);
        return new ResponseEntity<>(testResponse, HttpStatus.CREATED);
    }

    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizTestResponse> getQuizTest(@PathVariable String quizId) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        QuizTestResponse testResponse = quizService.getQuizTest(quizId, currentUser);
        return ResponseEntity.ok(testResponse);
    }

    @PostMapping("/quizzes/{quizId}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable String quizId,
            @RequestBody @Validated SubmitQuizRequest submitRequest) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        QuizResultResponse result = quizService.submitQuiz(quizId, submitRequest, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/quizzes/attempts")
    public ResponseEntity<java.util.List<com.studysync.model.QuizAttempt>> getQuizAttempts() {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(quizService.getAttemptsForUser(currentUser));
    }
}
