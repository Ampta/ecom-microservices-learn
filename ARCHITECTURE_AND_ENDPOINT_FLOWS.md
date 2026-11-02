# Enterprise Microservices: Architecture & Complete Endpoint Flow Diagrams

This document provides architectural breakdowns of every microservice and detailed sequence diagrams for **every endpoint flow** in the system.

---

## 1. High-Level System Architecture & Component Roles

```mermaid
flowchart TD
    Client["Client / Mobile / Postman"] -->|HTTP Port 8080| GW["API Gateway (Netty: 8080)"]

    subgraph ServiceRegistryCluster ["Service Registry Layer"]
        Eureka["Eureka Discovery Server<br/>(service-registry: 8761)"]
    end

    subgraph InfrastructureLayer ["Distributed Caching & DB Layer"]
        Redis["Redis 7 (Port: 6379)<br/>Rate Limiting Token Bucket"]
        UserDB[("user_db (MySQL: 3309)")]
        ProdDB[("product_db (MySQL: 3307)")]
        OrderDB[("order_db (MySQL: 3308)")]
    end

    subgraph MicroservicesCluster ["Internal Business Microservices"]
        US["user-service (8081)"]
        PS["product-service (8082)"]
        OS["order-service (8083)"]
    end

    GW <-->|Check Rate Limit| Redis
    GW <-->|Resolve lb://| Eureka

    US -.->|Heartbeat Registration| Eureka
    PS -.->|Heartbeat Registration| Eureka
    OS -.->|Heartbeat Registration| Eureka

    GW -->|Route /api/v1/auth/**| US
    GW -->|Route /api/v1/products/**| PS
    GW -->|Route /api/v1/orders/**| OS

    OS ==>|OpenFeign + Propagated JWT| PS

    US --> UserDB
    PS --> ProdDB
    OS --> OrderDB
```

---

## 2. In-Depth Service Responsibilities

### 1. `service-registry` (Eureka Server, Port 8761)
- **Role**: Central address book of the entire microservices ecosystem.
- **How It Works**:
  - Every microservice on boot reads `eureka.client.service-url.defaultZone` and sends its application name (`spring.application.name`), container IP, port, and health check URL.
  - Services send a **heartbeat every 30 seconds**. If Eureka misses heartbeats beyond the lease expiration threshold, it evicts the instance.
  - Client services (`api-gateway`, `order-service`) pull the registry table periodically and cache it in-memory. They do **not** query Eureka on every HTTP request.

### 2. `api-gateway` (Spring Cloud Gateway, Port 8080)
- **Role**: Perimeter reverse proxy, security enforcement boundary, and traffic controller.
- **Key Modules**:
  - **Reactive Non-Blocking Engine**: Runs on Project Reactor / Netty (no thread-per-request overhead).
  - **Dynamic Routing**: Uses `lb://<service-name>` to balance traffic across healthy instances discovered via Eureka.
  - **`JwtValidationFilter`**: Intercepts secured routes (`/api/v1/products/**`, `/api/v1/orders/**`). Cryptographically validates HMAC-SHA256 signature and expiration date. Rejects unauthorized requests with clean JSON 401s.
  - **Downstream Header Propagation**: Injects authenticated identity (`X-Auth-User-Email`, `X-Auth-User-Role`) into downstream requests.
  - **`RequestRateLimiter`**: Backed by Redis token buckets (10 req/sec limit, 20 burst capacity) keyed by client IP (`ipKeyResolver`).

### 3. `user-service` (Identity & Authentication, Port 8081)
- **Role**: User persistence, credential verification, and cryptographic token issuance.
- **Database**: `user_db` (MySQL Port 3309).
- **Key Modules**:
  - Spring Security 6 stateless filter chain.
  - BCrypt password encoder (one-way salt + hash).
  - `JwtService`: Signs tokens using HMAC-SHA256 with 24-hour expiration containing claims `sub` (email) and `role` (`ROLE_USER`).

### 4. `product-service` (Domain Service, Port 8082)
- **Role**: Catalog management and real-time inventory tracking.
- **Database**: `product_db` (MySQL Port 3307).
- **Key Modules**:
  - Spring Data JPA `ProductRepository`.
  - `@RestControllerAdvice` converting `ResourceNotFoundException` to clean JSON 404 responses.
  - Supplies inventory data to `order-service` via internal endpoints.

### 5. `order-service` (Business Orchestrator, Port 8083)
- **Role**: Transaction management, order placement, and cross-service orchestration.
- **Database**: `order_db` (MySQL Port 3308).
- **Key Modules**:
  - OpenFeign Client (`ProductClient`): Interacts with `product-service` dynamically.
  - `FeignClientConfig` (`RequestInterceptor`): Extracts the JWT Bearer token from the incoming request's `RequestContextHolder` and attaches it to the outgoing Feign request.
  - Resilience4j Circuit Breaker (`@CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")`): Gracefully catches downstream timeouts, marks order status as `FAILED`, and records diagnostic failure reasons.

---

## 3. Detailed Endpoint Sequence Diagrams

---

### Flow 1: User Registration (`POST /api/v1/auth/register`)
**Type**: Public Route (No Token Required)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant Redis as Redis (6379)
    participant Eureka as Eureka Server (8761)
    participant US as User Service (8081)
    participant DB as user_db (3309)

    Client->>GW: POST /api/v1/auth/register {name, email, password}
    GW->>Redis: Check IP Token Bucket (Rate Limiter)
    Redis-->>GW: Token available (Allow)
    GW->>Eureka: Resolve lb://user-service
    Eureka-->>GW: Returns instance IP:Port (localhost:8081)
    GW->>US: Forward POST /api/v1/auth/register
    US->>DB: SELECT count(*) FROM users WHERE email = ?
    DB-->>US: 0 (User does not exist)
    US->>US: Hash password using BCrypt
    US->>DB: INSERT INTO users (name, email, password, role)
    DB-->>US: Saved User (ID: 1)
    US->>US: JwtService.generateToken(email, role)
    US-->>GW: 201 Created {token, tokenType: "Bearer", expiresIn: 86400000}
    GW-->>Client: 201 Created {token, ...}
```

---

### Flow 2: User Login (`POST /api/v1/auth/login`)
**Type**: Public Route (Credential Verification & Token Issuance)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant US as User Service (8081)
    participant DB as user_db (3309)

    Client->>GW: POST /api/v1/auth/login {email, password}
    GW->>US: Forward to lb://user-service
    US->>DB: findByEmail(email)
    DB-->>US: User entity with BCrypt password
    US->>US: passwordEncoder.matches(rawPassword, encodedPassword)
    alt Password Matches
        US->>US: JwtService.generateToken(email, role)
        US-->>GW: 200 OK {token: "eyJhbGciOi...", role: "ROLE_USER"}
        GW-->>Client: 200 OK {token: "eyJhbGciOi..."}
    else Password Mismatch or User Not Found
        US-->>GW: 401 Unauthorized {error: "Invalid email or password"}
        GW-->>Client: 401 Unauthorized
    end
```

---

### Flow 3: Product Creation (`POST /api/v1/products`)
**Type**: Secured Route (Gateway JWT Validation Filter + Redis Rate Limiter)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant Redis as Redis (6379)
    participant PS as Product Service (8082)
    participant DB as product_db (3307)

    Client->>GW: POST /api/v1/products (Header: "Authorization: Bearer <token>")
    Note over GW: JwtValidationFilter Intercepts

    alt Missing or Malformed Token
        GW-->>Client: 401 Unauthorized {message: "Missing Authorization Header"}
    else Token Signature Invalid / Expired
        GW-->>Client: 401 Unauthorized {message: "Invalid or Expired JWT Token"}
    else Token Valid
        GW->>GW: Extract claims: user="jane@example.com", role="ROLE_USER"
        GW->>Redis: Check IP Rate Limit
        Redis-->>GW: Allowed
        GW->>PS: Forward request + Headers: X-Auth-User-Email, X-Auth-User-Role
        PS->>DB: INSERT INTO products (name, description, price, stockQuantity)
        DB-->>PS: Saved Product (ID: 1)
        PS-->>GW: 201 Created {id: 1, name: "MacBook Pro M3", price: 2499.99}
        GW-->>Client: 201 Created {id: 1, ...}
    end
```

---

### Flow 4: Get Product by ID (`GET /api/v1/products/{id}`)
**Type**: Secured Route with 404 Exception Handling

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant PS as Product Service (8082)
    participant DB as product_db (3307)

    Client->>GW: GET /api/v1/products/1 (Header: Authorization: Bearer <token>)
    GW->>GW: Validate JWT Token (Success)
    GW->>PS: Forward GET /api/v1/products/1
    PS->>DB: findById(1)
    alt Product Found
        DB-->>PS: Product entity
        PS-->>GW: 200 OK {id: 1, name: "MacBook Pro M3", stockQuantity: 20}
        GW-->>Client: 200 OK {id: 1, ...}
    else Product Not Found (e.g. ID: 99999)
        DB-->>PS: Optional.empty()
        PS->>PS: throw new ResourceNotFoundException("Product not found with id: 99999")
        Note over PS: GlobalExceptionHandler catches exception
        PS-->>GW: 404 Not Found {status: 404, error: "Not Found", message: "..."}
        GW-->>Client: 404 Not Found {status: 404, ...}
    end
```

---

### Flow 5: Order Creation — Happy Path (`POST /api/v1/orders`)
**Type**: Secured Route + OpenFeign Call + Inter-Service JWT Propagation

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant OS as Order Service (8083)
    participant Feign as Feign Interceptor
    participant Eureka as Eureka Server (8761)
    participant PS as Product Service (8082)
    participant DB as order_db (3308)

    Client->>GW: POST /api/v1/orders {userId: 1, productId: 1, quantity: 2}<br/>(Authorization: Bearer <token>)
    GW->>GW: Validate JWT Token
    GW->>OS: Forward POST /api/v1/orders (with Bearer Token)
    Note over OS: Request stored in ThreadLocal RequestContextHolder

    OS->>Feign: Call ProductClient.getProductById(1)
    Note over Feign: RequestInterceptor extracts Bearer Token from RequestContextHolder
    Feign->>Feign: Attach "Authorization: Bearer <token>" to outgoing request
    Feign->>Eureka: Resolve lb://product-service
    Eureka-->>Feign: Return IP:Port (localhost:8082)
    Feign->>PS: GET /api/v1/products/1 (with propagated Bearer Token)
    PS-->>Feign: Return Product {id: 1, price: 2499.99, stockQuantity: 20}
    Feign-->>OS: Return ProductResponseDto

    OS->>OS: Validate stock: stockQuantity (20) >= quantity (2)
    OS->>OS: Calculate total: 2499.99 * 2 = 4999.98
    OS->>DB: INSERT INTO order_master (userId, productId, qty, totalPrice, status="CREATED")
    DB-->>OS: Saved OrderMaster (ID: 1)
    OS-->>GW: 201 Created {id: 1, totalPrice: 4999.98, orderStatus: "CREATED"}
    GW-->>Client: 201 Created {id: 1, ...}
```

---

### Flow 6: Order Creation — Resilience4j Circuit Breaker Fallback
**Type**: Triggered when `product-service` is DOWN, times out, or instances drop from Eureka

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant OS as Order Service (8083)
    participant CB as Resilience4j Circuit Breaker
    participant PS as Product Service (OFFLINE)
    participant DB as order_db (3308)

    Client->>GW: POST /api/v1/orders {userId: 1, productId: 1, quantity: 2}<br/>(Authorization: Bearer <token>)
    GW->>GW: Validate JWT Token
    GW->>OS: Forward POST /api/v1/orders
    OS->>CB: Execute createOrder() wrapped in Circuit Breaker

    CB->>PS: GET http://product-service/api/v1/products/1
    Note over PS: Service is OFFLINE or Times Out (3000ms)
    PS--xCB: ConnectTimeoutException / Instance Not Found

    Note over CB: Failure detected! Fallback triggered immediately
    CB->>OS: Invoke createOrderFallback(request, throwable)
    OS->>DB: INSERT INTO order_master (userId, productId, totalPrice=0.00, status="FAILED", failureReason="Product Service unavailable")
    DB-->>OS: Saved OrderMaster (ID: 2)
    OS-->>GW: 503 Service Unavailable {id: 2, orderStatus: "FAILED", failureReason: "..."}
    GW-->>Client: 503 Service Unavailable (Graceful degradation, NO 500 crash)
```

---

### Flow 7: Redis Rate Limiting Flow (`HTTP 429 Too Many Requests`)
**Type**: Anti-Abuse & DDoS Protection at Perimeter

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant Redis as Redis (6379)
    participant Downstream as Downstream Microservice

    Note over Client: Client fires 25 rapid requests in < 1 second
    loop Requests 1 to 20
        Client->>GW: Request N
        GW->>Redis: EVALSHA token_bucket key=request_rate_limiter.127.0.0.1
        Redis-->>GW: Remaining tokens > 0 (Allowed)
        GW->>Downstream: Forward Request
        Downstream-->>Client: 200 OK
    end

    loop Requests 21 to 25 (Bucket Depleted)
        Client->>GW: Request N
        GW->>Redis: EVALSHA token_bucket key=request_rate_limiter.127.0.0.1
        Redis-->>GW: Remaining tokens = 0 (Denied)
        GW-->>Client: HTTP 429 Too Many Requests
    end
```

---

### Flow 8: Unauthorized Access Flow (`HTTP 401 Unauthorized`)
**Type**: Gateway JWT Validation Filter Interception

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as API Gateway (8080)
    participant Downstream as Downstream Microservice (Never Reached)

    Client->>GW: GET /api/v1/products (No Authorization header)
    Note over GW: JwtValidationFilter inspects request

    GW->>GW: check request.getHeaders().containsKey("Authorization")
    Note over GW: Evaluates to FALSE

    GW-->>Client: 401 Unauthorized {<br/>  "status": 401,<br/>  "error": "Unauthorized",<br/>  "message": "Missing Authorization Header",<br/>  "path": "/api/v1/products"<br/>}
    Note over Downstream: Downstream service is NEVER contacted (Zero resource waste)
```

---

## 4. Summary Table of All Endpoints & Filter Behaviors

| HTTP Method | Gateway Path | Route ID | Target Microservice | Filters Applied | Rate Limited? | Auth Required? | Expected Status Codes |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: |
| `POST` | `/api/v1/auth/register` | `user-service-route` | `lb://user-service` | `RequestRateLimiter` | Yes (10/s) | No (Public) | `201`, `400`, `409`, `429` |
| `POST` | `/api/v1/auth/login` | `user-service-route` | `lb://user-service` | `RequestRateLimiter` | Yes (10/s) | No (Public) | `200`, `400`, `401`, `429` |
| `POST` | `/api/v1/products` | `product-service-route` | `lb://product-service` | `JwtValidationFilter`, `RequestRateLimiter` | Yes (10/s) | **Yes** | `201`, `400`, `401`, `429` |
| `GET` | `/api/v1/products/{id}` | `product-service-route` | `lb://product-service` | `JwtValidationFilter`, `RequestRateLimiter` | Yes (10/s) | **Yes** | `200`, `401`, `404`, `429` |
| `GET` | `/api/v1/products` | `product-service-route` | `lb://product-service` | `JwtValidationFilter`, `RequestRateLimiter` | Yes (10/s) | **Yes** | `200`, `401`, `429` |
| `POST` | `/api/v1/orders` | `order-service-route` | `lb://order-service` | `JwtValidationFilter`, `RequestRateLimiter`, OpenFeign Interceptor, Resilience4j | Yes (10/s) | **Yes** | `201`, `400`, `401`, `429`, `503` (Fallback) |
| `GET` | `/api/v1/orders/{id}` | `order-service-route` | `lb://order-service` | `JwtValidationFilter`, `RequestRateLimiter` | Yes (10/s) | **Yes** | `200`, `401`, `404`, `429` |
| `GET` | `/api/v1/orders` | `order-service-route` | `lb://order-service` | `JwtValidationFilter`, `RequestRateLimiter` | Yes (10/s) | **Yes** | `200`, `401`, `429` |

