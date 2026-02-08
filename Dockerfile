# 1. 使用 Java 17 (OpenJDK) 作為基底環境
FROM eclipse-temurin:17-jdk-jammy

# 2. 設定工作目錄
WORKDIR /app

# 3. 把您的專案檔案全部複製進去
COPY . .

# 4. 給 Maven Wrapper 執行權限 (這一步在 Linux 環境非常重要！)
RUN chmod +x mvnw

# 5. 開始打包 (跳過測試以節省時間)
RUN ./mvnw clean package -DskipTests

# 6. 設定環境變數 (Render 會自動注入 PORT，這裡設個預設值)
ENV PORT=8080

# 7. 告訴 Render 啟動時要執行什麼指令
# 注意：這裡的 jar 檔名必須跟您 target 資料夾裡的一樣
# 通常 Maven 預設是 [artifactId]-[version].jar
CMD ["java", "-jar", "target/accounting-0.0.1-SNAPSHOT.jar"]