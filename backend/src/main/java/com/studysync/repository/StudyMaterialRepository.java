package com.studysync.repository;

import com.studysync.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<StudyMaterial> findByIdAndUser_Id(Long id, Long userId);
    boolean existsByIdAndUser_Id(Long id, Long userId);
    List<StudyMaterial> findByUserIsNull();
}
