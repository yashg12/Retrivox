package com.studysync.repository;

import com.studysync.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findAllByUserIdOrderByCompletedAtDesc(Long userId);
    List<QuizAttempt> findAllByUserIdAndStudyMaterialIdOrderByCompletedAtDesc(Long userId, Long studyMaterialId);
}
