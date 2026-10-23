package com.studysync.service;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleEmbeddingClient(
            @Value("${studysync.embedding.api-key:${studysync.llm.api-key:}}") String apiKey,
            @Value("${studysync.embedding.base-url:${studysync.llm.base-url:https://api.openai.com/v1}}") String baseUrl,
            @Value("${studysync.embedding.model:}") String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "https://api.openai.com/v1";
        if (model != null && !model.isBlank()) {
            this.model = model.trim();
        } else if (this.baseUrl.contains("googleapis.com")) {
            this.model = "gemini-embedding-001";
        } else {
            this.model = "text-embedding-3-small";
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text for embedding generation cannot be empty.");
        }
        List<List<Float>> results = generateEmbeddings(List.of(text));
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding service returned empty result.");
        }
        return results.get(0);
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        if (apiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Embedding service is not configured. Missing API key in environment variables.");
        }
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", texts
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize embedding request");
        }

        String endpoint = baseUrl + "/embeddings";
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach embedding service provider.");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            String errorMsg = "Upstream embedding API error with status " + statusCode;
            if (statusCode == 401 || statusCode == 403) {
                errorMsg = "Authentication failed with embedding API provider.";
            } else if (statusCode == 429) {
                errorMsg = "Rate limit or quota exceeded with embedding API provider.";
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMsg);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray() || dataNode.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding service returned malformed data array.");
            }

            // OpenAI embedding format: data array where each element has "embedding" array and "index"
            // Sort by index to maintain original order
            List<JsonNode> items = new ArrayList<>();
            for (JsonNode item : dataNode) {
                items.add(item);
            }
            items.sort((a, b) -> Integer.compare(a.path("index").asInt(0), b.path("index").asInt(0)));

            List<List<Float>> embeddings = new ArrayList<>();
            for (JsonNode item : items) {
                JsonNode vecNode = item.path("embedding");
                if (!vecNode.isArray() || vecNode.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Malformed vector in embedding response.");
                }
                List<Float> vector = new ArrayList<>();
                for (JsonNode num : vecNode) {
                    if (!num.isNumber()) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Non-numeric value in embedding vector.");
                    }
                    vector.add((float) num.asDouble());
                }
                embeddings.add(vector);
            }

            return embeddings;
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse response from embedding API.");
        }
    }
}
