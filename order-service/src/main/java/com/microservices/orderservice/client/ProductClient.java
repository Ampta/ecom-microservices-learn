package com.microservices.orderservice.client;

import com.microservices.orderservice.config.FeignClientConfig;
import com.microservices.orderservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative HTTP Feign Client for Product Service.
 * Resolves the service URL dynamically via Eureka service discovery:
 * GET http://product-service/api/v1/products/{id}
 * 
 * Configured with FeignClientConfig to automatically propagate the incoming
 * JWT Authorization Bearer token to product-service.
 */
@FeignClient(name = "product-service", configuration = FeignClientConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponseDto getProductById(@PathVariable("id") Long id);
}
