const app = {
    user: null, // Stores { googleId, nickname, ... }
    currentTab: 'dashboard',
    recordType: 'EXPENSE',
    categories: null, // ✨ 儲存後端回傳的分類結構

    // --- Init & Login ---
    init: function() {
        // Check if user is already logged in (simulated)
        const storedUser = localStorage.getItem('acc_user');
        if (storedUser) {
            this.user = JSON.parse(storedUser);
            this.showMainApp();
            this.loadCategories(); // ✨ 登入後預先載入分類
        } else {
            // Only init Google login if not logged in
            this.initGoogleLogin();
        }

        // Set default date for record
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('record-date').value = today;
    },

    initGoogleLogin: async function() {
        // Wait for Google Script to load
        if (typeof google === 'undefined' || !google.accounts) {
            setTimeout(() => this.initGoogleLogin(), 500);
            return;
        }

        try {
            // ✨ 改為從後端取得 Client ID，不寫死在前端
            const res = await fetch('/api/members/google-client-id');
            if (!res.ok) throw new Error("無法取得 Google Client ID");
            const data = await res.json();
            const clientId = data.clientId;

            google.accounts.id.initialize({
                client_id: clientId,
                callback: this.handleGoogleLogin.bind(this)
            });
            
            google.accounts.id.renderButton(
                document.getElementById("google-btn"),
                { theme: "outline", size: "large", width: 250 }  // customization attributes
            );
        } catch (e) {
            console.error("Google Login Init Error:", e);
        }
    },

    handleGoogleLogin: async function(response) {
        console.log("Google Token:", response.credential);
        try {
            const res = await fetch('/api/members/google-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token: response.credential })
            });

            if (res.ok) {
                this.user = await res.json();
                localStorage.setItem('acc_user', JSON.stringify(this.user));
                this.showMainApp();
                this.loadCategories();
            } else {
                alert('Google 登入失敗，請檢查後端驗證設定');
            }
        } catch (error) {
            console.error('Google Login error:', error);
            alert('連線錯誤');
        }
    },

    login: async function() {
        const googleId = document.getElementById('login-google-id').value;
        const nickname = document.getElementById('login-nickname').value;

        if (!googleId) {
            alert('請輸入 Google ID');
            return;
        }

        try {
            const response = await fetch(`/api/members/login?googleId=${encodeURIComponent(googleId)}&nickname=${encodeURIComponent(nickname)}`, {
                method: 'POST'
            });

            if (response.ok) {
                this.user = await response.json();
                localStorage.setItem('acc_user', JSON.stringify(this.user));
                this.showMainApp();
                this.loadCategories(); // ✨
            } else {
                alert('登入失敗');
            }
        } catch (error) {
            console.error('Login error:', error);
            alert('連線錯誤');
        }
    },
    
    // --- Categories Logic ---
    loadCategories: async function() {
        try {
            const res = await fetch(`/api/records/categories?googleId=${this.user.googleId}`);
            this.categories = await res.json();
        } catch (e) {
            console.error("無法載入分類", e);
        }
    },

    renderCategorySelect: function() {
        if (!this.categories) return;

        const catSelect = document.getElementById('record-category-select');
        const subSelect = document.getElementById('record-sub-select');
        const subWrapper = document.getElementById('record-sub-custom-wrapper');
        
        // 重置
        catSelect.innerHTML = '<option value="">請選擇主分類</option>';
        subSelect.innerHTML = '<option value="">請先選擇主分類</option>';
        subWrapper.style.display = 'none';
        subSelect.style.display = 'block';

        // 根據目前的 recordType (EXPENSE / INCOME) 填入選項
        const typeData = this.categories[this.recordType]; // 取得 Map<String, Set>
        if (typeData) {
            Object.keys(typeData).forEach(catName => {
                const opt = document.createElement('option');
                opt.value = catName;
                opt.textContent = catName;
                catSelect.appendChild(opt);
            });
        }
    },

    onCategoryChange: function() {
        const catSelect = document.getElementById('record-category-select');
        const subSelect = document.getElementById('record-sub-select');
        const subWrapper = document.getElementById('record-sub-custom-wrapper');
        const subInput = document.getElementById('record-sub-input');
        
        const selectedCat = catSelect.value;
        subSelect.innerHTML = '<option value="">請選擇子分類</option>';
        subWrapper.style.display = 'none';
        subSelect.style.display = 'block';
        subInput.value = ''; // 清空手動輸入

        if (!selectedCat || !this.categories) return;

        const subCats = this.categories[this.recordType][selectedCat] || [];
        
        // 填入現有子分類
        subCats.forEach(sub => {
            const opt = document.createElement('option');
            opt.value = sub;
            opt.textContent = sub;
            subSelect.appendChild(opt);
        });

        // 加入「新增」選項
        const addOpt = document.createElement('option');
        addOpt.value = "CUSTOM_NEW";
        addOpt.textContent = "➕ 新增子分類...";
        addOpt.style.color = "blue";
        subSelect.appendChild(addOpt);
    },

    onSubCategoryChange: function() {
        const subSelect = document.getElementById('record-sub-select');
        const subWrapper = document.getElementById('record-sub-custom-wrapper');
        const subInput = document.getElementById('record-sub-input');

        if (subSelect.value === 'CUSTOM_NEW') {
            subSelect.style.display = 'none';
            subWrapper.style.display = 'flex'; // Change to flex to align input and button
            subInput.focus();
        }
    },

    cancelCustomSub: function() {
        const subSelect = document.getElementById('record-sub-select');
        const subWrapper = document.getElementById('record-sub-custom-wrapper');
        const subInput = document.getElementById('record-sub-input');

        subWrapper.style.display = 'none';
        subSelect.style.display = 'block';
        subSelect.value = ""; // Reset select
        subInput.value = ""; // Clear input
    },

    // ... (rest of the file)

    showMainApp: function() {
        document.getElementById('login-section').classList.add('hidden');
        document.getElementById('main-app').classList.remove('hidden');
        document.getElementById('user-display-name').textContent = `你好，${this.user.nickname}`;
        this.loadDashboard();
    },

    logout: function() {
        localStorage.removeItem('acc_user');
        location.reload();
    },

    // --- Tabs ---
    switchTab: function(tabName) {
        this.currentTab = tabName;
        
        // Update UI
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
        
        // Find the tab element roughly by text or index (simplification for this example)
        // Better: add ID or data attribute to tabs. For now, matching by clicking logic in HTML.
        const tabNames = ['dashboard', 'accounts', 'records', 'family', 'settings'];
        const tabIndex = tabNames.indexOf(tabName);
        if(tabIndex >= 0) document.querySelectorAll('.tab')[tabIndex].classList.add('active');
        
        document.getElementById(`tab-${tabName}`).classList.remove('hidden');

        // Load data
        if (tabName === 'dashboard') this.loadDashboard();
        if (tabName === 'accounts') this.loadAccounts();
        if (tabName === 'records') this.loadRecords();
        if (tabName === 'family') this.loadFamily();
        if (tabName === 'settings') this.loadSettings();
    },

    // --- Settings ---
    loadSettings: function() {
        if (!this.user) return;
        
        // 預設值
        const time = this.user.reminderTime || "20:00"; // LocalTime string usually HH:mm:ss, but input type=time needs HH:mm
        // Handle "HH:mm:ss" from backend
        const formattedTime = time.length > 5 ? time.substring(0, 5) : time;
        
        document.getElementById('setting-reminder-time').value = formattedTime;
        document.getElementById('setting-reminder-enable').checked = this.user.enableReminder !== false; // Default true if undefined

        // Privacy Settings (Moved from Family tab)
        document.getElementById('setting-share-stats').checked = this.user.shareStats || false;
        document.getElementById('setting-share-accounts').checked = this.user.shareAccounts || false;
    },

    saveReminderSettings: async function() {
        const time = document.getElementById('setting-reminder-time').value;
        const enable = document.getElementById('setting-reminder-enable').checked;

        if (!time) return alert('請選擇時間');

        try {
            const res = await fetch(`/api/members/settings/reminder?googleId=${this.user.googleId}&time=${time}&enable=${enable}`, {
                method: 'PUT'
            });

            if (res.ok) {
                alert('設定已儲存');
                // Update local user object
                this.user.reminderTime = time;
                this.user.enableReminder = enable;
                localStorage.setItem('acc_user', JSON.stringify(this.user));
            } else {
                alert('儲存失敗');
            }
        } catch (e) {
            console.error(e);
            alert('連線錯誤');
        }
    },

    // --- Dashboard ---
    loadDashboard: async function() {
        if (!this.user) return;
        
        // 1. Get Stats
        try {
            const res = await fetch(`/api/records/stats?googleId=${this.user.googleId}`);
            const stats = await res.json();
            
            document.getElementById('total-income').textContent = `$ ${stats.totalIncome || 0}`;
            document.getElementById('total-expense').textContent = `$ ${stats.totalExpense || 0}`;
            document.getElementById('total-balance').textContent = `$ ${stats.balance || 0}`;
        } catch (e) {
            console.error(e);
        }

        // 2. Get Category Stats (Expense by default)
        try {
            const res = await fetch(`/api/records/stats/category?googleId=${this.user.googleId}&type=EXPENSE`);
            const cats = await res.json();
            
            const container = document.getElementById('category-stats-container');
            container.innerHTML = '';
            
            if (cats.length === 0) {
                container.innerHTML = '<p style="text-align:center; color:#888;">尚無支出紀錄</p>';
            } else {
                // Find max for progress bar
                const max = Math.max(...cats.map(c => c.totalAmount));
                
                cats.forEach(c => {
                    const percent = (c.totalAmount / max) * 100;
                    const html = `
                        <div class="stat-row">
                            <span>${c.category}</span>
                            <span>$ ${c.totalAmount}</span>
                        </div>
                        <div class="progress-bar-bg">
                            <div class="progress-bar-fill" style="width: ${percent}%"></div>
                        </div>
                    `;
                    container.innerHTML += html;
                });
            }
        } catch (e) {
            console.error(e);
        }
    },

    // --- Accounts ---
    loadAccounts: async function() {
        const res = await fetch(`/api/accounts?googleId=${this.user.googleId}`);
        const accounts = await res.json();
        const list = document.getElementById('accounts-list');
        list.innerHTML = '';

        if(accounts.length === 0) {
            list.innerHTML = '<p>還沒有帳戶，快去建立一個吧！</p>';
            return;
        }

        accounts.forEach(acc => {
            const div = document.createElement('div');
            div.className = 'list-item';
            div.innerHTML = `
                <div>
                    <strong>${acc.name}</strong>
                </div>
                <div class="amount income">$ ${acc.balance}</div>
            `;
            list.appendChild(div);
        });
    },

    createAccount: async function() {
        const name = document.getElementById('acc-name').value;
        const balance = document.getElementById('acc-balance').value;

        if(!name) return alert('請輸入名稱');

        const res = await fetch(`/api/accounts?googleId=${this.user.googleId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ name, balance: parseFloat(balance) })
        });

        if(res.ok) {
            alert('建立成功');
            this.closeModal('modal-account');
            this.loadAccounts(); // Refresh if on account tab
            this.loadDashboard(); // Refresh stats
        } else {
            alert('建立失敗');
        }
    },

    // --- Records ---
    loadRecords: async function() {
        const res = await fetch(`/api/records?googleId=${this.user.googleId}`);
        const records = await res.json();
        const list = document.getElementById('records-list');
        list.innerHTML = '';

        if(records.length === 0) {
            list.innerHTML = '<p>尚無紀錄</p>';
            return;
        }

        // Sort by date desc
        records.sort((a, b) => new Date(b.date) - new Date(a.date));

        records.forEach(r => {
            const div = document.createElement('div');
            div.className = 'list-item';
            const colorClass = r.type === 'INCOME' ? 'income' : 'expense';
            const sign = r.type === 'INCOME' ? '+' : '-';
            
            div.innerHTML = `
                <div>
                    <div style="font-weight:500;">${r.category} <small style="color:#888;">${r.subCategory || ''}</small></div>
                    <small style="color:#888;">${r.date} | ${r.account ? r.account.name : '未知帳戶'}</small>
                    <div style="font-size:0.8em; color:#666;">${r.note || ''}</div>
                </div>
                <div>
                    <span class="amount ${colorClass}">${sign} $ ${r.amount}</span>
                    <button class="btn btn-outline" style="padding: 2px 8px; font-size: 0.7rem; margin-left: 5px;" onclick="app.deleteRecord(${r.id})">刪</button>
                </div>
            `;
            list.appendChild(div);
        });
    },

    createRecord: async function() {
        const accountId = document.getElementById('record-account-select').value;
        const amount = document.getElementById('record-amount').value;
        const date = document.getElementById('record-date').value;
        const note = document.getElementById('record-note').value;

        // ✨ 取得分類 (處理下拉選單 vs 手動輸入)
        const catSelect = document.getElementById('record-category-select');
        const subSelect = document.getElementById('record-sub-select');
        const subInput = document.getElementById('record-sub-input');

        const category = catSelect.value;
        let subCategory = subSelect.value;

        // 如果是手動輸入模式
        if (subCategory === 'CUSTOM_NEW') {
            subCategory = subInput.value;
        }

        if(!accountId || !amount || !category || !date) {
            alert('請填寫完整資訊 (帳戶、金額、分類、日期)');
            return;
        }

        const payload = {
            type: this.recordType,
            category,
            subCategory,
            amount: parseFloat(amount),
            note,
            date
        };

        const res = await fetch(`/api/records?googleId=${this.user.googleId}&accountId=${accountId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });

        if(res.ok) {
            alert('記帳成功');
            this.closeModal('modal-record');
            this.loadRecords();
            this.loadDashboard();
            this.loadCategories(); // ✨ 重新載入分類
        } else {
            alert('失敗');
        }
    },

    deleteRecord: async function(id) {
        if(!confirm('確定刪除?')) return;
        const res = await fetch(`/api/records/${id}?googleId=${this.user.googleId}`, { method: 'DELETE' });
        if(res.ok) {
            this.loadRecords();
            this.loadDashboard();
        } else {
            alert('刪除失敗');
        }
    },

    setRecordType: function(type) {
        this.recordType = type;
        const btnExp = document.getElementById('btn-type-expense');
        const btnInc = document.getElementById('btn-type-income');
        
        if (type === 'EXPENSE') {
            btnExp.className = 'btn';
            btnInc.className = 'btn btn-outline';
        } else {
            btnExp.className = 'btn btn-outline';
            btnInc.className = 'btn';
        }
        this.renderCategorySelect(); // ✨ 切換類型時重繪選單
    },

    // --- Family ---
    loadFamily: async function() {
        try {
            // Check if user has family by getting overview
            // API: /api/family/overview?googleId=...
            // If user has no family, this might throw 500 or error based on controller logic
            // Controller says: if(me.getFamily() == null) throw new RuntimeException("你還沒加入家庭");
            
            const res = await fetch(`/api/family/overview?googleId=${this.user.googleId}`);
            if (res.status === 500) {
                // Assume no family
                document.getElementById('no-family-view').classList.remove('hidden');
                document.getElementById('family-view').classList.add('hidden');
                return;
            }

            const members = await res.json();
            document.getElementById('no-family-view').classList.add('hidden');
            document.getElementById('family-view').classList.remove('hidden');

            // Render Members
            const list = document.getElementById('family-members-list');
            list.innerHTML = '';
            members.forEach(m => {
                const card = document.createElement('div');
                card.className = 'card';
                card.style.marginBottom = '0';
                card.style.padding = '1rem';
                
                let statsHtml = '<small style="color:#aaa;">(不公開)</small>';
                if (m.shareStats && m.totalAssets != null) {
                    statsHtml = `<span style="font-weight:bold; color:var(--dark-green);">$ ${m.totalAssets}</span>`;
                }

                card.innerHTML = `
                    <h3 style="margin:0 0 5px 0;">${m.nickname}</h3>
                    <div>資產: ${statsHtml}</div>
                `;
                list.appendChild(card);
            });

            // Load Family Stats (Category)
            this.loadFamilyStats();
            
            // Check host requests
            this.checkJoinRequests();

        } catch (e) {
            console.error(e);
            alert('載入家庭資料錯誤');
        }
    },

    createFamily: async function() {
        const name = document.getElementById('new-family-name').value;
        if (!name) return;
        
        const res = await fetch(`/api/family/create?googleId=${this.user.googleId}&name=${encodeURIComponent(name)}`, { method: 'POST' });
        if (res.ok) {
            alert('家庭建立成功');
            this.loadFamily();
        } else {
            alert('建立失敗');
        }
    },

    joinFamily: async function() {
        const code = document.getElementById('join-family-code').value;
        if (!code) return;

        const res = await fetch(`/api/family/join?googleId=${this.user.googleId}&code=${code}`, { method: 'POST' });
        if (res.ok) {
            alert('申請已送出或已加入');
            this.loadFamily();
        } else {
            alert('加入失敗');
        }
    },

    updatePrivacy: async function() {
        const shareStats = document.getElementById('setting-share-stats').checked;
        const shareAccounts = document.getElementById('setting-share-accounts').checked;

        try {
            const res = await fetch(`/api/family/settings?googleId=${this.user.googleId}&shareStats=${shareStats}&shareAccounts=${shareAccounts}`, {
                method: 'PUT'
            });
            
            if (res.ok) {
                // ✨ 同步更新本地狀態
                this.user.shareStats = shareStats;
                this.user.shareAccounts = shareAccounts;
                localStorage.setItem('acc_user', JSON.stringify(this.user));
                
                // 這裡可以做個簡單的提示，例如按鈕變色或 console
                console.log('隱私設定已更新並儲存');
            } else {
                alert('隱私設定更新失敗');
                // 失敗時回復 checkbox 狀態 (選用)
                document.getElementById('setting-share-stats').checked = !shareStats; 
                document.getElementById('setting-share-accounts').checked = !shareAccounts;
            }
        } catch (e) {
            console.error(e);
            alert('連線錯誤，無法更新設定');
        }
    },

    loadFamilyStats: async function() {
        const res = await fetch(`/api/family/stats/category?googleId=${this.user.googleId}&type=EXPENSE`);
        const stats = await res.json();
        const list = document.getElementById('family-stats-list');
        list.innerHTML = '';
        
        if (stats.length === 0) {
            list.innerHTML = '<p>尚無家庭支出</p>';
            return;
        }

        const max = Math.max(...stats.map(s => s.totalAmount));
        stats.forEach(s => {
             const percent = (s.totalAmount / max) * 100;
             list.innerHTML += `
                <div class="stat-row">
                    <span>${s.category}</span>
                    <span>$ ${s.totalAmount}</span>
                </div>
                <div class="progress-bar-bg">
                    <div class="progress-bar-fill" style="width: ${percent}%; background-color: #ffb74d;"></div>
                </div>
             `;
        });
    },

    checkJoinRequests: async function() {
        // Try to fetch join requests. If I'm not host, this might fail or return empty?
        // Actually the API requires hostGoogleId. If I'm not host, maybe it just returns empty or 403.
        try {
            const res = await fetch(`/api/family/join-requests?hostGoogleId=${this.user.googleId}`);
            if (res.ok) {
                const reqs = await res.json();
                const list = document.getElementById('join-requests-list');
                
                if (reqs.length > 0) {
                    document.getElementById('host-zone').style.display = 'block';
                    list.innerHTML = '';
                    reqs.forEach(r => {
                        if(r.status === 'PENDING') {
                            const div = document.createElement('div');
                            div.className = 'list-item';
                            div.innerHTML = `
                                <span>${r.applicant.nickname} 申請加入</span>
                                <div>
                                    <button class="btn" style="font-size:0.8rem; padding:5px;" onclick="app.reviewRequest(${r.id}, true)">同意</button>
                                    <button class="btn btn-outline" style="font-size:0.8rem; padding:5px;" onclick="app.reviewRequest(${r.id}, false)">拒絕</button>
                                </div>
                            `;
                            list.appendChild(div);
                        }
                    });
                }
            }
        } catch (e) {
            // Not a host or error
        }
    },

    reviewRequest: async function(reqId, approve) {
        const res = await fetch(`/api/family/join/review?hostGoogleId=${this.user.googleId}&requestId=${reqId}&approve=${approve}`, { method: 'POST' });
        if (res.ok) {
            alert('已處理');
            this.checkJoinRequests();
            this.loadFamily();
        }
    },


    // --- Helpers ---
    showModal: async function(id) {
        document.getElementById(id).classList.add('show');
        
        // If opening record modal, load accounts for select
        if (id === 'modal-record') {
            this.renderCategorySelect(); // ✨ 打開時初始化分類選單
            const select = document.getElementById('record-account-select');
            select.innerHTML = '<option>載入中...</option>';
            const res = await fetch(`/api/accounts?googleId=${this.user.googleId}`);
            const accounts = await res.json();
            select.innerHTML = '';
            accounts.forEach(acc => {
                const opt = document.createElement('option');
                opt.value = acc.id;
                opt.textContent = `${acc.name} (餘額: ${acc.balance})`;
                select.appendChild(opt);
            });
        }
    },

    closeModal: function(id) {
        document.getElementById(id).classList.remove('show');
    }
};

// Start
app.init();
