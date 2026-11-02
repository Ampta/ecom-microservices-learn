# Enterprise Microservices Project - Complete Conversation Summary

## User (Prompt 1)

Phase 1: Docker Compose Setup
Prompt 1 (Infrastructure Setup):
"Write a complete docker-compose.yml file to set up localized Docker containers for an enterprise microservices project. Include:
Phase 1: Docker Compose Setup
Prompt 1 (Infrastructure Setup):
"Write a complete docker-compose.yml file to set up localized Docker containers for an enterprise microservices project. Include:

Three separate MySQL 8 containers (user_db, product_db, order_db) running on ports 3306, 3307, and 3308 respectively, with persistent volumes and explicit database names.

A Redis container on port 6379 for API Gateway rate limiting.

Provide docker-compose up -d execution verification commands."

A Redis container on port 6379 for API Gateway rate limiting.

Provide docker-compose up -d execution verification commands."

---

## Assistant (Response 1)

Here is the complete **Docker Compose** infrastructure configuration for your enterprise microservices project.

---

### File Structure Created

- [docker-compose.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/docker-compose.yml) — Service definitions, networking, volume mappings, and container health checks
- [.env](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/.env) — Default environment credentials and port definitions
- [.env.example](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/.env.example) — Template for version control
- [README.md](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/README.md) — Quick start and verification command documentation

---

### 1. `docker-compose.yml`

```yaml
services:
  # ============================================================================
  # User Service Database (MySQL 8)
  # ============================================================================
  user_db:
    image: mysql:8.0
    container_name: user_db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-rootpassword}
      MYSQL_DATABASE: user_db
      MYSQL_USER: ${MYSQL_USER:-appuser}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-apppassword}
    ports:
      - "3306:3306"
    volumes:
      - user_db_data:/var/lib/mysql
    networks:
      - microservices-net
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-rootpassword}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # ============================================================================
  # Product Service Database (MySQL 8)
  # ============================================================================
  product_db:
    image: mysql:8.0
    container_name: product_db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-rootpassword}
      MYSQL_DATABASE: product_db
      MYSQL_USER: ${MYSQL_USER:-appuser}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-apppassword}
    ports:
      - "3307:3306"
    volumes:
      - product_db_data:/var/lib/mysql
    networks:
      - microservices-net
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-rootpassword}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # ============================================================================
  # Order Service Database (MySQL 8)
  # ============================================================================
  order_db:
    image: mysql:8.0
    container_name: order_db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-rootpassword}
      MYSQL_DATABASE: order_db
      MYSQL_USER: ${MYSQL_USER:-appuser}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-apppassword}
    ports:
      - "3308:3306"
    volumes:
      - order_db_data:/var/lib/mysql
    networks:
      - microservices-net
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-rootpassword}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # ============================================================================
  # Redis Container (API Gateway Rate Limiting & Session Store)
  # ============================================================================
  redis:
    image: redis:7-alpine
    container_name: redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD:-redispassword} --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - microservices-net
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a ${REDIS_PASSWORD:-redispassword} ping || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 5s

# ==============================================================================
# Persistent Named Volumes
# ==============================================================================
volumes:
  user_db_data:
    driver: local
  product_db_data:
    driver: local
  order_db_data:
    driver: local
  redis_data:
    driver: local

# ==============================================================================
# Dedicated Bridge Network for Inter-Service Communication
# ==============================================================================
networks:
  microservices-net:
    driver: bridge
```

---

### 2. Environment Variables (`.env`)

```ini
# MySQL Configuration
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_USER=appuser
MYSQL_PASSWORD=apppassword

# Redis Configuration
REDIS_PORT=6379
REDIS_PASSWORD=redispassword
```

---

### 3. Execution & Verification Commands

#### A. Start the Infrastructure
```bash
docker compose up -d
# or using legacy Docker Compose syntax:
# docker-compose up -d
```

#### B. Check Container Status and Health
```bash
docker compose ps
```
> All containers should show status `Up (healthy)`.

#### C. Verify MySQL Databases

1. **Verify `user_db` on Port 3306**:
   ```bash
   docker exec -it user_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" user_db
   ```
2. **Verify `product_db` on Port 3307**:
   ```bash
   docker exec -it product_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" product_db
   ```
3. **Verify `order_db` on Port 3308**:
   ```bash
   docker exec -it order_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" order_db
   ```

#### D. Verify Redis (API Gateway Rate Limiting)

1. **Ping Test**:
   ```bash
   docker exec -it redis redis-cli -a redispassword ping
   ```
   *Expected Output: `PONG`*

2. **Rate Limiting Simulation Test (Key Expiry / TTL)**:
   ```bash
   # Set a sample rate-limiting key with a 60-second expiration
   docker exec -it redis redis-cli -a redispassword SET rate_limit:user:1001 1 EX 60

   # Verify the key and remaining TTL
   docker exec -it redis redis-cli -a redispassword GET rate_limit:user:1001
   docker exec -it redis redis-cli -a redispassword TTL rate_limit:user:1001
   ```

#### E. Verify Host Port Bindings
```bash
lsof -i :3306 -i :3307 -i :3308 -i :6379
```

#### F. Stop the Infrastructure
```bash
# Stop containers (preserves volume data)
docker compose down

# Stop containers and wipe volumes (fresh state)
docker compose down -v
```

---

## User (Prompt 2)

i have locally mysql so change 3306 port number and how i can connect with mysql workbench connection string

---

## Assistant (Response 2)

The host port for `user_db` has been changed from **`3306`** to **`3309`** in both [.env](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/.env) and [docker-compose.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/docker-compose.yml) so it will not collide with your local MySQL instance.

---

### Updated Port Mapping Overview

| Service Container | Container Internal Port | **Host Port (Your Machine)** | Database Name |
| :--- | :--- | :--- | :--- |
| **Your Local MySQL** | `3306` | **`3306`** *(kept free for your local instance)* | *(your local DB)* |
| **`user_db` (Docker)** | `3306` | **`3309`** *(remapped)* | `user_db` |
| **`product_db` (Docker)** | `3306` | **`3307`** | `product_db` |
| **`order_db` (Docker)** | `3306` | **`3308`** | `order_db` |
| **`redis` (Docker)** | `6379` | **`6379`** | N/A |

---

### How to Connect in MySQL Workbench

#### Option 1: Standard GUI Connection (Recommended)

1. Open **MySQL Workbench**.
2. Click the **`+`** icon next to **MySQL Connections** to create a new connection.
3. Configure the fields as follows:

| Field | Value for `user_db` | Value for `product_db` | Value for `order_db` |
| :--- | :--- | :--- | :--- |
| **Connection Name** | `Docker - User DB` | `Docker - Product DB` | `Docker - Order DB` |
| **Connection Method** | `Standard (TCP/IP)` | `Standard (TCP/IP)` | `Standard (TCP/IP)` |
| **Hostname** | **`127.0.0.1`** | **`127.0.0.1`** | **`127.0.0.1`** |
| **Port** | **`3309`** | **`3307`** | **`3308`** |
| **Username** | `appuser` *(or `root`)* | `appuser` *(or `root`)* | `appuser` *(or `root`)* |
| **Password** | Click *Store in Vault...* & enter **`apppassword`** *(or `rootpassword`)* | Click *Store in Vault...* & enter **`apppassword`** *(or `rootpassword`)* | Click *Store in Vault...* & enter **`apppassword`** *(or `rootpassword`)* |
| **Default Schema** | `user_db` | `product_db` | `order_db` |

4. Click **Test Connection** to confirm connectivity, then click **OK**.

> [!IMPORTANT]
> Always use **`127.0.0.1`** instead of `localhost` on macOS. Typing `localhost` can cause MySQL Workbench to look for the local UNIX domain socket (`/tmp/mysql.sock`), which would route to your machine's local MySQL rather than the Docker container.

---

#### Option 2: URI / Connection Strings

If you are using connection strings in MySQL Workbench or in application configuration files (`application.yml`, `.env`, etc.):

- **User DB:**
  ```text
  mysql://appuser:apppassword@127.0.0.1:3309/user_db
  ```
  *(Or Spring Boot JDBC URL: `jdbc:mysql://127.0.0.1:3309/user_db?useSSL=false&allowPublicKeyRetrieval=true`)*

- **Product DB:**
  ```text
  mysql://appuser:apppassword@127.0.0.1:3307/product_db
  ```
  *(Or Spring Boot JDBC URL: `jdbc:mysql://127.0.0.1:3307/product_db?useSSL=false&allowPublicKeyRetrieval=true`)*

- **Order DB:**
  ```text
  mysql://appuser:apppassword@127.0.0.1:3308/order_db
  ```
  *(Or Spring Boot JDBC URL: `jdbc:mysql://127.0.0.1:3308/order_db?useSSL=false&allowPublicKeyRetrieval=true`)*

---

### Verification from Terminal

To test the newly mapped port directly from your terminal:

```bash
# Verify connection to user_db on port 3309
mysql -h 127.0.0.1 -P 3309 -u appuser -papppassword -e "SELECT @@version, DATABASE();"

# Or through Docker exec:
docker exec -it user_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" user_db
```

---

## User (Prompt 3)

Phase 2: Eureka Discovery Server
Prompt 2 (Eureka Service Registry):
"Create a Spring Boot 3 / Java 17 Service Discovery application named service-registry running on port 8761.

Include pom.xml with spring-cloud-starter-netflix-eureka-server.

Show application.yml configured so that Eureka does not attempt to register with itself (register-with-eureka: false, fetch-registry: false).

Provide the main @EnableEurekaServer application class.

Explain briefly how microservices discover each other dynamically via service names instead of static IP addresses."

---

## Assistant (Response 3)

The **Eureka Service Registry (`service-registry`)** application has been created using **Spring Boot 3** and **Java 17**.

---

### Project Structure Created

- [service-registry/pom.xml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/service-registry/pom.xml) — Maven build file with Spring Boot 3.2.x & Spring Cloud 2023.0.x
- [service-registry/src/main/resources/application.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/service-registry/src/main/resources/application.yml) — Eureka Server configuration
- [service-registry/src/main/java/com/microservices/serviceregistry/ServiceRegistryApplication.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/service-registry/src/main/java/com/microservices/serviceregistry/ServiceRegistryApplication.java) — Main `@EnableEurekaServer` bootstrap class

---

### 1. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.microservices</groupId>
    <artifactId>service-registry</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>service-registry</name>
    <description>Eureka Service Discovery Server for Enterprise Microservices</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Spring Cloud Netflix Eureka Server -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>

        <!-- Spring Boot Actuator for health checks and metrics -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2. `application.yml`

```yaml
server:
  port: 8761

spring:
  application:
    name: service-registry

eureka:
  instance:
    hostname: localhost
  client:
    # Tell Eureka Server NOT to register itself as a client service
    register-with-eureka: false
    # Tell Eureka Server NOT to fetch registry cache from itself (standalone mode)
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    # Disable self-preservation for local dev so deregistered instances drop immediately
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000

# Actuator Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

---

### 3. Main `@EnableEurekaServer` Application Class

```java
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
```

---

### How Microservices Discover Each Other Dynamically (vs. Static IPs)

In a containerized or cloud-native architecture (Docker, Kubernetes, AWS), IP addresses and host ports are **ephemeral**—they change dynamically whenever containers restart, scale up/down, or reschedule onto different host nodes. Hardcoding static IP addresses breaks easily.

```
       1. Register on boot ("ORDER-SERVICE", IP:Port)
       2. Send periodic heartbeat (every 30s)
[ order-service ] ─────────────────────────┐
                                           │
                                           ▼
                                 ┌───────────────────┐
                                 │   Eureka Server   │
                                 │  (Port: 8761)     │
                                 └───────────────────┘
                                           ▲
       3. Fetch Registry & Cache locally   │
       4. Resolve "ORDER-SERVICE" -> IP    │
[ api-gateway / user-service ] ────────────┘
       5. Direct load-balanced call via Spring Cloud LoadBalancer
```

#### 1. Self-Registration on Startup
When a client service starts (e.g., `order-service` with `spring.application.name: order-service`), it publishes its metadata to Eureka:
- Service logical name: `ORDER-SERVICE`
- Current IP address and assigned port (e.g., `10.0.1.42:8083`)
- Health check and status endpoints

#### 2. Heartbeats & Health Leases
The registered service sends a heartbeat every 30 seconds. If Eureka misses heartbeats beyond the eviction lease period, it deregisters the unhealthy or terminated instance.

#### 3. Client-Side Registry Fetch & Caching
Client microservices and the API Gateway poll Eureka periodically (e.g., every 30s) to keep a local, in-memory cache of the service registry. They do **not** query Eureka synchronously on every HTTP request.

#### 4. Logical URL Resolution & Load Balancing
When a service calls another service using its logical name:
```java
// Example with WebClient / RestClient / FeignClient
String url = "http://ORDER-SERVICE/api/orders/123";
```
The client-side load balancer (**Spring Cloud LoadBalancer**) intercepts the request:
1. Looks up `ORDER-SERVICE` in its local registry cache.
2. Finds all healthy matching instances (e.g., `10.0.1.42:8083`, `10.0.1.43:8083`).
3. Selects an instance using a load-balancing algorithm (like Round Robin).
4. Rewrites the URL to the target instance's real IP/port and executes the HTTP call.

#### 5. Zero-Configuration Elastic Scaling
When you scale instances (`docker compose up --scale order-service=3`), new containers register their new IPs automatically without any manual updates to firewalls, gateways, or downstream service configurations.

---

## User (Prompt 4)

Phase 3: User Service (Authentication & JWT Generation)
Prompt 3 (User Service with Security):
"Create a Spring Boot 3 user-service on port 8081 connected to MySQL (user_db) and registered with Eureka.

Dependencies: JPA, MySQL Driver, Web, Security, Lombok, Eureka Client, jjwt (0.11.5).

User entity with id, name, email (unique), password, and role (ROLE_USER).

DTOs: RegisterRequestDto, LoginRequestDto, AuthResponseDto (containing JWT token).

JwtService: Generates JWT tokens containing username and role with 24-hour expiration and cryptographic signing using HMAC SHA256.

SecurityConfig: Configures Spring Security with BCrypt password encoding, stateless session management, and exposes /api/v1/auth/register and /api/v1/auth/login as public endpoints.

Provide complete implementation of AuthController, UserService, and JPA Repository."

---

## Assistant (Response 4)

The **User Service (`user-service`)** has been created with **Spring Boot 3**, **Java 17**, **Spring Security 6**, and **JJWT (0.11.5)**.

---

### Project Structure Created

- [user-service/pom.xml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/pom.xml) — Build configuration with JPA, MySQL, Security, Eureka Client, Lombok, and JJWT 0.11.5
- [user-service/src/main/resources/application.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/resources/application.yml) — Configuration on port `8081`, MySQL datasource (`user_db`), Eureka registration, and JWT secrets
- [user-service/src/main/java/com/microservices/userservice/entity/User.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/entity/User.java) — JPA Entity with unique email and `ROLE_USER`
- [user-service/src/main/java/com/microservices/userservice/dto/RegisterRequestDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/dto/RegisterRequestDto.java) — Registration DTO
- [user-service/src/main/java/com/microservices/userservice/dto/LoginRequestDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/dto/LoginRequestDto.java) — Login credentials DTO
- [user-service/src/main/java/com/microservices/userservice/dto/AuthResponseDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/dto/AuthResponseDto.java) — JWT Auth response DTO
- [user-service/src/main/java/com/microservices/userservice/repository/UserRepository.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/repository/UserRepository.java) — Spring Data JPA repository
- [user-service/src/main/java/com/microservices/userservice/service/JwtService.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/service/JwtService.java) — HMAC-SHA256 JWT generation with 24-hr expiration
- [user-service/src/main/java/com/microservices/userservice/service/UserService.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/service/UserService.java) — Business logic with BCrypt hashing and token issuance
- [user-service/src/main/java/com/microservices/userservice/config/SecurityConfig.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/config/SecurityConfig.java) — Spring Security filter chain and BCrypt bean
- [user-service/src/main/java/com/microservices/userservice/controller/AuthController.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/controller/AuthController.java) — REST endpoints for registration and login
- [user-service/src/main/java/com/microservices/userservice/exception/GlobalExceptionHandler.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/main/java/com/microservices/userservice/exception/GlobalExceptionHandler.java) — Centralized error handler

---

### 1. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.microservices</groupId>
    <artifactId>user-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>user-service</name>
    <description>User &amp; Authentication Service for Enterprise Microservices</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
        <jjwt.version>0.11.5</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Security Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Bean Validation Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MySQL Connector -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- JJWT Dependencies (0.11.5) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Project Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2. `application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service

  datasource:
    # Uses host port 3309 (avoiding port collision with local host MySQL)
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3309}/user_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${DB_USER:appuser}
    password: ${DB_PASSWORD:apppassword}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# JWT Security Configuration
jwt:
  # 256-bit secret key for HMAC-SHA256
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  # 24 hours in milliseconds (24 * 60 * 60 * 1000 = 86,400,000)
  expiration: 86400000

# Eureka Discovery Client Configuration
eureka:
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
  client:
    service-url:
      defaultZone: http://${EUREKA_SERVER:localhost}:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

# Actuator Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

---

### 3. User Entity (`User.java`)

```java
package com.microservices.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";
}
```

---

### 4. DTOs

#### `RegisterRequestDto.java`
```java
package com.microservices.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String role;
}
```

#### `LoginRequestDto.java`
```java
package com.microservices.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

#### `AuthResponseDto.java`
```java
package com.microservices.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private String email;

    private String role;
}
```

---

### 5. `JwtService.java`

```java
package com.microservices.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration; // 24 hours in milliseconds

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT token containing subject (username/email) and role.
     * Expiration is set to 24 hours with HMAC-SHA256 signature.
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, String username) {
        final String extractedUser = extractUsername(token);
        return (extractedUser.equals(username) && !isTokenExpired(token));
    }

    public long getExpirationTime() {
        return expiration;
    }
}
```

---

### 6. `SecurityConfig.java`

```java
package com.microservices.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

---

### 7. JPA Repository (`UserRepository.java`)

```java
package com.microservices.userservice.repository;

import com.microservices.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

---

### 8. `UserService.java`

```java
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
```

---

### 9. `AuthController.java`

```java
package com.microservices.userservice.controller;

import com.microservices.userservice.dto.AuthResponseDto;
import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.RegisterRequestDto;
import com.microservices.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

---

### API Verification Commands

#### 1. Register a New User
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "Password123"
  }'
```

#### 2. Log in and Receive JWT
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@example.com",
    "password": "Password123"
  }'
```

*Response (200 OK):*
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwic3ViIjoiamFuZUBleGFtcGxlLmNvbSIsImlhdCI6MTczMDQzMzYwMCwiZXhwIjoxNzMwNTIwMDAwfQ...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "email": "jane@example.com",
  "role": "ROLE_USER"
}
```

---

## User (Prompt 5)

write unit test for this i dont know how to write unit test for any application use this serviceimpl and write document step by step how to write unit for your requiremnt so that i can demonstrate and also give me initial knowledge to do that

---

## Assistant (Response 5)

Here is a complete, beginner-friendly guide and implementation for unit testing in enterprise Java/Spring Boot applications.

---

### Project Files Created

- [UserServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/service/UserServiceTest.java) — Core service unit test using JUnit 5 & Mockito
- [AuthControllerTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/controller/AuthControllerTest.java) — Web layer unit test using `MockMvc`
- [JwtServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/service/JwtServiceTest.java) — Pure unit test without mocks

---

## Part 1: Foundational Concepts (What You Need to Know First)

### 1. What is a Unit Test?
A **Unit Test** verifies the logic of a single class (the "unit") in complete isolation from the outside world (no real MySQL database, no real network, no Spring server running).
- Execution speed: **10 to 50 milliseconds** per test.
- Purpose: If a test fails, you know **exactly** which line in that specific class broke.

### 2. What is a "Mock"?
Think of a mock as a **stunt double** in a movie.
- Your `UserService` depends on `UserRepository`, `PasswordEncoder`, and `JwtService`.
- In a unit test, you do **not** want to connect to MySQL or run real database queries.
- Instead, you create a fake, simulated object (a **Mock**). You command it:
  *"When someone calls `userRepository.existsByEmail("test@example.com")`, return `false`."*

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

### 3. The 3 Core Tools in Spring Boot Unit Testing

| Annotation / Tool | What It Does | When to Use It |
| :--- | :--- | :--- |
| `@ExtendWith(MockitoExtension.class)` | Initializes Mockito in JUnit 5 without starting the heavy Spring container. | At the top of every Service test class. |
| `@Mock` | Creates a simulated dummy instance of a dependency. | For interfaces/classes your service calls (e.g., `UserRepository`). |
| `@InjectMocks` | Creates the real instance of your service and automatically injects all `@Mock` objects into its constructor. | For the class you are actually testing (`UserService`). |

---

## Part 2: The Universal 5-Step Recipe for ANY Service

Whenever you are given any service class to test, follow these 5 steps:

```mermaid
flowchart TD
    S1["Step 1: Identify Dependencies<br/>(What does this service inject?)"] --> S2["Step 2: List All Branches & Scenarios<br/>(Happy Path + Edge Cases + Exceptions)"]
    S2 --> S3["Step 3: Setup Test Class<br/>(@ExtendWith, @Mock, @InjectMocks)"]
    S3 --> S4["Step 4: Apply AAA Pattern<br/>(Arrange -> Act -> Assert)"]
    S4 --> S5["Step 5: Verify Collaborators<br/>(verify(mock, times(1)), verify(never()))"]
```

### The AAA Pattern (The Anatomy of Every Single Test)
Every test method must be divided into three clear blocks:
1. **Arrange (Given)**: Setup your test inputs and configure what the mocks should return (`when(...).thenReturn(...)`).
2. **Act (When)**: Execute the actual method under test (`userService.register(dto)`).
3. **Assert (Then)**: Check that the output matches expectations (`assertEquals`, `assertNotNull`, `assertThrows`).

---

## Part 3: The Complete Unit Test Implementation

Here is [UserServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/service/UserServiceTest.java):

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

    // 1. Mocks: Simulated collaborators
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    // 2. Class Under Test: Injects the 3 mocks above
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
            // 1. ARRANGE (Given)
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("encoded_bcrypt_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken("jane@example.com", "ROLE_USER")).thenReturn("mock-jwt-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);

            // 2. ACT (When)
            AuthResponseDto response = userService.register(registerRequest);

            // 3. ASSERT (Then)
            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("ROLE_USER", response.getRole());
            assertEquals(86400000L, response.getExpiresIn());

            // 4. VERIFY: Confirm collaborators were called exactly as intended
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

            // ArgumentCaptor allows you to inspect what exact User object was passed into save()
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
            // ARRANGE: Database finds nothing
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

            // Security check: Never generate JWT token if password verification failed
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }
    }
}
```

---

## Part 4: Step-by-Step Walkthrough (How to Explain Each Line)

When demonstrating this code to an interviewer or team lead:

### 1. How does Mockito simulate behavior?
> *"In the happy path test, I use `when(userRepository.existsByEmail("jane@example.com")).thenReturn(false)`. This instructs the mock: when `UserService` asks if this email exists, answer `false` without ever talking to MySQL."*

### 2. How do you test error conditions?
> *"To test our exception handling, I use `assertThrows(UserAlreadyExistsException.class, () -> userService.register(request))`. This verifies that our service throws the exact expected exception when `existsByEmail(...)` returns `true`."*

### 3. What makes an enterprise-grade unit test?
> *"It's not enough to check the return value. In enterprise banking or commerce applications, you must ensure side-effects did not happen when an error occurred. That's why I use `verify(userRepository, never()).save(...)`. This proves mathematically that if the email already exists, no data was ever written to the database."*

### 4. What is `ArgumentCaptor`?
> *"Sometimes a method creates an internal object (like `User.builder()...build()`) that isn't directly returned. With `ArgumentCaptor<User>`, I can capture the exact object passed into `userRepository.save()` and verify that the role was correctly assigned to `ROLE_ADMIN`."*

---

## Part 5: Interview & Demonstration Cheat Sheet

### Common Interview Questions & Exact Answers

#### Q1: "What is the difference between `@Mock` and `@MockBean`?"
- **`@Mock` (Mockito)**: Fast, lightweight plain Java mock. Does **not** load the Spring ApplicationContext. Use it for pure Service and Utility unit tests.
- **`@MockBean` (Spring Test)**: Replaces a bean inside the Spring ApplicationContext. Used in integration tests or `@WebMvcTest` (like in [AuthControllerTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/controller/AuthControllerTest.java)).

#### Q2: "What is the difference between a Unit Test and an Integration Test?"
- **Unit Test**: Tests one class in memory using mocks (`UserServiceTest`). Runs in milliseconds.
- **Integration Test**: Boots up Spring (`@SpringBootTest`), connects to a database (or Testcontainers / H2), tests multiple components working together end-to-end.

#### Q3: "What is the difference between `verify(mock, times(1))` and `verify(mock, never())`?"
- `times(1)` ensures a method was called exactly once.
- `never()` ensures a method was strictly **never** invoked (crucial for verifying that failures abort execution before persisting data).

---

## User (Prompt 6)

save in md file

---

## Assistant (Response 6)

The complete guide has been saved to [UNIT_TESTING_GUIDE.md](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/UNIT_TESTING_GUIDE.md).

### Summary of What's Included:
1. **Foundational Concepts**: Explains unit testing vs integration testing, why we mock, and the difference between `@Mock`, `@InjectMocks`, and `@MockBean`.
2. **The Universal 5-Step Recipe**: Framework for approaching unit testing for any service implementation.
3. **The AAA Pattern**: Detailed breakdown of Arrange, Act, and Assert.
4. **Complete Unit Test Implementations**:
   - [UserServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/service/UserServiceTest.java) — Service layer testing with Mockito.
   - [AuthControllerTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/controller/AuthControllerTest.java) — Web layer slice testing with `MockMvc`.
   - [JwtServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/user-service/src/test/java/com/microservices/userservice/service/JwtServiceTest.java) — Pure unit testing without mocks.
5. **Code Walkthrough & Interview Talking Points**: Key concepts to demonstrate (exception testing, `verify(..., never())`, `ArgumentCaptor`, and common interview Q&As).

---

## User (Prompt 7)

Phase 4: Product Service (Domain Microservice)
Prompt 4 (Product Service):
"Create a Spring Boot 3 product-service on port 8082 connected to MySQL (product_db) and registered with Eureka.

Dependencies: JPA, MySQL Driver, Web, Lombok, Eureka Client.

Entity Product: id, name, description, price (BigDecimal), stockQuantity (Integer).

Implement REST Endpoints:

POST /api/v1/products: Add new product.

GET /api/v1/products/{id}: Fetch product by ID (used for inventory check).

GET /api/v1/products: List all products.

Include Global Exception Handler (@RestControllerAdvice) to handle ResourceNotFoundException with clean JSON output."

---

## Assistant (Response 7)

The **Product Service (`product-service`)** domain microservice has been created with **Spring Boot 3**, **Java 17**, and **Spring Data JPA**.

---

### Project Structure Created

- [product-service/pom.xml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/pom.xml) — Build configuration with JPA, MySQL, Web, Lombok, and Eureka Client
- [product-service/src/main/resources/application.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/resources/application.yml) — Configuration on port `8082`, `product_db` connection (host port `3307`), and Eureka registration
- [product-service/src/main/java/com/microservices/productservice/entity/Product.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/entity/Product.java) — JPA Entity with `BigDecimal price` and `Integer stockQuantity`
- [product-service/src/main/java/com/microservices/productservice/dto/ProductRequestDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/dto/ProductRequestDto.java) — Request payload DTO with validations
- [product-service/src/main/java/com/microservices/productservice/dto/ProductResponseDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/dto/ProductResponseDto.java) — Response projection DTO
- [product-service/src/main/java/com/microservices/productservice/repository/ProductRepository.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/repository/ProductRepository.java) — Spring Data JPA repository
- [product-service/src/main/java/com/microservices/productservice/service/ProductService.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/service/ProductService.java) — Business logic for catalog and inventory lookup
- [product-service/src/main/java/com/microservices/productservice/controller/ProductController.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/controller/ProductController.java) — REST endpoints (`POST`, `GET /{id}`, `GET`)
- [product-service/src/main/java/com/microservices/productservice/exception/ResourceNotFoundException.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/exception/ResourceNotFoundException.java) — Custom 404 domain exception
- [product-service/src/main/java/com/microservices/productservice/exception/GlobalExceptionHandler.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/main/java/com/microservices/productservice/exception/GlobalExceptionHandler.java) — `@RestControllerAdvice` delivering clean JSON error responses
- [product-service/src/test/java/com/microservices/productservice/service/ProductServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/product-service/src/test/java/com/microservices/productservice/service/ProductServiceTest.java) — Unit tests covering happy and exception paths

---

### 1. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.microservices</groupId>
    <artifactId>product-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>product-service</name>
    <description>Product &amp; Catalog Microservice for Enterprise Microservices</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Bean Validation Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MySQL Connector -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Eureka Client Starter -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Project Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2. `application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: product-service

  datasource:
    # Connects to product_db on mapped host port 3307
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3307}/product_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${DB_USER:appuser}
    password: ${DB_PASSWORD:apppassword}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# Eureka Discovery Client Configuration
eureka:
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
  client:
    service-url:
      defaultZone: http://${EUREKA_SERVER:localhost}:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

---

### 3. Product Entity (`Product.java`)

```java
package com.microservices.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;
}
```

---

### 4. DTOs

#### `ProductRequestDto.java`
```java
package com.microservices.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
}
```

#### `ProductResponseDto.java`
```java
package com.microservices.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
}
```

---

### 5. `ProductService.java`

```java
package com.microservices.productservice.service;

import com.microservices.productservice.dto.ProductRequestDto;
import com.microservices.productservice.dto.ProductResponseDto;
import com.microservices.productservice.entity.Product;
import com.microservices.productservice.exception.ResourceNotFoundException;
import com.microservices.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto request) {
        log.info("Creating new product: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return mapToResponseDto(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return mapToResponseDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        log.info("Retrieving all products");

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .build();
    }
}
```

---

### 6. `ProductController.java`

```java
package com.microservices.productservice.controller;

import com.microservices.productservice.dto.ProductRequestDto;
import com.microservices.productservice.dto.ProductResponseDto;
import com.microservices.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        ProductResponseDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
}
```

---

### 7. Clean JSON Error Handling (`GlobalExceptionHandler.java`)

```java
package com.microservices.productservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("details", errors);
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
```

---

### API Verification Examples

#### 1. Add a Product (`POST /api/v1/products`)
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro M3",
    "description": "16-inch, 36GB RAM, 1TB SSD",
    "price": 2499.99,
    "stockQuantity": 15
  }'
```
*Response (201 Created):*
```json
{
  "id": 1,
  "name": "MacBook Pro M3",
  "description": "16-inch, 36GB RAM, 1TB SSD",
  "price": 2499.99,
  "stockQuantity": 15
}
```

#### 2. Get Product by ID (`GET /api/v1/products/1`)
```bash
curl -X GET http://localhost:8082/api/v1/products/1
```

#### 3. Test 404 Exception Handler (`GET /api/v1/products/999`)
```bash
curl -X GET http://localhost:8082/api/v1/products/999
```
*Clean JSON Response (404 Not Found):*
```json
{
  "timestamp": "2026-11-01T10:14:00.123456",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 999",
  "path": "/api/v1/products/999"
}
```

#### 4. List All Products (`GET /api/v1/products`)
```bash
curl -X GET http://localhost:8082/api/v1/products
```

---

## User (Prompt 8)

Phase 5: Order Service (Inter-Service Communication & Resilience)
Prompt 5 (Order Service with OpenFeign & Resilience4j):
"Create a Spring Boot 3 order-service on port 8083 connected to MySQL (order_db) and registered with Eureka.

Dependencies: JPA, MySQL Driver, Web, OpenFeign, Resilience4j, Eureka Client.

Entity OrderMaster: id, userId, productId, quantity, totalPrice, orderStatus (CREATED, FAILED).

Create OpenFeign Client ProductClient to invoke GET http://product-service/api/v1/products/{id} dynamically via Eureka.

Implement Resilience4j Circuit Breaker fallback: If product-service is down or times out, trigger a fallback method that returns a custom error/graceful response or marks the order status as FAILED.

Order logic: When POST /api/v1/orders is called with productId and quantity, fetch product details via Feign, calculate total price (price * quantity), update order status, and save to database."

---

## Assistant (Response 8)

The **Order Service (`order-service`)** has been created with **Spring Boot 3**, **Java 17**, **Spring Cloud OpenFeign**, and **Resilience4j Circuit Breaker**.

---

### Project Structure Created

- [order-service/pom.xml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/pom.xml) — Build file with JPA, MySQL, Web, OpenFeign, Resilience4j, Spring AOP, and Eureka Client
- [order-service/src/main/resources/application.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/resources/application.yml) — Configuration on port `8083`, `order_db` connection (host port `3308`), Eureka discovery, and Resilience4j threshold configuration
- [order-service/src/main/java/com/microservices/orderservice/entity/OrderStatus.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/entity/OrderStatus.java) — Status enum (`CREATED`, `FAILED`)
- [order-service/src/main/java/com/microservices/orderservice/entity/OrderMaster.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/entity/OrderMaster.java) — JPA Entity with `userId`, `productId`, `quantity`, `totalPrice`, and `orderStatus`
- [order-service/src/main/java/com/microservices/orderservice/client/ProductClient.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/client/ProductClient.java) — Declarative OpenFeign client calling `http://product-service/api/v1/products/{id}` via Eureka
- [order-service/src/main/java/com/microservices/orderservice/dto/OrderRequestDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/dto/OrderRequestDto.java) — Incoming order request payload
- [order-service/src/main/java/com/microservices/orderservice/dto/OrderResponseDto.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/dto/OrderResponseDto.java) — Order confirmation response
- [order-service/src/main/java/com/microservices/orderservice/service/OrderService.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/service/OrderService.java) — Business logic with `@CircuitBreaker(fallbackMethod = "createOrderFallback")`
- [order-service/src/main/java/com/microservices/orderservice/controller/OrderController.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/controller/OrderController.java) — REST endpoints for orders
- [order-service/src/main/java/com/microservices/orderservice/OrderServiceApplication.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/OrderServiceApplication.java) — Bootstrap class with `@EnableFeignClients`
- [order-service/src/test/java/com/microservices/orderservice/service/OrderServiceTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/test/java/com/microservices/orderservice/service/OrderServiceTest.java) — Unit tests covering calculation, stock check, and circuit breaker fallback

---

### Architecture & Resilience Flow

```
                                [ Client POST /api/v1/orders ]
                                               │
                                               ▼
                                      [ OrderService ]
                                               │
             ┌─────────────────────────────────┴─────────────────────────────────┐
             │ @CircuitBreaker(name="productService")                            │
             │                                                                   │
    (Happy Path: Service UP)                                          (Service DOWN / Timeout)
             │                                                                   │
             ▼                                                                   ▼
    [ ProductClient (Feign) ]                                      [ createOrderFallback() ]
             │                                                                   │
             ▼                                                                   │
    [ Eureka Resolution: "product-service" ]                                     │
             │                                                                   │
             ▼                                                                   │
    [ Product Service GET /{id} ]                                                │
             │                                                                   │
      Calculate Total Price                                            Persist Order as FAILED
     (price * qty) & Save Order                                       (totalPrice=0, failureReason)
             │                                                                   │
             ▼                                                                   ▼
       201 CREATED                                                 503 SERVICE_UNAVAILABLE (FAILED)
```

---

### 1. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.microservices</groupId>
    <artifactId>order-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>order-service</name>
    <description>Order Processing &amp; Resilient Inter-Service Communication Microservice</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- OpenFeign Declarative HTTP Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Resilience4j Circuit Breaker -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>

        <!-- Spring AOP for Resilience4j Aspect-Oriented Interceptors -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- MySQL Connector -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2. `application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: order-service

  datasource:
    # Connects to order_db on mapped host port 3308
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3308}/order_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${DB_USER:appuser}
    password: ${DB_PASSWORD:apppassword}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000
            readTimeout: 3000
            loggerLevel: full

# Resilience4j Circuit Breaker Configuration
resilience4j:
  circuitbreaker:
    instances:
      productService:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 5
        minimum-number-of-calls: 3
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 2
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true
  timelimiter:
    instances:
      productService:
        timeout-duration: 3s

# Eureka Discovery Client Configuration
eureka:
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
  client:
    service-url:
      defaultZone: http://${EUREKA_SERVER:localhost}:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

---

### 3. OpenFeign Client (`ProductClient.java`)

```java
package com.microservices.orderservice.client;

import com.microservices.orderservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative HTTP Feign Client for Product Service.
 * Resolves the service URL dynamically via Eureka service discovery:
 * GET http://product-service/api/v1/products/{id}
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponseDto getProductById(@PathVariable("id") Long id);
}
```

---

### 4. `OrderMaster.java` & `OrderStatus.java`

```java
package com.microservices.orderservice.entity;

public enum OrderStatus {
    CREATED,
    FAILED
}
```

```java
package com.microservices.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(length = 500)
    private String failureReason;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

### 5. `OrderService.java` with Circuit Breaker & Fallback

```java
package com.microservices.orderservice.service;

import com.microservices.orderservice.client.ProductClient;
import com.microservices.orderservice.dto.OrderRequestDto;
import com.microservices.orderservice.dto.OrderResponseDto;
import com.microservices.orderservice.dto.ProductResponseDto;
import com.microservices.orderservice.entity.OrderMaster;
import com.microservices.orderservice.entity.OrderStatus;
import com.microservices.orderservice.exception.OrderNotFoundException;
import com.microservices.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    /**
     * Creates an order by invoking ProductClient via Feign to fetch product details.
     * Protected by Resilience4j Circuit Breaker 'productService'.
     */
    @Transactional
    @CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")
    public OrderResponseDto createOrder(OrderRequestDto request) {
        log.info("Processing order for userId: {}, productId: {}, quantity: {}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        // 1. Fetch product details dynamically via Feign & Eureka
        ProductResponseDto product = productClient.getProductById(request.getProductId());

        if (product == null) {
            log.warn("Product not found with id: {}", request.getProductId());
            return persistFailedOrder(request, "Product not found with id: " + request.getProductId());
        }

        // 2. Validate stock availability
        if (product.getStockQuantity() == null || product.getStockQuantity() < request.getQuantity()) {
            String reason = String.format("Insufficient stock for productId: %d. Available: %d, Requested: %d",
                    request.getProductId(),
                    product.getStockQuantity() != null ? product.getStockQuantity() : 0,
                    request.getQuantity());
            log.warn(reason);
            return persistFailedOrder(request, reason);
        }

        // 3. Calculate total price (price * quantity)
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 4. Build and persist CREATED order
        OrderMaster order = OrderMaster.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .orderStatus(OrderStatus.CREATED)
                .build();

        OrderMaster savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with ID: {}, Total: {}", savedOrder.getId(), savedOrder.getTotalPrice());

        return mapToResponseDto(savedOrder);
    }

    /**
     * Resilience4j Circuit Breaker Fallback Method.
     * Triggered when Product Service is unreachable, throws an exception, or times out.
     * Marks the order status as FAILED and gracefully returns an informative response.
     */
    @Transactional
    public OrderResponseDto createOrderFallback(OrderRequestDto request, Throwable throwable) {
        String reason = "Product Service unavailable or timed out: " + throwable.getMessage();
        log.error("Circuit breaker triggered for productId: {}. Reason: {}", request.getProductId(), reason);
        return persistFailedOrder(request, reason);
    }

    private OrderResponseDto persistFailedOrder(OrderRequestDto request, String failureReason) {
        OrderMaster failedOrder = OrderMaster.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(BigDecimal.ZERO)
                .orderStatus(OrderStatus.FAILED)
                .failureReason(failureReason)
                .build();

        OrderMaster saved = orderRepository.save(failedOrder);
        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        OrderMaster order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        return mapToResponseDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private OrderResponseDto mapToResponseDto(OrderMaster order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .orderStatus(order.getOrderStatus())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
```

---

### 6. `OrderController.java`

```java
package com.microservices.orderservice.controller;

import com.microservices.orderservice.dto.OrderRequestDto;
import com.microservices.orderservice.dto.OrderResponseDto;
import com.microservices.orderservice.entity.OrderStatus;
import com.microservices.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto response = orderService.createOrder(request);

        HttpStatus status = (response.getOrderStatus() == OrderStatus.CREATED)
                ? HttpStatus.CREATED
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        OrderResponseDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}
```

---

### Verification Scenarios

#### Scenario 1: Happy Path (Both Services Running)
Create an order for `productId: 1` (MacBook Pro at \$2499.99, quantity: 2):
```bash
curl -X POST http://localhost:8083/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }'
```
*Response (201 Created):*
```json
{
  "id": 1,
  "userId": 1,
  "productId": 1,
  "quantity": 2,
  "totalPrice": 4999.98,
  "orderStatus": "CREATED",
  "failureReason": null,
  "createdAt": "2026-11-01T10:28:00.123456"
}
```

#### Scenario 2: Circuit Breaker Triggered (Product Service is DOWN)
Stop `product-service` and place the same order:
```bash
curl -X POST http://localhost:8083/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }'
```
*Graceful Fallback Response (503 Service Unavailable):*
```json
{
  "id": 2,
  "userId": 1,
  "productId": 1,
  "quantity": 2,
  "totalPrice": 0.00,
  "orderStatus": "FAILED",
  "failureReason": "Product Service unavailable or timed out: Load balancer does not contain an instance for the service product-service",
  "createdAt": "2026-11-01T10:29:15.987654"
}
```
> The order is safely persisted as `FAILED` in `order_db` instead of crashing with a 500 unhandled exception.

---

## User (Prompt 9)

before going to 6 phase tell what we are done curd operation we are just performing in md file and save then move Phase 6: API Gateway (Routing, Security, Rate Limiting)
Prompt 6 (Spring Cloud API Gateway):
"Create a Spring Cloud Gateway application api-gateway on port 8080 registered with Eureka and connected to Redis.

Dependencies: spring-cloud-starter-gateway, spring-cloud-starter-netflix-eureka-client, spring-boot-starter-data-redis-reactive, jjwt.

application.yml route configuration:

Route /api/v1/auth/** to lb://user-service (Public).

Route /api/v1/products/** to lb://product-service (Secured via JWT).

Route /api/v1/orders/** to lb://order-service (Secured via JWT).

Custom JwtValidationFilter (AbstractGatewayFilterFactory): Intercepts secured routes, extracts Authorization: Bearer <token>, validates token validity and expiration. Returns HTTP 401 if token is missing or invalid.

RequestRateLimiter filter configuration backed by Redis using KeyResolver based on client IP address (10 requests/sec limit)."

---

## Assistant (Response 9)

### Part 1: CRUD Operations & Architecture Summary (Phases 1–5)

Before proceeding to Phase 6, a detailed summary of all database models, entities, and CRUD operations implemented across Phases 1 through 5 has been saved to [CRUD_OPERATIONS_SUMMARY.md](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/CRUD_OPERATIONS_SUMMARY.md).

#### Quick Reference Table

| Service | Port | Database | Primary Entities | Key Operations |
| :--- | :--- | :--- | :--- | :--- |
| **`service-registry`** | `8761` | In-memory registry | Eureka Service Instances | Dynamic discovery & heartbeat registration |
| **`user-service`** | `8081` | `user_db` (Port `3309`) | `User` | **CREATE**: Register user with BCrypt hashed password<br/>**READ/AUTH**: Login & generate signed 24h JWT |
| **`product-service`** | `8082` | `product_db` (Port `3307`) | `Product` | **CREATE**: Add product (`name`, `price`, `stockQuantity`)<br/>**READ**: Get product by ID (used for inventory check)<br/>**READ**: List all products |
| **`order-service`** | `8083` | `order_db` (Port `3308`) | `OrderMaster` | **CREATE & ORCHESTRATE**: Calls `ProductClient` via Feign, verifies stock, calculates `totalPrice`, saves as `CREATED`<br/>**FAULT TOLERANCE**: Resilience4j Circuit Breaker fallback saves failed attempts as `FAILED` |

---

### Part 2: Phase 6 — Spring Cloud API Gateway

The **API Gateway (`api-gateway`)** has been created with **Spring Cloud Gateway**, **Reactive Redis**, and **JJWT**.

```
                   [ Incoming Client Request to http://localhost:8080 ]
                                          │
                                          ▼
                                   [ API Gateway ]
                                          │
            ┌─────────────────────────────┼─────────────────────────────┐
            │                             │                             │
    /api/v1/auth/**               /api/v1/products/**           /api/v1/orders/**
      (PUBLIC ROUTE)               (SECURED ROUTE)               (SECURED ROUTE)
            │                             │                             │
    [ RateLimiter (Redis) ]       [ JwtValidationFilter ]       [ JwtValidationFilter ]
            │                             │                             │
            │                       Valid?   Invalid?             Valid?   Invalid?
            │                         │         │                   │         │
            │                         ▼         ▼                   ▼         ▼
            │                    Pass to     Return 401        Pass to     Return 401
            │                  RateLimiter  Unauthorized     RateLimiter  Unauthorized
            │                         │                             │
            ▼                         ▼                             ▼
   lb://user-service          lb://product-service          lb://order-service
      (Port: 8081)               (Port: 8082)                  (Port: 8083)
```

---

### Project Structure Created

- [api-gateway/pom.xml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/pom.xml) — Reactive Gateway, Reactive Redis, JJWT, Eureka Client (non-blocking Netty engine)
- [api-gateway/src/main/resources/application.yml](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/src/main/resources/application.yml) — Route definitions (`user-service`, `product-service`, `order-service`) with Eureka load balancing (`lb://`) and Redis rate limits
- [api-gateway/src/main/java/com/microservices/apigateway/util/JwtUtil.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/src/main/java/com/microservices/apigateway/util/JwtUtil.java) — HMAC-SHA256 signature and expiration verification
- [api-gateway/src/main/java/com/microservices/apigateway/filter/JwtValidationFilter.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/src/main/java/com/microservices/apigateway/filter/JwtValidationFilter.java) — `AbstractGatewayFilterFactory` intercepting secured routes, returning JSON 401 on failure, and propagating user identity headers downstream
- [api-gateway/src/main/java/com/microservices/apigateway/config/RateLimiterConfig.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/src/main/java/com/microservices/apigateway/config/RateLimiterConfig.java) — Redis `KeyResolver` based on client remote IP address
- [api-gateway/src/main/java/com/microservices/apigateway/ApiGatewayApplication.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/src/main/java/com/microservices/apigateway/ApiGatewayApplication.java) — Gateway bootstrap application
- [api-gateway/src/test/java/com/microservices/apigateway/filter/JwtValidationFilterTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/api-gateway/src/test/java/com/microservices/apigateway/filter/JwtValidationFilterTest.java) — Reactive unit tests using `StepVerifier`

---

### 1. `pom.xml`

> **Note**: Spring Cloud Gateway is built on **Project Reactor** and **Netty**. Servlet containers (`spring-boot-starter-web` / Tomcat) are intentionally excluded to prevent classpath runtime conflicts.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.microservices</groupId>
    <artifactId>api-gateway</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>api-gateway</name>
    <description>Spring Cloud API Gateway for Enterprise Microservices</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
        <jjwt.version>0.11.5</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Cloud Gateway (Reactive Netty) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <!-- Eureka Client for Dynamic Routing via lb:// -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Spring Data Redis Reactive for Rate Limiter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>

        <!-- JJWT Dependencies (0.11.5) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2. `application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  # Redis Reactive Configuration for Rate Limiting
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redispassword}
      timeout: 2000ms

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

      routes:
        # ======================================================================
        # 1. User Service Route (PUBLIC: Registration & Login)
        # ======================================================================
        - id: user-service-route
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            # 10 req/sec limit on auth endpoints to prevent brute-force attacks
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@ipKeyResolver}"

        # ======================================================================
        # 2. Product Service Route (SECURED: Requires Valid JWT)
        # ======================================================================
        - id: product-service-route
          uri: lb://product-service
          predicates:
            - Path=/api/v1/products/**
          filters:
            - name: JwtValidationFilter
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@ipKeyResolver}"

        # ======================================================================
        # 3. Order Service Route (SECURED: Requires Valid JWT)
        # ======================================================================
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - name: JwtValidationFilter
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@ipKeyResolver}"

# Cryptographic Secret for JWT Verification
jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Eureka Discovery Client Configuration
eureka:
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
  client:
    service-url:
      defaultZone: http://${EUREKA_SERVER:localhost}:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
  endpoint:
    health:
      show-details: always
```

---

### 3. Custom `JwtValidationFilter.java`

```java
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
    }
}
```

---

### 4. Redis Client IP `RateLimiterConfig.java`

```java
package com.microservices.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the rate-limiting key based on the client's remote IP address.
     * Backed by Redis to enforce a 10 requests/second limit per IP address.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress())
                        .orElse("127.0.0.1")
        );
    }
}
```

---

### Verification via API Gateway (Port 8080)

All external client traffic now flows through `http://localhost:8080`:

#### 1. Public Route: Register & Login (No Token Required)
```bash
# Register User through Gateway
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "Password123"
  }'

# Login and capture the JWT Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@example.com",
    "password": "Password123"
  }' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
```

#### 2. Secured Route: Attempt Access Without Token (Returns 401)
```bash
curl -X GET http://localhost:8080/api/v1/products
```
*Response (401 Unauthorized):*
```json
{
  "timestamp": "2026-11-01T10:46:00.123456",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing Authorization Header",
  "path": "/api/v1/products"
}
```

#### 3. Secured Route: Access With Bearer Token (Returns 200 OK)
```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN"
```

#### 4. Secured Route: Create Order With Bearer Token
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 1
  }'
```

#### 5. Verify Redis Rate Limiter in Action
Check Redis keys created by the gateway:
```bash
docker exec -it redis redis-cli -a redispassword KEYS "request_rate_limiter*"
```

---

## User (Prompt 10)

if i want to implement apigateway step by step which file first and why i need to implementing what its solving and how to implement diffrent feature in another project create md file and save

---

## Assistant (Response 10)

The complete step-by-step implementation guide has been compiled and saved to [API_GATEWAY_IMPLEMENTATION_GUIDE.md](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/API_GATEWAY_IMPLEMENTATION_GUIDE.md).

---

### Key Takeaways from the Guide

#### 1. Why Do We Need an API Gateway? (Problems It Solves)
- **Port Exposure Prevention**: Rather than exposing internal microservice ports (`8081`, `8082`, `8083`) to the public internet, external clients only interact with a single port (`8080`).
- **Security Offloading**: Checks JWT signatures and expiration **once** at the perimeter rather than duplicating security code in every microservice.
- **Identity Propagation**: Injects authenticated user metadata (`X-Auth-User-Email`, `X-Auth-User-Role`) as HTTP headers for downstream services.
- **Rate Limiting & DDoS Defense**: Uses Redis token buckets to prevent brute-force attacks and abuse.
- **Decoupled Architecture**: Allows internal refactoring, port changes, and microservice splitting without breaking client/mobile app URLs.

---

#### 2. The Golden Rule (Reactive vs. Servlet)
> [!CAUTION]
> **Never include `spring-boot-starter-web` in an API Gateway.**
> Spring Cloud Gateway runs on an asynchronous, non-blocking **Netty** event-loop server (`spring-cloud-starter-gateway`). Adding Tomcat (`spring-boot-starter-web`) causes a classpath conflict that prevents application startup.

---

#### 3. Exact File Creation Order (Which File First and Why)

```
1. pom.xml ──────────► 2. application.yml ────────► 3. ApiGatewayApplication.java
                              │
7. Unit Tests ◄─────── 6. RateLimiterConfig.java ◄─ 5. JwtValidationFilter.java ◄── 4. JwtUtil.java
```

| Order | File | Why It Comes in This Order |
| :---: | :--- | :--- |
| **1st** | `pom.xml` | Establishes the non-blocking Netty classpath and brings in reactive Gateway dependencies before writing any code. |
| **2nd** | `application.yml` | Establishes the foundation: port (`8080`), Redis connection, Eureka server URL, and route predicates. |
| **3rd** | `ApiGatewayApplication.java` | Main bootstrap class to verify that the reactive gateway boots without dependency issues. |
| **4th** | `JwtUtil.java` | Cryptographic parsing helper created before the filter to avoid compilation errors. |
| **5th** | `JwtValidationFilter.java` | Extends `AbstractGatewayFilterFactory` and uses `JwtUtil` to inspect and secure routes. |
| **6th** | `RateLimiterConfig.java` | Defines the `@Bean KeyResolver ipKeyResolver()` referenced in the YAML routes (`#{@ipKeyResolver}`). |
| **7th** | `JwtValidationFilterTest.java` | Tests the reactive filter pipeline using `MockServerWebExchange` and `StepVerifier`. |

---

#### 4. Features Included for Any Other Project:
The [API_GATEWAY_IMPLEMENTATION_GUIDE.md](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/API_GATEWAY_IMPLEMENTATION_GUIDE.md) includes copy-paste implementations for:
1. **Path Rewriting**: `RewritePath` regex and `StripPrefix=1`.
2. **External / 3rd-Party API Routing**: Forwarding to Stripe/PayPal with custom API keys.
3. **Global CORS Handling**: Centralized cross-origin configuration in YAML.
4. **Distributed Request Tracing**: Injecting `X-Correlation-ID` headers across all microservices.
5. **Global Request & Response Logging**: Non-blocking incoming/outgoing performance logging.
6. **Gateway-Level Circuit Breakers**: Forwarding to fallback controllers if downstream services fail.
7. **Role-Based Authorization (RBAC)**: Enforcing `ROLE_ADMIN` checks at the gateway layer.

---

## User (Prompt 11)

Phase 7: Advanced Inter-Service JWT Propagation & Interview Wrap-Up
Prompt 7 (JWT Propagation & End-to-End Walkthrough):
"Show how to propagate the incoming JWT token from order-service down to product-service during OpenFeign calls.

Implement a Feign RequestInterceptor bean in order-service that extracts the Authorization header from the current RequestContextHolder and attaches it to the outgoing Feign request.

Provide a step-by-step Postman execution walkthrough showing:

Register user -> Receive JWT.

Create product (pass JWT in header).

Create order (pass JWT in header, trigger OpenFeign call).

Test Rate Limiting by firing rapid requests.

Test Circuit Breaker by stopping product-service container."

---

## Assistant (Response 11)

The implementation for **Phase 7: Inter-Service JWT Propagation & End-to-End Walkthrough** has been completed.

---

### Project Files Created / Updated

- [FeignClientConfig.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/config/FeignClientConfig.java) — `RequestInterceptor` bean extracting the JWT from `RequestContextHolder` and attaching it to outgoing Feign calls
- [ProductClient.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/main/java/com/microservices/orderservice/client/ProductClient.java) — Wired with `configuration = FeignClientConfig.class`
- [FeignClientConfigTest.java](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/order-service/src/test/java/com/microservices/orderservice/config/FeignClientConfigTest.java) — Unit test verifying token propagation and null-safety
- [END_TO_END_POSTMAN_WALKTHROUGH.md](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/END_TO_END_POSTMAN_WALKTHROUGH.md) — Step-by-step Postman testing guide including collection JSON

---

## 1. How JWT Propagation Works with OpenFeign

When a client calls `POST /api/v1/orders` through the API Gateway, the request reaches `order-service` with `Authorization: Bearer <token>`.

By default, when `order-service` executes `productClient.getProductById(productId)`, OpenFeign initiates a brand new HTTP request with **no headers**. If `product-service` is secured, it rejects the call with `401 Unauthorized`.

```
[ Client / Postman ]
        │
        ▼ (Authorization: Bearer <token>)
[ API Gateway (8080) ]
        │
        ▼ (Authorization: Bearer <token>)
[ OrderService (8083) ] ──► Stores request in ThreadLocal (RequestContextHolder)
        │
        ▼ invokes Feign ProductClient
┌────────────────────────────────────────────────────────┐
│ Feign RequestInterceptor                               │
│ 1. Extracts Authorization header from current request  │
│ 2. Attaches 'Authorization: Bearer <token>' to Feign   │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼ (Authorization: Bearer <token>)
               [ ProductService (8082) ]
```

### `FeignClientConfig.java`

```java
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
```

### `ProductClient.java`

```java
package com.microservices.orderservice.client;

import com.microservices.orderservice.config.FeignClientConfig;
import com.microservices.orderservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", configuration = FeignClientConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponseDto getProductById(@PathVariable("id") Long id);
}
```

---

## 2. Step-by-Step Postman Walkthrough

All requests target the **API Gateway on Port 8080**:

### Step 1: Register User & Receive Initial JWT
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/auth/register`
- **Headers**: `Content-Type: application/json`
- **Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "Password123"
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwic3ViIjoiamFuZUBleGFtcGxlLmNvbSI...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "email": "jane@example.com",
    "role": "ROLE_USER"
  }
  ```
  *(Copy the `token` value for the subsequent requests)*

---

### Step 2: Create Product (Passing JWT)
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/products`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <TOKEN_FROM_STEP_1>`
- **Body**:
  ```json
  {
    "name": "MacBook Pro M3",
    "description": "16-inch, 36GB RAM, 1TB SSD",
    "price": 2499.99,
    "stockQuantity": 20
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "id": 1,
    "name": "MacBook Pro M3",
    "description": "16-inch, 36GB RAM, 1TB SSD",
    "price": 2499.99,
    "stockQuantity": 20
  }
  ```

---

### Step 3: Create Order (Triggers Feign with Propagated JWT)
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/orders`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <TOKEN_FROM_STEP_1>`
- **Body**:
  ```json
  {
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }
  ```
- **Under the Hood**:
  1. Gateway verifies token and routes to `order-service`.
  2. `order-service` calls `product-service` via Feign.
  3. `jwtFeignRequestInterceptor` attaches `Authorization: Bearer <token>` to the Feign request.
  4. Total price calculated: `2499.99 * 2 = 4999.98`.
- **Response (`201 Created`)**:
  ```json
  {
    "id": 1,
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "totalPrice": 4999.98,
    "orderStatus": "CREATED",
    "failureReason": null,
    "createdAt": "2026-11-01T17:55:00.123456"
  }
  ```

---

### Step 4: Test Rate Limiting (Redis Token Bucket)
Fire 25 rapid requests to `http://localhost:8080/api/v1/products`:
- **Postman**: Use Collection Runner with 30 iterations and 0 ms delay.
- **Terminal Alternative**:
  ```bash
  for i in {1..25}; do
    curl -s -o /dev/null -w "Req $i: HTTP %{http_code}\n" \
      -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products
  done
  ```
- **Result**:
  - Requests 1–20: `HTTP 200 OK`
  - Requests 21–25: **`HTTP 429 Too Many Requests`**

---

### Step 5: Test Circuit Breaker (Stopping `product-service`)
1. Stop the `product-service` container or local process:
   ```bash
   docker stop product_db
   ```
2. Send the same order request (`POST http://localhost:8080/api/v1/orders`).
3. **Graceful Fallback Response (`503 Service Unavailable`)**:
   ```json
   {
     "id": 2,
     "userId": 1,
     "productId": 1,
     "quantity": 2,
     "totalPrice": 0.00,
     "orderStatus": "FAILED",
     "failureReason": "Product Service unavailable or timed out: Load balancer does not contain an instance for the service product-service",
     "createdAt": "2026-11-01T17:56:10.987654"
   }
   ```
   > The system gracefully catches the timeout/failure via Resilience4j, records the diagnostic reason in `order_db`, and avoids cascading 500 error crashes.

---

### Master Project Summary Across All 7 Phases

| Phase | Component | Key Technologies | Ports |
| :---: | :--- | :--- | :---: |
| **Phase 1** | Local Infrastructure | Docker Compose, MySQL 8, Redis 7 | `3309` (User), `3307` (Product), `3308` (Order), `6379` (Redis) |
| **Phase 2** | Service Registry | Spring Cloud Netflix Eureka Server | `8761` |
| **Phase 3** | User Service | Spring Boot 3, Spring Security 6, BCrypt, JJWT | `8081` |
| **Phase 4** | Product Service | Spring Boot 3, Spring Data JPA, MySQL | `8082` |
| **Phase 5** | Order Service | Spring Boot 3, OpenFeign, Resilience4j Circuit Breaker | `8083` |
| **Phase 6** | API Gateway | Spring Cloud Gateway, Reactive Redis, Netty, JWT Filter | `8080` |
| **Phase 7** | Inter-Service Security | Feign `RequestInterceptor`, Context Propagation, E2E Walkthrough | Gateway `8080` |

---

## User (Prompt 12)

give me postman collection to test this

---

## Assistant (Response 12)

A complete, automated **Postman Collection (v2.1)** and companion **Environment File** have been created in your workspace.

---

### Files Created

- [Enterprise_Microservices.postman_collection.json](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/Enterprise_Microservices.postman_collection.json) — Complete collection with 4 test suites and automated test scripts
- [Enterprise_Microservices.postman_environment.json](file:///Users/apple/Workspace/Leaning%20of%20interview/microservices/Enterprise_Microservices.postman_environment.json) — Local environment configuration (`base_url: http://localhost:8080`)

---

### How to Import into Postman (1-Click Setup)

1. Open **Postman**.
2. Click the **Import** button in the top-left corner.
3. Drag and drop both files:
   - `Enterprise_Microservices.postman_collection.json`
   - `Enterprise_Microservices.postman_environment.json`
4. In the top-right environment dropdown, select **Local Microservices Environment**.

---

### Collection Structure & Automated Flow

The collection includes automated **Post-Response Scripts** so you don't need to copy and paste tokens manually.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. Authentication (Public)                                                  │
│   ├── 1.1 Register New User  ──► Auto-extracts JWT into {{jwt_token}}       │
│   └── 1.2 Login User         ──► Auto-updates {{jwt_token}}                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ 2. Product Catalog (Secured via Gateway)                                    │
│   ├── 2.1 Create Product     ──► Uses {{jwt_token}}, saves {{product_id}}   │
│   ├── 2.2 Get Product By ID  ──► Verifies status 200 & product details      │
│   ├── 2.3 Get All Products   ──► Lists all catalog items                    │
│   └── 2.4 404 Handler Test   ──► Tests clean JSON @RestControllerAdvice 404 │
├─────────────────────────────────────────────────────────────────────────────┤
│ 3. Order Processing (OpenFeign Inter-Service Security)                      │
│   ├── 3.1 Create Order       ──► Feign passes JWT to Product Service (201)  │
│   ├── 3.2 Get Order By ID    ──► Verifies status CREATED & total price      │
│   ├── 3.3 Get All Orders     ──► Lists order history                        │
│   └── 3.4 Insufficient Stock ──► Verifies order marked as FAILED            │
├─────────────────────────────────────────────────────────────────────────────┤
│ 4. Security & Fault Tolerance Tests                                         │
│   ├── 4.1 Missing Header     ──► Verifies 401 Unauthorized rejection        │
│   ├── 4.2 Tampered Token     ──► Verifies 401 Invalid Token rejection       │
│   ├── 4.3 Circuit Breaker    ──► Tests 503 Fallback when Product Service DOWN│
│   └── 4.4 Rate Limiter       ──► Tests Redis 429 Too Many Requests (Runner) │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### Key Execution Highlights

#### 1. Zero-Touch Token Management
In **1.1 Register New User**, the following test script runs automatically:
```javascript
var jsonData = pm.response.json();
pm.collectionVariables.set("jwt_token", jsonData.token);
```
Every subsequent secured endpoint automatically references `Authorization: Bearer {{jwt_token}}`.

#### 2. Testing OpenFeign JWT Propagation
Running **3.1 Create Order** verifies the full end-to-end chain:
1. Gateway receives request on `8080`, validates `{{jwt_token}}`, and proxies to `order-service`.
2. `order-service` calls `product-service` via OpenFeign.
3. `jwtFeignRequestInterceptor` extracts the token from `RequestContextHolder` and attaches it to the outgoing call.
4. Total price is calculated and saved as `CREATED`.

#### 3. Testing Redis Rate Limiting (10 req/sec)
1. Right-click **4.4 Rate Limiting** or the entire collection.
2. Select **Run collection**.
3. Set **Iterations** to `30` and **Delay** to `0 ms`.
4. Click **Run Enterprise Microservices E2E Test Suite**.
> You will observe requests 1–20 return `200 OK`, while requests 21–30 return **`429 Too Many Requests`**.

#### 4. Testing Resilience4j Circuit Breaker
1. Stop the `product-service` container (`docker stop product_db` or stop the local application).
2. Execute **4.3 Circuit Breaker Test**.
3. Observe the graceful **`503 Service Unavailable`** response with `orderStatus: FAILED` instead of an unhandled crash.

---

## User (Prompt 13)

"Without making any changes to the content and while keeping the formatting, export the entire current conversation into a Markdown file named ./project_chat_summary.md in the project root."

---

## Assistant (Response 13)

*(Export generated into ./project_chat_summary.md)*
