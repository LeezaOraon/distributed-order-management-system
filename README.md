# Distributed Order Management System(DOMS)

> A production-grade distributed e-commerce backend built with Spring Boot 3 and Spring Cloud. Five independent microservices communicate through synchronous REST and asynchronous Kafka events, implementing a choreography-based saga pattern for distributed transactions.

---

## Architecture

```
                      Client
                         │
                  ┌──────▼───────┐
                  │  API Gateway  │  :8080
                  │  ─────────── │
                  │  JWT Filter  │
                  │  Rate Limit  │ ← Redis
                  │  Circuit Brk │ ← Resilience4j
                  └──────┬───────┘
                         │
          ┌──────────────┼──────────────┐
          │                             │
 ┌────────▼────────┐         ┌──────────▼────────┐
 │  order-service  │         │  order-service    │
 │     :8081       │         │     :8083         │  ← Load balanced
 └────────┬────────┘         └───────────────────┘
          │ Feign (sync stock check)
 ┌────────▼────────┐
 │inventory-service│  :8082
 └─────────────────┘
════════════════════════════════════════════
Apache Kafka  :9092
════════════════════════════════════════════
order-events          payment-events
│                      │
┌────────▼────────┐    ┌────────▼────────┐
│ payment-service │    │notification-svc │
│     :8083       │    │     :8084       │
└────────┬────────┘    └─────────────────┘
│ PaymentFailed → rollback
▼
order-service (CANCELLED) + inventory-service (release stock)
[Eureka Server :8761 — Service Discovery]
```

---

## How It Works

### Happy Path

1. Client places an order via API Gateway
2. `order-service` checks stock via Feign → reserves inventory
3. Publishes `OrderPlaced` event to Kafka
4. `payment-service` consumes event → processes payment → publishes `PaymentSuccess`
5. `notification-service` sends confirmation email

### Failure Path (Saga Rollback)

1. `payment-service` publishes `PaymentFailed`
2. `order-service` marks order as `CANCELLED`
3. `inventory-service` releases reserved stock

---

## Services

| Service              | Port    | Role                         |
| -------------------- |---------| ---------------------------- |
| api-gateway          | 8080    | Routing, auth, rate limiting |
| eureka-server        | 8761    | Service discovery            |
| order-service        | 8081    | Order management             |
| inventory-service    | 8082    | Stock management             |
| payment-service      | 8083    | Payment processing           |
| notification-service | 8084    | Email notifications          |

---

## Tech Stack

* Spring Boot 3, Spring Cloud
* Kafka (event-driven communication)
* Eureka (service discovery)
* API Gateway (routing + JWT + rate limiting)
* Redis (rate limiting)
* PostgreSQL / H2 (DB)
* Docker + Docker Compose
* Resilience4j (circuit breaker)

---

## Quick Start

### 1. Clone repo

```
git clone https://github.com/LeezaOraon/event-driven-order-management-system
cd event-driven-order-management-system
```

---

### 2. Start infrastructure

```
docker-compose up -d
```

---

### 3. Build project

```
mvn clean install
```

---

### 4. Start services

Start in this order:

```
eureka-server
inventory-service
order-service
payment-service
notification-service
api-gateway
```

---

## API Usage

### Get Token

```
POST /auth/login
```

---

### Place Order

```
POST /orders/api/orders
```

```json
{
  "productCode": "LAPTOP-001",
  "quantity": 2,
  "unitPrice": 999.99,
  "customerEmail": "test@example.com"
}
```

---

## Kafka Topics

| Topic          | Producer        | Consumers                        |
| -------------- | --------------- | -------------------------------- |
| order-events   | order-service   | payment-service                  |
| payment-events | payment-service | order + inventory + notification |

---

## Dashboards

* Eureka → http://localhost:8761
* Kafka UI → http://localhost:8090

---

## Key Features

* Event-driven microservices architecture
* Saga pattern for distributed transactions
* Asynchronous communication using Kafka
* Fault tolerance with circuit breaker
* Load balancing via Eureka
* Stateless JWT authentication

---

## Sample Data

| Product | Code       | Stock |
| ------- | ---------- | ----- |
| Laptop  | LAPTOP-001 | 50    |
| Phone   | PHONE-001  | 100   |

---

## Project Structure

```
orderflow/
├── api-gateway
├── eureka-server
├── order-service
├── inventory-service
├── payment-service
├── notification-service
├── docker-compose.yml
```
