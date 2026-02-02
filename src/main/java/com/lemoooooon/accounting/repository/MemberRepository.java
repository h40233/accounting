package com.lemoooooon.accounting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lemoooooon.accounting.model.Member;

// <Member, String> 的意思是：
// 1. 這個倉庫管理的是 Member 表格
// 2. 這個表格的主鍵 (PK) 是 String 型態 (因為你的 googleId 是 String)
@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
    // 這裡裡面目前什麼都不用寫！
    // 繼承 JpaRepository 後，你已經自動擁有 save(), findById(), delete() 等功能了
}