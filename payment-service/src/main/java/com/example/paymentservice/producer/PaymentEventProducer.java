package com.example.paymentservice.producer;

import com.example.paymentservice.dto.PaymentFailedEvent;
import com.example.paymentservice.dto.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        kafkaTemplate.send(paymentEventsTopic, event.getOrderNumber(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish PaymentSuccess: {}", ex.getMessage());
                    else log.info("PaymentSuccess published for order: {}", event.getOrderNumber());
                });
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(paymentEventsTopic, event.getOrderNumber(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish PaymentFailed: {}", ex.getMessage());
                    else log.info("PaymentFailed published for order: {}", event.getOrderNumber());
                });
    }
}