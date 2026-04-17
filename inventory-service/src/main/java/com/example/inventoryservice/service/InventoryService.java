package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.*;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryResponse addInventory(InventoryRequest request) {
        if (inventoryRepository.existsByProductCode(request.getProductCode())) {
            throw new RuntimeException("Product already exists: " + request.getProductCode());
        }
        Inventory inventory = Inventory.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .reservedQuantity(0)
                .build();
        return mapToResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public StockCheckResponse checkStock(String productCode, Integer requestedQuantity) {
        Inventory inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));
        int available = inventory.getAvailableQuantity();
        return StockCheckResponse.builder()
                .productCode(productCode)
                .available(available >= requestedQuantity)
                .availableQuantity(available)
                .requestedQuantity(requestedQuantity)
                .build();
    }

    @Transactional
    public InventoryResponse updateStock(String productCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        log.info("Updated stock for {} by {}", productCode, quantity);
        return mapToResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductCode(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));
    }

    private InventoryResponse mapToResponse(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId())
                .productCode(inv.getProductCode())
                .productName(inv.getProductName())
                .quantity(inv.getQuantity())
                .reservedQuantity(inv.getReservedQuantity())
                .availableQuantity(inv.getAvailableQuantity())
                .inStock(inv.getAvailableQuantity() > 0)
                .build();
    }
}
