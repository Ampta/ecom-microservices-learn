package com.microservices.apigateway.filter;

import com.microservices.apigateway.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Custom Gateway Filter to validate JWT tokens on secured routes.
 * Intercepts incoming requests, verifies Bearer token cryptographic signature
 * and expiration, and forwards verified user identity headers to downstream services.
 */
@Component
@Slf4j
public class JwtValidationFilter extends AbstractGatewayFilterFactory<JwtValidationFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtValidationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Check for Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization Header for URI: {}", request.getURI());
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            // 2. Validate Bearer format
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization Header format for URI: {}", request.getURI());
                return onError(exchange, "Invalid Authorization Header format. Expected 'Bearer <token>'", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7).trim();

            // 3. Cryptographically validate token & check expiration
            if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
                log.warn("Invalid or expired JWT token for URI: {}", request.getURI());
                return onError(exchange, "Invalid or Expired JWT Token", HttpStatus.UNAUTHORIZED);
            }

            // 4. Extract claims and propagate identity headers downstream
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User-Email", username)
                    .header("X-Auth-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    /**
     * Writes a clean JSON 401 Unauthorized response directly to the client.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorMessage,
                exchange.getRequest().getPath()
        );

        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Data
    public static class Config {
        // Optional configuration parameters can be added here
    }
}

