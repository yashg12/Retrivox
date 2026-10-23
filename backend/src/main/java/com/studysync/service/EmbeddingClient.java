package com.studysync.service;

import java.util.List;

public interface EmbeddingClient {
    List<Float> generateEmbedding(String text);
    List<List<Float>> generateEmbeddings(List<String> texts);
}
