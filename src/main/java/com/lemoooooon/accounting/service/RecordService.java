package com.lemoooooon.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lemoooooon.accounting.dto.CategoryStatsDto;
import com.lemoooooon.accounting.dto.StatsDto;
import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.model.Record; // ✨ Added
import com.lemoooooon.accounting.repository.AccountRepository;
import com.lemoooooon.accounting.repository.RecordRepository;

@Service
public class RecordService {
    // ... (前段代碼保持不變) ...

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private com.lemoooooon.accounting.repository.MemberRepository memberRepository;

    // ... (中間的方法如 createRecord, deleteRecord 等保持不變，略過以節省 tokens) ...

    @Transactional
    public Record createRecord(String googleId, Long accountId, Record record) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("找不到此帳戶"));
        if (!account.getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("你沒有權限使用此帳戶");
        }
        record.setAccount(account);
        if (record.getType() == Record.RecordType.INCOME) {
            account.setBalance(account.getBalance().add(record.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(record.getAmount()));
        }
        accountRepository.save(account);
        
        // ✨ 更新 Member 的最後記帳日期
        if (record.getDate().equals(LocalDate.now())) {
            com.lemoooooon.accounting.model.Member member = account.getMember();
            member.setLastRecordDate(LocalDate.now());
            memberRepository.save(member);
        }
        
        return recordRepository.save(record);
    }

    public List<Record> getRecordsByMember(String googleId) {
        return recordRepository.findByAccountMemberGoogleId(googleId);
    }

    @Transactional
    public void deleteRecord(Long recordId, String googleId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("找不到此紀錄"));

        if (!record.getAccount().getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("無權刪除");
        }
        Account account = record.getAccount();
        if (record.getType() == Record.RecordType.INCOME) {
            account.setBalance(account.getBalance().subtract(record.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(record.getAmount()));
        }
        accountRepository.save(account);
        recordRepository.delete(record);
    }

    @Transactional
    public Record updateRecord(Long id, String googleId, Record newRecordDto) {
        Record oldRecord = recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到此紀錄"));
        if (!oldRecord.getAccount().getMember().getGoogleId().equals(googleId)) {
            throw new RuntimeException("無權修改");
        }
        Account oldAccount = oldRecord.getAccount();
        if (oldRecord.getType() == Record.RecordType.INCOME) {
            oldAccount.setBalance(oldAccount.getBalance().subtract(oldRecord.getAmount()));
        } else {
            oldAccount.setBalance(oldAccount.getBalance().add(oldRecord.getAmount()));
        }
        accountRepository.save(oldAccount);

        oldRecord.setAmount(newRecordDto.getAmount());
        oldRecord.setCategory(newRecordDto.getCategory());
        oldRecord.setSubCategory(newRecordDto.getSubCategory());
        oldRecord.setDate(newRecordDto.getDate());
        oldRecord.setNote(newRecordDto.getNote());
        oldRecord.setType(newRecordDto.getType());

        if (newRecordDto.getAccount() != null && 
            newRecordDto.getAccount().getId() != null &&
            !newRecordDto.getAccount().getId().equals(oldAccount.getId())) {
            
            Account newAccount = accountRepository.findById(newRecordDto.getAccount().getId())
                    .orElseThrow(() -> new RuntimeException("找不到新指定的帳戶"));
            if (!newAccount.getMember().getGoogleId().equals(googleId)) {
                throw new RuntimeException("無權使用該帳戶");
            }
            oldRecord.setAccount(newAccount);
        }

        Account currentAccount = oldRecord.getAccount();
        if (oldRecord.getType() == Record.RecordType.INCOME) {
            currentAccount.setBalance(currentAccount.getBalance().add(oldRecord.getAmount()));
        } else {
            currentAccount.setBalance(currentAccount.getBalance().subtract(oldRecord.getAmount()));
        }
        accountRepository.save(currentAccount);
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

    public List<CategoryStatsDto> getCategoryStats(String googleId, LocalDate startDate, LocalDate endDate, Record.RecordType type) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();
        if (type == null) type = Record.RecordType.EXPENSE;
        return recordRepository.findCategoryStatsByDateRange(googleId, startDate, endDate, type);
    }

    /**
     * 取得分類選單 (預設 + 使用者歷史紀錄)
     * 使用 LinkedHashMap 確保順序
     */
    public Map<String, Map<String, Set<String>>> getCategoryOptions(String googleId) {
        // 1. 初始化預設結構 (使用 LinkedHashMap)
        Map<String, Map<String, Set<String>>> options = new LinkedHashMap<>();
        options.put("EXPENSE", new LinkedHashMap<>());
        options.put("INCOME", new LinkedHashMap<>());

        // --- 預設支出 (依照指定順序) ---
        addDefault(options, "EXPENSE", "食", "早餐", "午餐", "晚餐", "飲料", "零食", "消夜");
        addDefault(options, "EXPENSE", "衣", "衣服", "褲子", "鞋子", "配件", "衛生紙", "洗髮精", "日常用品");
        addDefault(options, "EXPENSE", "住", "房租", "水費", "電費", "瓦斯費", "網路費", "維修");
        addDefault(options, "EXPENSE", "行", "捷運", "公車", "加油", "停車費", "計程車", "保養");
        addDefault(options, "EXPENSE", "育", "書籍", "課程", "文具", "補習");
        addDefault(options, "EXPENSE", "樂", "電影", "遊戲", "旅遊", "聚餐", "訂閱服務");
        addDefault(options, "EXPENSE", "金融", "轉帳手續費", "利息支出", "保險");
        addDefault(options, "EXPENSE", "醫療", "掛號費", "藥品", "住院", "健檢"); // ✨ 新增主分類，位於金融與其他之間
        addDefault(options, "EXPENSE", "其他", "雜支", "捐款"); // ✨ 移除這裡的「醫療」

        // --- 預設收入 ---
        addDefault(options, "INCOME", "工作", "薪水", "獎金", "加班費");
        addDefault(options, "INCOME", "副業", "外包", "兼職", "網拍");
        addDefault(options, "INCOME", "金融投資", "股息", "價差獲利", "銀行利息");
        addDefault(options, "INCOME", "其他", "中獎", "紅包", "退稅");

        // 2. 讀取使用者歷史紀錄
        List<Record> history = getRecordsByMember(googleId);
        for (Record r : history) {
            String type = r.getType().name(); 
            String cat = r.getCategory();
            String sub = r.getSubCategory();

            if (cat != null && !cat.isEmpty()) {
                // computeIfAbsent 若 key 已存在 (預設值)，不會改變其順序；若不存在 (自訂值)，則會加在最後面
                options.get(type).computeIfAbsent(cat, k -> new HashSet<>());
                
                if (sub != null && !sub.isEmpty()) {
                    options.get(type).get(cat).add(sub);
                }
            }
        }
        
        return options;
    }

    private void addDefault(Map<String, Map<String, Set<String>>> options, String type, String category, String... subs) {
        // 這裡依然可以用 HashSet 存小分類，因為小分類順序通常沒那麼嚴格，且 Set 可自動去重
        // 如果連小分類都要排序，這裡也要改成 LinkedHashSet
        Set<String> subSet = options.get(type).computeIfAbsent(category, k -> new java.util.LinkedHashSet<>()); 
        subSet.addAll(Arrays.asList(subs));
    }
}