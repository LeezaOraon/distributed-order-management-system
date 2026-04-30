package com.example.notificationservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by payment-service → consumed here to send order confirmation email.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSuccessEvent {
    private String eventId;
    private String orderNumber;
    private BigDecimal amount;
    private String customerEmail;
    private LocalDateTime occurredAt;
}