package com.microservices.apigateway.filter;

import com.microservices.apigateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtValidationFilter Gateway Unit Tests")
class JwtValidationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtValidationFilter jwtValidationFilter;

    private JwtValidationFilter.Config config;

    @BeforeEach
    void setUp() {
        config = new JwtValidationFilter.Config();
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
    void apply_MissingAuthHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtValidationFilter.apply(config).filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Token is invalid")
    void apply_InvalidToken_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        StepVerifier.create(jwtValidationFilter.apply(config).filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Should propagate request downstream with user headers when Token is valid")
    void apply_ValidToken_PassesDownstream() {
        String token = "valid-jwt-token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtUtil.extractUsername(token)).thenReturn("jane@example.com");
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_USER");
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(jwtValidationFilter.apply(config).filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(any());
    }
}

