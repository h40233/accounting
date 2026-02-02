package com.lemoooooon.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.repository.MemberRepository;

@Service // 告訴 Spring Boot 這是一位廚師
public class MemberService {

    // 注入 MemberRepository (呼叫倉庫管理員)
    @Autowired
    private MemberRepository memberRepository;

    /**
     * 處理登入邏輯：
     * 1. 如果會員不存在 -> 註冊新會員
     * 2. 如果會員存在 -> 更新最後登入時間 (非記帳時間)
     */
    public Member login(String googleId, String googleName, String email) {
        // 嘗試去倉庫找人
        Optional<Member> existingMember = memberRepository.findById(googleId);

        if (existingMember.isPresent()) {
            // 找到了！是老客戶
            Member member = existingMember.get();
            return member;
        } else {
            // 找不到！是新客戶，幫他註冊
            Member newMember = new Member();
            newMember.setGoogleId(googleId);
            // ✨ 關鍵點：預設使用 Google 的名字 ✨
            // 如果 googleName 是空的 (例如某些隱私設定)，就給個 "新用戶"
            newMember.setNickname(googleName != null && !googleName.isEmpty() ? googleName : "新用戶");
            newMember.setEmail(email);
            newMember.setCreatedAt(LocalDateTime.now());

            // ✨✨✨ 關鍵修改：幫新用戶建立預設帳戶 ✨✨✨
            // A. 建立一個新帳戶物件
            Account defaultAccount = new Account();
            defaultAccount.setName("現金"); // 預設叫 "現金"
            defaultAccount.setBalance(BigDecimal.ZERO); // 預設 0 元 (或你想送他 100 元也可以)
            defaultAccount.setMember(newMember); // 設定這個帳戶屬於這個新會員

            // B. 把帳戶放進會員的口袋 (List)
            // 因為是剛 new 出來的 Member，accounts 可能是 null，所以要先 new 一個清單
            if (newMember.getAccounts() == null) {
                newMember.setAccounts(new ArrayList<>());
            }
            newMember.getAccounts().add(defaultAccount);

            // 3. 存檔
            // 因為我們在 Member.java 有設定 CascadeType.ALL
            // 所以存 Member 的時候，Hibernate 會順便幫我們把 Account 也存進去！
            return memberRepository.save(newMember);
        }
    }
    
    // 透過 ID 找人的輔助方法
    public Member getMember(String googleId) {
        return memberRepository.findById(googleId)
                .orElseThrow(() -> new RuntimeException("找不到使用者 ID: " + googleId));
    }
}
