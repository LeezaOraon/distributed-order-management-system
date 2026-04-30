package com.example.inventoryservice.dto;

import lombok.*;
import java.time.LocalDateTime;

/** Published by payment-service on charge failure. inventory-service releases the reservation. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentFailedEvent {
    private String eventId;
    private String orderNumber;
    private String productCode;
    private Integer quantity;
    private String reason;
    private LocalDateTime occurredAt;
}