package com.studysync.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "study_material", indexes = {
    @Index(name = "idx_study_material_user_id", columnList = "user_id")
})
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "studyMaterial", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<StudyMaterialChunk> chunks = new java.util.ArrayList<>();

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getUserId() { return user != null ? user.getId() : null; }

    public java.util.List<StudyMaterialChunk> getChunks() { return chunks; }
    public void setChunks(java.util.List<StudyMaterialChunk> chunks) { this.chunks = chunks; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
