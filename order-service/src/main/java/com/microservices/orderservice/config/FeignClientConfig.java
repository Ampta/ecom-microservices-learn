package com.microservices.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign Client Configuration for Inter-Service Security.
 *
 * Implements a RequestInterceptor bean that retrieves the Authorization header
 * (JWT Bearer token) from the incoming HTTP request context via RequestContextHolder
 * and propagates it to all outgoing OpenFeign calls (e.g., to product-service).
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public RequestInterceptor jwtFeignRequestInterceptor() {
        return (RequestTemplate requestTemplate) -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

                if (authHeader != null && !authHeader.isBlank()) {
                    log.info("Propagating Authorization header to downstream Feign call: {}", requestTemplate.url());
                    requestTemplate.header(HttpHeaders.AUTHORIZATION, authHeader);
                } else {
                    log.warn("No Authorization header found on incoming request to propagate to Feign client.");
                }
            } else {
                log.warn("No RequestContextHolder attributes found. Request may be running on an isolated worker thread.");
            }
        };
    }
}

