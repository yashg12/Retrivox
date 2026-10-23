package com.studysync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysync.dto.AuthResponse;
import com.studysync.dto.LoginRequest;
import com.studysync.dto.RegisterRequest;
import com.studysync.dto.UserProfileDto;
import com.studysync.model.User;
import com.studysync.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_ValidRequest_Returns201Created() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser@test.edu", "password123", "New User");
        UserProfileDto profile = new UserProfileDto(1L, "newuser@test.edu", "New User", "ROLE_USER", Instant.now());
        AuthResponse response = new AuthResponse("mock.jwt.token", profile);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("newuser@test.edu"));
    }

    @Test
    void register_Conflict_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@test.edu", "password123", "User");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_ShortPassword_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("user@test.edu", "short", "User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ValidCredentials_Returns200() throws Exception {
        LoginRequest request = new LoginRequest("user@test.edu", "password123");
        UserProfileDto profile = new UserProfileDto(1L, "user@test.edu", "User", "ROLE_USER", Instant.now());
        AuthResponse response = new AuthResponse("login.jwt.token", profile);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login.jwt.token"))
                .andExpect(jsonPath("$.user.id").value(1));
    }

    @Test
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("user@test.edu", "wrong");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_Authenticated_ReturnsUserProfile() throws Exception {
        User user = new User("user@test.edu", "hash", "User");
        user.setId(7L);
        UserProfileDto profile = new UserProfileDto(7L, "user@test.edu", "User", "ROLE_USER", Instant.now());

        when(authService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(authService.toUserProfileDto(user)).thenReturn(profile);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.edu"))
                .andExpect(jsonPath("$.id").value(7));
    }
}
