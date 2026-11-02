# Complete Guide: Unit Testing in Spring Boot 3 & Enterprise Microservices

This guide provides foundational knowledge, a step-by-step framework, complete source code, and demonstration talking points for writing unit tests in Spring Boot applications using **JUnit 5** and **Mockito**.

---

## Table of Contents
1. [Foundational Concepts (The "Why" and "What")](#1-foundational-concepts)
2. [The Universal 5-Step Unit Testing Recipe](#2-the-universal-5-step-recipe)
3. [The AAA Pattern (Arrange - Act - Assert)](#3-the-aaa-pattern)
4. [Complete Unit Test Implementation (`UserServiceTest`)](#4-complete-implementation-userservicetest)
5. [Web Layer Unit Testing (`AuthControllerTest`)](#5-web-layer-unit-testing-authcontrollertest)
6. [Pure Unit Testing Without Mocks (`JwtServiceTest`)](#6-pure-unit-testing-without-mocks-jwtservicetest)
7. [Step-by-Step Code Walkthrough](#7-step-by-step-code-walkthrough)
8. [Interview & Demonstration Cheat Sheet](#8-interview--demonstration-cheat-sheet)

---

## 1. Foundational Concepts

### What is a Unit Test?
A **Unit Test** tests a single class (the "unit") in complete isolation from external infrastructure:
- **No real database** (no MySQL / PostgreSQL queries).
- **No real network calls** (no HTTP calls, no Eureka lookups).
- **No full Spring container startup** (tests run in ~10–50 milliseconds instead of 5–15 seconds).

### Why Do We "Mock"?
A class rarely operates alone; it has collaborators (e.g., `UserService` depends on `UserRepository`, `PasswordEncoder`, and `JwtService`).

If you run tests against a live database:
- Tests become slow.
- Tests become flaky (if someone deletes a row in MySQL, tests fail).
- You cannot easily test hard error conditions (e.g., simulating a database connection timeout).

A **Mock** is a simulated "stunt double" that mimics the behavior of a real collaborator. You define its return values explicitly:
> *"When `userRepository.existsByEmail("jane@example.com")` is invoked, return `false`."*

```
┌────────────────────────────────────────────────────────┐
│                   UNIT UNDER TEST                      │
│                    [ UserService ]                     │
└──────────────────────────┬─────────────────────────────┘
                           │ calls dependencies
       ┌───────────────────┼────────────────────┐
       ▼                   ▼                    ▼
┌──────────────┐   ┌───────────────┐   ┌────────────────┐
│@Mock         │   │@Mock          │   │@Mock           │
│UserRepository│   │PasswordEncoder│   │JwtService      │
│(Simulated DB)│   │(Simulated Enc)│   │(Simulated JWT) │
└──────────────┘   └───────────────┘   └────────────────┘
```

### Core Annotation Comparison

| Annotation / Tool | Library | What It Does | When to Use It |
| :--- | :--- | :--- | :--- |
| `@ExtendWith(MockitoExtension.class)` | Mockito / JUnit 5 | Initializes Mockito annotations without booting Spring. | At the top of every service test class. |
| `@Mock` | Mockito | Creates a fake simulated instance of a dependency. | On collaborator fields (e.g., `UserRepository`). |
| `@InjectMocks` | Mockito | Creates the real instance of the class under test and injects the `@Mock` fields into it. | On the target class under test (`UserService`). |
| `@MockBean` | Spring Boot Test | Replaces a Spring bean inside the `ApplicationContext`. | In slice tests like `@WebMvcTest`. |

---

## 2. The Universal 5-Step Recipe

When given any business service implementation, follow this 5-step recipe:

```
Step 1: Identify Dependencies ──► Step 2: List Scenarios ──► Step 3: Setup Annotations
                                                                       │
Step 5: Verify Collaborators ◄── Step 4: Apply AAA Pattern ◄───────────┘
```

1. **Step 1: Identify Dependencies**:
   Inspect the constructor or `@Autowired` fields of your service class. Each collaborator becomes a `@Mock`.
2. **Step 2: List Scenarios (Branch Coverage)**:
   - **Happy Path**: What should happen when valid inputs are given?
   - **Validation / Precondition Failure**: What happens if an entity already exists or is missing?
   - **Business Logic Failure**: What happens if a password is wrong or balance is insufficient?
3. **Step 3: Setup Annotations**:
   Add `@ExtendWith(MockitoExtension.class)` on the test class. Add `@Mock` on dependencies and `@InjectMocks` on the service.
4. **Step 4: Apply the AAA Pattern**:
   Structure every test method into Arrange, Act, and Assert blocks.
5. **Step 5: Verify Collaborators**:
   Use Mockito's `verify(...)` to confirm that critical methods were called (or **never** called).

---

## 3. The AAA Pattern

Every unit test must have 3 clear phases:

```java
@Test
void exampleTest() {
    // 1. ARRANGE (Given): Set up data and instruct mocks
    when(mockDependency.someMethod("input")).thenReturn("expectedOutput");

    // 2. ACT (When): Execute the method being tested
    String result = serviceUnderTest.execute("input");

    // 3. ASSERT (Then): Verify the returned values and interactions
    assertEquals("expectedOutput", result);
    verify(mockDependency, times(1)).someMethod("input");
}
```

---

## 4. Complete Implementation (`UserServiceTest`)

Location: `src/test/java/com/microservices/userservice/service/UserServiceTest.java`

```java
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    // 1. MOCKS: Simulated dependencies
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    // 2. SYSTEM UNDER TEST: Real instance with mocks injected
    @InjectMocks
    private UserService userService;

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
            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("ROLE_USER", response.getRole());
            assertEquals(86400000L, response.getExpiresIn());

            // VERIFY INTERACTIONS
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

            // ArgumentCaptor allows inspecting the exact User object passed to repository.save()
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("ROLE_ADMIN", userCaptor.getValue().getRole());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException and NEVER save user when email already exists")
        void register_EmailAlreadyExists_ThrowsException() {
            // ARRANGE: Email already exists in repository
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

            // ACT & ASSERT
            UserAlreadyExistsException exception = assertThrows(
                    UserAlreadyExistsException.class,
                    () -> userService.register(registerRequest)
            );

            assertTrue(exception.getMessage().contains("User already exists with email: jane@example.com"));

            // CRITICAL ENTERPRISE CHECK: Guarantee database save was NEVER called
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

            // VERIFY
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
            // ARRANGE: User exists, but passwordEncoder returns false
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", "encoded_bcrypt_password")).thenReturn(false);

            // ACT & ASSERT
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.login(loginRequest)
            );

            assertEquals("Invalid email or password", exception.getMessage());

            // Security check: Never issue JWT token if password verification failed
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }
    }
}
```

---

## 5. Web Layer Unit Testing (`AuthControllerTest`)

Location: `src/test/java/com/microservices/userservice/controller/AuthControllerTest.java`

Use `@WebMvcTest` when you want to verify:
- HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`).
- JSON request and response payloads.
- `@Valid` annotation constraint handling.

```java
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
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should return 400 Bad Request on invalid email")
    void register_InvalidEmail_Returns400BadRequest() throws Exception {
        RegisterRequestDto invalidRequest = RegisterRequestDto.builder()
                .name("Jane Doe")
                .email("not-an-email") // Invalid format
                .password("123")        // Too short (< 6)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
```

---

## 6. Pure Unit Testing Without Mocks (`JwtServiceTest`)

Location: `src/test/java/com/microservices/userservice/service/JwtServiceTest.java`

When a class has no external dependencies (only algorithmic logic), do **not** use mocks. Test it directly:

```java
@DisplayName("JwtService Pure Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", 
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    @DisplayName("Should generate valid JWT and extract correct claims")
    void generateToken_Success() {
        String token = jwtService.generateToken("jane@example.com", "ROLE_USER");

        assertNotNull(token);
        assertEquals("jane@example.com", jwtService.extractUsername(token));
        assertEquals("ROLE_USER", jwtService.extractRole(token));
    }
}
```

---

## 7. Step-by-Step Code Walkthrough

### 1. How Mockito Simulates Behavior
```java
when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
```
- Informs Mockito: whenever `UserService` invokes `existsByEmail` with `"jane@example.com"`, immediately return `false`.
- No database connection is opened.

### 2. How to Test Exceptions
```java
UserAlreadyExistsException exception = assertThrows(
    UserAlreadyExistsException.class,
    () -> userService.register(registerRequest)
);
```
- Executes the lambda `userService.register(registerRequest)`.
- If the method completes normally without throwing `UserAlreadyExistsException`, the test **fails**.

### 3. Why `verify(..., never())` is Critical in Enterprise Code
```java
verify(userRepository, never()).save(any(User.class));
```
- Verifying the return value is not enough. You must ensure **side effects** (like writing bad data to MySQL or issuing a token to an unverified user) did not occur.

### 4. What is `ArgumentCaptor`?
```java
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
verify(userRepository).save(userCaptor.capture());
assertEquals("ROLE_ADMIN", userCaptor.getValue().getRole());
```
- Captures the internal `User` object constructed inside `register()` so you can assert its private fields.

---

## 8. Interview & Demonstration Cheat Sheet

### Key Questions & Model Answers

#### Q1: "What is the difference between `@Mock` and `@MockBean`?"
> *"`@Mock` is a lightweight Mockito mock used for pure unit testing without loading any Spring context. `@MockBean` is a Spring Boot annotation that replaces or adds a bean inside the Spring `ApplicationContext`, commonly used with `@WebMvcTest` or `@SpringBootTest`."*

#### Q2: "What is the difference between `@InjectMocks` and `@Mock`?"
> *"`@Mock` creates the simulated collaborator. `@InjectMocks` creates the actual class under test and injects all created mocks into its constructor or fields."*

#### Q3: "What is the difference between a Unit Test and an Integration Test?"
> *"A Unit Test isolates a single class in memory using mocks and executes in milliseconds. An Integration Test tests multiple components interacting together (e.g., Spring container, database transactions, HTTP endpoints) and is slower but validates end-to-end compatibility."*

#### Q4: "How do you run the tests from the terminal?"
```bash
# Run all unit tests
mvn test

# Run only the UserServiceTest
mvn test -Dtest=UserServiceTest

# Run a specific test method
mvn test -Dtest=UserServiceTest#register_Success
```

