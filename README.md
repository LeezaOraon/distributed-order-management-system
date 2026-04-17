# Microservices Architecture Project

A production-grade microservices system built with Spring Boot 3 & Spring Cloud.

---

## Architecture

```
Client
  │
  ▼
API Gateway (Spring Cloud Gateway)
  │   ├── JWT Filter
  │   ├── Rate Limiter (Redis)
  │   └── Circuit Breaker (Resilience4j)
  │
  ├──▶ Order Service       :8081  (2 instances, load balanced)
  ├──▶ Inventory Service   :8082
  ├──▶ Payment Service     :8083
  └──▶ Notification Service:8084
         │
    [Eureka Server :8761 — Service Discovery]
         │
      [Kafka :9092 — Event Bus]
```

---

## ️ Tech Stack

| Layer        | Technology                        |
|--------------|----------------------------------|
| Framework    | Spring Boot 3, Spring Cloud      |
| Gateway      | Spring Cloud Gateway             |
| Discovery    | Netflix Eureka                   |
| Messaging    | Apache Kafka                     |
| Load Balance | Spring Cloud LoadBalancer        |
| Resilience   | Resilience4j                     |
| Auth         | Spring Security + JWT            |
| Database     | PostgreSQL                       |
| Cache        | Redis                            |
| Monitoring   | Prometheus + Grafana             |
| Testing      | JUnit, Testcontainers            |
| DevOps       | Docker, GitHub Actions           |

---

## Quick Start

### 1. Start infrastructure
```bash
docker-compose up -d
```

### 2. Verify containers
```bash
docker-compose ps
```

### 3. Build services
```bash
mvn clean package -DskipTests
```

### 4. Run order-service
```bash
cd order-service
mvn spring-boot:run
```

### 5. Run inventory-service (new terminal)
```bash
cd inventory-service
mvn spring-boot:run
```

---

## Modules

| Module            | Port | Description                 |
|------------------|------|-----------------------------|
| order-service     | 8081 | Place and manage orders     |
| inventory-service | 8082 | Manage product stock        |

---

## API Endpoints (Week 1)

###  Order Service
- `POST   /api/orders`
- `GET    /api/orders`
- `GET    /api/orders/{id}`
- `PATCH  /api/orders/{id}/status`

###  Inventory Service
- `POST   /api/inventory`
- `GET    /api/inventory`
- `GET    /api/inventory/{productCode}`
- `GET    /api/inventory/check/{code}?quantity=N`
- `PATCH  /api/inventory/{code}/stock?quantity=N`

---

## Progress

- [x] Week 1: Project setup, Docker Compose, multi-module Maven
- [ ] Week 1: Core microservices
- [ ] Week 1: Eureka + Feign Client
- [ ] Week 2: API Gateway + Load Balancer
- [ ] Week 3: Event-Driven with Kafka
- [ ] Week 4: Monitoring, Testing, Deployment

---

## Notes

- Project is under active development 