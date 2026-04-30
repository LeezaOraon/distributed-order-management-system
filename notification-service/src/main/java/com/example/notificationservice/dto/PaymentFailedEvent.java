package com.example.notificationservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentFailedEvent {
    private String eventId;
    private String orderNumber;
    private String reason;
    private LocalDateTime occurredAt;
}