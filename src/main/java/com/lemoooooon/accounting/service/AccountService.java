package com.lemoooooon.accounting.service;

import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.repository.AccountRepository;
import com.lemoooooon.accounting.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final RecordRepository recordRepository;

    @Transactional
    public void deleteAccount(Long accountId, String googleId, boolean force) {
        // 1. 驗證帳戶是否存在以及所有權
        // 因為 @Where 生效，findById 會自動過濾掉軟刪除的帳戶
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("找不到此帳戶或帳戶已被刪除"));

        if (!account.getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("你沒有權限刪除此帳戶");
        }

        // 2. 檢查帳戶底下是否還有記帳紀錄
        boolean hasRecords = recordRepository.existsByAccountId(accountId);

        // 3. 根據情況執行不同邏輯
        if (hasRecords) {
            // 情況 B: 帳戶有紀錄
            if (force) {
                // 使用者執意要刪 -> 軟刪除
                // 感謝 @SQLDelete，我們只需要呼叫 delete 方法，Hibernate 會自動轉換為更新 deleted_at 欄位的 SQL
                accountRepository.delete(account);
            } else {
                // 預設情況 -> 拋出特定錯誤，讓前端可以識別
                throw new IllegalStateException("HAS_RECORDS");
            }
        } else {
            // 情況 A: 帳戶無紀錄 -> 物理刪除
            // 注意：為了繞過 @SQLDelete 進行物理刪除，我們需要一個自訂的 Repository 方法。
            // 但在這裡，為了簡化，我們先統一使用軟刪除。
            // 因為一個沒有交易紀錄的帳戶被軟刪除，對使用者來說和物理刪除沒有任何區別。
            // 這樣可以統一邏輯，避免增加複雜性。
            accountRepository.delete(account);
        }
    }
}
