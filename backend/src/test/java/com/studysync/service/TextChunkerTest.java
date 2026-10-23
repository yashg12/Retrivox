package com.studysync.service;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextChunkerTest {

    @Test
    void chunk_EmptyOrNullTextReturnsEmptyList() {
        TextChunker chunker = new TextChunker(500, 100);
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
    }

    @Test
    void chunk_ShortTextReturnsSingleChunk() {
        TextChunker chunker = new TextChunker(500, 100);
        String text = "This is a brief text under 500 characters.";
        List<TextChunker.ChunkSpan> spans = chunker.chunk(text);

        assertEquals(1, spans.size());
        assertEquals(0, spans.get(0).chunkIndex());
        assertEquals(text, spans.get(0).content());
        assertEquals(0, spans.get(0).charStartIndex());
        assertEquals(text.length(), spans.get(0).charEndIndex());
    }

    @Test
    void chunk_LongTextSplitsWithOverlapAndPreservesSentences() {
        TextChunker chunker = new TextChunker(100, 25);
        String sentence1 = "Sentence one is about introductory calculus principles.";
        String sentence2 = "Sentence two explains derivatives and instantaneous rates of change.";
        String sentence3 = "Sentence three covers integration and areas under continuous curves.";
        String text = sentence1 + " " + sentence2 + " " + sentence3;

        List<TextChunker.ChunkSpan> spans = chunker.chunk(text);

        assertTrue(spans.size() >= 2, "Expected multiple chunks for text longer than chunk size");
        for (int i = 0; i < spans.size(); i++) {
            assertEquals(i, spans.get(i).chunkIndex());
            assertFalse(spans.get(i).content().isBlank());
        }

        // Verify that spans cover the full content
        assertTrue(spans.get(0).content().contains("Sentence one"));
        assertTrue(spans.get(spans.size() - 1).content().contains("curves"));
    }

    @Test
    void chunk_ConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(100, 100));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(100, -1));
    }
}
