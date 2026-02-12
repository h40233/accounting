package com.lemoooooon.accounting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE accounts SET deleted_at = NOW() WHERE id = ?")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 帳戶名稱：現金、郵局...

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO; // 餘額，預設為 0

    @SoftDelete
    private LocalDateTime deletedAt;

    // 關聯回 Member (知道這本存摺是誰的)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnoreProperties({"accounts", "records", "hibernateLazyInitializer", "handler"})
    private Member member;

    // 一個帳戶有多筆交易紀錄
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonIgnore // 避免無限迴圈
    private List<Record> records;
}