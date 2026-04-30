package com.example.orderservice;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.messaging.OrderEventProducer;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(1L)
                .orderNumber("test-uuid-123")
                .productCode("LAPTOP-001")
                .quantity(2)
                .totalPrice(new BigDecimal("2000.00"))
                .status(Order.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void placeOrder_shouldCreateOrderSuccessfully() {
        OrderRequest request = new OrderRequest("LAPTOP-001", 2, new BigDecimal("1000.00"));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getProductCode()).isEqualTo("LAPTOP-001");
        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void placeOrder_shouldThrowWhenProductCodeBlank() {
        OrderRequest request = new OrderRequest("", 2, new BigDecimal("1000.00"));
        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product code is required");
    }

    @Test
    void placeOrder_shouldThrowWhenQuantityZero() {
        OrderRequest request = new OrderRequest("LAPTOP-001", 0, new BigDecimal("1000.00"));
        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than 0");
    }

    @Test
    void getOrderById_shouldReturnOrderWhenFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        OrderResponse response = orderService.getOrderById(1L);
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_shouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllOrders_shouldReturnList() {
        when(orderRepository.findAll()).thenReturn(List.of(sampleOrder));
        List<OrderResponse> orders = orderService.getAllOrders();
        assertThat(orders).hasSize(1);
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderShipped() {
        sampleOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel");
    }
}