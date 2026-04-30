package com.example.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSuccessEvent {
    private String eventId;
    private String orderNumber;
    private BigDecimal amount;
    private String customerEmail;
    private LocalDateTime occurredAt;
}