package com.lemoooooon.accounting.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lemoooooon.accounting.dto.CategoryStatsDto;
import com.lemoooooon.accounting.dto.FamilyDetailDto;
import com.lemoooooon.accounting.dto.FamilyMemberDto;
import com.lemoooooon.accounting.dto.StatsDto;
import com.lemoooooon.accounting.model.FacilyJoinRequest;
import com.lemoooooon.accounting.model.Family;
import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.model.Record;
import com.lemoooooon.accounting.repository.MemberRepository;
import com.lemoooooon.accounting.repository.RecordRepository;
import com.lemoooooon.accounting.service.FamilyService;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    @Autowired private FamilyService familyService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RecordRepository recordRepository;

    // 創建家庭
    @PostMapping("/create")
    public Family create(@RequestParam String googleId, @RequestParam String name) {
        return familyService.createFamily(googleId, name);
    }

    // 加入家庭
    @PostMapping("/join")
    public Family join(@RequestParam String googleId, @RequestParam String code) {
        return familyService.joinFamily(googleId, code);
    }

    // 更新隱私設定
    @PutMapping("/settings")
    public String updateSettings(@RequestParam String googleId, 
                                 @RequestParam boolean shareStats, 
                                 @RequestParam boolean shareAccounts) {
        familyService.updatePrivacy(googleId, shareStats, shareAccounts);
        return "設定已更新";
    }

    // ✨ 查詢家庭總覽 (包含成員列表、依據隱私設定顯示的資產)
    @GetMapping("/overview")
    public List<FamilyMemberDto> getFamilyOverview(@RequestParam String googleId) {
        Member me = memberRepository.findById(googleId).orElseThrow();
        if (me.getFamily() == null) throw new RuntimeException("你還沒加入家庭");

        List<Member> members = me.getFamily().getMembers();
        List<FamilyMemberDto> dtos = new ArrayList<>();

        for (Member m : members) {
            FamilyMemberDto dto = new FamilyMemberDto();
            dto.setNickname(m.getNickname());
            dto.setShareStats(m.isShareStats());
            dto.setShareAccounts(m.isShareAccounts());

            // 隱私邏輯判斷
            if (m.isShareStats()) {
                // 這裡你要去計算該成員的總資產 (邏輯略，可呼叫既有的 Service)
                // dto.setTotalAssets(...);
            }

            if (m.isShareAccounts()) {
                // 這裡轉換 Account 為 AccountDto (避免循環參照)
                // dto.setAccounts(...);
            }
            
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * 查詢當前家庭的詳細資訊 (包含家長和邀請碼)
     */
    @GetMapping("/details")
    public FamilyDetailDto getFamilyDetails(@RequestParam String googleId) {
        return familyService.getFamilyDetails(googleId);
    }

    // 簡單版：只列出家庭成員 (任何家庭成員都可呼叫)
    @GetMapping("/members")
    public List<Member> getFamilyMembers(@RequestParam String googleId) {
        return familyService.getFamilyMembers(googleId);
    }

    // ✨ 查詢「全家人」的流水帳 (Record 是純公開的)
    @GetMapping("/records")
    public List<Record> getFamilyRecords(@RequestParam String googleId) {
        Member me = memberRepository.findById(googleId).orElseThrow();
        if (me.getFamily() == null) throw new RuntimeException("你還沒加入家庭");

        return recordRepository.findByFamilyId(me.getFamily().getId());
    }

    // ✨ 家庭分類統計總覽
    // 範例：
    // 查家庭支出分類：GET /api/family/stats/category?googleId=...&type=EXPENSE
    // 查家庭收入分類：GET /api/family/stats/category?googleId=...&type=INCOME
    // 可選 startDate / endDate 篩選期間
    @GetMapping("/stats/category")
    public List<CategoryStatsDto> getFamilyCategoryStats(
            @RequestParam String googleId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Record.RecordType type
    ) {
        return familyService.getFamilyCategoryStats(googleId, startDate, endDate, type);
    }

    // 查看家庭中某位成員的帳戶列表
    @GetMapping("/member/accounts")
    public List<com.lemoooooon.accounting.model.Account> getMemberAccountsInFamily(
            @RequestParam String googleId,
            @RequestParam String targetGoogleId
    ) {
        return familyService.getMemberAccountsInFamily(googleId, targetGoogleId);
    }

    // 查看家庭中某位成員的記帳紀錄
    @GetMapping("/member/records")
    public List<Record> getMemberRecordsInFamily(
            @RequestParam String googleId,
            @RequestParam String targetGoogleId
    ) {
        return familyService.getMemberRecordsInFamily(googleId, targetGoogleId);
    }

    // 查看家庭中某位成員的個人統計
    @GetMapping("/member/stats")
    public StatsDto getMemberStatsInFamily(
            @RequestParam String googleId,
            @RequestParam String targetGoogleId
    ) {
        return familyService.getMemberStatsInFamily(googleId, targetGoogleId);
    }

    // Host 查看自己家庭的待審核清單
    @GetMapping("/join-requests")
    public List<FacilyJoinRequest> getPendingJoinRequests(@RequestParam String hostGoogleId) {
        return familyService.getPendingJoinRequests(hostGoogleId);
    }

    // Host 審核加入申請
    @PostMapping("/join/review")
    public String reviewJoinRequest(
            @RequestParam String hostGoogleId,
            @RequestParam Long requestId,
            @RequestParam boolean approve
    ) {
        familyService.reviewJoinRequest(hostGoogleId, requestId, approve);
        return approve ? "已同意加入申請" : "已拒絕加入申請";
    }
}