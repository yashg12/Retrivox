package com.studysync.model;

import jakarta.persistence.*;

@Entity
@Table(name = "study_material_chunk", indexes = {
    @Index(name = "idx_chunk_material_id", columnList = "material_id")
})
public class StudyMaterialChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private StudyMaterial studyMaterial;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "char_start_index")
    private Integer charStartIndex;

    @Column(name = "char_end_index")
    private Integer charEndIndex;

    @Column(columnDefinition = "TEXT")
    private String embeddingJson;

    public StudyMaterialChunk() {
    }

    public StudyMaterialChunk(StudyMaterial studyMaterial, Integer chunkIndex, String content, Integer charStartIndex, Integer charEndIndex, String embeddingJson) {
        this.studyMaterial = studyMaterial;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.charStartIndex = charStartIndex;
        this.charEndIndex = charEndIndex;
        this.embeddingJson = embeddingJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudyMaterial getStudyMaterial() {
        return studyMaterial;
    }

    public void setStudyMaterial(StudyMaterial studyMaterial) {
        this.studyMaterial = studyMaterial;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCharStartIndex() {
        return charStartIndex;
    }

    public void setCharStartIndex(Integer charStartIndex) {
        this.charStartIndex = charStartIndex;
    }

    public Integer getCharEndIndex() {
        return charEndIndex;
    }

    public void setCharEndIndex(Integer charEndIndex) {
        this.charEndIndex = charEndIndex;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }
}
