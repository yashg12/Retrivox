package com.studysync.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-sufficiently-long-for-hmac-sha256-signing-1234567890",
                3600000 // 1 hour
        );
    }

    @Test
    void generateToken_And_ValidateToken_Success() {
        String token = tokenProvider.generateToken(42L, "alice@university.edu", "ROLE_USER");
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("alice@university.edu", tokenProvider.getEmailFromToken(token));
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    void validateToken_MalformedToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid.token.string"));
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Expiration ms = -1000 (already expired)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "test-secret-key-that-is-sufficiently-long-for-hmac-sha256-signing-1234567890",
                -1000
        );
        String expiredToken = expiredProvider.generateToken(1L, "bob@test.com", "ROLE_USER");
        assertFalse(tokenProvider.validateToken(expiredToken));
    }

    @Test
    void shortSecret_PaddedWithSha256_WorksSafely() {
        JwtTokenProvider shortKeyProvider = new JwtTokenProvider("short-secret", 3600000);
        String token = shortKeyProvider.generateToken(99L, "charlie@test.com", "ROLE_USER");
        assertTrue(shortKeyProvider.validateToken(token));
        assertEquals("charlie@test.com", shortKeyProvider.getEmailFromToken(token));
    }
}
