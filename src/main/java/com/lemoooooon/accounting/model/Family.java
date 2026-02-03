package com.lemoooooon.accounting.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "families")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 家庭名稱 (例如: 彭家記帳)

    @Column(unique = true, nullable = false)
    private String inviteCode; // 邀請碼 (唯一索引，加速查詢)

    /**
     * 家庭擁有者 / 建立者 (host)
     * 之後審核加入申請時會用到這個欄位。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Member host;

    // 一個家庭有多個成員
    @OneToMany(mappedBy = "family")
    @JsonManagedReference // 防止 JSON 無限迴圈
    private List<Member> members;
}
