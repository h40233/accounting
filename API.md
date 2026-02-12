# Accounting API Documentation

> **Base URL**: `/api`
> **Format**: JSON
> **Date Format**: `YYYY-MM-DD`

---

## 1. Member (會員)

### 1.1 測試登入 (Dev Login)
快速建立或登入測試帳號。

*   **URL**: `POST /members/login`
*   **Query Params**:
    *   `googleId`: `user123`
    *   `nickname`: `測試小草`
*   **Request Sample**: (None)
*   **Response Sample**:
    ```json
    {
      "googleId": "user123",
      "email": "test@gmail.com",
      "nickname": "測試小草",
      "createdAt": "2026-02-04T10:00:00",
      "lastRecordAt": null,
      "shareStats": false,
      "shareAccounts": false
    }
    ```

### 1.2 Google 正式登入
*   **URL**: `POST /members/google-login`
*   **Request Sample**:
    ```json
    {
      "token": "eyJhbGciOiJSUzI1NiIs..."
    }
    ```
*   **Response Sample**:
    ```json
    {
      "googleId": "10987654321",
      "email": "realuser@gmail.com",
      "nickname": "Real User",
      "avatarUrl": "https://lh3.googleusercontent.com/a/ACg8ocJ...=s96-c",
      "createdAt": "2026-02-04T12:00:00",
      "shareStats": false,
      "shareAccounts": false
    }
    ```

### 1.3 更新暱稱
*   **URL**: `PUT /members/nickname`
*   **Query Params**:
    *   `googleId`: `user123`
*   **Request Sample**:
    ```json
    {
      "nickname": "新的名字"
    }
    ```
*   **Response Sample**: (回傳更新後的 Member 物件)
    ```json
    {
      "googleId": "user123",
      "email": "test@gmail.com",
      "nickname": "新的名字",
      "avatarUrl": null,
      "createdAt": "2026-02-04T10:00:00",
      "lastRecordAt": null,
      "shareStats": false,
      "shareAccounts": false
    }
    ```

### 1.4 更新頭像
使用者需先將圖片上傳至圖床 (如 Cloudinary)，再將取得的圖片 URL 傳給此 API。
*   **URL**: `PUT /members/avatar`
*   **Query Params**:
    *   `googleId`: `user123`
*   **Request Sample**:
    ```json
    {
      "avatarUrl": "https://my-image-host.com/new-avatar.jpg"
    }
    ```
*   **Response Sample**: (回傳更新後的 Member 物件)
    ```json
    {
      "googleId": "user123",
      "email": "test@gmail.com",
      "nickname": "新的名字",
      "avatarUrl": "https://my-image-host.com/new-avatar.jpg",
      "createdAt": "2026-02-04T10:00:00",
      "lastRecordAt": null,
      "shareStats": false,
      "shareAccounts": false
    }
    ```

---

## 2. Accounts (帳戶)

### 2.1 建立帳戶
*   **URL**: `POST /accounts`
*   **Query Params**: `googleId=user123`
*   **Request Sample**:
    ```json
    {
      "name": "玉山銀行",
      "balance": 50000
    }
    ```
*   **Response Sample**:
    ```json
    {
      "id": 1,
      "name": "玉山銀行",
      "balance": 50000
    }
    ```

### 2.2 查詢我的帳戶列表
*   **URL**: `GET /accounts`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```json
    [
      {
        "id": 1,
        "name": "玉山銀行",
        "balance": 50000
      },
      {
        "id": 2,
        "name": "錢包",
        "balance": 1200
      }
    ]
    ```

### 2.3 刪除帳戶
實作混合刪除策略：
1.  若帳戶下無任何記帳紀錄，則直接刪除。
2.  若帳戶下尚有關聯紀錄，則會回傳 `409 Conflict` 錯誤，前端需提示使用者。
3.  若使用者執意要刪除，需在請求中加上 `force=true` 參數，後端會對此帳戶進行「軟刪除」。

*   **URL**: `DELETE /accounts/{id}`
*   **Query Params**:
    *   `googleId`: `user123` (必須)
    *   `force`: `true` (可選, 預設 `false`)
*   **Success Response (204 No Content)**:
    ```
    (No content, just HTTP status 204)
    ```
*   **Conflict Response (409 Conflict)**:
    當帳戶尚有關聯紀錄且未使用 `force=true` 時回傳。
    ```json
    {
      "error": "HAS_RECORDS",
      "message": "此帳戶尚有關聯紀錄，請確認是否強制刪除"
    }
    ```

---

## 3. Records (記帳)

### 3.1 新增記帳
*   **URL**: `POST /records`
*   **Query Params**:
    *   `googleId`: `user123`
    *   `accountId`: `1` (扣款/入帳的帳戶ID)
*   **Request Sample**:
    ```json
    {
      "type": "EXPENSE",
      "category": "食",
      "subCategory": "早餐",
      "amount": 75,
      "date": "2026-02-04",
      "note": "蛋餅加紅茶"
    }
    ```
*   **Response Sample**:
    ```json
    {
      "id": 101,
      "type": "EXPENSE",
      "category": "食",
      "subCategory": "早餐",
      "amount": 75.00,
      "date": "2026-02-04",
      "note": "蛋餅加紅茶",
      "account": { "id": 1, "name": "玉山銀行" }
    }
    ```

### 3.2 查詢我的記帳紀錄
*   **URL**: `GET /records`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```json
    [
      {
        "id": 101,
        "type": "EXPENSE",
        "category": "食",
        "subCategory": "早餐",
        "amount": 75.00,
        "date": "2026-02-04",
        "account": { "id": 1, "name": "玉山銀行" }
      },
      {
        "id": 100,
        "type": "INCOME",
        "category": "工作",
        "subCategory": "薪水",
        "amount": 50000.00,
        "date": "2026-02-01",
        "account": { "id": 1, "name": "玉山銀行" }
      }
    ]
    ```

### 3.3 刪除記帳
*   **URL**: `DELETE /records/{id}`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```plaintext
    刪除成功！
    ```

### 3.4 修改記帳
*   **URL**: `PUT /records/{id}`
*   **Query Params**: `googleId=user123`
*   **Request Sample**: (同 3.1 新增記帳)
*   **Response Sample**: (同 3.1 回傳更新後的物件)

### 3.5 取得個人總收支統計
*   **URL**: `GET /records/stats`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```json
    {
      "totalIncome": 50000.00,
      "totalExpense": 75.00,
      "balance": 49925.00
    }
    ```

### 3.6 取得個人分類統計 (圓餅圖資料)
*   **URL**: `GET /records/stats/category`
*   **Query Params**:
    *   `googleId`: `user123`
    *   `type`: `EXPENSE` (or `INCOME`)
    *   `startDate`: `2026-02-01` (Optional)
    *   `endDate`: `2026-02-28` (Optional)
*   **Response Sample**:
    ```json
    [
      { "category": "食", "totalAmount": 4500.00 },
      { "category": "行", "totalAmount": 1200.00 },
      { "category": "住", "totalAmount": 15000.00 }
    ]
    ```

### 3.7 取得分類選單 (動態結構)
回傳預設分類加上使用者歷史自訂的子分類。
*   **URL**: `GET /records/categories`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```json
    {
      "EXPENSE": {
        "食": ["早餐", "午餐", "晚餐", "飲料", "自訂消夜"],
        "衣": ["衣服", "褲子", "鞋子"],
        "住": ["房租", "水費", "電費"],
        "行": ["捷運", "公車", "加油"],
        "醫療": ["掛號費", "藥品"],
        "其他": ["雜支"]
      },
      "INCOME": {
        "工作": ["薪水", "獎金"],
        "金融投資": ["股息"]
      }
    }
    ```

---

## 4. Family (家庭)

### 4.1 建立家庭
*   **URL**: `POST /family/create`
*   **Query Params**:
    *   `googleId`: `user123`
    *   `name`: `幸福一家`
*   **Response Sample**:
    ```json
    {
      "id": 1,
      "name": "幸福一家",
      "inviteCode": "A1B2C3",
      "host": { "googleId": "user123", "nickname": "測試小草" }
    }
    ```

### 4.2 加入家庭
*   **URL**: `POST /family/join`
*   **Query Params**:
    *   `googleId`: `user456`
    *   `code`: `A1B2C3`
*   **Response Sample**:
    ```json
    {
      "id": 1,
      "name": "幸福一家",
      "inviteCode": "A1B2C3"
    }
    ```

### 4.3 查詢家庭資訊
任何家庭成員都可以呼叫，用來查詢家庭的家長 (Host) 與邀請碼。
*   **URL**: `GET /family/details`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```json
    {
      "id": 1,
      "name": "幸福一家",
      "inviteCode": "A1B2C3",
      "host": {
        "nickname": "家長的名字",
        "avatarUrl": "https://.../host_avatar.jpg"
      }
    }
    ```

### 4.4 家庭總覽 (成員與資產)
根據成員的隱私設定，`totalAssets` 和 `accounts` 可能為 null。
*   **URL**: `GET /family/overview`
*   **Query Params**: `googleId=user123`
*   **Response Sample**:
    ```json
    [
      {
        "nickname": "爸爸",
        "shareStats": true,
        "shareAccounts": false,
        "totalAssets": 150000.00,
        "accounts": null
      },
      {
        "nickname": "媽媽",
        "shareStats": true,
        "shareAccounts": true,
        "totalAssets": 200000.00,
        "accounts": [
            { "id": 5, "name": "私房錢", "balance": 200000.00 }
        ]
      },
      {
        "nickname": "小明",
        "shareStats": false,
        "shareAccounts": false,
        "totalAssets": null,
        "accounts": null
      }
    ]
    ```

### 4.5 更新個人隱私設定
*   **URL**: `PUT /family/settings`
*   **Query Params**:
    *   `googleId`: `user123`
    *   `shareStats`: `true`
    *   `shareAccounts`: `false`
*   **Response Sample**:
    ```plaintext
    設定已更新
    ```

### 4.6 查詢加入申請 (Host Only)
*   **URL**: `GET /family/join-requests`
*   **Query Params**: `hostGoogleId=user123`
*   **Response Sample**:
    ```json
    [
      {
        "id": 10,
        "applicant": { "nickname": "想加入的陌生人" },
        "status": "PENDING",
        "createdAt": "2026-02-04T15:30:00"
      }
    ]
    ```

### 4.7 審核加入申請 (Host Only)
*   **URL**: `POST /family/join/review`
*   **Query Params**:
    *   `hostGoogleId`: `user123`
    *   `requestId`: `10`
    *   `approve`: `true`
*   **Response Sample**:
    ```plaintext
    已同意加入申請
    ```

### 4.8 查詢全家流水帳 (公開紀錄)
*   **URL**: `GET /family/records`
*   **Query Params**: `googleId=user123`
*   **Response Sample**: (List of Records, 同 3.2)

### 4.9 查詢全家支出分類統計
*   **URL**: `GET /family/stats/category`
*   **Query Params**:
    *   `googleId`: `user123`
    *   `type`: `EXPENSE`
*   **Response Sample**: (List of CategoryStatsDto, 同 3.6)
