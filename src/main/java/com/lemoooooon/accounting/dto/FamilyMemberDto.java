package com.lemoooooon.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMemberDto {
    private String nickname;
    private boolean shareStats;
    private boolean shareAccounts;
    
    // 如果對方願意分享，這裡才會有值；否則為 null
    private BigDecimal totalAssets; 
    private List<AccountDto> accounts; 
}
