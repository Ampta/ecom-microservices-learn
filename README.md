# Microservices Infrastructure Setup (Phase 1)

This repository contains the localized Docker Compose configuration for enterprise microservices infrastructure.

## Services & Architecture

| Container Name | Technology | Host Port | Container Port | Database Name | Persistent Volume | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `user_db` | MySQL 8.0 | `3309` | `3306` | `user_db` | `user_db_data` | User microservice persistence (remapped from 3306) |
| `product_db` | MySQL 8.0 | `3307` | `3306` | `product_db` | `product_db_data` | Product & inventory persistence |
| `order_db` | MySQL 8.0 | `3308` | `3306` | `order_db` | `order_db_data` | Order & transaction persistence |
| `redis` | Redis 7 Alpine | `6379` | `6379` | N/A | `redis_data` | API Gateway rate limiting & session cache |

All containers communicate over the dedicated bridge network `microservices-net`.

---

## Connecting via MySQL Workbench

### Connection Details Table

| Field | User DB | Product DB | Order DB |
| :--- | :--- | :--- | :--- |
| **Connection Method** | Standard (TCP/IP) | Standard (TCP/IP) | Standard (TCP/IP) |
| **Hostname** | `127.0.0.1` | `127.0.0.1` | `127.0.0.1` |
| **Port** | `3309` | `3307` | `3308` |
| **Username** | `appuser` (or `root`) | `appuser` (or `root`) | `appuser` (or `root`) |
| **Password** | `apppassword` (or `rootpassword`) | `apppassword` (or `rootpassword`) | `apppassword` (or `rootpassword`) |
| **Default Schema** | `user_db` | `product_db` | `order_db` |

### URI / Connection Strings

- **User DB:** `mysql://appuser:apppassword@127.0.0.1:3309/user_db`
- **Product DB:** `mysql://appuser:apppassword@127.0.0.1:3307/product_db`
- **Order DB:** `mysql://appuser:apppassword@127.0.0.1:3308/order_db`

---

## Quick Start

### 1. Start All Containers

```bash
docker compose up -d
```

### 2. Verify Container Health & Status

```bash
docker compose ps
```

---

## Verification Commands

### 1. Verify MySQL Databases

#### User Database (`3309`):
```bash
docker exec -it user_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" user_db
```

#### Product Database (`3307`):
```bash
docker exec -it product_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" product_db
```

#### Order Database (`3308`):
```bash
docker exec -it order_db mysql -u appuser -papppassword -e "SELECT @@version, DATABASE();" order_db
```

### 2. Verify Redis Container (`6379`)

```bash
docker exec -it redis redis-cli -a redispassword ping
```
