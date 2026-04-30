package com.example.orderservice.messaging;

import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    /**
     * Saga rollback: when payment fails, cancel the order.
     * The inventory reservation release is handled by inventory-service
     * listening to the same PaymentFailed event.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.payment-events}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("PaymentFailed received — rolling back order: {}, reason: {}",
                event.getOrderNumber(), event.getReason());

        orderRepository.findByOrderNumber(event.getOrderNumber())
                .ifPresentOrElse(order -> {
                            order.setStatus(Order.OrderStatus.CANCELLED);
                            orderRepository.save(order);
                            log.info("Order {} cancelled due to payment failure", event.getOrderNumber());
                        }, () ->
                                log.error("Cannot rollback — order not found: {}", event.getOrderNumber())
                );
    }
}