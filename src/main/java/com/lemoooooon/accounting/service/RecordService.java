package com.lemoooooon.accounting.service;

import com.lemoooooon.accounting.dto.StatsDto;
import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.model.Record;
import com.lemoooooon.accounting.repository.AccountRepository;
import com.lemoooooon.accounting.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private AccountRepository accountRepository; // 新增：我們需要找帳戶

    /**
     * 新增記帳 (同時更新帳戶餘額)
     * 注意：現在需要傳入 accountId
     */
    @Transactional
    public Record createRecord(String googleId, Long accountId, Record record) {
        // 1. 找出帳戶
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("找不到此帳戶"));

        // 2. 安全檢查：確認這個帳戶是屬於這個人的
        if (!account.getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("你沒有權限使用此帳戶");
        }

        // 3. 設定關聯
        record.setAccount(account);
        
        // 4. ✨ 自動計算餘額 ✨
        if (record.getType() == Record.RecordType.INCOME) {
            // 收入：帳戶錢變多
            account.setBalance(account.getBalance().add(record.getAmount()));
        } else {
            // 支出：帳戶錢變少
            account.setBalance(account.getBalance().subtract(record.getAmount()));
        }
        
        // 5. 儲存帳戶更新 (因為有 @Transactional，這行其實可以省，但寫著比較保險)
        accountRepository.save(account);

        // 6. 儲存紀錄
        return recordRepository.save(record);
    }

    public List<Record> getRecordsByMember(String googleId) {
        // 使用修復後的 Repository 方法
        return recordRepository.findByAccountMemberGoogleId(googleId);
    }

    @Transactional
    public void deleteRecord(Long recordId, String googleId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("找不到此紀錄"));

        if (!record.getAccount().getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("無權刪除");
        }
        
        // ✨ 刪除時，要把餘額「還原」 ✨
        Account account = record.getAccount();
        if (record.getType() == Record.RecordType.INCOME) {
            // 原本是收入，刪掉後餘額要扣掉
            account.setBalance(account.getBalance().subtract(record.getAmount()));
        } else {
            // 原本是支出，刪掉後餘額要加回來
            account.setBalance(account.getBalance().add(record.getAmount()));
        }
        accountRepository.save(account);

        recordRepository.delete(record);
    }

    /**
     * 修改記帳紀錄 (包含餘額連動修正)
     * 支援：改金額、改分類、改帳戶、改收支類型
     */
    @Transactional // 務必要加！保證全部步驟一起成功或一起失敗
    public Record updateRecord(Long id, String googleId, Record newRecordDto) {
        // 1. 找出舊的紀錄 (資料庫裡的現狀)
        Record oldRecord = recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到此紀錄"));

        // 2. 檢查權限
        if (!oldRecord.getAccount().getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("無權修改");
        }

        // ==========================================
        // Step 1: 【還原】舊紀錄的影響 (Undo)
        // ==========================================
        Account oldAccount = oldRecord.getAccount();
        if (oldRecord.getType() == Record.RecordType.INCOME) {
            // 舊的是收入，現在要撤銷 -> 把錢扣回去
            oldAccount.setBalance(oldAccount.getBalance().subtract(oldRecord.getAmount()));
        } else {
            // 舊的是支出，現在要撤銷 -> 把錢加回來
            oldAccount.setBalance(oldAccount.getBalance().add(oldRecord.getAmount()));
        }
        accountRepository.save(oldAccount); // 暫存舊帳戶狀態

        // ==========================================
        // Step 2: 【更新】資料欄位
        // ==========================================
        oldRecord.setAmount(newRecordDto.getAmount());
        oldRecord.setCategory(newRecordDto.getCategory());
        oldRecord.setSubCategory(newRecordDto.getSubCategory());
        oldRecord.setDate(newRecordDto.getDate());
        oldRecord.setNote(newRecordDto.getNote());
        oldRecord.setType(newRecordDto.getType());

        // 檢查是否要【換帳戶】?
        // 如果前端傳來的資料裡有帶 account 且 ID 不一樣，代表要換帳戶
        if (newRecordDto.getAccount() != null && 
            newRecordDto.getAccount().getId() != null &&
            !newRecordDto.getAccount().getId().equals(oldAccount.getId())) {
            
            // 找出新帳戶
            Account newAccount = accountRepository.findById(newRecordDto.getAccount().getId())
                    .orElseThrow(() -> new RuntimeException("找不到新指定的帳戶"));
            
            // 權限檢查 (確認新帳戶也是這個人的)
            if (!newAccount.getMember().getGoogleId().equals(googleId)) {
                throw new RuntimeException("無權使用該帳戶");
            }
            
            // 設定換成新帳戶
            oldRecord.setAccount(newAccount);
        }

        // ==========================================
        // Step 3: 【套用】新紀錄的影響 (Redo)
        // ==========================================
        // 注意：這裡要用 oldRecord.getAccount()，因為上面可能已經換過帳戶了
        Account currentAccount = oldRecord.getAccount();
        
        if (oldRecord.getType() == Record.RecordType.INCOME) {
            // 新的是收入 -> 加錢
            currentAccount.setBalance(currentAccount.getBalance().add(oldRecord.getAmount()));
        } else {
            // 新的是支出 -> 扣錢
            currentAccount.setBalance(currentAccount.getBalance().subtract(oldRecord.getAmount()));
        }
        accountRepository.save(currentAccount);

        // 4. 存檔並回傳
        return recordRepository.save(oldRecord);
    }
    
    public StatsDto getStats(String googleId) {
        BigDecimal totalIncome = recordRepository.findTotalIncome(googleId);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        BigDecimal totalExpense = recordRepository.findTotalExpense(googleId);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpense);
        return new StatsDto(totalIncome, totalExpense, balance);
    }
}