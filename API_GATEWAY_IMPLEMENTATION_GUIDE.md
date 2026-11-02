# Spring Cloud API Gateway: The Complete Architecture & Step-by-Step Implementation Guide

This guide explains **why** you need an API Gateway, **what problems** it solves in enterprise architectures, the **exact order** in which you should create files when building one from scratch, and **how to implement standard features** (CORS, Rate Limiting, JWT validation, Path Rewriting, Circuit Breakers, Correlation IDs) in any project.

---

## Table of Contents
1. [What is an API Gateway & What Problems Does It Solve?](#1-what-is-an-api-gateway--what-problems-does-it-solve)
2. [The Golden Rule: Reactive (Netty) vs. Servlet (Tomcat)](#2-the-golden-rule-reactive-netty-vs-servlet-tomcat)
3. [Exact Step-by-Step File Creation Sequence](#3-exact-step-by-step-file-creation-sequence)
4. [Detailed Breakdown of Each Step](#4-detailed-breakdown-of-each-step)
5. [How to Implement Common Features in Any Project](#5-how-to-implement-common-features-in-any-project)
   - [Feature 1: Path Rewriting (`RewritePath` & `StripPrefix`)](#feature-1-path-rewriting-rewritepath--stripprefix)
   - [Feature 2: Routing to External / 3rd-Party APIs](#feature-2-routing-to-external--3rd-party-apis)
   - [Feature 3: Centralized CORS Configuration](#feature-3-centralized-cors-configuration)
   - [Feature 4: Request Tracing & Correlation ID Injection](#feature-4-request-tracing--correlation-id-injection)
   - [Feature 5: Global Request & Response Logging Filter](#feature-5-global-request--response-logging-filter)
   - [Feature 6: Gateway-Level Circuit Breakers](#feature-6-gateway-level-circuit-breakers)
   - [Feature 7: Role-Based Authorization at Gateway](#feature-7-role-based-authorization-at-gateway)
6. [Top 5 Pitfalls & Troubleshooting](#6-top-5-pitfalls--troubleshooting)

---

## 1. What is an API Gateway & What Problems Does It Solve?

In a microservices architecture, you have dozens or hundreds of independent services running on different ports and hosts.

### The Chaos WITHOUT an API Gateway:
```
[ Frontend / Mobile App ]
     ├──► Direct HTTP call to user-service:8081 (Must know port 8081)
     ├──► Direct HTTP call to product-service:8082 (Must know port 8082)
     └──► Direct HTTP call to order-service:8083 (Must know port 8083)
```
- **Port Exposure**: Every microservice must expose its port publicly to the internet, creating a huge attack surface.
- **Repeated Security**: Every single microservice must duplicate code to parse JWT tokens, check expiration, and validate permissions.
- **CORS Hell**: Each service must configure browser CORS headers independently.
- **DDoS Vulnerability**: If a malicious user spams an endpoint, downstream services get overwhelmed and crash.
- **Client Coupling**: If you split `order-service` into `order-service` and `payment-service`, you break the mobile app!

---

### The Order WITH an API Gateway:
```
[ Frontend / Mobile App ]
           │
           ▼ (Single Entry Point: port 8080)
   ┌────────────────────────────────────────────────────────┐
   │                   API GATEWAY (8080)                   │
   │  • Reverse Proxy & Dynamic Routing (lb://)             │
   │  • Centralized Security (Validates JWT Once)           │
   │  • Rate Limiting (Redis Token Bucket: 10 req/sec)      │
   │  • Global CORS & SSL Termination                       │
   │  • Distributed Tracing (Correlation IDs)               │
   └───────────────────────┬────────────────────────────────┘
                           │ Internal Private Network
       ┌───────────────────┼───────────────────┐
       ▼                   ▼                   ▼
 [ user-service ]   [ product-service ]  [ order-service ]
   (Port: 8081)        (Port: 8082)        (Port: 8083)
   (Hidden inside)     (Hidden inside)     (Hidden inside)
```

### Real-World Problems It Solves:
1. **Single Public Entry Point**: External clients only know `https://api.yourdomain.com:443` (or `localhost:8080`). All internal microservice ports are sealed in a private network or Docker network.
2. **Centralized Authentication Offloading**: The Gateway checks the JWT cryptographic signature and expiration **once**. Internal microservices can trust requests coming from the Gateway.
3. **Identity Propagation**: The Gateway extracts the user ID, email, and role from the JWT and injects them as standard HTTP headers (`X-Auth-User-Email`, `X-Auth-User-Role`) for downstream services.
4. **Rate Limiting & Anti-Abuse**: Prevents brute-force attacks and noisy neighbors using Redis token buckets.
5. **Decoupled Architecture**: You can rename, refactor, or migrate internal services without changing public URLs.

---

## 2. The Golden Rule: Reactive (Netty) vs. Servlet (Tomcat)

> [!CAUTION]
> **NEVER add `spring-boot-starter-web` to an API Gateway project!**

- Traditional Spring Boot applications (`user-service`, `product-service`) use **Spring MVC**, which runs on an embedded **Apache Tomcat** servlet container using a **blocking thread-per-request model** (1 thread per active connection).
- **Spring Cloud Gateway** uses **Spring WebFlux** and runs on an embedded **Eclipse Netty** server using a **non-blocking event-loop model**. A handful of threads handle 50,000+ concurrent connections.
- If you include `spring-boot-starter-web`, Spring will detect Tomcat and refuse to start, throwing:
  ```
  Spring MVC found on classpath, which is incompatible with Spring Cloud Gateway.
  ```

---

## 3. Exact Step-by-Step File Creation Sequence

When building an API Gateway from scratch in any project, create files in this **exact order**:

```mermaid
flowchart TD
    F1["1. pom.xml<br/>(Establish non-blocking WebFlux & Netty classpath)"] --> F2["2. application.yml<br/>(Define port, Redis, Eureka, and routing tables)"]
    F2 --> F3["3. ApiGatewayApplication.java<br/>(Spring Boot main entry class)"]
    F3 --> F4["4. JwtUtil.java<br/>(Cryptographic helper for JWT parsing & validation)"]
    F4 --> F5["5. JwtValidationFilter.java<br/>(Custom GatewayFilterFactory for route security)"]
    F5 --> F6["6. RateLimiterConfig.java<br/>(Redis KeyResolver for IP-based throttling)"]
    F6 --> F7["7. JwtValidationFilterTest.java<br/>(Reactive unit tests using StepVerifier)"]
```

### Why this order?
1. **`pom.xml` first**: You cannot write a single line of Java code without importing the reactive Gateway classes (`AbstractGatewayFilterFactory`, `ServerWebExchange`, `Mono`).
2. **`application.yml` second**: The gateway routes and configuration define *what* the application does.
3. **`ApiGatewayApplication.java` third**: Verifies that the gateway can boot up on Netty without dependency conflicts.
4. **`JwtUtil.java` fourth**: The filter needs this utility to parse tokens; building the helper first prevents compilation errors.
5. **`JwtValidationFilter.java` fifth**: The filter connects the routing configuration in YAML to the JWT utility.
6. **`RateLimiterConfig.java` sixth**: Provides the `KeyResolver` bean referenced by YAML (`#{@ipKeyResolver}`).
7. **Tests seventh**: Validates the reactive pipeline using `MockServerWebExchange`.

---

## 4. Detailed Breakdown of Each Step

### Step 1: `pom.xml`
Include `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`, `spring-boot-starter-data-redis-reactive`, and `jjwt`.

```xml
<!-- Core Reactive Gateway (Netty Engine) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- Eureka Client for dynamic lb:// routing -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- Reactive Redis for Rate Limiter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>

<!-- JJWT for token validation -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

---

### Step 2: `application.yml`
Define the routes. The `lb://` prefix instructs Spring Cloud LoadBalancer to look up instances registered in Eureka.

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  data:
    redis:
      host: localhost
      port: 6379
      password: redispassword

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        # Public Route (No JWT filter)
        - id: auth-route
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/**

        # Secured Route (Custom Filter + Rate Limiter)
        - id: product-route
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
```

---

### Step 3: `ApiGatewayApplication.java`
The bootstrap class.

```java
package com.microservices.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

---

### Step 4: `JwtUtil.java`
Handles cryptographic parsing of HMAC-SHA256 signatures and token expiration.

```java
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}
```

---

### Step 5: `JwtValidationFilter.java`
Custom filter extending `AbstractGatewayFilterFactory`. Note how it mutates the request to add downstream headers.

```java
@Component
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

            // 1. Validate Header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 2. Validate Token
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Invalid or Expired JWT", HttpStatus.UNAUTHORIZED);
            }

            // 3. Propagate Identity to Downstream Microservices
            String userEmail = jwtUtil.extractUsername(token);
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User-Email", userEmail)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"status\":%d,\"error\":\"%s\"}", status.value(), err);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {}
}
```

---

### Step 6: `RateLimiterConfig.java`
Defines the `KeyResolver` bean for Redis rate limiting.

```java
@Configuration
public class RateLimiterConfig {

    @Bean
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

## 5. How to Implement Common Features in Any Project

Here is how to add enterprise features to Spring Cloud Gateway in other projects:

### Feature 1: Path Rewriting (`RewritePath` & `StripPrefix`)

#### Use Case:
The public client calls `/orders/123`, but the internal service expects `/api/v1/orders/123`.

```yaml
- id: rewrite-order-route
  uri: lb://order-service
  predicates:
    - Path=/orders/**
  filters:
    # Rewrites /orders/123 -> /api/v1/orders/123
    - RewritePath=/orders/(?<segment>.*), /api/v1/orders/$\{segment}
```

#### StripPrefix Use Case:
The client calls `/products-api/api/v1/products`. You want to drop `/products-api`:
```yaml
- id: strip-prefix-route
  uri: lb://product-service
  predicates:
    - Path=/products-api/**
  filters:
    - StripPrefix=1
```

---

### Feature 2: Routing to External / 3rd-Party APIs

#### Use Case:
Route `/api/v1/payments/**` to an external Stripe or PayPal service instead of an internal Eureka service.

```yaml
- id: stripe-external-route
  uri: https://api.stripe.com
  predicates:
    - Path=/api/v1/payments/**
  filters:
    - SetPath=/v1/charges
    - AddRequestHeader=Authorization, Bearer sk_live_your_stripe_key
```

---

### Feature 3: Centralized CORS Configuration

Do not configure CORS in individual microservices. Configure it once in the Gateway's `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "https://myfrontend.com"
              - "http://localhost:3000"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
```

---

### Feature 4: Request Tracing & Correlation ID Injection

#### Use Case:
Assign a unique UUID (`X-Correlation-ID`) to every incoming request. If an issue occurs, you can search that UUID across the logs of all microservices.

```java
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Check if client passed an existing Correlation ID, otherwise generate one
        String correlationId = Optional.ofNullable(request.getHeaders().getFirst(CORRELATION_ID_HEADER))
                .orElse(UUID.randomUUID().toString());

        // 2. Inject into downstream request headers
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        // 3. Add to response headers so client receives it in responses
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Execute before all other filters
    }
}
```

---

### Feature 5: Global Request & Response Logging Filter

```java
@Component
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        log.info("GATEWAY INCOMING: [{} {}]", method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("GATEWAY OUTGOING: [{} {}] -> Status: {} ({} ms)", method, path, status, duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
```

---

### Feature 6: Gateway-Level Circuit Breakers

Prevent cascading failures if a downstream microservice is completely unresponsive:

```yaml
- id: resilient-order-route
  uri: lb://order-service
  predicates:
    - Path=/api/v1/orders/**
  filters:
    - name: CircuitBreaker
      args:
        name: orderCircuitBreaker
        fallbackUri: forward:/fallback/orders-down
```

Controller for Fallback:
```java
@RestController
public class FallbackController {

    @RequestMapping("/fallback/orders-down")
    public ResponseEntity<Map<String, String>> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Order service is currently experiencing high latency. Please retry shortly."));
    }
}
```

---

### Feature 7: Role-Based Authorization at Gateway

Inspect roles directly inside the filter configuration:

```java
@Component
public class RoleCheckFilter extends AbstractGatewayFilterFactory<RoleCheckFilter.Config> {

    public RoleCheckFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String role = exchange.getRequest().getHeaders().getFirst("X-Auth-User-Role");
            if (role == null || !role.equals(config.getRequiredRole())) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    @Data
    public static class Config {
        private String requiredRole;
    }
}
```

In `application.yml`:
```yaml
filters:
  - name: RoleCheckFilter
    args:
      requiredRole: ROLE_ADMIN
```

---

## 6. Top 5 Pitfalls & Troubleshooting

### 1. `Spring MVC found on classpath`
- **Cause**: Someone added `spring-boot-starter-web` to the Gateway's `pom.xml`.
- **Fix**: Remove `spring-boot-starter-web` and ensure `spring-cloud-starter-gateway` is present.

### 2. Calling Blocking Methods (`block()`, `Thread.sleep()`)
- **Cause**: Calling `.block()` or `Thread.sleep()` inside a Gateway filter halts a Netty event loop thread, dropping throughput from 50,000 req/sec to 10 req/sec.
- **Fix**: Never call blocking methods. Always return `Mono<Void>` or `Flux<T>`.

### 3. Infinite Redirect Loops on Public Routes
- **Cause**: Applying `JwtValidationFilter` globally or on `/api/v1/auth/login`.
- **Fix**: Only apply authentication filters to specific secured route blocks, or check if the requested path is in a whitelist before verifying the token.

### 4. Redis Rate Limiter Not Enforcing Limits
- **Cause**: `KeyResolver` returned `null` or `Mono.empty()`. If the key is empty, the rate limiter allows the request through without decrementing a token.
- **Fix**: Always provide a fallback key in `KeyResolver` (e.g. `.orElse("127.0.0.1")`).

### 5. `503 Service Unavailable: Load balancer does not contain an instance`
- **Cause**: `lb://service-name` doesn't match `spring.application.name` in the target service, or the service hasn't registered with Eureka yet.
- **Fix**: Check `http://localhost:8761` to confirm the service name is visible in the Eureka dashboard.

