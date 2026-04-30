package com.example.inventoryservice.consumer;

import com.example.inventoryservice.dto.PaymentFailedEvent;
import com.example.inventoryservice.exception.ResourceNotFoundException;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga rollback consumer.
 *
 * When payment fails the choreography saga requires every participant
 * to undo its local state change. inventory-service reserved stock when
 * OrderPlaced arrived — now it releases that reservation.
 *
 * This keeps inventory consistent without a central coordinator.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final InventoryRepository inventoryRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-events}",
            groupId = "inventory-service-payment-group",
            containerFactory = "paymentEventListenerFactory"
    )
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("PaymentFailed received in inventory-service — releasing reservation for order: {}",
                event.getOrderNumber());

        /*
         * We don't store orderNumber → productCode in inventory-service.
         * In a real system you'd either:
         *   a) Store a reservations table here (productCode, orderNumber, qty), or
         *   b) Include productCode + quantity in the PaymentFailedEvent.
         *
         * For this project we enrich PaymentFailedEvent with those fields.
         * See the updated PaymentFailedEvent DTO below.
         */
        if (event.getProductCode() == null) {
            log.warn("PaymentFailedEvent missing productCode — cannot release stock for order: {}",
                    event.getOrderNumber());
            return;
        }

        inventoryRepository.findByProductCode(event.getProductCode())
                .ifPresentOrElse(inventory -> {
                            int toRelease = event.getQuantity() != null ? event.getQuantity() : 0;
                            int newReserved = Math.max(0, inventory.getReservedQuantity() - toRelease);
                            inventory.setReservedQuantity(newReserved);
                            inventoryRepository.save(inventory);
                            log.info("Released {} units of {} — new reserved qty: {}",
                                    toRelease, event.getProductCode(), newReserved);
                        }, () ->
                                log.error("Cannot release stock — product not found: {}", event.getProductCode())
                );
    }
}