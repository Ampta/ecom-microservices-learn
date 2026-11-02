package com.microservices.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure Unit Test for JwtService.
 * 
 * Demonstrates testing a class without mocks. We instantiate JwtService directly
 * and use ReflectionTestUtils to inject @Value properties.
 */
@DisplayName("JwtService Pure Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject 256-bit secret and expiration using Spring ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L); // 24h
    }

    @Test
    @DisplayName("Should generate valid JWT token containing subject and role")
    void generateToken_Success() {
        String token = jwtService.generateToken("jane@example.com", "ROLE_USER");

        assertNotNull(token);
        assertFalse(token.isBlank());

        // Verify claims extracted from token
        String extractedUser = jwtService.extractUsername(token);
        String extractedRole = jwtService.extractRole(token);

        assertEquals("jane@example.com", extractedUser);
        assertEquals("ROLE_USER", extractedRole);
    }

    @Test
    @DisplayName("Should validate token against correct username")
    void validateToken_CorrectUser_ReturnsTrue() {
        String token = jwtService.generateToken("jane@example.com", "ROLE_USER");

        boolean isValid = jwtService.validateToken(token, "jane@example.com");
        boolean isInvalid = jwtService.validateToken(token, "other@example.com");

        assertTrue(isValid);
        assertFalse(isInvalid);
    }

    @Test
    @DisplayName("Should detect expired token correctly")
    void tokenExpiration_Check() {
        // Set expiration to negative value (-1000ms) to simulate already expired token
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);

        String expiredToken = jwtService.generateToken("jane@example.com", "ROLE_USER");

        assertThrows(Exception.class, () -> jwtService.isTokenExpired(expiredToken));
    }
}

