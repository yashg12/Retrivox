package com.studysync.repository;

import com.studysync.model.StudyMaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialChunkRepository extends JpaRepository<StudyMaterialChunk, Long> {
    List<StudyMaterialChunk> findByStudyMaterialIdOrderByChunkIndexAsc(Long materialId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByStudyMaterialId(Long materialId);
    boolean existsByStudyMaterialId(Long materialId);
}
