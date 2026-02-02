package com.lemoooooon.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data // Lombok 幫你生成 Getter/Setter
@AllArgsConstructor // 幫你生成全參數建構子
@NoArgsConstructor
public class StatsDto {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
}