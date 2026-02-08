# 青青草原記帳本 (Accounting)

這是一個基於 Spring Boot 開發的家庭記帳應用程式，旨在提供清新、自然的記帳體驗，並支援家庭成員間的資產共享與隱私控管。

## 🚀 技術棧

*   **後端**: Java 17, Spring Boot 3, Spring Data JPA
*   **資料庫**: H2 (開發用) / MySQL (生產用)
*   **前端**: HTML5, CSS3, Vanilla JavaScript (無須編譯)
*   **建置工具**: Maven

## ✨ 主要功能

1.  **個人記帳**:
    *   快速紀錄每日收支。
    *   支援動態分類：系統會自動記住您輸入過的自訂子分類。
    *   多帳戶管理：現金、銀行帳戶、投資帳戶等。
2.  **視覺化報表**:
    *   即時查看總資產、總收入與總支出。
    *   支出分類長條圖。
3.  **家庭共享**:
    *   透過「邀請碼」邀請家人加入。
    *   家長 (Host) 審核機制。
    *   **隱私控管**: 可自由決定是否向家人公開「總資產」或「詳細帳戶」。
    *   家庭全體收支統計。

## 🛠️ 如何執行

1.  **環境需求**:
    *   JDK 17+
    *   Maven 3.6+

2.  **啟動專案**:
    ```bash
    # 使用 Maven Wrapper 啟動
    ./mvnw spring-boot:run
    ```

3.  **開啟應用**:
    *   打開瀏覽器前往: `http://localhost:8080/app.html`
    *   (原 `index.html` 為 API 測試頁面，請使用 `app.html` 獲得完整 UI 體驗)

4.  **測試帳號**:
    *   在登入畫面直接輸入任意 Google ID (如 `user1`) 與暱稱即可進入。

## 📁 專案結構

*   `src/main/java`: 後端原始碼 (Controller, Service, Repository, Model)。
*   `src/main/resources/static`: 前端靜態資源。
    *   `app.html`: 主應用程式頁面。
    *   `css/style.css`: 青青草原風格樣式表。
    *   `js/app.js`: 前端邏輯 (API 串接、UI 互動)。

資料庫使用Neon，後端使用Render部屬