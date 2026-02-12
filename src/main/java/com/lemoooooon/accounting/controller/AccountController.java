package com.lemoooooon.accounting.controller;

import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.repository.AccountRepository;
import com.lemoooooon.accounting.repository.MemberRepository;
import com.lemoooooon.accounting.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AccountService accountService;

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

    // 刪除帳戶
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(
            @PathVariable Long id,
            @RequestParam String googleId,
            @RequestParam(defaultValue = "false") boolean force) {
        try {
            accountService.deleteAccount(id, googleId, force);
            // 成功刪除，回傳 204 No Content
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            if ("HAS_RECORDS".equals(e.getMessage())) {
                // 回傳 409 Conflict，並附帶特定錯誤訊息，讓前端可以識別
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "HAS_RECORDS", "message", "此帳戶尚有關聯紀錄，請確認是否強制刪除"));
            }
            // 其他類型的 IllegalStateException
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // 其他像是找不到帳戶之類的錯誤
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}