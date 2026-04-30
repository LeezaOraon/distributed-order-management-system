package com.example.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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