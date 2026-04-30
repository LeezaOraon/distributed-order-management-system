package com.example.paymentservice.service;

import com.example.paymentservice.dto.*;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.producer.PaymentEventProducer;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    @Value("${app.payment.failure-threshold}")
    private BigDecimal failureThreshold;

    /**
     * Processes a mock payment for an order.
     * Fails if amount > failureThreshold — lets you test the failure saga
     * by sending an order with a very high totalPrice.
     */
    @Transactional
    public void processPayment(OrderPlacedEvent order) {
        // Idempotency check — don't process twice
        if (paymentRepository.existsByOrderNumber(order.getOrderNumber())) {
            log.warn("Payment already processed for order: {}", order.getOrderNumber());
            return;
        }

        log.info("Processing payment for order={}, amount={}",
                order.getOrderNumber(), order.getTotalPrice());

        boolean shouldFail = order.getTotalPrice().compareTo(failureThreshold) > 0;

        Payment payment = Payment.builder()
                .orderNumber(order.getOrderNumber())
                .amount(order.getTotalPrice())
                .status(shouldFail ? Payment.PaymentStatus.FAILED : Payment.PaymentStatus.SUCCESS)
                .failureReason(shouldFail ? "Amount exceeds threshold (simulated decline)" : null)
                .build();

        paymentRepository.save(payment);

        if (shouldFail) {
            log.warn("Payment FAILED for order: {} — amount {} exceeds threshold {}",
                    order.getOrderNumber(), order.getTotalPrice(), failureThreshold);

            eventProducer.publishPaymentFailed(PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderNumber(order.getOrderNumber())
                    .productCode(order.getProductCode())
                    .quantity(order.getQuantity())
                    .reason("Simulated decline: amount exceeds threshold")
                    .occurredAt(LocalDateTime.now())
                    .build());
        } else {
            log.info("Payment SUCCESS for order: {}", order.getOrderNumber());

            eventProducer.publishPaymentSuccess(PaymentSuccessEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderNumber(order.getOrderNumber())
                    .amount(order.getTotalPrice())
                    .customerEmail(order.getCustomerEmail())
                    .occurredAt(LocalDateTime.now())
                    .build());
        }
    }
}