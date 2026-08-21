## Konu 20: Web ve API Güvenliği (HTTPS, OWASP, CORS, SSL/TLS, CSP)

Web güvenliği, modern uygulamaların en kritik yönüdür. Bir geliştirici olarak, OWASP Top 10 risklerini, HTTPS/TLS'in nasıl çalıştığını, CORS politikalarını ve API güvenliği best practice'lerini derinlemesine bilmelisiniz.

---

### 1. HTTPS ve SSL/TLS

**Analoji:** HTTP, açık bir kartpostal gibidir. Herkes okuyabilir. HTTPS ise mühürlü bir zarf gibidir. Sadece gönderen ve alan okuyabilir.

#### HTTP vs HTTPS

| Özellik | HTTP | HTTPS |
| :--- | :--- | :--- |
| **Port** | 80 | 443 |
| **Şifreleme** | Yok (Plain text) | TLS/SSL ile şifrelenmiş |
| **Güvenlik** | Güvensiz | Güvenli |
| **SEO** | Düşük sıralama | Google öncelik verir |

#### TLS Handshake (El Sıkışma)

1.  **Client Hello:** İstemci desteklediği şifreleme algoritmalarını gönderir.
2.  **Server Hello:** Sunucu bir algoritma seçer ve sertifikasını (certificate) gönderir.
3.  **Certificate Verification:** İstemci sertifikayı doğrular (CA - Certificate Authority).
4.  **Key Exchange:** Symmetric key oluşturulur (Public/Private key ile).
5.  **Encrypted Communication:** Artık tüm iletişim symmetric key ile şifrelenir.

**Symmetric vs Asymmetric Encryption:**
*   **Asymmetric (Public/Private Key):** Yavaş, sadece handshake'te kullanılır.
*   **Symmetric (AES):** Hızlı, veri iletişiminde kullanılır.

#### Spring Boot'ta HTTPS

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: tomcat
```

**Self-Signed Certificate Oluşturma (Development):**
```bash
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

**Production:** Let's Encrypt (ücretsiz) veya DigiCert, Comodo gibi CA'lardan sertifika alın.

---

### 2. OWASP Top 10 (2021)

OWASP (Open Web Application Security Project), en yaygın web güvenlik açıklarını listeler.

#### 1. Broken Access Control

**Sorun:** Kullanıcılar yetkisiz kaynaklara erişebilir.

**Örnek:**
```
GET /api/users/123/orders
// Kullanıcı 456, kullanıcı 123'ün siparişlerini görebiliyor!
```

**Çözüm:**
```java
@PreAuthorize("#userId == authentication.principal.id")
@GetMapping("/api/users/{userId}/orders")
public List<Order> getOrders(@PathVariable Long userId) {
    // Sadece kendi siparişlerini görebilir
}
```

#### 2. Cryptographic Failures

**Sorun:** Hassas veriler şifrelenmemiş saklanır veya iletilir.

**Çözüm:**
*   HTTPS kullanın.
*   Veritabanında hassas verileri (kredi kartı) şifreleyin.
*   bcrypt/Argon2 ile şifre hash'leyin.

#### 3. Injection (SQL, NoSQL, Command)

**Sorun:** Kullanıcı girdisi doğrudan sorguya eklenir.

**SQL Injection:**
```java
// KÖTÜ
String query = "SELECT * FROM users WHERE username = '" + username + "'";
// username = "admin' OR '1'='1" → Tüm kullanıcılar döner!

// İYİ (Prepared Statement)
String query = "SELECT * FROM users WHERE username = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setString(1, username);
```

**JPA/Hibernate otomatik olarak Prepared Statement kullanır.**

#### 4. Insecure Design

**Sorun:** Güvenlik, tasarım aşamasında düşünülmemiş.

**Çözüm:** Threat Modeling, Security by Design prensipleri.

#### 5. Security Misconfiguration

**Sorun:** Varsayılan ayarlar, gereksiz özellikler açık.

**Örnekler:**
*   Varsayılan admin şifresi değiştirilmemiş.
*   Stack trace production'da gösteriliyor.
*   Gereksiz HTTP metodları (TRACE, OPTIONS) açık.

**Çözüm:**
```yaml
server:
  error:
    include-stacktrace: never # Production'da stack trace gösterme
```

#### 6. Vulnerable and Outdated Components

**Sorun:** Eski, güvenlik açığı olan kütüphaneler kullanılıyor.

**Çözüm:**
*   Dependency'leri düzenli güncelleyin.
*   OWASP Dependency-Check, Snyk gibi araçlar kullanın.

#### 7. Identification and Authentication Failures

**Sorun:** Zayıf kimlik doğrulama, session yönetimi.

**Çözüm:**
*   MFA kullanın.
*   Session timeout ayarlayın.
*   Brute-force koruması (rate limiting).

#### 8. Software and Data Integrity Failures

**Sorun:** Güvenilmeyen kaynaklardan kod/veri kullanılıyor.

**Çözüm:**
*   Dependency'leri güvenilir kaynaklardan indirin (Maven Central).
*   CI/CD pipeline'da integrity check yapın.

#### 9. Security Logging and Monitoring Failures

**Sorun:** Güvenlik olayları loglanmıyor, izlenmiyor.

**Çözüm:**
*   Başarısız login denemelerini loglayın.
*   SIEM (Security Information and Event Management) kullanın.

#### 10. Server-Side Request Forgery (SSRF)

**Sorun:** Sunucu, kullanıcının belirttiği URL'e istek gönderir.

**Örnek:**
```java
// KÖTÜ
String url = request.getParameter("imageUrl");
URL imageUrl = new URL(url);
InputStream stream = imageUrl.openStream();
// Kullanıcı: http://localhost:8080/admin → İç servislere erişir!
```

**Çözüm:** URL whitelist kullanın, internal IP'lere istek göndermeyin.

---

### 3. CORS (Cross-Origin Resource Sharing)

**Analoji:** CORS, bir "sınır kapısı" gibidir. Farklı domain'lerden gelen istekleri kontrol eder.

#### Same-Origin Policy

Browser, güvenlik nedeniyle farklı origin'lerden (domain, port, protocol) AJAX isteklerini engeller.

**Origin:**
*   `https://example.com:443` ≠ `http://example.com:80` (farklı protocol)
*   `https://example.com` ≠ `https://api.example.com` (farklı subdomain)

#### CORS Konfigürasyonu

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://frontend.example.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

**Dikkat:** `allowedOrigins("*")` ve `allowCredentials(true)` birlikte kullanılamaz (güvenlik riski).

#### Preflight Request

Bazı istekler (POST, custom headers) için browser önce bir `OPTIONS` isteği gönderir (preflight).

```http
OPTIONS /api/users HTTP/1.1
Origin: https://frontend.example.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Content-Type
```

Sunucu izin verirse:
```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: https://frontend.example.com
Access-Control-Allow-Methods: POST
Access-Control-Allow-Headers: Content-Type
```

---

### 4. Content Security Policy (CSP)

**Analoji:** CSP, bir "güvenlik görevlisi" gibidir. Hangi kaynaklardan (script, style, image) içerik yüklenebileceğini kontrol eder.

**Amaç:** XSS (Cross-Site Scripting) saldırılarını önlemek.

```java
@GetMapping("/")
public String index(HttpServletResponse response) {
    response.setHeader("Content-Security-Policy", 
        "default-src 'self'; script-src 'self' https://cdn.example.com; style-src 'self' 'unsafe-inline'");
    return "index";
}
```

**Directive'ler:**
*   `default-src 'self'`: Varsayılan olarak sadece kendi domain'den.
*   `script-src 'self' https://cdn.example.com`: Script sadece bu kaynaklardan.
*   `img-src *`: Resimler her yerden yüklenebilir.
*   `'unsafe-inline'`: Inline script/style'a izin ver (önerilmez).

---

### 5. API Security Best Practices

#### 1. Authentication & Authorization

**JWT Token:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**API Key:**
```http
X-API-Key: your-secret-api-key
```

**OAuth2:** Üçüncü parti uygulamalar için (Google, GitHub login).

#### 2. Rate Limiting

```java
@GetMapping("/api/search")
@RateLimiter(name = "searchApi", fallbackMethod = "rateLimitFallback")
public List<Result> search(@RequestParam String query) {
    // ...
}

public List<Result> rateLimitFallback(String query, Exception ex) {
    throw new TooManyRequestsException("Rate limit exceeded. Try again later.");
}
```

#### 3. Input Validation

```java
@PostMapping("/api/users")
public User createUser(@Valid @RequestBody UserDTO userDTO) {
    // @Valid otomatik validasyon yapar
}

public class UserDTO {
    @NotBlank
    @Email
    private String email;
    
    @Size(min = 8, max = 50)
    private String password;
    
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String username;
}
```

#### 4. HTTPS Only

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        return http.build();
    }
}
```

#### 5. API Versioning

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    // Eski API
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    // Yeni API (breaking changes)
}
```

---

### 6. Kritik Mülakat Soruları 

#### Soru 1: HTTPS'te Man-in-the-Middle (MITM) saldırısı nasıl önlenir?
**Cevap:** TLS sertifikası, CA (Certificate Authority) tarafından imzalanır. Browser, sertifikayı CA'nın public key'i ile doğrular. Sahte sertifika kullanılamaz.

**Ek Koruma:** Certificate Pinning (mobil uygulamalarda).

#### Soru 2: CSRF (Cross-Site Request Forgery) nedir? Nasıl önlenir?
**Cevap:** Kötü niyetli bir site, kullanıcının oturumunu kullanarak istemediği bir işlem yaptırır.

**Örnek:** Kullanıcı bankaya login olmuş. Kötü site bir form gönderir: `POST /transfer?to=attacker&amount=1000`.

**Çözüm:**
*   **CSRF Token:** Her form/istek için benzersiz token.
*   **SameSite Cookie:** Cookie sadece aynı site'tan gelen isteklerde gönderilir.

```java
http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

#### Soru 3: XSS (Cross-Site Scripting) nedir? Nasıl önlenir?
**Cevap:** Kullanıcı girdisi HTML'e enjekte edilir, kötü amaçlı JavaScript çalışır.

**Örnek:**
```html
<div>Hoşgeldin, <%= username %></div>
<!-- username = "<script>alert('XSS')</script>" -->
```

**Çözüm:**
*   Tüm kullanıcı girdilerini **escape** edin (Thymeleaf otomatik yapar).
*   CSP kullanın.
*   `HttpOnly` cookie kullanın (JavaScript erişemez).

#### Soru 4: API'de idempotency nedir? Neden önemlidir?
**Cevap:** Aynı isteğin birden fazla kez gönderilmesi, aynı sonucu üretmelidir.
*   **GET, PUT, DELETE:** Idempotent olmalıdır.
*   **POST:** Genellikle idempotent değildir (her istek yeni kaynak oluşturur).
*   **Önem:** Network hatası durumunda client isteği güvenle tekrar gönderebilmelidir (Retry logic).

#### Soru 5 (Tricky): `HSTS` (HTTP Strict Transport Security) nedir?
**Cevap:** Sunucunun tarayıcıya "Benimle sadece HTTPS ile konuş, asla HTTP kullanma" demesidir.
*   **Header:** `Strict-Transport-Security: max-age=31536000; includeSubDomains`
*   **Amaç:** SSL Stripping saldırılarını önler. İlk istekten sonra tarayıcı HTTP isteğini otomatik HTTPS'e çevirir.

#### Soru 6 (Tricky): JWT "None" algoritması saldırısı nedir?
**Cevap:** Bazı JWT kütüphaneleri, header'da `alg: none` gönderilirse imza doğrulamasını atlar.
*   **Saldırı:** Saldırgan token'ı alır, payload'ı değiştirir (admin yapar), header'ı `none` yapar ve imzayı siler. Sunucu bunu kabul ederse yetki yükseltilmiş olur.
*   **Çözüm:** Kütüphanenizi güncelleyin ve `none` algoritmasını sunucuda açıkça reddedin.

---

**Örnek:** `POST /api/orders` → Sipariş oluşturulur. Ağ hatası nedeniyle istek tekrar gönderilirse, iki sipariş oluşmamalı.

**Çözüm:** Idempotency Key kullanın:
```http
POST /api/orders
Idempotency-Key: unique-request-id-123
```

Sunucu, aynı key ile gelen istekleri tespit eder ve tekrar işlemez.

#### Soru 5: API Gateway'de hangi güvenlik önlemleri alınmalıdır?
**Cevap:**
*   **Authentication/Authorization:** JWT doğrulama.
*   **Rate Limiting:** DDoS koruması.
*   **IP Whitelisting/Blacklisting:** Belirli IP'leri engelle.
*   **Request/Response Validation:** Şüpheli istekleri filtrele.
*   **Logging & Monitoring:** Tüm istekleri logla, anomali tespiti.

---

### 7. Geliştirici İpuçları

*   **Security Headers:** Tüm response'lara güvenlik header'ları ekleyin:
    ```java
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("X-XSS-Protection", "1; mode=block");
    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    ```

*   **Least Privilege Principle:** Kullanıcılara minimum gerekli yetkileri verin. Admin yetkisi gereksiz yere vermeyin.

*   **Defense in Depth:** Tek bir güvenlik katmanına güvenmeyin. Çok katmanlı güvenlik (firewall, WAF, authentication, authorization, encryption).

*   **Security Audits:** Düzenli penetrasyon testleri yaptırın. OWASP ZAP, Burp Suite gibi araçlar kullanın.

*   **Secrets Management:** API key, DB şifrelerini kodda tutmayın. HashiCorp Vault, AWS Secrets Manager kullanın.

*   **API Documentation:** Swagger/OpenAPI ile API dokümante edin. Güvenlik gereksinimlerini (authentication, rate limits) belirtin.

---

