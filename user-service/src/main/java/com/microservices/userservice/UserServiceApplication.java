package com.microservices.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * UserServiceApplication
 * 
 * Microservice handling user registration, authentication,
 * and JWT generation. Connects to MySQL user_db and registers with Eureka.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

