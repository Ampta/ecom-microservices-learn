package com.microservices.serviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ServiceRegistryApplication
 * 
 * Eureka Discovery Server for enterprise microservices.
 * Acts as the centralized registry where all client microservices
 * (e.g., user-service, product-service, order-service, api-gateway)
 * register and locate each other dynamically.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}

