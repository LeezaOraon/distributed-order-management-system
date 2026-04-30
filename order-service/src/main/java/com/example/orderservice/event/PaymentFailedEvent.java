package com.example.orderservice.event;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Published by payment-service to "payment-events" topic on charge failure.
 * order-service listens and cancels the order (saga rollback).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {
    private String eventId;
    private String orderNumber;
    private String reason;
    private LocalDateTime occurredAt;
}