# 1. 改用官方 Maven 映像檔 (裡面已經裝好 Maven 了，不需要 mvnw)
FROM maven:3.9.6-eclipse-temurin-17

# 2. 設定工作目錄
WORKDIR /app

# 3. 複製所有檔案
COPY . .

# 4. 直接用 'mvn' 指令打包 (注意：這裡不用 ./mvnw 了)
RUN mvn clean package -DskipTests

# 5. 設定 Port
ENV PORT=8080

# 6. 啟動指令 (請確認您的 jar 檔名是否包含版本號)
# 如果您的 pom.xml 版本是 0.0.1-SNAPSHOT，那這行就是對的
CMD ["sh", "-c", "java -Dgoogle.client.id=${GOOGLE_CLIENT_ID} -jar target/accounting-0.0.1-SNAPSHOT.jar"]