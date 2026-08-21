## Konu 11: Behavior-Driven Development (BDD), Cucumber ve Advanced Mockito

Behavior-Driven Development (BDD), yazılım geliştirme sürecinde teknik olmayan paydaşlarla (Product Owner, Business Analist) ortak bir dil kullanarak testlerin yazılmasını sağlayan bir yaklaşımdır. Bir geliştirici olarak, Cucumber ile BDD testleri yazmayı, Mockito'nun ileri düzey özelliklerini kullanmayı ve bu araçların mikroservis mimarilerinde nasıl kullanılacağını bilmelisiniz.

---

### 1. Behavior-Driven Development (BDD) Nedir?

**Analoji:** BDD, bir "senaryo filmi" yazmak gibidir. Teknik detaylara girmeden, kullanıcının ne yapmak istediğini (behavior) hikaye tarzında anlatırsınız. Bu hikaye hem iş insanı hem de geliştirici tarafından anlaşılır.

#### TDD vs BDD

| Özellik | TDD (Test-Driven Development) | BDD (Behavior-Driven Development) |
| :--- | :--- | :--- |
| **Odak** | Kodun doğruluğu (How) | Davranışın doğruluğu (What) |
| **Dil** | Teknik (assert, expected, actual) | Doğal dil (Given-When-Then) |
| **Test Yazanlar** | Geliştiriciler | Geliştiriciler + Product Owner + QA |
| **Amaç** | Kod tasarımı iyileştirmek | İş gereksinimlerini doğrulamak |

**BDD'nin 3 Temel Prensibi:**
1.  **Given (Verilen):** Başlangıç durumu / Ön koşullar.
2.  **When (Ne zaman):** Bir eylem gerçekleştiğinde.
3.  **Then (O zaman):** Beklenen sonuç.

---

### 2. Cucumber (BDD Framework)

Cucumber, BDD testlerini **Gherkin** dili ile yazmayı sağlayan bir framework'tür. Gherkin, doğal dil (İngilizce, Türkçe vb.) kullanarak test senaryolarını tanımlar.

#### Gherkin Sözdizimi

**Feature Dosyası (`login.feature`):**
```gherkin
# language: tr
Özellik: Kullanıcı Girişi
  Bir kullanıcı olarak, sisteme giriş yapabilmeliyim
  
  Senaryo: Başarılı kullanıcı girişi
    Diyelim ki kullanıcı login sayfasında
    Ve kullanıcı adı "test@example.com"
    Ve şifre "12345"
    Eğer ki kullanıcı "Giriş Yap" butonuna tıklarsa
    O zaman kullanıcı ana sayfaya yönlendirilmeli
    Ve hoşgeldin mesajı görüntülenmeli

  Senaryo: Hatalı şifre ile giriş
    Diyelim ki kullanıcı login sayfasında
    Eğer ki kullanıcı yanlış şifre girererse
    O zaman "Geçersiz şifre" hata mesajı görüntülenmeli
```

**İngilizce Versiyonu:**
```gherkin
Feature: User Login
  As a user, I want to login to the system
  
  Scenario: Successful login
    Given user is on login page
    And username is "test@example.com"
    And password is "12345"
    When user clicks "Login" button
    Then user should be redirected to home page
    And welcome message should be displayed
```

#### Step Definitions (Adım Tanımları)

Gherkin dosyasındaki her adım, bir Java metoduna eşlenir:

```java
import io.cucumber.java.en.*;
import static org.junit.jupiter.api.Assertions.*;

public class LoginSteps {
    
    private LoginPage loginPage;
    private String username;
    private String password;
    private HomePage homePage;
    
    @Given("user is on login page")
    public void userIsOnLoginPage() {
        loginPage = new LoginPage();
        loginPage.navigate();
    }
    
    @And("username is {string}")
    public void usernameIs(String user) {
        this.username = user;
    }
    
    @And("password is {string}")
    public void passwordIs(String pass) {
        this.password = pass;
    }
    
    @When("user clicks {string} button")
    public void userClicksButton(String buttonName) {
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        homePage = loginPage.clickLogin();
    }
    
    @Then("user should be redirected to home page")
    public void userShouldBeRedirectedToHomePage() {
        assertTrue(homePage.isDisplayed());
    }
    
    @And("welcome message should be displayed")
    public void welcomeMessageShouldBeDisplayed() {
        assertTrue(homePage.getWelcomeMessage().contains("Hoşgeldiniz"));
    }
}
```

#### Scenario Outline (Veri Güdümlü Testler)

Aynı senaryoyu farklı verilerle çalıştırmak için:

```gherkin
Senaryo Taslağı: Farklı kullanıcılarla giriş
  Diyelim ki kullanıcı login sayfasında
  Eğer ki kullanıcı adı "<username>" ve şifre "<password>" girilirse
  O zaman durum "<result>" olmalı

  Örnekler:
    | username          | password | result  |
    | test@example.com  | 12345    | success |
    | admin@example.com | admin123 | success |
    | wrong@example.com | wrong    | error   |
```

**Maven Dependency:**
```xml
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
</dependency>
```

---

### 3. Advanced Mockito (İleri Düzey)

Mockito'nun temellerini Unit Testing bölümünde ele aldık. Şimdi daha ileri seviye kullanımlarına bakalım.

#### 1. ArgumentCaptor (Argüman Yakalama)

Mock metoduna geçilen argümanları yakalayıp doğrulama yapmak için:

```java
@Test
void testEmailSending() {
    // Arrange
    @Captor
    ArgumentCaptor<Email> emailCaptor;
    MockitoAnnotations.openMocks(this);
    
    // Act
    userService.registerUser("test@example.com");
    
    // Argümanı yakala
    verify(emailService).sendEmail(emailCaptor.capture());
    
    // Yakalanan argümanı doğrula
    Email sentEmail = emailCaptor.getValue();
    assertEquals("test@example.com", sentEmail.getTo());
    assertTrue(sentEmail.getSubject().contains("Hoşgeldiniz"));
}
```

#### 2. Answer Interface (Dinamik Davranış)

Metodun çağrılma anında dinamik cevap üretmek için:

```java
when(calculator.add(anyInt(), anyInt())).thenAnswer(invocation -> {
    int a = invocation.getArgument(0);
    int b = invocation.getArgument(1);
    return a + b; // Gerçek işlem yapılıyor
});
```

#### 3. InOrder (Metot Çağrı Sırası Doğrulama)

Metodların belirli bir sırada çağrıldığını doğrulamak için:

```java
@Test
void testOrderedOperations() {
    InOrder inOrder = inOrder(userRepository, emailService);
    
    userService.registerUser("test@example.com");
    
    // Önce database'e kayıt, sonra email gönderimi yapılmalı
    inOrder.verify(userRepository).save(any(User.class));
    inOrder.verify(emailService).sendEmail(any(Email.class));
}
```

#### 4. Spy vs Mock (Tekrar, Daha Detaylı)

```java
@Test
void demonstrateSpyVsMock() {
    // Mock: Tamamen sahte
    List<String> mockList = mock(ArrayList.class);
    mockList.add("item"); // Hiçbir şey yapmaz
    assertEquals(0, mockList.size()); // Mock'ta stub edilmemiş her şey default değer döner
    
    // Spy: Gerçek nesnenin "casuslanmış" hali
    List<String> spyList = spy(new ArrayList<>());
    spyList.add("item"); // GERÇEK add() çalışır
    assertEquals(1, spyList.size()); // Gerçek boyut
    
    // Spy'da istediğimiz metodu stub edebiliriz
    when(spyList.size()).thenReturn(100);
    assertEquals(100, spyList.size()); // size() mock'landı
}
```

#### 5. Mockito BDD Syntax (BDDMockito)

Mockito'yu BDD tarzı kullanmak için:

```java
import static org.mockito.BDDMockito.*;

@Test
void testWithBDDStyle() {
    // Given (Arrange)
    given(userRepository.findById(1)).willReturn(Optional.of(new User("Ali")));
    
    // When (Act)
    User user = userService.getUserById(1);
    
    // Then (Assert)
    then(userRepository).should().findById(1);
    assertEquals("Ali", user.getName());
}
```

#### 6. Mockito Annotations Summary

| Anotasyon | Açıklama | Kullanım |
| :--- | :--- | :--- |
| `@Mock` | Tamamen sahte nesne oluşturur | Bağımlılıkları mock'lamak için |
| `@Spy` | Gerçek nesneyi "casuslar" | Kısmi mock'lama için |
| `@InjectMocks` | Mock'ları bağımlılık olarak enjekte eder | Test edilen sınıfa mock'ları vermek için |
| `@Captor` | ArgumentCaptor oluşturur | Argümanları yakalamak için |

---

### 4. Cucumber + Spring Boot Integration

Spring Boot uygulamasını Cucumber ile test etmek:

**Cucumber Test Runner:**
```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberIntegrationTest {
}
```

**Spring Context ile Step Definitions:**
```java
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringCucumberConfig {
}
```

```java
public class UserSteps {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private ResponseEntity<UserDTO> response;
    
    @When("client creates user with name {string}")
    public void clientCreatesUser(String name) {
        UserDTO user = new UserDTO(name, "test@example.com");
        response = restTemplate.postForEntity("/api/users", user, UserDTO.class);
    }
    
    @Then("response code should be {int}")
    public void responseCodeShouldBe(int expectedCode) {
        assertEquals(expectedCode, response.getStatusCodeValue());
    }
}
```

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: BDD'nin TDD'ye göre avantajı nedir?
**Cevap:**
*   **Ortak Dil:** Business, QA ve Dev aynı dili konuşur. Miscommunication azalır.
*   **Living Documentation:** Feature dosyaları hem test hem de dokümandır. Güncel kalır.
*   **Erken Feedback:** İş gereksinimleri netleşmeden test yazılamaz, bu da eksik gereksinimleri erken ortaya çıkarır.

**Dezavantaj:** Ekstra abstraction layer (Gherkin → Step Definitions) bakım maliyeti yaratır.

#### Soru 2: Cucumber'da Background ne işe yarar?
**Cevap:** Her senaryoda tekrar eden "Given" adımlarını tek yerde toplamak için kullanılır.

```gherkin
Background:
  Given user is logged in
  And user has admin rights

Scenario: Create user
  When user creates a new user
  ...

Scenario: Delete user
  When user deletes a user
  ...
```
Her iki senaryoda da "Background" adımları önce çalışır.

#### Soru 3: Mockito'da `verify()` ile `times()` nasıl kullanılır?
**Cevap:**
```java
verify(userRepository).save(any(User.class)); // En az 1 kez çağrıldı mı?
verify(userRepository, times(1)).save(any(User.class)); // Tam 1 kez
verify(userRepository, times(2)).save(any(User.class)); // Tam 2 kez
verify(userRepository, never()).delete(any(User.class)); // Hiç çağrılmadı
verify(userRepository, atLeast(1)).findAll(); // En az 1 kez
verify(userRepository, atMost(3)).findAll(); // En fazla 3 kez
```

#### Soru 4: Cucumber'da Hooks nedir?
**Cevap:** Her senaryodan önce/sonra çalışan setup/teardown metodlarıdır.

```java
import io.cucumber.java.Before;
import io.cucumber.java.After;

public class Hooks {
    
    @Before // Her scenariodan önce
    public void setup() {
        // Test verisi hazırlama
    }
    
    @After // Her scenariodan sonra
    public void tearDown() {
        // Temizlik
    }
    
    @Before("@database") // Sadece @database tag'li senaryolarda
    public void setupDatabase() {
        // Database hazırlığı
    }
}
```

#### Soru 5 (Tricky): `Scenario Outline` ile `Data Table` farkı nedir?
**Cevap:**
*   **Scenario Outline:** Senaryoyu her veri satırı için **TEKRAR** çalıştırır (Loop).
*   **Data Table:** Senaryo **TEK** kere çalışır, veriler step definition içinde bir liste/map olarak işlenir.
*   **Trap:** "İkisi de tablo, aynıdır" yanlış. Çalışma mantığı tamamen farklıdır.

#### Soru 6 (Tricky): Cucumber testleri paralel çalıştırılabilir mi?
**Cevap:** Evet, JUnit 5 veya Maven Surefire plugin konfigürasyonu ile mümkündür.
*   **Dikkat:** Paralel çalışırken browser session'ları veya veritabanı verileri karışabilir. Thread-safe testler yazılmalıdır.

#### Soru 7: Mockito'da `doReturn()` ile `when().thenReturn()` farkı nedir?
**Cevap:**
*   **`when().thenReturn()`:** Spy kullanırken GERÇEK metodu çağırır (yan etki olabilir).
*   **`doReturn()`:** Spy kullanırken gerçek metodu ÇAĞIRMAZ (güvenlidir).

```java
List<String> spyList = spy(new ArrayList<>());

// Risk: get(0) gerçekten çağrılır, liste boşsa IndexOutOfBoundsException
when(spyList.get(0)).thenReturn("first");

// Güvenli: get(0) çağrılmaz
doReturn("first").when(spyList).get(0);
```

---

### 6. Geliştirici İpuçları

*   **Cucumber Report Plugin:** HTML raporları oluşturmak için `cucumber-reporting` Maven plugin'ini kullanın. CI/CD'de görselleştirilmiş test sonuçları görürsünüz.

*   **Gherkin Best Practices:**
    *   Feature dosyalarını kısa ve odaklı tutun (bir feature = bir özellik).
    *   "And" yerine "Given/When/Then" tercih edin (okunabilirlik artar).
    *   Implementation detaylarından kaçının (UI elementleri yazmayın, davranış yazın).

*   **Mockito ArgumentMatchers:**
    *   `any()` yerine mümkünse spesifik matcher kullanın: `eq(5)`, `contains("test")`.
    *   `anyInt()` varken `any()` kullanmayın (tip güvenliği için).

*   **Mockito Strict Stubbing:** Mockito 2.x ile kullanılmayan stub'lar (unused stubs) uyarı verir. `@MockitoSettings(strictness = Strictness.LENIENT)` ile gevşetebilirsiniz ama önerilmez.

*   **BDD + Contract Testing:** Mikroservislerde Cucumber ile acceptance testleri yazın ama API contract'larını Pact ile de doğrulayın. İkisi birbirini tamamlar.

*   **Parallel Cucumber Execution:** Büyük Cucumber test suite'leri varsa, Maven Surefire plugin ile paralel çalıştırın (`parallel=methods`). Test süresi dramatik azalır.

---

