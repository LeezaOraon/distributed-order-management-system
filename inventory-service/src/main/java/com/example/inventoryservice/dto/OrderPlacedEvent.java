package com.example.inventoryservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Consumed by inventory-service from the order-events topic.
 * When received, we confirm/lock the reservation that was optimistically
 * made via the Feign call during order placement (Week 1 approach).
 * In a pure event-driven design, reserveStock() moves here entirely.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderPlacedEvent {
    private String eventId;
    private String orderNumber;
    private String productCode;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String customerEmail;
    private LocalDateTime occurredAt;
}