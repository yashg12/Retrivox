package com.studysync.controller;

import com.studysync.dto.AskQuestionRequest;
import com.studysync.dto.CreateStudyMaterialRequest;
import com.studysync.dto.QuestionAnswerResponse;
import com.studysync.dto.SummaryResponse;
import com.studysync.model.StudyMaterial;
import com.studysync.repository.StudyMaterialRepository;
import com.studysync.service.RagService;
import com.studysync.service.StudySummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.List;

@RestController
@RequestMapping("/api/study-materials")
@Validated
public class StudyMaterialController {

    private final StudyMaterialRepository repository;
    private final StudySummaryService summaryService;
    private final RagService ragService;
    private final com.studysync.service.FlashcardService flashcardService;
    private final com.studysync.service.AuthService authService;

    public StudyMaterialController(
            StudyMaterialRepository repository,
            StudySummaryService summaryService,
            RagService ragService,
            com.studysync.service.FlashcardService flashcardService,
            com.studysync.service.AuthService authService) {
        this.repository = repository;
        this.summaryService = summaryService;
        this.ragService = ragService;
        this.flashcardService = flashcardService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<StudyMaterial> create(@RequestBody @Validated CreateStudyMaterialRequest request) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        StudyMaterial material = new StudyMaterial();
        material.setTitle(request.getTitle());
        material.setSubject(request.getSubject());
        material.setFileName(request.getFileName());
        material.setUser(currentUser);
        StudyMaterial saved = repository.save(material);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public List<StudyMaterial> listAll() {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        return repository.findAllByUser_IdOrderByCreatedAtDesc(currentUser.getId());
    }

    @GetMapping("/{id}")
    public StudyMaterial getById(@PathVariable Long id) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        StudyMaterial material = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found"));
        if (material.getUser() != null && !currentUser.getId().equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }
        return material;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        StudyMaterial material = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found"));
        if (material.getUser() != null && !currentUser.getId().equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }
        ragService.clearChunksForMaterial(id);
        repository.delete(material);
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<String> uploadDocument(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file cannot be empty.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds limit of 5 MB.");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        boolean isPdf = "application/pdf".equals(contentType) || fileName.endsWith(".pdf");
        boolean isText = "text/plain".equals(contentType) || "text/markdown".equals(contentType)
                || fileName.endsWith(".txt") || fileName.endsWith(".md");

        if (!isPdf && !isText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type. Supported formats: PDF, TXT, MD.");
        }

        StudyMaterial material = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "StudyMaterial not found"));
        if (material.getUser() != null && !currentUser.getId().equals(material.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this study material.");
        }

        String extracted = "";
        if (isPdf) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                extracted = stripper.getText(document).trim();
            } catch (java.io.IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process PDF file.");
            }
        } else {
            try {
                extracted = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read text file content.");
            }
        }

        if (extracted.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extracted text is empty.");
        }

        material.setExtractedText(extracted);
        ragService.clearChunksForMaterial(id);
        repository.save(material);
        return ResponseEntity.ok("Document text extracted and saved.");
    }

    @PostMapping("/{id}/summary")
    public ResponseEntity<SummaryResponse> generateSummary(@PathVariable Long id) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        SummaryResponse response = summaryService.generateAndSaveSummary(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<SummaryResponse> getSummary(@PathVariable Long id) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        SummaryResponse response = summaryService.getSummary(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<QuestionAnswerResponse> askQuestion(
            @PathVariable Long id,
            @RequestBody @Validated AskQuestionRequest request) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        QuestionAnswerResponse response = ragService.askQuestion(id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/flashcards")
    public ResponseEntity<com.studysync.dto.FlashcardsResponse> generateFlashcards(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int count) {
        com.studysync.model.User currentUser = authService.getCurrentAuthenticatedUser();
        com.studysync.dto.FlashcardsResponse response = flashcardService.generateFlashcards(id, count, currentUser);
        return ResponseEntity.ok(response);
    }
}


