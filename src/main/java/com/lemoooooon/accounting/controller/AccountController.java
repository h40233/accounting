package com.lemoooooon.accounting.controller;

import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.repository.AccountRepository;
import com.lemoooooon.accounting.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private MemberRepository memberRepository;

    // 建立新帳戶 (例如: 現金, 銀行)
    // POST /api/accounts?googleId=user123
    // Body: { "name": "現金", "balance": 0 }
    @PostMapping
    public Account createAccount(@RequestParam String googleId, @RequestBody Account account) {
        Member member = memberRepository.findById(googleId)
                .orElseThrow(() -> new RuntimeException("找無此人"));
        
        account.setMember(member);
        // 如果沒傳餘額，預設為 0
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        return accountRepository.save(account);
    }

    // 查詢某人的所有帳戶
    @GetMapping
    public List<Account> getAccounts(@RequestParam String googleId) {
        return accountRepository.findByMemberGoogleId(googleId);
    }
}