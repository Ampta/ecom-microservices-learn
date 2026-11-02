package com.microservices.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ProductServiceApplication
 * 
 * Domain microservice managing product catalog and inventory.
 * Connects to MySQL product_db on port 3307 and registers with Eureka.
 */
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}

