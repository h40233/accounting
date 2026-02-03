package com.lemoooooon.accounting.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lemoooooon.accounting.dto.CategoryStatsDto;
import com.lemoooooon.accounting.dto.StatsDto;
import com.lemoooooon.accounting.model.Account;
import com.lemoooooon.accounting.model.FacilyJoinRequest;
import com.lemoooooon.accounting.model.Family;
import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.model.Record;
import com.lemoooooon.accounting.repository.AccountRepository;
import com.lemoooooon.accounting.repository.FacilyJoinRequestRepository;
import com.lemoooooon.accounting.repository.FamilyRepository;
import com.lemoooooon.accounting.repository.MemberRepository;
import com.lemoooooon.accounting.repository.RecordRepository;

@Service
public class FamilyService {

    @Autowired
    private FamilyRepository familyRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private FacilyJoinRequestRepository joinRequestRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private com.lemoooooon.accounting.service.RecordService recordService;

    /**
     * 1. 創建家庭
     */
    @Transactional
    public Family createFamily(String googleId, String familyName) {
        Member member = memberRepository.findById(googleId).orElseThrow();

        if (member.getFamily() != null) {
            throw new RuntimeException("你已經加入一個家庭了，無法創建新家庭");
        }

        Family family = new Family();
        family.setName(familyName);
        family.setInviteCode(generateUniqueInviteCode()); // 生成不重複代碼
        family.setHost(member); // 設定家庭擁有者

        familyRepository.save(family);

        // 把自己加進去
        member.setFamily(family);
        member.setShareStats(true); // 創建者預設開啟分享(可選)
        memberRepository.save(member);

        return family;
    }

    /**
     * 2. 申請加入家庭 (輸入代碼)
     * 不會立刻加入，而是產生一筆待審核申請，交由 host 決定。
     */
    @Transactional
    public Family joinFamily(String googleId, String inviteCode) {
        Member member = memberRepository.findById(googleId).orElseThrow();

        if (member.getFamily() != null) {
            throw new RuntimeException("你已經有家庭了，請先退出再加入");
        }

        Family family = familyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("無效的邀請碼"));

        // 如果已對同一個家庭送出待審核申請，就不允許重複送
        boolean existsPending = joinRequestRepository
                .existsByApplicantGoogleIdAndFamilyIdAndStatus(
                        googleId,
                        family.getId(),
                        FacilyJoinRequest.Status.PENDING
                );
        if (existsPending) {
            throw new RuntimeException("你已經送出加入申請，請等待家庭擁有者審核");
        }

        // 建立一筆新的加入申請
        FacilyJoinRequest request = new FacilyJoinRequest();
        request.setFamily(family);
        request.setApplicant(member);
        request.setStatus(FacilyJoinRequest.Status.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        joinRequestRepository.save(request);

        // 回傳家庭基本資訊 (前端可用來顯示「已送出申請給 XXX 家庭」)
        return family;
    }

    /**
     * 3. 更新隱私設定
     */
    public void updatePrivacy(String googleId, boolean shareStats, boolean shareAccounts) {
        Member member = memberRepository.findById(googleId).orElseThrow();
        member.setShareStats(shareStats);
        member.setShareAccounts(shareAccounts);
        memberRepository.save(member);
    }

    /**
     * Host 查看自己家庭的待審核加入申請
     */
    public java.util.List<FacilyJoinRequest> getPendingJoinRequests(String hostGoogleId) {
        return joinRequestRepository.findByFamilyHostGoogleIdAndStatus(
                hostGoogleId,
                FacilyJoinRequest.Status.PENDING
        );
    }

    /**
     * Host 同意或拒絕某一筆加入申請
     */
    @Transactional
    public void reviewJoinRequest(String hostGoogleId, Long requestId, boolean approve) {
        FacilyJoinRequest request = joinRequestRepository
                .findByIdAndFamilyHostGoogleId(requestId, hostGoogleId)
                .orElseThrow(() -> new RuntimeException("找不到此加入申請，或你沒有權限審核"));

        if (request.getStatus() != FacilyJoinRequest.Status.PENDING) {
            throw new RuntimeException("此申請已被處理過");
        }

        request.setDecidedAt(LocalDateTime.now());
        if (!approve) {
            request.setStatus(FacilyJoinRequest.Status.REJECTED);
            joinRequestRepository.save(request);
            return;
        }

        // 同意加入
        Family family = request.getFamily();
        Member applicant = request.getApplicant();

        // 檢查家庭人數上限
        if (family.getMembers() != null && family.getMembers().size() >= 5) {
            throw new RuntimeException("該家庭人數已達上限 (5人)");
        }

        // 申請人若已經有其他家庭，這裡簡單擋掉
        if (applicant.getFamily() != null && !family.equals(applicant.getFamily())) {
            throw new RuntimeException("申請人已經加入其他家庭");
        }

        applicant.setFamily(family);
        memberRepository.save(applicant);

        request.setStatus(FacilyJoinRequest.Status.APPROVED);
        joinRequestRepository.save(request);
    }

    /**
     * 4. 家庭分類統計 (Category Stats)
     * 依照目前登入者所在的家庭，統計整個家庭在指定期間、指定收支類型下，
     * 各分類(category)的加總金額。
     */
    public java.util.List<CategoryStatsDto> getFamilyCategoryStats(
            String googleId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Record.RecordType type
    ) {
        Member me = memberRepository.findById(googleId).orElseThrow();
        if (me.getFamily() == null) {
            throw new RuntimeException("你還沒加入家庭");
        }

        // 日期預設：當月第一天 ~ 今日
        if (startDate == null) {
            startDate = java.time.LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = java.time.LocalDate.now();
        }

        // 類型預設：EXPENSE (支出)
        if (type == null) {
            type = Record.RecordType.EXPENSE;
        }

        return recordRepository.findFamilyCategoryStatsByDateRange(
                me.getFamily().getId(),
                startDate,
                endDate,
                type
        );
    }

    /**
     * 5. 查詢家庭成員清單 (任何家庭成員都可以看)
     */
    public java.util.List<Member> getFamilyMembers(String googleId) {
        Member me = memberRepository.findById(googleId).orElseThrow();
        Family family = me.getFamily();
        if (family == null) {
            throw new RuntimeException("你還沒加入家庭");
        }
        // 直接回傳 family.members，前端只要顯示 nickname 即可
        return family.getMembers();
    }

    private void ensureSameFamily(String viewerGoogleId, String targetGoogleId) {
        Member viewer = memberRepository.findById(viewerGoogleId).orElseThrow();
        Member target = memberRepository.findById(targetGoogleId).orElseThrow();

        if (viewer.getFamily() == null || target.getFamily() == null ||
            !viewer.getFamily().getId().equals(target.getFamily().getId())) {
            throw new RuntimeException("你們不在同一個家庭，無法查看對方資料");
        }
    }

    /**
     * 6. 查看家庭中某位成員的帳戶列表
     */
    public java.util.List<Account> getMemberAccountsInFamily(String viewerGoogleId, String targetGoogleId) {
        ensureSameFamily(viewerGoogleId, targetGoogleId);
        return accountRepository.findByMemberGoogleId(targetGoogleId);
    }

    /**
     * 7. 查看家庭中某位成員的記帳紀錄
     */
    public java.util.List<Record> getMemberRecordsInFamily(String viewerGoogleId, String targetGoogleId) {
        ensureSameFamily(viewerGoogleId, targetGoogleId);
        return recordRepository.findByAccountMemberGoogleId(targetGoogleId);
    }

    /**
     * 8. 查看家庭中某位成員的個人統計 (收入/支出/結餘)
     */
    public StatsDto getMemberStatsInFamily(String viewerGoogleId, String targetGoogleId) {
        ensureSameFamily(viewerGoogleId, targetGoogleId);
        // 直接重用 RecordService 的統計邏輯
        return recordService.getStats(targetGoogleId);
    }

    // --- 輔助方法：生成 6 碼亂數 ---
    private String generateUniqueInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code;
        
        // 迴圈直到生成一個沒人用過的代碼 (通常一次就會過)
        do {
            code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (familyRepository.existsByInviteCode(code.toString()));
        
        return code.toString();
    }
}
