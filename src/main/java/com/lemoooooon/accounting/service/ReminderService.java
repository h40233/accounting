package com.lemoooooon.accounting.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.repository.MemberRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReminderService {

    @Autowired
    private MemberRepository memberRepository;

    /**
     * 每分鐘檢查一次是否需要發送提醒
     */
    @Scheduled(cron = "0 * * * * ?") // 每一分鐘的第 0 秒執行
    @Transactional(readOnly = true)
    public void checkAndSendReminders() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();

        List<Member> members = memberRepository.findAll();
        
        for (Member member : members) {
            if (member.isEnableReminder() && member.getReminderTime() != null) {
                // 檢查時間是否匹配 (只比對時與分)
                if (member.getReminderTime().withSecond(0).withNano(0).equals(now)) {
                    // 檢查今天是否已記帳
                    if (member.getLastRecordDate() == null || !member.getLastRecordDate().equals(today)) {
                        sendReminder(member);
                    }
                }
            }
        }
    }

    private void sendReminder(Member member) {
        // 由於沒有實際的 Email 或 App 推播服務，這裡模擬推播 log
        // 在實際專案中，這裡會呼叫 EmailService 或 FCM (Firebase Cloud Messaging)
        log.info("🔔 [每日提醒] 親愛的 {} ({})，今天還沒記帳喔！養成好習慣，現在就去記一筆吧！", 
                member.getNickname(), member.getEmail());
        
        System.out.println(">>> 🔔 推播發送給: " + member.getNickname() + " <" + member.getEmail() + ">");
    }
}
