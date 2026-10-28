package com.studysync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.dto.*;
import com.studysync.model.Quiz;
import com.studysync.model.QuizQuestion;
import com.studysync.model.StudyMaterial;
import com.studysync.repository.QuizAttemptRepository;
import com.studysync.repository.QuizRepository;
import com.studysync.repository.StudyMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final StudyMaterialRepository materialRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final int maxInputChars;

    private static final Set<Integer> ALLOWED_COUNTS = Set.of(5, 10, 20);
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    public QuizService(
            StudyMaterialRepository materialRepository,
            QuizRepository quizRepository,
            LlmClient llmClient,
            @Value("${studysync.llm.max-input-chars:120000}") int maxInputChars) {
        this(materialRepository, quizRepository, null, llmClient, maxInputChars);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public QuizService(
            StudyMaterialRepository materialRepository,
            QuizRepository quizRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) QuizAttemptRepository attemptRepository,
            LlmClient llmClient,
            @Value("${studysync.llm.max-input-chars:120000}") int maxInputChars) {
        this.materialRepository = materialRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
        this.maxInputChars = maxInputChars > 0 ? maxInputChars : 120000;
    }

    public QuizTestResponse generateQuiz(Long materialId, GenerateQuizRequest request) {
        return generateQuiz(materialId, request, null);
    }

    public QuizTestResponse generateQuiz(Long materialId, GenerateQuizRequest request, com.studysync.model.User currentUser) {
        int count = request.getQuestionCount() != null ? request.getQuestionCount() : 5;
        if (!ALLOWED_COUNTS.contains(count)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid questionCount: " + count + ". Supported counts are 5, 10, or 20.");
        }

        String difficulty = request.getDifficulty() != null ? request.getDifficulty().trim() : "Medium";
        if (!ALLOWED_DIFFICULTIES.contains(difficulty.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid difficulty: " + difficulty + ". Supported difficulties are Easy, Medium, or Hard.");
        }
        // Normalize difficulty casing
        String normalizedDifficulty = difficulty.substring(0, 1).toUpperCase(Locale.ROOT)
                + difficulty.substring(1).toLowerCase(Locale.ROOT);

        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found"));

        if (currentUser != null && material.getUser() != null && !currentUser.getId().equals(material.getUser().getId())) {
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

        String rawLlmResponse = llmClient.generateMcqs(extractedText.trim(), count, normalizedDifficulty);
        List<QuizQuestion> questions = parseAndValidateLlmOutput(rawLlmResponse, count);

        String quizId = UUID.randomUUID().toString();
        Long userId = currentUser != null ? currentUser.getId() : null;
        Quiz quiz = new Quiz(quizId, materialId, userId, questions.size(), normalizedDifficulty, questions);
        quizRepository.save(quiz);

        return toQuizTestResponse(quiz);
    }

    public QuizTestResponse getQuizTest(String quizId) {
        return getQuizTest(quizId, null);
    }

    public QuizTestResponse getQuizTest(String quizId, com.studysync.model.User currentUser) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id: " + quizId));

        if (currentUser != null && quiz.getUserId() != null && !currentUser.getId().equals(quiz.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this quiz.");
        }

        return toQuizTestResponse(quiz);
    }

    public QuizResultResponse submitQuiz(String quizId, SubmitQuizRequest submitRequest) {
        return submitQuiz(quizId, submitRequest, null);
    }

    public QuizResultResponse submitQuiz(String quizId, SubmitQuizRequest submitRequest, com.studysync.model.User currentUser) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id: " + quizId));

        if (currentUser != null && quiz.getUserId() != null && !currentUser.getId().equals(quiz.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this quiz.");
        }

        Map<Integer, String> studentAnswers = submitRequest.getAnswers() != null
                ? submitRequest.getAnswers()
                : Collections.emptyMap();

        int score = 0;
        List<QuestionResultDto> details = new ArrayList<>();

        for (QuizQuestion q : quiz.getQuestions()) {
            String studentChoice = studentAnswers.get(q.getId());
            boolean isCorrect = studentChoice != null && studentChoice.trim().equalsIgnoreCase(q.getCorrectAnswer().trim());
            if (isCorrect) {
                score++;
            }
            details.add(new QuestionResultDto(
                    q.getId(),
                    q.getQuestion(),
                    q.getOptions(),
                    studentChoice,
                    q.getCorrectAnswer(),
                    isCorrect,
                    q.getExplanation()
            ));
        }

        int total = quiz.getQuestions().size();
        double percentage = total > 0 ? ((double) score / total) * 100.0 : 0.0;
        // round to 2 decimal places
        percentage = Math.round(percentage * 100.0) / 100.0;

        if (currentUser != null && attemptRepository != null) {
            com.studysync.model.QuizAttempt attempt = new com.studysync.model.QuizAttempt(
                    currentUser,
                    quiz.getId(),
                    quiz.getStudyMaterialId(),
                    total,
                    score,
                    percentage
            );
            attemptRepository.save(attempt);
        }

        return new QuizResultResponse(quiz.getId(), total, score, percentage, details);
    }

    public List<com.studysync.model.QuizAttempt> getAttemptsForUser(com.studysync.model.User currentUser) {
        return getAttemptsForUser(currentUser, null);
    }

    public List<com.studysync.model.QuizAttempt> getAttemptsForUser(com.studysync.model.User currentUser, Long studyMaterialId) {
        if (currentUser == null || attemptRepository == null) {
            return Collections.emptyList();
        }
        if (studyMaterialId != null) {
            return attemptRepository.findAllByUserIdAndStudyMaterialIdOrderByCompletedAtDesc(currentUser.getId(), studyMaterialId);
        }
        return attemptRepository.findAllByUserIdOrderByCompletedAtDesc(currentUser.getId());
    }

    private List<QuizQuestion> parseAndValidateLlmOutput(String rawJson, int expectedCount) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned empty MCQ content.");
        }

        // Clean any possible markdown code blocks ```json ... ```
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastBackticks = cleaned.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastBackticks).trim();
            }
        }

        List<Map<String, Object>> parsedList;
        try {
            parsedList = objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM response was not valid JSON array: " + e.getMessage());
        }

        if (parsedList == null || parsedList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned an empty question list.");
        }

        List<QuizQuestion> validatedQuestions = new ArrayList<>();
        int idSequence = 1;

        for (Map<String, Object> item : parsedList) {
            Object qObj = item.get("question");
            Object optObj = item.get("options");
            Object ansObj = item.get("correctAnswer");
            Object expObj = item.get("explanation");

            if (qObj == null || !(optObj instanceof List<?>) || ansObj == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Malformed question structure from LLM.");
            }

            String questionText = String.valueOf(qObj).trim();
            String correctAnswer = String.valueOf(ansObj).trim();
            String explanation = expObj != null ? String.valueOf(expObj).trim() : "Correct answer based on study material.";

            List<?> rawOptions = (List<?>) optObj;
            if (rawOptions.size() != 4) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Question does not have exactly 4 options: " + questionText);
            }

            List<String> options = rawOptions.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .collect(Collectors.toList());

            // Check that correct answer matches one of the options
            boolean found = options.stream().anyMatch(opt -> opt.equalsIgnoreCase(correctAnswer));
            if (!found) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Correct answer does not match any of the provided 4 options.");
            }

            validatedQuestions.add(new QuizQuestion(idSequence++, questionText, options, correctAnswer, explanation));
        }

        return validatedQuestions;
    }

    private QuizTestResponse toQuizTestResponse(Quiz quiz) {
        List<QuizTestQuestionDto> testQuestions = quiz.getQuestions().stream()
                .map(q -> new QuizTestQuestionDto(q.getId(), q.getQuestion(), q.getOptions()))
                .collect(Collectors.toList());

        return new QuizTestResponse(
                quiz.getId(),
                quiz.getStudyMaterialId(),
                quiz.getQuestionCount(),
                quiz.getDifficulty(),
                testQuestions,
                quiz.getCreatedAt()
        );
    }
}
