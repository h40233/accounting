package com.lemoooooon.accounting.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.lemoooooon.accounting.model.Member;
import com.lemoooooon.accounting.service.GoogleAuthService;
import com.lemoooooon.accounting.service.MemberService;

import java.util.Map;

@RestController // 1. 告訴 Spring Boot 這是一個 RESTful API 入口
@RequestMapping("/api/members") // 2. 設定此控制器的「根網址」
public class MemberController {

    @Value("${google.client.id}")
    private String googleClientId;

    @Autowired
    private MemberService memberService; 

    @Autowired
    private GoogleAuthService googleAuthService; 

    /**
     * 更新提醒設定
     */
    @PutMapping("/settings/reminder")
    public void updateReminderSettings(
            @RequestParam String googleId,
            @RequestParam String time, // Format: HH:mm
            @RequestParam boolean enable) {
        memberService.updateReminderSettings(googleId, time, enable);
    }

    // 因為前端傳來的 JSON 長得像 {"googleId": "...", "nickname": "..."}
    public static class LoginRequest {
        public String googleId;
        public String nickname;
    }

    /**
     * 取得 Google Client ID (避免寫死在前端)
     */
    @GetMapping("/google-client-id")
    public Map<String, String> getGoogleClientId() {
        return Map.of("clientId", googleClientId);
    }

    /**
     * 開發測試用登入
     * 網址: POST /api/members/login?googleId=user123
     */
    @PostMapping("/login")
    public Member login(
            @RequestParam String googleId,
            // required = false 代表這個參數可傳可不傳
            @RequestParam(required = false) String nickname 
    ) {
        // 如果沒傳 nickname，就給一個預設值 "測試用戶"
        String nameToUse = (nickname != null) ? nickname : "測試用戶";
        
        // email 暫時給空字串或是假資料
        return memberService.login(googleId, nameToUse, "test@gmail.com");
    }

    /**
     * 正式 Google 登入
     * 前端只要傳: { "token": "eyJhbGciOi..." }
     */
    @PostMapping("/google-login")
    public Member googleLogin(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        // 1. 驗證 Token 並取得資料
        GoogleIdToken.Payload payload = googleAuthService.verifyToken(token);
        
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name"); // ✨ 這裡就是 Google 上的名字 (例如: 彭鎬偉)
        
        // 2. 呼叫 Service，把 Google 名字傳進去當預設值
        return memberService.login(googleId, name, email);
    }
}