package com.example.orderservice.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published to Kafka topic "order-events" whenever a new order is placed.
 * All downstream services (payment, inventory, notification) consume this.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent {
    private String eventId;          // UUID — idempotency key for consumers
    private String orderNumber;
    private String productCode;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String customerEmail;    // notification-service will use this
    private LocalDateTime occurredAt;
}