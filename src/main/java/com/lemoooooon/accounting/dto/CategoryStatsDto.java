package com.lemoooooon.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryStatsDto {
    private String category;  // 例如：食
    private BigDecimal totalAmount; // 例如：5000
}