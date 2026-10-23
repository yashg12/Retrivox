package com.studysync.dto;

import java.time.Instant;

public class SummaryResponse {
    private Long id;
    private String title;
    private String summary;
    private Instant createdAt;

    public SummaryResponse() {}

    public SummaryResponse(Long id, String title, String summary, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
