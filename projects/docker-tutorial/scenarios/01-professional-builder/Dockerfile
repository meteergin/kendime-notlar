# --- Stage 1: Builder ---
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# 1. Bağımlılıkları Cache'le
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Kaynak kodu kopyala ve build et
COPY src ./src
RUN mvn clean package -DskipTests

# 3. Layer'ları ayıkla (Spring Boot 2.3+)
WORKDIR /app/target
RUN java -Djarmode=layertools -jar *.jar extract

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /application

# 4. Güvenlik: Non-root user oluştur
RUN addgroup -S spring && adduser -S spring -G spring

# 5. Layer'ları kopyala
COPY --from=builder /app/target/dependencies/ ./
COPY --from=builder /app/target/spring-boot-loader/ ./
COPY --from=builder /app/target/snapshot-dependencies/ ./
COPY --from=builder /app/target/application/ ./

# 6. Yetkileri ayarla
RUN chown -R spring:spring /application
USER spring:spring

# 7. JVM Tuning ve Başlatma
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
