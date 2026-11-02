package com.microservices.userservice.service;

import com.microservices.userservice.dto.AuthResponseDto;
import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.RegisterRequestDto;
import com.microservices.userservice.entity.User;
import com.microservices.userservice.exception.InvalidCredentialsException;
import com.microservices.userservice.exception.UserAlreadyExistsException;
import com.microservices.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for UserService.
 *
 * Uses JUnit 5 (@ExtendWith(MockitoExtension.class)) and Mockito.
 * No Spring context or database is loaded, making these tests run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    // 1. MOCK DEPENDENCIES: Simulated objects that UserService collaborates with
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    // 2. INJECT MOCKS: Instantiates UserService and injects the 3 mocks above into it
    @InjectMocks
    private UserService userService;

    // Test fixture data
    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequestDto.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("Password123")
                .build();

        loginRequest = LoginRequestDto.builder()
                .email("jane@example.com")
                .password("Password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .password("encoded_bcrypt_password")
                .role("ROLE_USER")
                .build();
    }

    // =========================================================================
    // REGISTRATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("register() method tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register user and return AuthResponseDto with JWT")
        void register_Success() {
            // ARRANGE (Given)
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("encoded_bcrypt_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken("jane@example.com", "ROLE_USER")).thenReturn("mock-jwt-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);

            // ACT (When)
            AuthResponseDto response = userService.register(registerRequest);

            // ASSERT (Then)
            assertNotNull(response, "Response should not be null");
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("ROLE_USER", response.getRole());
            assertEquals(86400000L, response.getExpiresIn());

            // VERIFY INTERACTIONS: Confirm collaborators were called exactly as expected
            verify(userRepository, times(1)).existsByEmail("jane@example.com");
            verify(passwordEncoder, times(1)).encode("Password123");
            verify(userRepository, times(1)).save(any(User.class));
            verify(jwtService, times(1)).generateToken("jane@example.com", "ROLE_USER");
        }

        @Test
        @DisplayName("Should use custom role when role is provided in RegisterRequestDto")
        void register_WithCustomRole_Success() {
            // ARRANGE
            registerRequest.setRole("ROLE_ADMIN");
            User adminUser = User.builder()
                    .id(2L)
                    .name("Admin User")
                    .email("jane@example.com")
                    .password("encoded_bcrypt_password")
                    .role("ROLE_ADMIN")
                    .build();

            when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_bcrypt_password");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);
            when(jwtService.generateToken("jane@example.com", "ROLE_ADMIN")).thenReturn("mock-admin-token");

            // ACT
            AuthResponseDto response = userService.register(registerRequest);

            // ASSERT
            assertEquals("ROLE_ADMIN", response.getRole());

            // Use ArgumentCaptor to inspect the exact User entity passed to repository.save()
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("ROLE_ADMIN", userCaptor.getValue().getRole());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException and NEVER save user when email already exists")
        void register_EmailAlreadyExists_ThrowsException() {
            // ARRANGE
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

            // ACT & ASSERT
            UserAlreadyExistsException exception = assertThrows(
                    UserAlreadyExistsException.class,
                    () -> userService.register(registerRequest),
                    "Expected register() to throw UserAlreadyExistsException, but it didn't"
            );

            assertTrue(exception.getMessage().contains("User already exists with email: jane@example.com"));

            // CRITICAL VERIFICATION: Ensure no database insert or token generation occurred
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }
    }

    // =========================================================================
    // LOGIN TESTS
    // =========================================================================
    @Nested
    @DisplayName("login() method tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully authenticate user and return AuthResponseDto with JWT")
        void login_Success() {
            // ARRANGE
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", "encoded_bcrypt_password")).thenReturn(true);
            when(jwtService.generateToken("jane@example.com", "ROLE_USER")).thenReturn("mock-jwt-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);

            // ACT
            AuthResponseDto response = userService.login(loginRequest);

            // ASSERT
            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("ROLE_USER", response.getRole());

            // VERIFY INTERACTIONS
            verify(userRepository, times(1)).findByEmail("jane@example.com");
            verify(passwordEncoder, times(1)).matches("Password123", "encoded_bcrypt_password");
            verify(jwtService, times(1)).generateToken("jane@example.com", "ROLE_USER");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when email is not found in database")
        void login_EmailNotFound_ThrowsException() {
            // ARRANGE
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

            // ACT & ASSERT
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.login(loginRequest)
            );

            assertEquals("Invalid email or password", exception.getMessage());

            // Security check: Never check password or generate token if user does not exist
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password does not match")
        void login_PasswordMismatch_ThrowsException() {
            // ARRANGE
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", "encoded_bcrypt_password")).thenReturn(false);

            // ACT & ASSERT
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.login(loginRequest)
            );

            assertEquals("Invalid email or password", exception.getMessage());

            // Security check: Never generate JWT token if password verification failed
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }
    }
}

