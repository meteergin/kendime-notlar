## Konu 9: Unit Testing (JUnit & TestNG)

Unit testing, yazılım geliştirmenin ayrılmaz bir parçasıdır ve kod kalitesinin, bakımının ve güvenilirliğinin temelidir. Bir geliştirici olarak, sadece test yazmayı değil, **test-driven development (TDD)**, **mocking**, **test coverage** ve **test piramidi** gibi ileri düzey kavramları bilmeli ve uygulayabilmelisiniz.

---

### 1. Unit Testing Nedir?

**Analoji:** Unit test, bir araba üretim hattında her bir parçanın (motor, fren, direksiyon) tek başına test edilmesidir. Entegrasyon testi ise tüm parçaları birleştirip arabanın çalışıp çalışmadığını görmektir.

#### Temel Kavramlar
*   **Unit (Birim):** Test edilebilecek en küçük kod parçası (genellikle bir metot veya sınıf).
*   **Amaç:** Kodun beklendiği gibi çalıştığını doğrulamak, regresyonları (kodun bozulmasını) erken tespit etmek.
*   **Özellikleri:**
    *   **Hızlı:** Milisaniyeler içinde çalışmalı.
    *   **Bağımsız:** Diğer testlerden veya dış kaynaklardan (DB, network) etkilenmemeli.
    *   **Tekrarlanabilir:** Aynı sonucu her seferinde vermeli.

---

### 2. JUnit (Java Unit Testing Framework)

JUnit, Java dünyasının en yaygın kullanılan test framework'üdür. Spring Boot varsayılan olarak JUnit 5 (Jupiter) kullanır.

#### JUnit 5 Mimarisi
1.  **JUnit Platform:** Test motorunu çalıştıran temel katman.
2.  **JUnit Jupiter:** JUnit 5'in yeni programlama modeli ve API'si.
3.  **JUnit Vintage:** JUnit 3 ve 4 testlerini çalıştırmak için backward compatibility.

#### Temel Anotasyonlar

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    
    private Calculator calculator;
    
    @BeforeAll // Tüm testlerden ÖNCE 1 kez çalışır (static olmalı)
    static void setupAll() {
        System.out.println("Test suite başlatılıyor...");
    }
    
    @BeforeEach // Her testten ÖNCE çalışır
    void setup() {
        calculator = new Calculator();
    }
    
    @Test // Test metodu
    void testAddition() {
        int result = calculator.add(2, 3);
        assertEquals(5, result, "2 + 3 = 5 olmalı");
    }
    
    @Test
    @DisplayName("Sıfıra bölme hatası fırlatmalı")
    void testDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> {
            calculator.divide(10, 0);
        });
    }
    
    @Test
    @Disabled("Geçici olarak devre dışı")
    void testUnfinishedFeature() {
        // ...
    }
    
    @AfterEach // Her testten SONRA çalışır
    void tearDown() {
        calculator = null;
    }
    
    @AfterAll // Tüm testlerden SONRA 1 kez çalışır (static olmalı)
    static void tearDownAll() {
        System.out.println("Test suite tamamlandı.");
    }
}
```

#### Assertion Metodları (JUnit 5)

```java
// Eşitlik kontrolü
assertEquals(expected, actual);
assertNotEquals(expected, actual);

// Boolean kontrolü
assertTrue(condition);
assertFalse(condition);

// Null kontrolü
assertNull(object);
assertNotNull(object);

// Array/Koleksiyon kontrolü
assertArrayEquals(expectedArray, actualArray);

// Exception kontrolü
assertThrows(Exception.class, () -> { /* kod */ });

// Timeout kontrolü
assertTimeout(Duration.ofSeconds(1), () -> { /* kod */ });

// Toplu doğrulama (hepsi çalışır, sonunda toplu rapor verir)
assertAll(
    () -> assertEquals(2, calculator.add(1, 1)),
    () -> assertEquals(0, calculator.subtract(5, 5))
);
```

#### Parameterized Tests (Parametreli Testler)

Aynı testi farklı verilerle çalıştırmak için:

```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
void testPositiveNumbers(int number) {
    assertTrue(number > 0);
}

@ParameterizedTest
@CsvSource({
    "1, 1, 2",
    "2, 3, 5",
    "5, 5, 10"
})
void testAddition(int a, int b, int expected) {
    assertEquals(expected, calculator.add(a, b));
}

@ParameterizedTest
@MethodSource("provideStrings")
void testStringLength(String input, int expectedLength) {
    assertEquals(expectedLength, input.length());
}

static Stream<Arguments> provideStrings() {
    return Stream.of(
        Arguments.of("test", 4),
        Arguments.of("hello", 5)
    );
}
```

---

### 3. TestNG (Test Next Generation)

TestNG, JUnit'e alternatif ve bazı açılardan daha güçlü bir test framework'üdür. Özellikle entegrasyon ve end-to-end testlerde tercih edilir.

#### JUnit vs TestNG

| Özellik | JUnit 5 | TestNG |
| :--- | :--- | :--- |
| **Test Grupları** | `@Tag` (sınırlı) | `@Test(groups = {"smoke", "regression"})` (güçlü) |
| **Bağımlılık** | Desteklenmez | `@Test(dependsOnMethods = {"init"})` |
| **Paralel Çalıştırma** | Manuel yapılandırma | XML ile kolay yapılandırma |
| **Test Sırası** | `@TestMethodOrder` | `@Test(priority = 1)` |
| **Raporlama** | Basit | Detaylı HTML/XML raporlar |

#### TestNG Örneği

```java
import org.testng.annotations.*;
import static org.testng.Assert.*;

public class LoginTest {
    
    @BeforeClass // Sınıf seviyesinde 1 kez çalışır
    public void setupClass() {
        System.out.println("Test sınıfı başlatılıyor...");
    }
    
    @BeforeMethod // Her test metodundan önce
    public void setup() {
        // Test verisi hazırlama
    }
    
    @Test(priority = 1, groups = {"smoke"})
    public void testValidLogin() {
        assertTrue(loginService.login("user", "pass"));
    }
    
    @Test(priority = 2, dependsOnMethods = {"testValidLogin"})
    public void testDashboard() {
        // testValidLogin başarılı olmazsa bu test çalışmaz
    }
    
    @Test(groups = {"regression"})
    public void testInvalidLogin() {
        assertFalse(loginService.login("", ""));
    }
    
    @Test(expectedExceptions = NullPointerException.class)
    public void testNullInput() {
        loginService.login(null, null);
    }
    
    @AfterMethod
    public void tearDown() {
        // Temizlik işlemi
    }
}
```

**TestNG XML Yapılandırması:**
```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Test Suite" parallel="methods" thread-count="5">
    <test name="Smoke Tests">
        <groups>
            <run>
                <include name="smoke"/>
            </run>
        </groups>
        <classes>
            <class name="com.example.LoginTest"/>
        </classes>
    </test>
</suite>
```

---

### 4. Mocking (Mock Nesneler)

**Analoji:** Mocking, bir filmin dublörlü çekilmesi gibidir. Gerçek oyuncu (gerçek bağımlılık) yerine dublör (mock) kullanılır. Sadece o sahnenin (test edilen metodun) doğru çalışıp çalışmadığı test edilir.

#### Neden Mock Kullanılır?
*   **Bağımsızlık:** Dış bağımlılıklardan (DB, API, dosya sistemi) kurtulmak.
*   **Hız:** Gerçek veritabanına bağlanmak yerine bellekte çalışır.
*   **Kontrol:** Bağımlılığın davranışını tamamen kontrol edersiniz (beklenmedik durumlar simüle edilebilir).

#### Mockito (En Popüler Mocking Framework)

```java
import org.mockito.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    
    @Mock // Mock nesne oluşturur
    private UserRepository userRepository;
    
    @InjectMocks // Mock'ları bağımlılık olarak enjekte eder
    private UserService userService;
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Mock'ları başlat
    }
    
    @Test
    void testFindUserById() {
        // Arrange (Hazırlık): Mock davranışı tanımlama
        User mockUser = new User(1, "Ali");
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        
        // Act (Eylem): Test edilecek metodu çalıştırma
        User result = userService.findUserById(1);
        
        // Assert (Doğrulama)
        assertNotNull(result);
        assertEquals("Ali", result.getName());
        
        // Verify (Doğrulama): Mock metodu gerçekten çağrıldı mı?
        verify(userRepository, times(1)).findById(1);
    }
    
    @Test
    void testDeleteUser() {
        doNothing().when(userRepository).deleteById(1);
        
        userService.deleteUser(1);
        
        verify(userRepository).deleteById(1);
    }
    
    @Test
    void testExceptionHandling() {
        when(userRepository.findById(999))
            .thenThrow(new RuntimeException("User not found"));
        
        assertThrows(RuntimeException.class, () -> {
            userService.findUserById(999);
        });
    }
}
```

**Mockito Yetenekleri:**
*   `when(...).thenReturn(...)`: Metot çağrısına cevap döndür.
*   `when(...).thenThrow(...)`: Exception fırlat.
*   `doNothing()`: void metotlar için.
*   `verify(...)`: Metodun çağrılıp çağrılmadığını doğrula.
*   `any()`, `anyInt()`: Herhangi bir argüman kabul et.

---

### 5. Test Coverage (Test Kapsamı)

**Analoji:** Test coverage, bir haritanın ne kadarının keşfedildiğini gösteren bir metriktir. %100 coverage, tüm toprakların keşfedildiği anlamına gelir ama her köşede hazine olduğu anlamına gelmez.

#### Coverage Türleri
*   **Line Coverage:** Kaç satır kod çalıştırıldı?
*   **Branch Coverage:** Tüm if/else dalları test edildi mi?
*   **Method Coverage:** Kaç metot çağrıldı?

**Araçlar:** JaCoCo (Java Code Coverage), SonarQube

**Maven Konfigürasyonu (JaCoCo):**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Komut:** `mvn clean test jacoco:report`

**Uyarı:** %100 coverage hedeflemeyin. %80-90 yeterlidir. Getter/Setter gibi trivial metodları test etmek zaman kaybıdır.

---

### 6. Kritik Mülakat Soruları 

#### Soru 1: Unit Test ile Integration Test farkı nedir?
**Cevap:**
*   **Unit Test:** Tek bir birimi (metot/sınıf) izole eder, dış bağımlılıkları mock'lar. Hızlıdır.
*   **Integration Test:** Birden fazla komponentin birlikte çalışmasını test eder (örn: Service + Repository + DB). Yavaştır.

**Test Piramidi:** Çoğunluk Unit Test, orta Integration Test, az End-to-End Test olmalıdır.

#### Soru 2: `@Mock` ile `@Spy` farkı nedir (Mockito)?
**Cevap:**
*   **`@Mock`:** Tamamen sahte nesne. Tüm metodlar stub edilmelidir.
*   **`@Spy`:** Gerçek nesnenin "casusu"dur. Gerçek metodlar çalışır ama istediğiniz metodları stub edebilirsiniz.

```java
@Spy
private List<String> spyList = new ArrayList<>();

@Test
void testSpy() {
    spyList.add("real");
    when(spyList.size()).thenReturn(100); // size() mock'landı
    
    assertEquals(1, spyList.size()); // Gerçek add() çalıştı ama size() mock
}
```

#### Soru 3: TDD (Test-Driven Development) nedir?
**Cevap:** Önce testi yazma, sonra kodu yazma yaklaşımıdır.
1.  **Red:** Başarısız test yaz (henüz kod yok).
2.  **Green:** Testi geçirecek minimum kodu yaz.
3.  **Refactor:** Kodu iyileştir (test hala geçmeli).

**Avantajı:** Tasarım kalitesi artar, kod daha test edilebilir olur.

#### Soru 4: Flaky test nedir? Nasıl önlenir?
**Cevap:** Bazen geçen bazen başarısız olan kararsız testlerdir. Genellikle:
*   **Thread/concurrency sorunları:** `Thread.sleep()` kullanımı.
*   **Zamana bağlı kodlar:** `new Date()` kullanımı.
*   **Dış bağımlılıklar:** Gerçek network çağrısı.

**Çözüm:** Mock kullanın, sabit zaman inject edin (`Clock` sınıfı), deterministik yapın.

#### Soru 5: JUnit 5'te `@Nested` ne işe yarar?
**Cevap:** Test sınıfları içinde iç içe test grupları oluşturmaya yarar. İlgili testleri hiyerarşik şekilde organize eder.

```java
class CalculatorTest {
    @Nested
    class AdditionTests {
        @Test void testPositive() { }
        @Test void testNegative() { }
    }
    
    @Nested
    class DivisionTests {
        @Test void testNormal() { }
        @Test void testByZero() { }
    }
}
```

---

### 7. Geliştirici İpuçları

*   **AAA Pattern:** Her test Arrange (Hazırlık), Act (Eylem), Assert (Doğrulama) yapısında olmalı. Okunabilirlik artar.
*   **FIRST Prensipleri:** Fast (Hızlı), Independent (Bağımsız), Repeatable (Tekrarlanabilir), Self-validating (Kendi kendini doğrulayan), Timely (Zamanında yazılan).
*   **Test İsimlendirme:** `testMethodName_Scenario_ExpectedResult` formatı kullanın. Örn: `testWithdraw_InsufficientBalance_ThrowsException`.
*   **Given-When-Then:** BDD (Behavior-Driven Development) tarzı okunabilir testler için: `given()`, `when()`, `then()` metodları.
*   **Test Data Builder Pattern:** Karmaşık test verileri oluşturmak için builder pattern kullanın.
*   **Mutation Testing:** Kodunuzu kasıtlı olarak bozun (mutasyon), testler yakalamalı. PIT (Pitest) kullanabilirsiniz.

---

