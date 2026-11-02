# Microservices Architecture: Completed CRUD Operations & Service Summary (Phases 1–5)

This document provides a comprehensive summary of all CRUD operations, database models, and architectural components implemented across Phases 1 through 5 of our Enterprise Microservices project.

---

## Architecture Overview

```
                               ┌────────────────────────────────┐
                               │   Eureka Discovery Server      │
                               │   (service-registry: 8761)     │
                               └───────────────▲────────────────┘
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               │                               │                               │
       ┌───────▼────────┐             ┌────────▼────────┐             ┌────────▼────────┐
       │  user-service  │             │ product-service │◄────────────┤  order-service  │
       │  (Port: 8081)  │             │  (Port: 8082)   │ OpenFeign & │  (Port: 8083)   │
       └───────┬────────┘             └────────┬────────┘ Resilience4j└────────┬────────┘
               │                               │                               │
       ┌───────▼────────┐             ┌────────▼────────┐             ┌────────▼────────┐
       │ user_db (MySQL)│             │product_db(MySQL)│             │ order_db (MySQL)│
       │ Host Port 3309 │             │ Host Port 3307  │             │ Host Port 3308  │
       └────────────────┘             └─────────────────┘             └─────────────────┘
```

---

## 1. Phase 1: Localized Infrastructure (Docker Compose)

| Container Name | Engine | Host Port | Internal Port | Database / Resource Name | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `user_db` | MySQL 8.0 | `3309` | `3306` | `user_db` | User identity & authentication data |
| `product_db` | MySQL 8.0 | `3307` | `3306` | `product_db` | Product catalog & inventory data |
| `order_db` | MySQL 8.0 | `3308` | `3306` | `order_db` | Order transactions & processing history |
| `redis` | Redis 7 Alpine | `6379` | `6379` | Cache / Keyspace | API Gateway rate limiting & token store |

---

## 2. Phase 2: Eureka Service Registry (`service-registry`)

- **Port**: `8761`
- **Role**: Centralized service discovery registry.
- **Key Features**:
  - Standalone mode (`register-with-eureka: false`, `fetch-registry: false`).
  - Dynamic discovery allowing services to locate each other using logical names (`lb://user-service`, `lb://product-service`, `lb://order-service`) rather than hardcoded IP addresses.

---

## 3. Phase 3: User Service (`user-service`)

- **Port**: `8081`
- **Database**: `user_db` (Port `3309`)
- **Security**: Spring Security 6 + BCrypt + JJWT (0.11.5)

### Database Entity: `User` (`users` table)
| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier |
| `name` | `VARCHAR(255)` | NOT NULL | User's full name |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | User login email |
| `password` | `VARCHAR(255)` | NOT NULL | BCrypt hashed password |
| `role` | `VARCHAR(255)` | NOT NULL (Default: `ROLE_USER`) | Authorization role |

### CRUD & Auth Operations
| HTTP Method | Endpoint | Request Body | Response Status | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | `RegisterRequestDto` (`name`, `email`, `password`, `role`) | `201 Created` | **CREATE**: Validates email uniqueness, hashes password via BCrypt, saves to MySQL, and generates a signed 24h JWT. |
| `POST` | `/api/v1/auth/login` | `LoginRequestDto` (`email`, `password`) | `200 OK` | **READ / AUTHENTICATE**: Validates credentials against hashed password, issues JWT token with claims (`sub`, `role`). |

---

## 4. Phase 4: Product Service (`product-service`)

- **Port**: `8082`
- **Database**: `product_db` (Port `3307`)

### Database Entity: `Product` (`products` table)
| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique product identifier |
| `name` | `VARCHAR(255)` | NOT NULL | Product title |
| `description` | `VARCHAR(1000)` | NULLABLE | Detailed description |
| `price` | `DECIMAL(10,2)` | NOT NULL | Unit price |
| `stockQuantity`| `INT` | NOT NULL | Available inventory count |

### CRUD Operations
| HTTP Method | Endpoint | Request Body | Response Status | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/products` | `ProductRequestDto` (`name`, `description`, `price`, `stockQuantity`) | `201 Created` | **CREATE**: Validates input constraints and inserts a new product record into MySQL. |
| `GET` | `/api/v1/products/{id}` | None | `200 OK` (or `404 Not Found`) | **READ (One)**: Retrieves product details. Used directly by the Order Service to verify stock and price. |
| `GET` | `/api/v1/products` | None | `200 OK` | **READ (All)**: Lists all catalog products. |

---

## 5. Phase 5: Order Service (`order-service`)

- **Port**: `8083`
- **Database**: `order_db` (Port `3308`)
- **Inter-Service Tech**: Spring Cloud OpenFeign + Resilience4j Circuit Breaker

### Database Entity: `OrderMaster` (`order_master` table)
| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique order tracking number |
| `userId` | `BIGINT` | NOT NULL | User who placed the order |
| `productId` | `BIGINT` | NOT NULL | Product ordered |
| `quantity` | `INT` | NOT NULL | Number of units requested |
| `totalPrice` | `DECIMAL(12,2)` | NOT NULL | Calculated (`unit_price * quantity`) |
| `orderStatus` | `VARCHAR(50)` | NOT NULL (`CREATED`, `FAILED`) | Lifecycle state of order |
| `failureReason` | `VARCHAR(500)` | NULLABLE | Reason if order failed or fallback triggered |
| `createdAt` | `DATETIME` | NOT NULL | Timestamp of creation |

### CRUD & Orchestration Operations
| HTTP Method | Endpoint | Request Body | Response Status | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/orders` | `OrderRequestDto` (`userId`, `productId`, `quantity`) | `201 Created` / `503 Unavailable` | **CREATE & ORCHESTRATE**: Calls `ProductClient` via Feign. Validates stock, calculates total price (`price * qty`), and persists order as `CREATED`. |
| `FALLBACK` | *Circuit Breaker Triggered* | `OrderRequestDto`, `Throwable` | `503 Unavailable` | **FALLBACK (Resilience4j)**: When Product Service is down or times out, catches failure, sets status to `FAILED`, stores error reason, and persists safely. |
| `GET` | `/api/v1/orders/{id}` | None | `200 OK` (or `404 Not Found`) | **READ (One)**: Fetches order tracking details. |
| `GET` | `/api/v1/orders` | None | `200 OK` | **READ (All)**: Lists all order history records. |

---

## Summary of All Ports in the Microservices Ecosystem

| Service | Port | Technology Stack |
| :--- | :--- | :--- |
| **API Gateway** *(Phase 6)* | `8080` | Spring Cloud Gateway, Reactive Redis, Netty |
| **Service Registry** *(Phase 2)* | `8761` | Eureka Server |
| **User Service** *(Phase 3)* | `8081` | Spring Boot 3, Spring Security 6, JJWT |
| **Product Service** *(Phase 4)* | `8082` | Spring Boot 3, Spring Data JPA |
| **Order Service** *(Phase 5)* | `8083` | Spring Boot 3, OpenFeign, Resilience4j |
| **User DB (Docker)** | `3309` | MySQL 8 |
| **Product DB (Docker)**| `3307` | MySQL 8 |
| **Order DB (Docker)** | `3308` | MySQL 8 |
| **Redis (Docker)** | `6379` | Redis 7 Alpine |

