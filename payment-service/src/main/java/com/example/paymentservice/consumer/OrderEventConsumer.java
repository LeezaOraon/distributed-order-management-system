package com.example.paymentservice.consumer;

import com.example.paymentservice.dto.OrderPlacedEvent;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("OrderPlaced received — order={}, amount={}",
                event.getOrderNumber(), event.getTotalPrice());
        paymentService.processPayment(event);
    }
}