package com.studysync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(
            @Value("${studysync.llm.api-key:}") String apiKey,
            @Value("${studysync.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${studysync.llm.model:gpt-4o-mini}") String model,
            @Value("${studysync.llm.max-tokens:1000}") int maxTokens) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "https://api.openai.com/v1";
        this.model = model != null && !model.isBlank() ? model.trim() : "gpt-4o-mini";
        this.maxTokens = maxTokens > 0 ? maxTokens : 1000;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateSummary(String extractedText) {
        if (apiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM service is not configured. Missing API key in environment variables.");
        }

        String systemPrompt = "You are an expert academic tutor. Your task is to summarize ONLY the provided study material. "
                + "Strict rules:\n"
                + "1. Rely strictly on facts mentioned directly in the provided text. Do NOT assume, extrapolate, or bring in outside knowledge.\n"
                + "2. Do NOT invent facts or hallucinate details beyond what is explicitly stated in the source text.\n"
                + "3. If the provided text is too brief, fragmentary, or lacks sufficient details to formulate a full study summary, explicitly state that the source material contains insufficient information to produce a detailed summary.\n"
                + "4. Structure the summary cleanly with Key Points and Takeaways based solely on the provided text.";
        String userPrompt = "Here is the exact extracted text from the study material:\n"
                + "--- START SOURCE TEXT ---\n"
                + extractedText
                + "\n--- END SOURCE TEXT ---\n\n"
                + "Provide a summary grounded strictly in the source text above:";

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.3
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize LLM request");
        }

        String endpoint = baseUrl + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach LLM service provider.");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            String errorMsg = "Upstream LLM API error with status " + statusCode;
            if (statusCode == 401 || statusCode == 403) {
                errorMsg = "Authentication failed with LLM API provider.";
            } else if (statusCode == 429) {
                errorMsg = "Rate limit or quota exceeded with LLM API provider.";
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMsg);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned empty or malformed summary content.");
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse response from LLM API.");
        }
    }

    @Override
    public String generateMcqs(String extractedText, int count, String difficulty) {
        if (apiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM service is not configured. Missing API key in environment variables.");
        }

        String systemPrompt = "You are an expert exam creator and academic examiner. Your task is to generate exactly " + count
                + " multiple-choice questions (MCQs) of " + difficulty + " difficulty based SOLELY on the provided study material.\n\n"
                + "Strict rules:\n"
                + "1. Rely strictly on facts mentioned directly in the provided text. Do NOT assume, extrapolate, or invent facts.\n"
                + "2. If the text does not contain enough information to formulate " + count + " questions, you must still return ONLY valid questions derived strictly from the text.\n"
                + "3. Output MUST be a valid JSON array and NOTHING ELSE (no markdown fences, no surrounding commentary).\n"
                + "4. Each element in the JSON array must follow this exact structure:\n"
                + "   {\n"
                + "     \"question\": \"The question text\",\n"
                + "     \"options\": [\"Option A text\", \"Option B text\", \"Option C text\", \"Option D text\"],\n"
                + "     \"correctAnswer\": \"Option A text\",\n"
                + "     \"explanation\": \"Brief explanation grounded in the text why this option is correct.\"\n"
                + "   }\n"
                + "5. 'options' must contain exactly 4 unique strings.\n"
                + "6. 'correctAnswer' must EXACTLY match one of the 4 strings in 'options'.";

        String userPrompt = "Here is the exact source text from the study material:\n"
                + "--- START SOURCE TEXT ---\n"
                + extractedText
                + "\n--- END SOURCE TEXT ---\n\n"
                + "Generate " + count + " MCQs (" + difficulty + " difficulty) as a valid JSON array now:";

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 2500,
                "temperature", 0.2
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize LLM request");
        }

        String endpoint = baseUrl + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach LLM service provider.");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            String errorMsg = "Upstream LLM API error with status " + statusCode;
            if (statusCode == 401 || statusCode == 403) {
                errorMsg = "Authentication failed with LLM API provider.";
            } else if (statusCode == 429) {
                errorMsg = "Rate limit or quota exceeded with LLM API provider.";
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMsg);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned empty or malformed MCQ content.");
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse response from LLM API.");
        }
    }

    @Override
    public String answerQuestion(String question, List<String> contextChunks) {
        if (apiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM service is not configured. Missing API key in environment variables.");
        }

        StringBuilder contextBuilder = new StringBuilder();
        if (contextChunks != null && !contextChunks.isEmpty()) {
            for (int i = 0; i < contextChunks.size(); i++) {
                contextBuilder.append("--- SOURCE CHUNK ").append(i + 1).append(" ---\n")
                        .append(contextChunks.get(i).trim())
                        .append("\n\n");
            }
        }

        String systemPrompt = "You are an expert academic tutor assisting students. "
                + "Your task is to answer the student's question based EXCLUSIVELY on the provided source excerpts.\n\n"
                + "Strict rules:\n"
                + "1. Rely strictly on facts mentioned directly in the provided source excerpts. Do NOT assume, extrapolate, or bring in outside knowledge.\n"
                + "2. Do NOT invent facts or hallucinate details beyond what is explicitly stated.\n"
                + "3. If the provided excerpts do not contain enough facts to answer the question, or if no excerpts are provided, your entire answer MUST state:\n"
                + "   \"The provided study material does not contain sufficient information to answer this question.\"\n"
                + "4. Keep your answer clear, direct, and factual.";

        String userPrompt = "Here are the relevant source excerpts from the study material:\n\n"
                + contextBuilder.toString()
                + "Student question: " + question + "\n\n"
                + "Answer based strictly on the source excerpts:";

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.1
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize LLM request");
        }

        String endpoint = baseUrl + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach LLM service provider.");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            String errorMsg = "Upstream LLM API error with status " + statusCode;
            if (statusCode == 401 || statusCode == 403) {
                errorMsg = "Authentication failed with LLM API provider.";
            } else if (statusCode == 429) {
                errorMsg = "Rate limit or quota exceeded with LLM API provider.";
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMsg);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned empty or malformed answer content.");
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse response from LLM API.");
        }
    }

    @Override
    public String generateFlashcards(String extractedText, int count) {
        if (apiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM service is not configured. Missing API key in environment variables.");
        }

        int targetCount = count > 0 ? count : 5;
        String systemPrompt = "You are an expert study aid and flashcard generator. Your task is to generate exactly " + targetCount
                + " high-yield study flashcards based SOLELY on the provided study material.\n\n"
                + "Strict rules:\n"
                + "1. Rely strictly on facts mentioned directly in the provided text. Do NOT assume, extrapolate, or invent facts.\n"
                + "2. 'front' should be a concise question or concept term.\n"
                + "3. 'back' should be a clear, accurate explanation or answer grounded in the source text.\n"
                + "4. Output MUST be a valid JSON array and NOTHING ELSE (no markdown fences, no surrounding commentary).\n"
                + "5. Format:\n"
                + "[\n"
                + "  {\n"
                + "    \"id\": 1,\n"
                + "    \"front\": \"What is ...?\",\n"
                + "    \"back\": \"...\"\n"
                + "  }\n"
                + "]";

        String userPrompt = "Generate " + targetCount + " flashcards from this study material:\n\n" + extractedText.trim();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
        );

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize LLM request.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach LLM service provider.");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            String errorMsg = "Upstream LLM API error with status " + statusCode;
            if (statusCode == 401 || statusCode == 403) {
                errorMsg = "Authentication failed with LLM API provider.";
            } else if (statusCode == 429) {
                errorMsg = "Rate limit or quota exceeded with LLM API provider.";
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMsg);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM returned empty or malformed flashcard content.");
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse response from LLM API.");
        }
    }
}


