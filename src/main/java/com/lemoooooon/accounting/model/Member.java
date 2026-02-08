package com.lemoooooon.accounting.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Data // ✨ 魔法 1：包辦 Getter/Setter
@NoArgsConstructor // ✨ 魔法 2：無參建構子
@AllArgsConstructor // ✨ 魔法 3：全參建構子
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Member {

    @Id
    @Column(name = "google_id")
    private String googleId; // 使用 String 存 Google ID

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    // 上次記帳日期 (用於每日提醒)
    private LocalDate lastRecordDate;
    
    // 每日提醒時間 (預設晚上 20:00)
    private LocalTime reminderTime = LocalTime.of(20, 0);
    
    // 是否開啟提醒
    private boolean enableReminder = true;

    // 新增這段：一個 Member 有多個 Account
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("member") // 避免迴圈
    private List<Account> accounts;

    @ManyToOne
    @JoinColumn(name = "family_id")
    @JsonBackReference // 防止 JSON 無限迴圈
    private Family family;
    // 隱私設定：是否對家人公開統計數據 (預設 false)
    private boolean shareStats = false;
    // 隱私設定：是否對家人公開帳戶列表 (預設 false)
    private boolean shareAccounts = false;
}