package com.example.inventoryservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {
    private String productCode;
    private String productName;
    private Integer quantity;
}