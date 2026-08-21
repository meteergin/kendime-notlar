## Konu 13: Spring Security, Authentication & Authorization, OAuth2, JWT

Spring Security, kurumsal Java uygulamalarının güvenlik katmanıdır. Bir geliştirici olarak, Authentication (kimlik doğrulama) ve Authorization (yetkilendirme) arasındaki farkı bilmeli, OAuth2 ve JWT'nin ne zaman ve nasıl kullanılacağını anlamalı ve güvenlik açıklarına (CSRF, XSS, SQL Injection) karşı nasıl korunacağınızı bilmelisiniz.

---

### 1. Authentication vs Authorization

**Analoji:**
*   **Authentication (Kimlik Doğrulama):** Bir binaya girerken kimliğinizi gösterirsiniz. "Sen kimsin?" sorusuna cevaptır.
*   **Authorization (Yetkilendirme):** Kimliğiniz doğrulandıktan sonra hangi odalara girebileceğiniz kontrol edilir. "Ne yapmana izin var?" sorusuna cevaptır.

| Kavram | Açıklama | Örnek |
| :--- | :--- | :--- |
| **Authentication** | Kullanıcının kim olduğunu doğrulama | Username/Password, OAuth2, JWT |
| **Authorization** | Kullanıcının ne yapabileceğini belirleme | ROLE_ADMIN, ROLE_USER, Permission-based |

---

### 2. Spring Security Mimarisi

Spring Security, **Filter Chain** (Filtre Zinciri) üzerine kurulmuştur. Her HTTP isteği bir dizi güvenlik filtresinden geçer.

#### Temel Bileşenler

1.  **SecurityFilterChain:** Güvenlik filtrelerinin sıralı listesi.
2.  **AuthenticationManager:** Kimlik doğrulama işlemini yönetir.
3.  **UserDetailsService:** Kullanıcı bilgilerini (username, password, roles) yükler.
4.  **PasswordEncoder:** Şifreleri hash'ler (BCrypt, Argon2).
5.  **SecurityContext:** Kimliği doğrulanmış kullanıcı bilgisini tutar (ThreadLocal).

#### Basit Konfigürasyon (Spring Security 6+)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API için CSRF kapatılabilir
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // Basic Auth
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### 3. Authentication Mekanizmaları

#### 1. Form-Based Login (Session-Based)
Geleneksel web uygulamaları için. Kullanıcı login olur, sunucu bir **Session** oluşturur ve **JSESSIONID** cookie'si döner.

**Avantajları:**
*   Basit ve olgun.
*   Sunucu tarafında session yönetimi (logout kolay).

**Dezavantajları:**
*   Stateful (Sunucu session bilgisini saklar, ölçeklenebilirlik sorunu).
*   Mikroservis mimarilerinde uygun değil.

#### 2. HTTP Basic Authentication
Her istekte `Authorization: Basic base64(username:password)` header'ı gönderilir.

**Kullanım:** Sadece HTTPS ile kullanılmalı (şifre base64 ile encode edilir ama şifrelenmez).

#### 3. JWT (JSON Web Token) Authentication
Modern, **stateless** kimlik doğrulama yöntemi. Sunucu session tutmaz, tüm bilgi token içindedir.

**JWT Yapısı:** `Header.Payload.Signature`
*   **Header:** Token tipi ve algoritma (HS256, RS256).
*   **Payload:** Kullanıcı bilgileri (claims): `sub` (subject/username), `exp` (expiration), `roles`.
*   **Signature:** Header + Payload'ın secret key ile imzalanmış hali (değiştirilmediğini garanti eder).

```java
// JWT Oluşturma (io.jsonwebtoken:jjwt)
String jwt = Jwts.builder()
    .setSubject(username)
    .claim("roles", roles)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 gün
    .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
    .compact();

// JWT Doğrulama
Claims claims = Jwts.parser()
    .setSigningKey(SECRET_KEY)
    .parseClaimsJws(jwt)
    .getBody();
```

**JWT Filter (Spring Security):**
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

### 4. OAuth2 ve OpenID Connect

**Analoji:** OAuth2, bir uygulamanın (örn: Bir fotoğraf baskı servisi) sizin adınıza başka bir servise (Google Photos) erişmesine izin vermenizdir. Şifrenizi paylaşmadan, sadece "Fotoğraflarımı görebilir" yetkisi verirsiniz.

#### OAuth2 Rolleri
1.  **Resource Owner:** Kullanıcı (veriyi sahiplenen).
2.  **Client:** Uygulamanız (veriye erişmek isteyen).
3.  **Authorization Server:** Kimlik doğrulama yapan sunucu (Google, GitHub, Keycloak).
4.  **Resource Server:** Korunan veriyi tutan sunucu (API).

#### OAuth2 Grant Types (Akış Türleri)

| Grant Type | Kullanım | Güvenlik |
| :--- | :--- | :--- |
| **Authorization Code** | Web uygulamaları (Backend var) | En güvenli (Client Secret kullanır) |
| **Implicit** (Deprecated) | SPA (Tek sayfa uygulamaları) | Güvensiz (Token URL'de görünür) |
| **Password** | Güvenilir uygulamalar (Kendi login sayfanız) | Orta (Kullanıcı şifresini görür) |
| **Client Credentials** | Makine-makine iletişimi (Mikroservisler arası) | Yüksek (Kullanıcı yok) |
| **PKCE (Authorization Code + PKCE)** | Mobil/SPA uygulamaları | Çok güvenli (Client Secret gerekmez) |

#### Spring Security OAuth2 Client (Google Login Örneği)

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
            scope: profile, email
```

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .oauth2Login(Customizer.withDefaults()); // OAuth2 Login aktif
        
        return http.build();
    }
}
```

#### OpenID Connect (OIDC)
OAuth2'nin üzerine kurulmuş bir kimlik katmanıdır. OAuth2 sadece **yetkilendirme** yapar, OIDC ise **kimlik doğrulama** da yapar.
*   OAuth2: "Bu uygulama fotoğraflarıma erişebilir."
*   OIDC: "Bu uygulama benim kim olduğumu bilir (isim, email)."

**ID Token:** OIDC'de JWT formatında kullanıcı bilgilerini içeren token.

---

### 5. Güvenlik Best Practices

#### 1. CSRF (Cross-Site Request Forgery) Koruması
**Sorun:** Kötü niyetli bir site, kullanıcının oturumunu kullanarak istemediği bir işlem yaptırabilir.
*   **Çözüm:** Spring Security varsayılan olarak CSRF koruması aktiftir. Her form POST isteğinde bir CSRF token gönderilir.
*   **REST API:** Stateless (JWT) kullanıyorsanız CSRF korumasını kapatabilirsiniz (`csrf().disable()`).

#### 2. XSS (Cross-Site Scripting) Koruması
**Sorun:** Kullanıcı girdisi HTML'e enjekte edilirse, kötü amaçlı JavaScript çalışabilir.
*   **Çözüm:** Tüm kullanıcı girdilerini escape edin (Thymeleaf otomatik yapar). `Content-Security-Policy` header'ı kullanın.

#### 3. SQL Injection Koruması
**Sorun:** Kullanıcı girdisi SQL sorgusuna direkt eklenir.
*   **Çözüm:** Asla string concatenation kullanmayın. **Prepared Statements** (JPA/Hibernate otomatik kullanır) veya **Named Parameters** kullanın.

#### 4. Şifre Saklama
*   **Asla plain text** saklamayın.
*   **BCrypt** veya **Argon2** kullanın (SHA-256 yeterli değildir, çok hızlı kırılır).
*   **Salt** kullanın (BCrypt otomatik ekler).

---

### 6. Kritik Mülakat Soruları 

#### Soru 1: JWT'nin avantajları ve dezavantajları nelerdir?
**Cevap:**

**Avantajları:**
*   **Stateless:** Sunucu session tutmaz, yatay ölçeklenebilir.
*   **Mikroservisler:** Token tüm servislerde geçerlidir.
*   **Mobile-friendly:** Cookie gerekmez.

**Dezavantajları:**
*   **Token iptali zor:** Logout yapıldığında token hala geçerlidir (expiration süresine kadar). Çözüm: Blacklist veya kısa expiration + Refresh Token.
*   **Token boyutu:** Cookie'den büyüktür (her istekte gönderilir).
*   **Güvenlik:** Secret key sızdırılırsa tüm tokenlar tehlikededir.

#### Soru 2: Access Token vs Refresh Token farkı nedir?
**Cevap:**
*   **Access Token:** Kısa ömürlüdür (15 dakika). API isteklerinde kullanılır.
*   **Refresh Token:** Uzun ömürlüdür (7 gün, 30 gün). Access token süresi dolduğunda yeni access token almak için kullanılır. Sadece `/refresh` endpoint'ine gönderilir, diğer API isteklerinde kullanılmaz.

**Amaç:** Access token çalınırsa kısa sürede geçersiz olur. Refresh token daha güvenli saklanır (HttpOnly cookie).

#### Soru 3: `@PreAuthorize` ve `@Secured` farkı nedir?
**Cevap:**
*   **`@Secured("ROLE_ADMIN")`:** Basit rol kontrolü. SpEL desteklemez.
*   **`@PreAuthorize("hasRole('ADMIN') and #username == authentication.name")`:** SpEL (Spring Expression Language) destekler. Karmaşık koşullar yazılabilir (metot parametrelerine erişim).

**Kullanım:** `@PreAuthorize` daha esnektir, tercih edilir.

#### Soru 4: OAuth2'de Authorization Code Flow neden Implicit Flow'dan daha güvenlidir?
**Cevap:**
*   **Implicit Flow:** Access token doğrudan URL fragment'inde (`#access_token=...`) döner. Browser history'de kalabilir, JavaScript ile erişilebilir.
*   **Authorization Code Flow:** Önce bir **code** döner (kısa ömürlü). Bu code, backend'den **Client Secret** ile birlikte token endpoint'ine gönderilir ve token alınır. Client Secret frontend'de asla görünmez.

**Sonuç:** Authorization Code + PKCE, modern uygulamalar için standart olmuştur.

#### Soru 5: Spring Security'de `SecurityContext` nasıl çalışır? Thread-safe midir?
**Cevap:** `SecurityContext`, kimliği doğrulanmış kullanıcı bilgisini tutar ve **ThreadLocal** kullanır. Her thread'in kendi SecurityContext'i vardır, bu yüzden thread-safe'tir.
*   **Sorun:** Asenkron metodlarda (`@Async`) veya yeni thread'lerde SecurityContext kaybolur.
*   **Çözüm:** `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)` veya manuel olarak context'i yeni thread'e kopyalayın.

#### Soru 6 (Tricky): CSRF Token nerede saklanmalıdır? Cookie vs LocalStorage?
**Cevap:**
*   **Cookie (HttpOnly):** XSS saldırılarına karşı güvenlidir ama CSRF saldırılarına açıktır (zaten CSRF token bunun için var).
*   **LocalStorage:** XSS saldırılarına karşı savunmasızdır (JS okuyabilir).
*   **Best Practice:** Refresh Token `HttpOnly Cookie`'de, Access Token memory'de tutulmalı. CSRF token ise header'da gönderilmeli.

#### Soru 7 (Tricky): BCrypt hash'i her seferinde neden farklı çıkar? Nasıl doğrulanır?
**Cevap:** BCrypt her hash işleminde rastgele bir **Salt** üretir ve bunu hash'in içine gömer.
*   Aynı şifre ("123456") her seferinde farklı hash üretir.
*   Doğrulama (`matches`) yapılırken, hash içindeki salt çıkarılır ve girilen şifre o salt ile tekrar hash'lenip karşılaştırılır.

---

### 7. Geliştirici İpuçları

*   **JWT Secret Key Yönetimi:** Secret key'i asla kodda tutmayın. Environment variable veya HashiCorp Vault kullanın. Üretimde RS256 (Asymmetric) kullanmayı düşünün (Private key sunucuda, Public key tüm servislerde).

*   **CORS Yapılandırması:** Frontend farklı domain'de ise (örn: `localhost:3000`), Spring Security'de CORS'u doğru yapılandırın:
    ```java
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
    ```

*   **Rate Limiting:** Brute-force saldırılarına karşı login endpoint'ine rate limiting ekleyin (Bucket4j, Resilience4j).

*   **Audit Logging:** Kimlik doğrulama başarısızlıklarını, yetkilendirme ihlallerini loglayın. SIEM sistemlerine gönderin.

*   **Token Rotation:** Refresh token kullanıldığında hem yeni access token hem de yeni refresh token verin (eski refresh token'ı geçersiz kılın). Bu, token çalınma riskini azaltır.

*   **Method Security:** Controller seviyesinde değil, Service seviyesinde `@PreAuthorize` kullanın. Böylece iş mantığı her zaman korunur (farklı bir endpoint'ten çağrılsa bile).

---

