package com.example.paymentservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {
    private String eventId;
    private String orderNumber;

    private String productCode;
    private Integer quantity;

    private String reason;
    private LocalDateTime occurredAt;
}