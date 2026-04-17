package com.example.inventoryservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCheckResponse {
    private String productCode;
    private boolean available;
    private Integer availableQuantity;
    private Integer requestedQuantity;
}
