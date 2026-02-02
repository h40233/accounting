package com.lemoooooon.accounting.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lemoooooon.accounting.dto.CategoryStatsDto;
import com.lemoooooon.accounting.model.Record;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {

    List<Record> findByAccountMemberGoogleId(String googleId);
    
    // 也可以加一個：查詢某段時間內的紀錄
    // 自動生成 SQL: SELECT * FROM records WHERE member_id = ? AND date BETWEEN ? AND ?
    List<Record> findByAccountMemberGoogleIdAndDateBetween(String googleId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    // 1. 計算總收入 (找出某人所有 type = INCOME 的金額總和)
    // @Query 裡面寫的是 JPQL (類似 SQL，但對象是 Java 類別)
    @Query("SELECT SUM(r.amount) FROM Record r WHERE r.account.member.googleId = :googleId AND r.type = 'INCOME'")
    BigDecimal findTotalIncome(String googleId);

    @Query("SELECT SUM(r.amount) FROM Record r WHERE r.account.member.googleId = :googleId AND r.type = 'EXPENSE'")
    BigDecimal findTotalExpense(String googleId);

    // 查詢某個使用者的「各分類總支出」
    // SQL 邏輯：根據 category 分組，然後把 amount 加總
    @Query("SELECT new CategoryStatsDto(r.category, SUM(r.amount)) " +
           "FROM Record r " +
           "WHERE r.account.member.googleId = :googleId " +
           "AND r.type = 'EXPENSE' " +
           "GROUP BY r.category")
    List<CategoryStatsDto> findCategoryStats(String googleId);
}
