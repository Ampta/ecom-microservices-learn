package com.microservices.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ApiGatewayApplication
 * 
 * Spring Cloud API Gateway running on port 8080.
 * Handles reverse proxying, dynamic Eureka load balancing (lb://),
 * Redis-backed distributed rate limiting, and JWT authentication filtering.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

