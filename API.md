Base URL: /api Host: (Localhost:8080 or deployed host)

1. 會員 (Member)
1.1 開發測試用登入
快速建立或登入一個測試帳號。

Method: POST
URL: /api/members/login
Query Params:
googleId (required): 模擬的 Google ID (如 user123)
nickname (optional): 暱稱 (預設 "測試用戶")
Response Example:
{
  "googleId": "user123",
  "email": "test@gmail.com",
  "nickname": "測試用戶",
  "createdAt": "2023-10-27T10:00:00",
  "lastRecordAt": null,
  "shareStats": false,
  "shareAccounts": false
}
1.2 Google 正式登入
驗證 Google ID Token 並登入/註冊。

Method: POST
URL: /api/members/google-login
Request Body:
{
  "token": "eyJhbGciOiJSUzI1NiIs..."
}
Response: 同 1.1
2. 帳戶 (Account)
2.1 建立帳戶
Method: POST
URL: /api/accounts
Query Params:
googleId (required): 使用者 ID
Request Body:
{
  "name": "玉山銀行",
  "balance": 10000
}
Response Example:
{
  "id": 1,
  "name": "玉山銀行",
  "balance": 10000
}
2.2 查詢我的帳戶列表
Method: GET
URL: /api/accounts
Query Params:
googleId (required): 使用者 ID
Response Example:
[
  { "id": 1, "name": "現金", "balance": 500 },
  { "id": 2, "name": "銀行", "balance": 10000 }
]
3. 記帳 (Record)
3.1 新增記帳
Method: POST
URL: /api/records
Query Params:
googleId (required): 使用者 ID
accountId (required): 關聯的帳戶 ID (會扣款/入帳)
Request Body:
{
  "type": "EXPENSE",       // 或 "INCOME"
  "category": "食物",
  "subCategory": "早餐",
  "amount": 100.00,
  "note": "好吃",
  "date": "2023-10-27"
}
Response Example:
{
  "id": 5,
  "type": "EXPENSE",
  "amount": 100.00,
  "date": "2023-10-27",
  "account": { "id": 1, "name": "現金" }
  // ... 其他欄位
}
3.2 查詢我的記帳紀錄
Method: GET
URL: /api/records
Query Params:
googleId (required)
Response: List of Record objects.
3.3 修改記帳
Method: PUT
URL: /api/records/{id}
Query Params:
googleId (required)
Request Body: (同 3.1)
3.4 刪除記帳
Method: DELETE
URL: /api/records/{id}
Query Params:
googleId (required)
Response: String ("刪除成功！")
3.5 查詢個人統計 (總收支)
Method: GET
URL: /api/records/stats
Query Params:
googleId (required)
Response Example:
{
  "totalIncome": 50000.00,
  "totalExpense": 20000.00,
  "balance": 30000.00
}
3.6 查詢個人分類統計 (圓餅圖用)
Method: GET
URL: /api/records/stats/category
Query Params:
googleId (required)
startDate (optional, YYYY-MM-DD)
endDate (optional, YYYY-MM-DD)
type (optional): EXPENSE 或 INCOME
Response Example:
[
  { "category": "食物", "totalAmount": 5000 },
  { "category": "交通", "totalAmount": 1200 }
]
4. 家庭 (Family)
4.1 建立家庭
Method: POST
URL: /api/family/create
Query Params:
googleId (required): 建立者 ID
name (required): 家庭名稱
Response Example:
{
  "id": 1,
  "name": "彭家記帳",
  "inviteCode": "A1B2C3", // 自動生成
  "host": { ... }
}
4.2 申請加入家庭
Method: POST
URL: /api/family/join
Query Params:
googleId (required): 申請人 ID
code (required): 邀請碼
Response: Family 物件 (若需要審核則可能稍後才會正式生效，依 Service 邏輯而定，目前 Controller 看起來是直接回傳 Family)。
4.3 取得加入申請清單 (Host 專用)
Method: GET
URL: /api/family/join-requests
Query Params:
hostGoogleId (required)
Response Example:
[
  {
    "id": 1,
    "applicant": { "nickname": "小明", ... },
    "status": "PENDING",
    "createdAt": "..."
  }
]
4.4 審核加入申請 (Host 專用)
Method: POST
URL: /api/family/join/review
Query Params:
hostGoogleId (required)
requestId (required): 申請單 ID
approve (required): true (同意)/ ``false (拒絕)
Response: String ("已同意加入申請" 或 "已拒絕加入申請")
4.5 更新個人隱私設定
Method: PUT
URL: /api/family/settings
Query Params:
googleId (required)
shareStats (boolean): 是否分享統計數據
shareAccounts (boolean): 是否分享帳戶列表
4.6 家庭總覽
取得家庭成員列表，以及他們願意公開的資訊。

Method: GET
URL: /api/family/overview
Query Params: googleId
Response Example:
[
  {
    "nickname": "爸爸",
    "shareStats": true,
    "shareAccounts": false,
    "totalAssets": 1000000, // 因 shareStats=true 可見
    "accounts": null       // 因 shareAccounts=false 不可見
  },
  {
    "nickname": "媽媽",
    "shareStats": true,
    "shareAccounts": true,
    "totalAssets": 500000,
    "accounts": [ { "name": "私房錢", "balance": 500000 } ]
  }
]
4.7 家庭流水帳
Method: GET
URL: /api/family/records
Query Params: googleId
Response: List of Records (所有家庭成員的記帳)。
4.8 家庭分類統計
Method: GET
URL: /api/family/stats/category
Query Params: googleId, startDate, endDate, type
Response: List of CategoryStatsDto (全家人的加總)。
4.9 查看特定家人的詳細資料
僅在對方開啟隱私權限時有資料。

URL:
/api/family/member/accounts: 對方的帳戶
/api/family/member/records: 對方的記帳
/api/family/member/stats: 對方的統計
Query Params:
googleId: 我的 ID
targetGoogleId: 對方的 ID