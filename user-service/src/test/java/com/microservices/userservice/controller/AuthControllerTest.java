package com.microservices.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.userservice.config.SecurityConfig;
import com.microservices.userservice.dto.AuthResponseDto;
import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.RegisterRequestDto;
import com.microservices.userservice.exception.InvalidCredentialsException;
import com.microservices.userservice.exception.UserAlreadyExistsException;
import com.microservices.userservice.service.JwtService;
import com.microservices.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller Unit Test (Web Layer Slice Test)
 * 
 * Uses @WebMvcTest which only boots up the web layer (AuthController and SecurityConfig).
 * Demonstrates HTTP request simulation, JSON serialization, and status code verification.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController Web Layer Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/auth/register - Should return 201 Created and AuthResponseDto")
    void register_ValidRequest_Returns201Created() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("Password123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .token("mock-jwt-token")
                .tokenType("Bearer")
                .email("jane@example.com")
                .role("ROLE_USER")
                .expiresIn(86400000L)
                .build();

        when(userService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should return 400 Bad Request when validation fails")
    void register_InvalidEmail_Returns400BadRequest() throws Exception {
        RegisterRequestDto invalidRequest = RegisterRequestDto.builder()
                .name("Jane Doe")
                .email("not-an-email") // Invalid email format
                .password("123")        // Password too short (< 6 chars)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 200 OK with JWT token")
    void login_ValidCredentials_Returns200Ok() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("jane@example.com")
                .password("Password123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .token("mock-jwt-token")
                .tokenType("Bearer")
                .email("jane@example.com")
                .role("ROLE_USER")
                .expiresIn(86400000L)
                .build();

        when(userService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 401 Unauthorized on invalid credentials")
    void login_InvalidCredentials_Returns401Unauthorized() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("jane@example.com")
                .password("WrongPassword")
                .build();

        when(userService.login(any(LoginRequestDto.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

