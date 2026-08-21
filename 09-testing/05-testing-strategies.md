# Test Stratejileri (Test Piramidi, TDD, Contract Testing)

> **Analoji:** Test stratejisi bir "sağlık check-up planı" gibidir. Her gün tansiyon ölçmek (unit test), ayda bir kan tahlili (integration test), yılda bir tam check-up (E2E test). Hepsini her gün yapmak gereksiz ve pahalıdır.

---

## 1. Test Piramidi

```
         /\
        /E2E\          (Az, Yavaş, Pahalı)    ~%10
       /──────\
      /  Integ \        (Orta)                  ~%20
     /──────────\
    /    UNIT    \      (Çok, Hızlı, Ucuz)     ~%70
   /──────────────\
```

| Katman | Süre | Maliyet | Kapsam |
| :--- | :--- | :--- | :--- |
| **Unit** | ms | Düşük | Tek sınıf/metot |
| **Integration** | sn | Orta | Katmanlar arası |
| **E2E** | dk | Yüksek | Tüm sistem |

---

## 2. TDD (Test-Driven Development)

### Red → Green → Refactor

1. **Red:** Başarısız test yaz (henüz kod yok)
2. **Green:** Testi geçirecek **minimum** kodu yaz
3. **Refactor:** Kodu iyileştir (test hala geçmeli)

```java
// 1. RED: Test yaz (başarısız olacak)
@Test
void shouldCalculateDiscountForPremiumCustomer() {
    PricingService service = new PricingService();
    BigDecimal result = service.calculateDiscount(CustomerType.PREMIUM, new BigDecimal("100"));
    assertEquals(new BigDecimal("15.00"), result); // %15 indirim
}

// 2. GREEN: Minimum kod yaz
public BigDecimal calculateDiscount(CustomerType type, BigDecimal price) {
    if (type == CustomerType.PREMIUM) {
        return price.multiply(new BigDecimal("0.15"));
    }
    return BigDecimal.ZERO;
}

// 3. REFACTOR: Strateji pattern'e dönüştür
```

---

## 3. Contract Testing (Pact/Spring Cloud Contract)

Mikroservis mimarisinde consumer ve provider arasındaki API sözleşmesini doğrular.

```java
// Consumer tarafı (Pact)
@Pact(consumer = "OrderService")
public RequestResponsePact createPact(PactDslWithProvider builder) {
    return builder
        .given("User exists")
        .uponReceiving("Get user by ID")
            .path("/api/users/42")
            .method("GET")
        .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .integerType("id", 42)
                .stringType("name", "Mete"))
        .toPact();
}

// Provider tarafı: Pact dosyasını doğrular
@Provider("UserService")
@PactBroker(url = "https://pact-broker.example.com")
class UserProviderTest {
    // Provider pact'i karşılıyor mu?
}
```

---

## 4. Mutation Testing

Kodunuzu kasıtlı olarak bozun (mutant), testler yakalamalı. Yakalayamazsa testleriniz zayıf.

```xml
<!-- PIT (Pitest) Maven Plugin -->
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.0</version>
    <configuration>
        <targetClasses>
            <param>com.example.service.*</param>
        </targetClasses>
        <mutationThreshold>80</mutationThreshold>
    </configuration>
</plugin>
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: Test piramidini neden tersine çevirmek kötüdür?
**Cevap:** "Ice Cream Cone" anti-pattern: Çok E2E, az unit test. E2E testler yavaş, kırılgan ve bakımı pahalıdır. Feedback döngüsü uzar, CI/CD pipeline saatlerce sürer.

### Soru 2: Contract testing ne zaman gereklidir?
**Cevap:** Farklı takımlar tarafından geliştirilen servisler arasında. API değişikliğinin consumer'ları bozup bozmadığını derleme zamanında yakalarsınız.

### Soru 3 (Tricky): %100 code coverage yeterli midir?
**Cevap:** Hayır! Coverage sadece "hangi satırlar çalıştırıldı" ölçer, "doğru test edildi mi" ölçmez. Mutation testing ile testlerin gerçek kalitesini ölçün.

---

## 6. Geliştirici İpuçları

- **Test İsimlendirme:** `methodName_scenario_expectedBehavior` → `calculateDiscount_premiumCustomer_returns15Percent`
- **Testcontainers:** Integration testlerde H2 yerine gerçek PostgreSQL container'ı kullanın. Daha güvenilir sonuçlar.
- **Paralel Test:** `@Execution(ExecutionMode.CONCURRENT)` ile JUnit 5 testlerini paralel çalıştırın.
- **Test Data Builder:** Karmaşık test verisi için Builder pattern kullanın. Factory method'larla okunabilir test verisi oluşturun.
