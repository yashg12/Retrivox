package com.studysync.service;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    public record ChunkSpan(int chunkIndex, String content, int charStartIndex, int charEndIndex) {}

    private final int targetChunkSize;
    private final int chunkOverlap;

    public TextChunker() {
        this(1000, 200);
    }

    public TextChunker(int targetChunkSize, int chunkOverlap) {
        if (targetChunkSize <= 0) {
            throw new IllegalArgumentException("targetChunkSize must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= targetChunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be non-negative and less than targetChunkSize");
        }
        this.targetChunkSize = targetChunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<ChunkSpan> chunk(String text) {
        List<ChunkSpan> spans = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return spans;
        }

        int textLength = text.length();
        if (textLength <= targetChunkSize) {
            spans.add(new ChunkSpan(0, text.trim(), 0, textLength));
            return spans;
        }

        int start = 0;
        int chunkIdx = 0;

        while (start < textLength) {
            int end = Math.min(start + targetChunkSize, textLength);

            if (end < textLength) {
                // Try to find a sentence boundary (. ! ? \n) within the last 30% of the chunk window
                int searchStart = Math.max(start + (int) (targetChunkSize * 0.7), start);
                int sentenceBreak = findSentenceBoundary(text, searchStart, end);
                if (sentenceBreak != -1) {
                    end = sentenceBreak;
                } else {
                    // Fall back to a whitespace boundary
                    int spaceBreak = findWhitespaceBoundary(text, searchStart, end);
                    if (spaceBreak != -1) {
                        end = spaceBreak;
                    }
                }
            }

            String chunkContent = text.substring(start, end).trim();
            if (!chunkContent.isEmpty()) {
                spans.add(new ChunkSpan(chunkIdx++, chunkContent, start, end));
            }

            if (end >= textLength) {
                break;
            }

            // Calculate next start with overlap
            int nextStart = end - chunkOverlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return spans;
    }

    private int findSentenceBoundary(String text, int searchStart, int searchEnd) {
        for (int i = searchEnd - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?' || c == '\n') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (Character.isWhitespace(next) || next == '\n' || next == '\r') {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private int findWhitespaceBoundary(String text, int searchStart, int searchEnd) {
        for (int i = searchEnd - 1; i >= searchStart; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return -1;
    }
}
