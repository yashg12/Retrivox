package com.studysync.dto;

public class ChunkSourceDto {
    private int chunkIndex;
    private String snippet;
    private double similarity;

    public ChunkSourceDto() {}

    public ChunkSourceDto(int chunkIndex, String snippet, double similarity) {
        this.chunkIndex = chunkIndex;
        this.snippet = snippet;
        this.similarity = similarity;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
