## Konu 10: Integration Testing, REST Assured ve JMeter

Integration testing ve performans testleri, modern yazılım geliştirme süreçlerinin kritik bileşenleridir. Bir geliştirici olarak, API testlerini otomatize etmek (REST Assured), performans darboğazlarını tespit etmek (JMeter) ve sistemin tüm katmanlarının birlikte doğru çalıştığını garanti etmek için derin bilgi ve deneyime sahip olmalısınız.

---

### 1. Integration Testing (Entegrasyon Testi)

**Analoji:** Integration testing, bir orkestradır. Her müzisyen (unit) kendi enstrümanını mükemmel çalabilir ama orkestra olarak uyumlu mu? Diğer enstrümanlarla uyum içinde mi?

#### Unit Test vs Integration Test

| Özellik | Unit Test | Integration Test |
| :--- | :--- | :--- |
| **Kapsam** | Tek bir sınıf/metot | Birden fazla component/layer |
| **Bağımlılıklar** | Mock/Stub | Gerçek bağımlılıklar (DB, External API) |
| **Hız** | Çok hızlı (ms) | Yavaş (saniyeler) |
| **Amaç** | Kod doğruluğu | Entegrasyon doğruluğu |
| **Test Sayısı** | Çok fazla | Orta seviye |

#### Spring Boot ile Integration Test

Spring Boot, `@SpringBootTest` anotasyonu ile tüm application context'i yükleyerek tam entegrasyon testleri yazmayı kolaylaştırır.

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase // H2 gibi embedded DB kullanır
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate; // HTTP istek göndermek için
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }
    
    @Test
    void testCreateUser() {
        // Arrange
        UserDTO newUser = new UserDTO("Ali", "ali@example.com");
        
        // Act
        ResponseEntity<UserDTO> response = restTemplate.postForEntity(
            "/api/users", 
            newUser, 
            UserDTO.class
        );
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("Ali", response.getBody().getName());
        
        // Verify database
        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
    }
    
    @Test
    void testGetUserById() {
        // Database'e test verisi ekle
        User savedUser = userRepository.save(new User("Ece", "Ece@example.com"));
        
        // HTTP GET isteği
        ResponseEntity<UserDTO> response = restTemplate.getForEntity(
            "/api/users/" + savedUser.getId(), 
            UserDTO.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Ece", response.getBody().getName());
    }
}
```

**Önemli Anotasyonlar:**
*   `@SpringBootTest`: Application context'i tamamen yükler.
*   `@WebMvcTest(UserController.class)`: Sadece web layer'ı test eder (Service mock'lanır).
*   `@DataJpaTest`: Sadece JPA repository'leri test eder (H2 embedded DB ile).
*   `@AutoConfigureMockMvc`: MockMvc kullanarak controller'ları test eder (sunucu başlatmaz).

---

### 2. REST Assured (API Test Otomasyonu)

**Analoji:** REST Assured, API'leri konuşan bir dil gibidir. Postman'daki manuel işlemleri kod olarak yazıp otomatize eder.

REST Assured, RESTful API'leri test etmek için tasarlanmış, **BDD tarzı** (Given-When-Then) bir Java kütüphanesidir.

#### Temel Kullanım

```java
import io.restassured.RestAssured;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

class ApiTest {
    
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://api.example.com";
        RestAssured.basePath = "/v1";
    }
    
    @Test
    void testGetAllUsers() {
        given()
            .header("Authorization", "Bearer token123")
        .when()
            .get("/users")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].name", notNullValue())
            .time(lessThan(2000L)); // Response time < 2 saniye
    }
    
    @Test
    void testCreateUser() {
        String requestBody = """
            {
                "name": "Ahmet",
                "email": "ahmet@example.com"
            }
            """;
        
        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Ahmet"));
    }
    
    @Test
    void testUpdateUser() {
        int userId = 123;
        
        given()
            .contentType("application/json")
            .body("{ \"name\": \"Mehmet\" }")
        .when()
            .put("/users/" + userId)
        .then()
            .statusCode(200)
            .body("name", equalTo("Mehmet"));
    }
    
    @Test
    void testDeleteUser() {
        given()
        .when()
            .delete("/users/123")
        .then()
            .statusCode(204);
    }
}
```

#### İleri Düzey Özellikler

**1. JSON Path ile Derin Doğrulama:**
```java
given()
.when()
    .get("/users")
.then()
    .body("users.findAll { it.age > 18 }.name", hasItems("Ali", "Ece"))
    .body("users[0].address.city", equalTo("Istanbul"));
```

**2. Response'u Yakalama ve Kullanma:**
```java
Response response = given()
    .when()
    .post("/users")
    .then()
    .extract().response();

int userId = response.jsonPath().getInt("id");
String name = response.jsonPath().getString("name");
```

**3. Request/Response Logging:**
```java
given()
    .log().all() // Tüm request detaylarını logla
.when()
    .get("/users")
.then()
    .log().body() // Sadece response body'yi logla
    .statusCode(200);
```

**4. Schema Validation (JSON Schema):**
```java
given()
.when()
    .get("/users")
.then()
    .assertThat()
    .body(matchesJsonSchemaInClasspath("user-schema.json"));
```

**Maven Dependency:**
```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.3.2</version>
    <scope>test</scope>
</dependency>
```

---

### 3. JMeter (Performans ve Yük Testi)

**Analoji:** JMeter, binlerce kullanıcıyı simüle eden bir "stres testi robotu"dur. Sisteminizin kaç kişiyi kaldırabileceğini, hangi noktada çökeceğini test eder.

#### JMeter Nedir?
Apache JMeter, **performans**, **yük** ve **stres** testleri yapmak için kullanılan açık kaynak bir araçtır. Web servisleri, veritabanları, FTP ve daha fazlasını test edebilir.

#### Temel Kavramlar

1.  **Thread Group:** Sanal kullanıcıları (threads) temsil eder.
    *   **Number of Threads:** Kaç kullanıcı simüle edilecek (örn: 100).
    *   **Ramp-Up Period:** Kullanıcılar kaç saniyede başlatılacak (örn: 10 saniye → 10 kullanıcı/saniye).
    *   **Loop Count:** Her kullanıcı testi kaç kez tekrarlayacak.

2.  **Samplers:** Gerçek istekleri gönderen bileşenler (HTTP Request, JDBC Request vb.).

3.  **Listeners:** Sonuçları görüntüleyen ve raporlayan bileşenler (View Results Tree, Summary Report).

4.  **Assertions:** Response'u doğrulayan kontroller (Response Code, Response Time).

5.  **Timers:** İstekler arasında bekleme süresi (Constant Timer, Gaussian Random Timer).

#### Basit HTTP Test Planı

```
Test Plan
└── Thread Group (100 users, 20 sec ramp-up, 10 loops)
    ├── HTTP Request Defaults (Base URL: https://api.example.com)
    ├── HTTP Header Manager (Content-Type: application/json)
    ├── HTTP Request Sampler (GET /api/users)
    ├── Response Assertion (Status Code = 200)
    ├── Duration Assertion (Response Time < 500ms)
    ├── Constant Timer (1000ms pause between requests)
    └── Listeners
        ├── View Results Tree
        ├── Summary Report
        └── Aggregate Report
```

#### CLI ile JMeter Çalıştırma (CI/CD İçin)

GUI yerine komut satırı ile çalıştırmak daha performanslıdır:
```bash
jmeter -n -t test-plan.jmx -l results.jtl -e -o ./report
```
*   `-n`: Non-GUI mod
*   `-t`: Test plan dosyası
*   `-l`: Sonuç dosyası
*   `-e -o`: HTML rapor oluştur

#### Performans Metrikleri

| Metrik | Açıklama | İdeal Değer |
| :--- | :--- | :--- |
| **Response Time (Avg)** | Ortalama yanıt süresi | < 500ms (Web), < 100ms (API) |
| **Throughput** | Saniyede işlenen istek sayısı (req/sec) | Yüksek olmalı |
| **Error Rate** | Hatalı istek yüzdesi | < %1 |
| **90th Percentile** | İsteklerin %90'ı bu süreden kısa | < 1 saniye |

---

### 4. Kritik Mülakat Soruları 

#### Soru 1: `@SpringBootTest` ile `@WebMvcTest` farkı nedir?
**Cevap:**
*   **`@SpringBootTest`:** Tüm application context'i yükler. Gerçek entegrasyon testi yapar. Yavaştır.
*   **`@WebMvcTest`:** Sadece web layer'ı yükler (Controller). Service ve Repository mock'lanır. Hızlıdır, controller testi için idealdir.

#### Soru 2: REST Assured'da `given()`, `when()`, `then()` ne anlama gelir?
**Cevap:** **BDD (Behavior-Driven Development)** tarzı yazım:
*   **`given()`:** Test ön koşulları (headers, auth, request body).
*   **`when()`:** Test eylemi (HTTP metodu ve endpoint).
*   **`then()`:** Doğrulama (status code, response body).

Bu yapı testleri çok okunabilir kılar.

#### Soru 3: JMeter'da "Ramp-Up Period" nedir?
**Cevap:** Tüm thread'lerin (sanal kullanıcıların) kaç saniyede başlatılacağını belirtir.
*   **Örnek:** 100 thread, 10 saniye ramp-up → Her saniye 10 kullanıcı başlatılır.
*   **Amaç:** Ani yük (spike) yerine kademeli yük artışı simüle etmek (daha gerçekçi).

#### Soru 4: Performans testi ile yük testi (load test) farkı nedir?
**Cevap:**
*   **Performans Testi:** Sistemin belirli bir yük altındaki davranışını ölçmek (response time, throughput).
*   **Yük Testi (Load Test):** Sistemi beklenen maksimum yük altında test etmek (örn: Black Friday trafiği).
*   **Stres Testi (Stress Test):** Sistemi çökme noktasına kadar zorlamak ve hangi noktada başarısız olduğunu bulmak.

#### Soru 5: Test piramidinde Integration Test'in yeri nedir?
**Cevap:**
```
        /\
       /E2E\       (Az sayıda, yavaş, pahalı)
      /------\
     /  INT  \     (Orta seviye, dengeli)
    /--------\
   /   UNIT   \    (Çok sayıda, hızlı, ucuz)
  /____________\
```
*   **Unit:** %70
*   **Integration:** %20
*   **E2E:** %10

Integration testler, unit testlerden daha yavaş ama E2E'den daha hızlıdır. Dengeli bir test stratejisi için gereklidir.

#### Soru 6 (Tricky): `@Transactional` anotasyonu testlerde nasıl davranır?
**Cevap:** Test sınıfında veya metodunda `@Transactional` kullanırsanız, test bittiğinde transaction otomatik olarak **ROLLBACK** edilir (geri alınır).
*   **Neden:** Veritabanını kirletmemek için.
*   **Trap:** "Commit edilir" sanılabilir. Eğer commit edilmesini istiyorsanız `@Commit` veya `@Rollback(false)` kullanmalısınız.

#### Soru 7 (Tricky): `MockMvc` ile `TestRestTemplate` farkı nedir?
**Cevap:**
*   **`MockMvc`:** Sunucuyu başlatmaz (serverless). DispatcherServlet'i mock'lar. Çok hızlıdır.
*   **`TestRestTemplate`:** Gerçek bir HTTP isteği gönderir (sunucu çalışmalıdır). Daha yavaştır ama tam entegrasyon testidir.

---

### 5. Geliştirici İpuçları

*   **Testcontainers:** Docker container'ları kullanarak gerçek veritabanı (PostgreSQL, MongoDB) ile integration test yapın. Test bitince container otomatik silinir.
    ```java
    @Testcontainers
    class IntegrationTest {
        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    }
    ```

*   **REST Assured Filters:** Request/Response manipülasyonu için filtreler kullanın (örn: tüm isteklere otomatik auth token eklemek).

*   **JMeter Distributed Testing:** Çok yüksek yük testi için birden fazla JMeter slave makinesi kullanarak distributed (dağıtık) test yapın.

*   **Synthetic Monitoring:** Production'da JMeter scriptlerini periyodik olarak çalıştırarak (her 5 dakikada bir) sistemin sağlığını izleyin.

*   **Contract Testing:** Mikroservis mimarisinde API contract'larını test edin (Pact, Spring Cloud Contract). Consumer ve provider arasındaki contract'ın bozulmadığını garanti edin.

*   **Performance Baseline:** İlk başarılı performans testinin metriklerini baseline (referans) olarak kaydedin. Sonraki testlerde regresyon olup olmadığını bu baseline ile karşılaştırın.

---

