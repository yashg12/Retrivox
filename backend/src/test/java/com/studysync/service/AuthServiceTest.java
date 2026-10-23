package com.studysync.service;

import com.studysync.dto.AuthResponse;
import com.studysync.dto.LoginRequest;
import com.studysync.dto.RegisterRequest;
import com.studysync.model.User;
import com.studysync.repository.UserRepository;
import com.studysync.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void register_Success_HashesPasswordAndIssuesJwt() {
        RegisterRequest request = new RegisterRequest("student@university.edu", "secretPassword123", "Alice Student");

        when(userRepository.existsByEmailIgnoreCase("student@university.edu")).thenReturn(false);
        when(passwordEncoder.encode("secretPassword123")).thenReturn("$2a$10$hashedPasswordSample");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(tokenProvider.generateToken(eq(10L), eq("student@university.edu"), eq("ROLE_USER")))
                .thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("student@university.edu", response.getUser().getEmail());
        assertEquals("Alice Student", response.getUser().getFullName());
        assertEquals(10L, response.getUser().getId());

        verify(passwordEncoder).encode("secretPassword123");
        verify(userRepository).save(argThat(user ->
                "$2a$10$hashedPasswordSample".equals(user.getPasswordHash()) &&
                !"secretPassword123".equals(user.getPasswordHash())
        ));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsConflict() {
        RegisterRequest request = new RegisterRequest("existing@university.edu", "password123", "Bob");
        when(userRepository.existsByEmailIgnoreCase("existing@university.edu")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success_ReturnsTokenAndProfile() {
        LoginRequest request = new LoginRequest("student@university.edu", "correctPassword");
        User user = new User("student@university.edu", "$2a$10$hashedPassword", "Alice");
        user.setId(5L);

        when(userRepository.findByEmailIgnoreCase("student@university.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "$2a$10$hashedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(5L, "student@university.edu", "ROLE_USER")).thenReturn("login.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("login.jwt.token", response.getToken());
        assertEquals(5L, response.getUser().getId());
        assertEquals("student@university.edu", response.getUser().getEmail());
    }

    @Test
    void login_IncorrectPassword_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("student@university.edu", "wrongPassword");
        User user = new User("student@university.edu", "$2a$10$hashedPassword", "Alice");

        when(userRepository.findByEmailIgnoreCase("student@university.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(tokenProvider, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_UnknownEmail_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("unknown@university.edu", "anyPassword");
        when(userRepository.findByEmailIgnoreCase("unknown@university.edu")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
