package com.example.orderservice.repository;

import com.example.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>{
    Optional<Order> findByOrderNumber(String orderNumber);
}