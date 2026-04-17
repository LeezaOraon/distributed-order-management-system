package com.example.orderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    private String productCode;
    private Integer quantity;
    private BigDecimal unitPrice;
}
