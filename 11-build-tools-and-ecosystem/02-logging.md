## Konu 8: Logging Frameworks (SLF4J, Logback, Log4j2)

Logging, uygulamanın çalışma zamanı davranışını izlemek, hata ayıklamak ve performans analizi yapmak için kritik bir mekanizmadır. Bir geliştirici olarak, sadece `System.out.println()` kullanmak yerine, doğru logging framework'ünü seçmeli, log seviyelerini etkili kullanmalı ve üretim ortamında performans kaybına yol açmayacak şekilde yapılandırmalısınız.

---

### 1. SLF4J (Simple Logging Facade for Java)

**Analoji:** SLF4J bir "evrensel adaptör" gibidir. Farklı logging framework'lerinin (Logback, Log4j2, JUL) önüne konulmuş bir **facade (cephe)** katmanıdır. Kodunuz SLF4J'yi kullanır, arkada hangi logging implementasyonu çalıştığını bilmez ve umursamaz.

#### Neden SLF4J?
*   **Bağımsızlık:** Kodunuzu belirli bir logging framework'üne bağımlı kılmaz. Gelecekte Logback'ten Log4j2'ye geçiş yaparsanız, kodunuz değişmez, sadece dependency değişir.
*   **Performans:** Placeholder (`{}`) kullanarak string concatenation maliyetinden kurtarır.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    public void createUser(String username) {
        log.info("Creating user: {}", username); // Placeholder kullanımı
        // ...
        log.debug("User created successfully with ID: {}", userId);
    }
}
```

**Dependency:**
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
```

---

### 2. Logback (SLF4J'nin Native Implementation'ı)

**Analoji:** Logback, SLF4J'nin "yerli uygulaması" (native speaker) gibidir. SLF4J'yi yazan aynı kişi (Ceki Gülcü) tarafından geliştirilmiştir ve birbirleriyle mükemmel uyum içinde çalışır.

#### Özellikleri
*   **Hızlı ve Hafif:** Log4j 1.x'e göre çok daha performanslıdır.
*   **Otomatik Reload:** XML yapılandırması değiştiğinde JVM restart gerekmez.
*   **Asynch Logging:** I/O işlemlerini arka planda yapar (non-blocking).
*   **Conditional Processing:** Belirli koşullara göre log yazma.

#### Logback Yapılandırması (`logback.xml`)

```xml
<configuration>
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Rolling File Appender (Günlük rotasyon) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory> <!-- 30 gün saklansın -->
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
    
    <!-- Paket bazlı seviye ayarı -->
    <logger name="com.example.repository" level="DEBUG" />
    <logger name="org.springframework" level="WARN" />
</configuration>
```

**Dependency:**
```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

---

### 3. Log4j2 (Apache'nin Yeni Nesil Logging Framework'ü)

**Analoji:** Log4j2, "güçlendirilmiş spor araba" gibidir. Log4j 1.x'in tüm sorunları giderilmiş, performans özellik ve güvenlik çok ciddi seviyeye çıkarılmıştır.

#### Özellikleri
*   **Çok Yüksek Performans:** Async logger'lar LMAX Disruptor kullanır (garbage-free, lock-free).
*   **Plugin Mimarisi:** Özelleştirilebilir ve genişletilebilir.
*   **Lambda Desteği:** Log ifadelerinde lambda kullanabilirsiniz (lazy evaluation).
*   **Güvenlik:** Log4Shell (CVE-2021-44228) gibi güvenlik açıkları kapatılmıştır (v2.17.0+).

#### Log4j2 Yapılandırması (`log4j2.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        
        <RollingFile name="RollingFile" fileName="logs/app.log"
                     filePattern="logs/app-%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
                <SizeBasedTriggeringPolicy size="100 MB"/>
            </Policies>
        </RollingFile>
    </Appenders>
    
    <Loggers>
        <Logger name="com.example" level="debug" additivity="false">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="RollingFile"/>
        </Logger>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

**Dependency:**
```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.21.1</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId> <!-- SLF4J bridge -->
    <version>2.21.1</version>
</dependency>
```

---

### 4. Log Seviyeleri (Log Levels)

| Seviye | Açıklama | Kullanım |
| :--- | :--- | :--- |
| **TRACE** | En detaylı bilgi | Çok nadir, debugging için (örn: döngü içi değerler) |
| **DEBUG** | Geliştirme ve hata ayıklama bilgisi | Development ortamında, production'da kapatılmalı |
| **INFO** | Genel bilgilendirme mesajları | Uygulama başlatıldı, kullanıcı login oldu |
| **WARN** | Uyarı (hata değil ama dikkat gerekir) | Deprecated API kullanımı, yavaş sorgu |
| **ERROR** | Hata (uygulama çalışmaya devam eder) | Exception yakalandı, işlem başarısız |
| **FATAL** | Kritik hata (uygulama sonlanabilir) | Database bağlantısı yok, disk dolu |

**Kural:** Production'da genellikle **INFO** seviyesi kullanılır. DEBUG ve TRACE çok fazla log üretir, performans düşürür ve disk doldurur.

---

### 5. Logback vs Log4j2

| Özellik | Logback | Log4j2 |
| :--- | :--- | :--- |
| **Performans** | Hızlı | ÇOK DAHA HIZLI (Async modda) |
| **Konfigurasyon** | XML, Groovy | XML, JSON, YAML, Properties |
| **Garbage-Free** | Hayır | Evet (Garbage Collector yükü minimum) |
| **Lambda Desteği** | Hayır | Evet |
| **Spring Boot Varsayılan** | Evet | Hayır (manuel geçiş gerekir) |
| **Topluluk** | Olgun, geniş kullanım | Hızla büyüyen |

**Tavsiye:** Yeni projeler için **Log4j2** tercih edin (performans ve özellik zenginliği). Mevcut Spring Boot projeleri zaten Logback kullanıyordur, değiştirmeye gerek olmayabilir.

---

### 6. Kritik Mülakat Soruları 

#### Soru 1: SLF4J neden sadece bir facade'dir? Neden kendi başına log yazamaz?
**Cevap:** SLF4J sadece bir **API (Interface)**'dir. Gerçek logging işini yapacak bir implementation (Logback, Log4j2) gerekir. Eğer classpath'te implementasyon yoksa, loglar yazılmaz (sessizce yok sayılır veya uyarı verir). Bu sayede uygulama kodu logging framework'üne bağımlı olmaz, sadece SLF4J arayüzüne bağımlıdır.

#### Soru 2: Placeholder `{}` kullanımı neden önemlidir?
**Cevap:**
*   **Yanlış:** `log.debug("User: " + username + " logged in");` → String concatenation her durumda yapılır, log seviyesi DEBUG değilse bile.
*   **Doğru:** `log.debug("User: {} logged in", username);` → Eğer log seviyesi INFO ise, string birleştirme ASLA yapılmaz. Performans kazancı büyüktür.

#### Soru 3: Async logging nedir ve ne zaman kullanılmalıdır?
**Cevap:** Log yazma işlemi I/O yoğundur (disk, network). Sync (senkron) loglamada, log satırı yazılana kadar thread bloklanır. **Async (asenkron) logging**, log mesajlarını bir kuyruğa atar ve arka planda başka bir thread yazar. Ana thread hemen devam eder.
*   **Kullanım:** Yüksek throughput gerektiren sistemlerde (API servisleri, microservices).
*   **Risk:** JVM aniden kapanırsa, kuyruktaki loglar kaybolabilir.

**Logback Async:**
```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE" />
</appender>
```

#### Soru 4: MDC (Mapped Diagnostic Context) nedir?
**Cevap:** MDC, thread bazında anahtar-değer çiftleri saklamaya yarar. Genellikle **request ID**, **user ID** gibi bilgileri tüm log satırlarına eklemek için kullanılır.

```java
MDC.put("userId", "12345");
log.info("User action"); // Log: "userId=12345 - User action"
MDC.clear(); // Thread pool kullanıyorsanız mutlaka temizleyin!
```

**Kullanım:** Dağıtık sistemlerde (microservices) istek takibi için.

#### Soru 5: Log4Shell (CVE-2021-44228) nedir? Nasıl korunulur?
**Cevap:** 2021'de keşfedilen kritik bir güvenlik açığıdır. Log4j 2.x'in JNDI lookup özelliği kötüye kullanılarak **Remote Code Execution (RCE)** yapılabiliyordu.
*   **Çözüm:** Log4j2'yi en az **2.17.0** versiyonuna güncelleyin veya JNDI lookup'ı devre dışı bırakın (`log4j2.formatMsgNoLookups=true`).

#### Soru 6 (Tricky): `System.out.println()` ile logging framework kullanımı arasındaki fark nedir?
**Cevap:**
*   **System.out:** Senkrondur, thread'i bloklar. Log seviyesi, formatlama, dosyaya yazma gibi özellikler yoktur. Production'da asla kullanılmamalı.
*   **Logging Framework:** Async yazma, seviye kontrolü, structured logging, rotation gibi özellikler sunar.
*   **Trap:** "Debug için System.out yeter" denmemeli, commit edildiğinde codebase'de kalır ve performans sorunu yaratır.

#### Soru 7 (Tricky): Logback'te `<logger name="com.example" level="DEBUG" />` ile `<root level="INFO">` varsa, `com.example.MyClass` için ne olur?
**Cevap:** **DEBUG** seviyesinde log yaz

ar.
*   Logger hiyerarşisi vardır. Daha spesifik olan (com.example) önceliklidir.
*   **Trap:** "Root her zaman geçerlidir" yanlış. Package-level logger override eder.

---

### 7. Geliştirici İpuçları

*   **Structured Logging:** JSON formatında log yazmak, log analiz araçları (ELK, Splunk) için idealdir. Logback ve Log4j2 JSON encoder destekler.
*   **Log Aggregation:** Production'da binlerce mikroservis olabilir. Merkezi bir log toplama sistemi (ELK Stack, Graylog) kullanın. Her servis kendi dosyasına yazmak yerine merkezi bir yere gönderin.
*   **Sensitive Data Masking:** Loglamadan önce hassas bilgileri (şifre, kredi kartı) maskeleyin. Logback'te `MaskingMessageConverter` kullanabilirsiniz.
*   **Production'da DEBUG Kapatın:** Geçici hata ayıklama için bile production'da DEBUG açarsanız, disk dolabilir ve performans düşer. Bunun yerine, belirli bir süre (örn: 5 dakika) için dinamik olarak seviye değiştirin (JMX veya Spring Boot Actuator ile).
*   **Exception Loglama:** `log.error("Error", exception)` şeklinde exception nesnesini ikinci parametre olarak verin. Stack trace otomatik yazdırılır. `toString()` ile yazdırmayın, detayları kaybedersiniz.

---

