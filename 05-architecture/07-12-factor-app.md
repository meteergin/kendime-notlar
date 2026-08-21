# 12-Factor App

> **Analoji:** 12-Factor App, bir "sağlıklı yaşam rehberi" gibidir. Bu 12 kuralı takip eden uygulamalar bulut ortamında sağlıklı yaşar, kolay büyür ve nadiren hasta olur.

---

## 12 Faktör ve Java/Spring Boot Karşılıkları

### 1. Codebase — Tek Kod Tabanı, Çok Deploy

Bir uygulama = bir Git repo. Dev, staging, prod aynı codebase'den deploy edilir.

```
❌ Farklı ortamlar için farklı repository
✅ Tek repository, ortam farkı konfigürasyonla sağlanır
```

### 2. Dependencies — Bağımlılıkları Açıkça Bildir

Tüm bağımlılıklar `pom.xml` / `build.gradle` ile tanımlanır. Sistem kütüphanelerine güvenme.

```xml
<!-- pom.xml'de explicit dependency -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3. Config — Konfigürasyonu Ortam Değişkenlerinde Tut

Kod içinde sabit değer (hardcode) yok. Ortam bazlı ayarlar `Environment Variable` veya `ConfigMap`'te.

```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# Kubernetes ConfigMap
apiVersion: v1
kind: ConfigMap
data:
  DATABASE_URL: jdbc:postgresql://db-host:5432/mydb
```

### 4. Backing Services — Destek Servislerini Kaynak Olarak Ele Al

DB, Redis, Kafka, SMTP → hepsi değiştirilebilir (swappable) kaynaklardır. Bağlantı bilgisi config'den gelir.

```
Lokal PostgreSQL → AWS RDS geçişi sadece URL değişikliği ile yapılmalı
```

### 5. Build, Release, Run — Aşamaları Kesin Ayır

| Aşama | Açıklama | Araç |
| :--- | :--- | :--- |
| **Build** | Kod → artifact (JAR) | Maven, Gradle |
| **Release** | Artifact + config → deploy paketi | Docker image + ConfigMap |
| **Run** | Çalışan süreç | Kubernetes Pod |

### 6. Processes — Uygulamayı Durumsuz Süreçler Olarak Çalıştır

**Stateless!** Session, cache gibi veriler uygulama belleğinde tutulmaz. Redis veya DB'de saklanır.

```java
// ❌ YANLIŞ - Stateful
@Controller
public class CartController {
    private Map<String, Cart> carts = new HashMap<>(); // Bellekte tutuluyor!
}

// ✅ DOĞRU - Stateless
@Controller
public class CartController {
    @Autowired
    private RedisTemplate<String, Cart> redisTemplate; // Dışarıda tutuluyor
}
```

### 7. Port Binding — Portu Bağla ve Servis Olarak Çalıştır

Uygulama kendi web sunucusunu taşır (embedded Tomcat). Harici bir Tomcat/JBoss'a deploy etme.

```properties
server.port=${PORT:8080}  # Ortam değişkeninden veya 8080
```

### 8. Concurrency — Yatay Ölçekleme (Scale Out)

Daha fazla trafik = daha fazla instance (pod). Thread sayısını artırmak değil, instance sayısını artır.

```yaml
# Kubernetes HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          averageUtilization: 70
```

### 9. Disposability — Hızlı Başla, Zarif Kapat

Uygulama saniyeler içinde başlamalı ve `SIGTERM` alınca açık işleri tamamlayıp kapanmalı (graceful shutdown).

```yaml
# Spring Boot graceful shutdown
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 10. Dev/Prod Parity — Ortamları Benzer Tut

Geliştirme ortamı production'a olabildiğince benzemeli. H2 yerine Testcontainers ile gerçek PostgreSQL.

### 11. Logs — Logları Olay Akışı Olarak Ele Al

Dosyaya yazma, `stdout`'a yaz. Log toplama altyapısı (ELK, Fluentd) oradan alır.

```yaml
# Spring Boot → stdout'a JSON formatında log
logging:
  pattern:
    console: '{"time":"%d","level":"%p","logger":"%c","msg":"%m"}%n'
```

### 12. Admin Processes — Yönetim Görevlerini Tek Seferlik Süreç Olarak Çalıştır

DB migration, data fix gibi işlemler ayrı bir süreç olarak çalışır. Kubernetes Job/CronJob kullanın.

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration
spec:
  template:
    spec:
      containers:
        - name: migrate
          image: myapp:latest
          command: ["java", "-jar", "app.jar", "--spring.flyway.enabled=true"]
      restartPolicy: Never
```

---

## Mülakat Sorusu: 12-Factor App prensiplerinden hangisi en çok ihlal edilir?

**Cevap:** **Factor 3 (Config)** ve **Factor 6 (Stateless Processes)**. Geliştiriciler sıklıkla:
- DB şifrelerini `application.properties`'e commit eder (Factor 3 ihlali)
- HTTP session'ı bellekte tutar, sticky session kullanır (Factor 6 ihlali)

**Çözüm:** HashiCorp Vault + Spring Cloud Config (Factor 3), Redis + Spring Session (Factor 6).
