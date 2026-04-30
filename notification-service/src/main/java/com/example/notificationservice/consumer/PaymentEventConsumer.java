package com.example.notificationservice.consumer;

import com.example.notificationservice.dto.PaymentFailedEvent;
import com.example.notificationservice.dto.PaymentSuccessEvent;
import com.example.notificationservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the payment-events topic.
 *
 * Both PaymentSuccess and PaymentFailed land on the same topic.
 * We receive raw bytes, inspect the eventId/type, and route to
 * the right handler. This is a common pattern when multiple event
 * types share a topic — alternative is separate topics per type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-events}",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(ConsumerRecord<String, String> record) {
        log.debug("Received payment event for key={}", record.key());

        try {
            // Peek at the raw JSON to determine event type
            var node = objectMapper.readTree(record.value());

            if (node.has("amount") && node.has("customerEmail")) {
                // Has amount + customerEmail → PaymentSuccess
                var event = objectMapper.treeToValue(node, PaymentSuccessEvent.class);
                log.info("PaymentSuccess received — order={}, email={}",
                        event.getOrderNumber(), event.getCustomerEmail());
                emailService.sendOrderConfirmation(
                        event.getCustomerEmail(),
                        event.getOrderNumber(),
                        event.getAmount()
                );

            } else if (node.has("reason")) {
                // Has reason → PaymentFailed
                var event = objectMapper.treeToValue(node, PaymentFailedEvent.class);
                log.warn("PaymentFailed received — order={}, reason={}",
                        event.getOrderNumber(), event.getReason());
                emailService.sendPaymentFailedNotification(
                        event.getOrderNumber(),
                        event.getReason()
                );

            } else {
                log.warn("Unknown payment event shape, skipping: {}", record.value());
            }

        } catch (Exception e) {
            log.error("Error processing payment event key={}: {}", record.key(), e.getMessage(), e);
            // Don't re-throw — let Kafka commit the offset so we don't get stuck in a retry loop.
            // In production: send to a dead-letter topic instead.
        }
    }
}