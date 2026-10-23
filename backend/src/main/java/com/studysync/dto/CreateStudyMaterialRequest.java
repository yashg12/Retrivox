package com.studysync.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateStudyMaterialRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotBlank(message = "Subject must not be blank")
    private String subject;

    @NotBlank(message = "File name must not be blank")
    private String fileName;

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
