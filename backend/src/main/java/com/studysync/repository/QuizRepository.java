package com.studysync.repository;

import com.studysync.model.Quiz;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class QuizRepository {
    private final Map<String, Quiz> quizStore = new ConcurrentHashMap<>();

    public Quiz save(Quiz quiz) {
        quizStore.put(quiz.getId(), quiz);
        return quiz;
    }

    public Optional<Quiz> findById(String id) {
        return Optional.ofNullable(quizStore.get(id));
    }
}
