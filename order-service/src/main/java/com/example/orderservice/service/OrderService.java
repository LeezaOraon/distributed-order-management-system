package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.messaging.OrderEventProducer;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.orderservice.event.OrderPlacedEvent;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderEventProducer orderEventProducer;

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        validateRequest(request);

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .productCode(request.getProductCode())
                .quantity(request.getQuantity())
                .totalPrice(request.getUnitPrice().multiply(
                        java.math.BigDecimal.valueOf(request.getQuantity())))
                .status(Order.OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        log.info("Order placed: {} for product {}", saved.getOrderNumber(), saved.getProductCode());

        orderEventProducer.publishOrderPlaced(
                OrderPlacedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderNumber(saved.getOrderNumber())
                        .productCode(saved.getProductCode())
                        .quantity(saved.getQuantity())
                        .totalPrice(saved.getTotalPrice())
                        .customerEmail("test@email.com")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(status);

        log.info("Order {} status updated to {}", order.getOrderNumber(), status);

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order {} cancelled", order.getOrderNumber());
    }

    private void validateRequest(OrderRequest request) {
        if (request.getProductCode() == null || request.getProductCode().isBlank()) {
            throw new IllegalArgumentException("Product code is required");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (request.getUnitPrice() == null || request.getUnitPrice().signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than 0");
        }
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .productCode(order.getProductCode())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}