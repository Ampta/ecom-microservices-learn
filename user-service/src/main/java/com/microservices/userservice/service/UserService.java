package com.microservices.userservice.service;

import com.microservices.userservice.dto.AuthResponseDto;
import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.RegisterRequestDto;
import com.microservices.userservice.entity.User;
import com.microservices.userservice.exception.InvalidCredentialsException;
import com.microservices.userservice.exception.UserAlreadyExistsException;
import com.microservices.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user, hashes their password, persists to MySQL,
     * and generates an initial JWT authentication token.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        String assignedRole = (request.getRole() != null && !request.getRole().isBlank()) 
                ? request.getRole() 
                : "ROLE_USER";

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * Authenticates an existing user against BCrypt hashed credentials
     * and issues a signed JWT token upon success.
     */
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Attempting login for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Password mismatch for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}

