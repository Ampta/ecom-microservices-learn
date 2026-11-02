# End-to-End Postman Walkthrough & JWT Propagation Guide

This document provides a step-by-step execution guide to test the entire enterprise microservices architecture through the **API Gateway (Port 8080)**, including inter-service JWT propagation, distributed rate limiting, and circuit breaker resilience.

---

## Architecture Flow Summary

```
[ Postman / Client ]
        │
        ▼ (Port 8080)
┌────────────────────────────────────────────────────────┐
│                   API GATEWAY (8080)                   │
│  • Validates JWT Bearer Token                          │
│  • Enforces Redis Rate Limit (10 req/s)                │
└───────────────────────┬────────────────────────────────┘
                        │
      ┌─────────────────┼──────────────────┐
      │ (Public)        │ (Secured)        │ (Secured)
      ▼                 ▼                  ▼
[ user-service ] [ product-service ] [ order-service ]
  (Port: 8081)     (Port: 8082)        (Port: 8083)
                        ▲                  │
                        │ OpenFeign Call   │
                        │ + Propagated JWT │
                        └──────────────────┘
```

---

## Port & Endpoint Reference Table

All external requests must target the **API Gateway on Port 8080**:

| Step | Operation | Gateway URL (`http://localhost:8080`) | Method | Auth Required? | Target Internal Service |
| :---: | :--- | :--- | :---: | :---: | :--- |
| **1** | Register User | `/api/v1/auth/register` | `POST` | No (Public) | `user-service:8081` |
| **2** | Login User | `/api/v1/auth/login` | `POST` | No (Public) | `user-service:8081` |
| **3** | Create Product | `/api/v1/products` | `POST` | **Yes** (Bearer JWT) | `product-service:8082` |
| **4** | Query Product | `/api/v1/products/{id}` | `GET` | **Yes** (Bearer JWT) | `product-service:8082` |
| **5** | Create Order | `/api/v1/orders` | `POST` | **Yes** (Bearer JWT) | `order-service:8083` ➔ calls `product-service` |
| **6** | Rate Limit Test | Rapid requests to `/api/v1/products` | `GET` | **Yes** (Bearer JWT) | Gateway + Redis |
| **7** | Fallback Test | Create order while product service down | `POST` | **Yes** (Bearer JWT) | `order-service` Fallback |

---

## Step-by-Step Postman Execution Walkthrough

### Step 1: Register User & Receive Initial JWT

- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/auth/register`
- **Headers**:
  - `Content-Type: application/json`
- **Body (raw JSON)**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "Password123",
    "role": "ROLE_USER"
  }
  ```

#### Expected Response (`201 Created`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwic3ViIjoiamFuZUBleGFtcGxlLmNvbSIsImlhdCI6MTczMDQzMzYwMCwiZXhwIjoxNzMwNTIwMDAwfQ...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "email": "jane@example.com",
  "role": "ROLE_USER"
}
```

> **Postman Tip**: In Postman's **Tests** tab, add this script to automatically store the token in an environment variable `jwt_token`:
> ```javascript
> var jsonData = pm.response.json();
> pm.environment.set("jwt_token", jsonData.token);
> ```

---

### Step 2: Login (Alternative Token Retrieval)

- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/auth/login`
- **Headers**:
  - `Content-Type: application/json`
- **Body (raw JSON)**:
  ```json
  {
    "email": "jane@example.com",
    "password": "Password123"
  }
  ```

#### Expected Response (`200 OK`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "email": "jane@example.com",
  "role": "ROLE_USER"
}
```

---

### Step 3: Create Product (Passing JWT in Authorization Header)

- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/products`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{jwt_token}}`
- **Body (raw JSON)**:
  ```json
  {
    "name": "MacBook Pro M3",
    "description": "16-inch, 36GB RAM, 1TB SSD",
    "price": 2499.99,
    "stockQuantity": 20
  }
  ```

#### Expected Response (`201 Created`):
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

### Step 4: Create Order (Testing Inter-Service JWT Propagation)

When you send this request:
1. **API Gateway** intercepts `POST /api/v1/orders`, verifies the JWT token, and routes to `order-service`.
2. **`order-service`** receives the request in `OrderService.createOrder()`.
3. To calculate total price, `order-service` calls `ProductClient.getProductById(1)`.
4. The **`jwtFeignRequestInterceptor`** in `FeignClientConfig` extracts the `Authorization: Bearer <token>` from the current `RequestContextHolder` and attaches it to the outgoing Feign request.
5. **`product-service`** receives the authenticated Feign call and returns the product details.
6. `order-service` validates stock, calculates `totalPrice = 2499.99 * 2 = 4999.98`, and saves the order as `CREATED`.

- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/orders`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{jwt_token}}`
- **Body (raw JSON)**:
  ```json
  {
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }
  ```

#### Expected Response (`201 Created`):
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

### Step 5: Test Redis Rate Limiting (10 requests/sec limit)

The API Gateway is configured with a Redis token bucket:
- Replenish rate: **10 requests per second**
- Burst capacity: **20 requests**

#### How to Test:
1. In Postman, right-click your collection or request `GET http://localhost:8080/api/v1/products`.
2. Click **Run collection** / **Runner**.
3. Set **Iterations** to `30` and **Delay** to `0 ms`.
4. Click **Run**.

Alternatively, test from terminal using `curl`:
```bash
for i in {1..25}; do
  curl -s -o /dev/null -w "Request $i: HTTP %{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/v1/products
done
```

#### Expected Output:
- Requests 1 through 20: `HTTP 200`
- Requests 21 through 25: **`HTTP 429 Too Many Requests`**

Verify in Redis:
```bash
docker exec -it redis redis-cli -a redispassword KEYS "request_rate_limiter*"
```

---

### Step 6: Test Circuit Breaker Resilience (Stopping `product-service`)

#### 1. Stop `product-service`:
Stop the `product-service` process (or stop the container):
```bash
# If running locally, stop the terminal process for product-service
# Or stop the container:
docker stop product_db
```

#### 2. Execute the Order Request Again:
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/v1/orders`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{jwt_token}}`
- **Body**:
  ```json
  {
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }
  ```

#### Expected Response (`503 Service Unavailable`):
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

#### Verification:
The service did **not** crash with an unhandled 500 error. The `createOrderFallback` method caught the failure, set the order status to `FAILED`, recorded the diagnostic error reason, and saved it safely in `order_db`.

---

## Ready-to-Import Postman Collection JSON

Save this snippet as `Microservices_E2E_Test.postman_collection.json` and import it into Postman:

```json
{
  "info": {
    "name": "Enterprise Microservices E2E Test Suite",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Register User (Public)",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Jane Doe\",\n  \"email\": \"jane@example.com\",\n  \"password\": \"Password123\"\n}"
        },
        "url": {"raw": "http://localhost:8080/api/v1/auth/register", "protocol": "http", "host": ["localhost"], "port": "8080", "path": ["api", "v1", "auth", "register"]}
      }
    },
    {
      "name": "2. Login User (Public)",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"jane@example.com\",\n  \"password\": \"Password123\"\n}"
        },
        "url": {"raw": "http://localhost:8080/api/v1/auth/login", "protocol": "http", "host": ["localhost"], "port": "8080", "path": ["api", "v1", "auth", "login"]}
      }
    },
    {
      "name": "3. Create Product (Secured via Gateway)",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"},
          {"key": "Authorization", "value": "Bearer {{jwt_token}}"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"MacBook Pro M3\",\n  \"description\": \"16-inch, 36GB RAM, 1TB SSD\",\n  \"price\": 2499.99,\n  \"stockQuantity\": 20\n}"
        },
        "url": {"raw": "http://localhost:8080/api/v1/products", "protocol": "http", "host": ["localhost"], "port": "8080", "path": ["api", "v1", "products"]}
      }
    },
    {
      "name": "4. Create Order (Triggers Feign with Propagated JWT)",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"},
          {"key": "Authorization", "value": "Bearer {{jwt_token}}"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userId\": 1,\n  \"productId\": 1,\n  \"quantity\": 2\n}"
        },
        "url": {"raw": "http://localhost:8080/api/v1/orders", "protocol": "http", "host": ["localhost"], "port": "8080", "path": ["api", "v1", "orders"]}
      }
    }
  ]
}
```

