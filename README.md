# Distributed Order Management System (DOMS)

> Event-driven microservices built with Spring Boot 3, Kafka, the choreography saga pattern,
> and Spring Cloud.

---

## Architecture

```
                              Client
                                 │
                                 ▼
              ┌──────────────────────────────────────┐
              │        api-gateway  :8080            │
              │  • JwtAuthFilter      (per-route)    │
              │  • RequestRateLimiter (Redis, 5 rps) │
              │  • CircuitBreaker     (Resilience4j) │
              │  • lb:// resolution   (via Eureka)   │
              └───────┬──────────────────────┬───────┘
                      │ /api/orders/**       │ /api/inventory/**
                      ▼                      ▼
          ┌────────────────────┐   ┌─────────────────────┐
          │  order-service     │   │  inventory-service  │
          │  :8081             │   │  :8082              │
          └─────┬────────▲─────┘   └──────────▲──────────┘
                │        │                    │
       produces │        │ consumes           │ consumes
    OrderPlaced │        │ PaymentFailed      │ PaymentFailed
                ▼        │                    │
     ╔═══════════════════════════════════════════════════════╗
     ║  Apache Kafka   localhost:9092  ·  kafka:29092         ║
     ║  topics:  order-events  ·  payment-events              ║
     ╚═══════▲═══════════════════════════╤═══════════════════╝
             │ produces Payment*         │ consumes Payment*
             │                           ▼
      ┌──────┴────────────┐   ┌──────────────────────┐
      │  payment-service  │   │ notification-service │
      │  :8083            │   │ :8084                │
      └───────────────────┘   └──────────────────────┘

      [ eureka-server :8761 — service registry (HTTP Basic) ]
      [ redis :6379 · postgres :5432 · kafka-ui :8090 ]
```

---

## How It Works

### Happy path

1. Client calls `POST /api/orders` through the gateway (JWT validated, rate limit checked).
2. `order-service` validates, saves the order as `PENDING`, and returns `201` immediately.
3. It publishes `OrderPlaced` to `order-events`, keyed by `orderNumber`.
4. `payment-service` consumes it, checks idempotency, records the payment, and publishes
   `PaymentSuccess` to `payment-events`.
5. `notification-service` consumes it and sends a confirmation email.

### Failure path (saga rollback)

1. `payment-service` declines the charge and publishes `PaymentFailed`
   (enriched with `productCode` and `quantity`).
2. `order-service` sets the order to `CANCELLED`.
3. `inventory-service` releases the reservation.
4. `notification-service` logs the failure.

Steps 2–4 run **in parallel**, in three separate consumer groups, off the same topic.

Payment failure is simulated deterministically: any order whose `totalPrice` exceeds
`app.payment.failure-threshold` (default `9999.00`) is declined.

---

## Services

| Service              | Port | Role                                   | REST API |
| -------------------- | ---- | -------------------------------------- | -------- |
| api-gateway          | 8080 | Routing, JWT, rate limiting, fallbacks | yes      |
| eureka-server        | 8761 | Service discovery                      | UI only  |
| order-service        | 8081 | Order lifecycle                        | yes      |
| inventory-service    | 8082 | Stock and reservations                 | yes      |
| payment-service      | 8083 | Payment processing (mock)              | no — Kafka only |
| notification-service | 8084 | Email notifications                    | no — Kafka only |

---

## Tech Stack

* Java 17, Spring Boot 3.2.0, Spring Cloud 2023.0.1
* Apache Kafka — event-driven communication
* Netflix Eureka — service discovery
* Spring Cloud Gateway — edge routing (reactive / WebFlux)
* Redis — distributed rate limiting
* Resilience4j — circuit breaking
* H2 (in-memory) — per-service database
* Docker Compose — infrastructure
* JJWT — token issuing and validation
* Spring Mail — SMTP via Mailtrap

---

## Kafka Topics

| Topic            | Producer        | Consumers                                          |
| ---------------- | --------------- | -------------------------------------------------- |
| `order-events`   | order-service   | payment-service                                     |
| `payment-events` | payment-service | order-service · inventory-service · notification-service |

Events are keyed by `orderNumber`, so all events for one order land on the same partition and
stay ordered.

**Consumer groups:** `order-service-group`, `payment-service-group`,
`inventory-service-payment-group`, `notification-service-group`.

---

## Quick Start

### 1. Clone

```bash
git clone https://github.com/LeezaOraon/distributed-order-management-system.git
cd distributed-order-management-system
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

Brings up PostgreSQL, Redis, ZooKeeper, Kafka and Kafka UI. Wait for the health checks:

```bash
docker-compose ps
```

### 3. Build

```bash
mvn clean install
```

### 4. Start the services

Start in this order — Eureka first, gateway last:

```bash
mvn -pl eureka-server        spring-boot:run
mvn -pl inventory-service    spring-boot:run
mvn -pl order-service        spring-boot:run
mvn -pl payment-service      spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl api-gateway          spring-boot:run
```

Each in its own terminal. Confirm registration at http://localhost:8761 (`admin` / `secret`).

### Optional — second order-service instance (load balancing demo)

```bash
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=instance2
```

> **Note:** `application-instance2.yml` binds port **8083**, which collides with
> `payment-service`. Change it to `8085` before running both.

---

## API Reference

All examples go through the gateway on `:8080`. Services can also be called directly on their
own ports, which bypasses JWT and rate limiting — useful for debugging.

> The gateway rate-limits to **5 requests/second** per IP. Rapid loops will return `429`.

### Authentication

`/auth/**` is the only route without the JWT filter — you can't require a token to get a token.

**Get a token**

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123","role":"USER"}'
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "alice",
  "role": "USER",
  "type": "Bearer"
}
```

**Save it to a variable** (used by every example below)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123","role":"USER"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo $TOKEN
```

**Validate a token**

```bash
curl -s http://localhost:8080/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

---

### Orders

**Place an order** — succeeds, triggers the happy-path saga

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "productCode": "LAPTOP-001",
        "quantity": 2,
        "unitPrice": 999.99
      }'
```

```json
{
  "id": 1,
  "orderNumber": "3f2b8c1a-...",
  "productCode": "LAPTOP-001",
  "quantity": 2,
  "totalPrice": 1999.98,
  "status": "PENDING",
  "createdAt": "2026-09-13T10:15:30.123"
}
```

**Place an order that fails payment** — triggers the rollback saga
(`totalPrice` = 19999.80, above the 9999.00 threshold)

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "productCode": "LAPTOP-001",
        "quantity": 20,
        "unitPrice": 999.99
      }'
```

Returns `201 PENDING`, then watch the `payment-service` and `order-service` logs — the order
moves to `CANCELLED` a moment later.

**List all orders**

```bash
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

**Get an order by id**

```bash
curl -s http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Get an order by order number**

```bash
curl -s http://localhost:8080/api/orders/number/3f2b8c1a-... \
  -H "Authorization: Bearer $TOKEN"
```

**Update status** — `PENDING` · `CONFIRMED` · `SHIPPED` · `DELIVERED` · `CANCELLED`

```bash
curl -s -X PATCH "http://localhost:8080/api/orders/1/status?status=CONFIRMED" \
  -H "Authorization: Bearer $TOKEN"
```

**Cancel an order** — `204 No Content`; rejected with `400` if already `SHIPPED`/`DELIVERED`

```bash
curl -s -X DELETE http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $TOKEN" -i
```

---

### Inventory

**List all stock**

```bash
curl -s http://localhost:8080/api/inventory \
  -H "Authorization: Bearer $TOKEN"
```

**Get one product**

```bash
curl -s http://localhost:8080/api/inventory/LAPTOP-001 \
  -H "Authorization: Bearer $TOKEN"
```

```json
{
  "id": 1,
  "productCode": "LAPTOP-001",
  "productName": "ThinkPad X1 Carbon",
  "quantity": 50,
  "reservedQuantity": 0,
  "availableQuantity": 50,
  "inStock": true
}
```

**Check availability**

```bash
curl -s "http://localhost:8080/api/inventory/check/LAPTOP-001?quantity=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Add a new product** — `201 Created`

```bash
curl -s -X POST http://localhost:8080/api/inventory \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "productCode": "MOUSE-001",
        "productName": "MX Master 3S",
        "quantity": 200
      }'
```

**Adjust stock** — the value is a delta; negative reduces

```bash
curl -s -X PATCH "http://localhost:8080/api/inventory/LAPTOP-001/stock?quantity=25" \
  -H "Authorization: Bearer $TOKEN"
```

**Reserve stock** — `409 Conflict` if insufficient

```bash
curl -s -X POST "http://localhost:8080/api/inventory/LAPTOP-001/reserve?quantity=5" \
  -H "Authorization: Bearer $TOKEN"
```

**Release a reservation**

```bash
curl -s -X POST "http://localhost:8080/api/inventory/LAPTOP-001/release?quantity=5" \
  -H "Authorization: Bearer $TOKEN"
```

---

### Error responses

**No token** → `401`

```bash
curl -s http://localhost:8080/api/orders -i
```

```json
{"error":"Missing or malformed Authorization header","status":401}
```

**Validation failure** → `400`

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productCode":"","quantity":1,"unitPrice":100}'
```

**Not found** → `404`

```bash
curl -s http://localhost:8080/api/orders/9999 \
  -H "Authorization: Bearer $TOKEN"
```

**Insufficient stock** → `409`

```bash
curl -s -X POST "http://localhost:8080/api/inventory/TABLET-001/reserve?quantity=1" \
  -H "Authorization: Bearer $TOKEN" -i
```

**Rate limit exceeded** → `429`

```bash
for i in $(seq 1 20); do
  curl -s -o /dev/null -w "%{http_code} " http://localhost:8080/api/orders \
    -H "Authorization: Bearer $TOKEN"
done; echo
```

**Circuit breaker open** → `503` — stop `order-service`, then:

```bash
for i in $(seq 1 12); do
  curl -s -o /dev/null http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"
done

curl -s http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"
```

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Order service is temporarily unavailable. Please try again in a moment.",
  "service": "order-service"
}
```

---

### Health checks

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health
curl -s http://localhost:8761/actuator/health
```

---

## Calling Services Directly

Bypasses the gateway — no JWT, no rate limiting.

```bash
# order-service
curl -s http://localhost:8081/api/orders
curl -s -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productCode":"PHONE-001","quantity":1,"unitPrice":799.00}'

# inventory-service
curl -s http://localhost:8082/api/inventory
curl -s "http://localhost:8082/api/inventory/check/PHONE-001?quantity=3"
```

The `smoke-test.sh` script exercises both services directly:

```bash
chmod +x smoke-test.sh && ./smoke-test.sh
```

---

## Seed Data

`inventory-service` seeds three products on first start (only when the table is empty):

| Product            | Code       | Quantity | Reserved | Available |
| ------------------ | ---------- | -------- | -------- | --------- |
| ThinkPad X1 Carbon | LAPTOP-001 | 50       | 0        | 50        |
| Pixel 8 Pro        | PHONE-001  | 120      | 5        | 115       |
| iPad Air           | TABLET-001 | 0        | 0        | 0 (for testing out-of-stock) |

---

## Dashboards

| Tool        | URL                     | Credentials     |
| ----------- | ----------------------- | --------------- |
| Eureka      | http://localhost:8761   | `admin` / `secret` |
| Kafka UI    | http://localhost:8090   | none            |
| H2 console  | http://localhost:8081/h2-console | JDBC `jdbc:h2:mem:testdb`, user `sa`, no password |

---

## Configuration

Notable settings, all in each service's `application.yml`:

| Setting | Value | Service |
| --- | --- | --- |
| `app.payment.failure-threshold` | `9999.00` | payment-service |
| `app.jwt.expiration-ms` | `86400000` (24h) | api-gateway |
| `redis-rate-limiter.replenishRate` | `5` req/s | api-gateway |
| `redis-rate-limiter.burstCapacity` | `10` | api-gateway |
| `failureRateThreshold` | `50` % over a 10-call window | api-gateway |
| `waitDurationInOpenState` | `10s` | api-gateway |
| Producer | `acks=all`, `retries=3`, `enable.idempotence=true` | order, payment |
| Consumer | `auto-offset-reset=earliest`, `enable.auto.commit=false` | all consumers |

---

## Key Features

* Event-driven microservices with Kafka
* Choreography saga for distributed transactions, with compensating actions
* Idempotent payment processing (`existsByOrderNumber` + unique constraint)
* Producer durability — `acks=all` with idempotence
* Per-key ordering via `orderNumber` as the partition key
* Circuit breaking with structured fallback responses
* Redis-backed distributed rate limiting
* Client-side load balancing through Eureka (`lb://`)
* Stateless JWT authentication at the edge

---

## Project Structure

```
distributed-order-management-system/
├── api-gateway/           # Spring Cloud Gateway — JWT, rate limit, circuit breaker
├── eureka-server/         # Service registry
├── order-service/         # Orders — produces OrderPlaced, consumes PaymentFailed
├── inventory-service/     # Stock — consumes PaymentFailed
├── payment-service/       # Payments — consumes OrderPlaced, produces PaymentSuccess/Failed
├── notification-service/  # Email — consumes payment-events
├── docker-compose.yml     # Kafka, ZooKeeper, Redis, Postgres, Kafka UI
├── init-db.sql            # Postgres schema provisioning (not yet used)
├── smoke-test.sh          # Direct-to-service smoke tests
└── pom.xml                # Parent POM
```
