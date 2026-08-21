## Konu 24: Yazılım Tasarımı ve Mimarisi (Clean Code & Architecture)

Bir yazılım mühendisi sadece "çalışan kod" yazmaz; **okunabilir**, **bakımı kolay**, **test edilebilir** ve **değişime direnç göstermeyen** kod yazar. Bu bölüm, Clean Code prensipleri ve mimari yaklaşımları özetler.

---

### 1. Temel Clean Code Prensipleri

#### 1.1 Be Consistent (Tutarlı Olun)
Kod tabanında bir stil veya yaklaşım seçtiyseniz, her yerde onu uygulayın.
*   **Kötü:** Bir yerde `fetchUser`, başka yerde `retrieveAccount`, başka yerde `getData`.
*   **İyi:** Tüm veri çekme işlemleri için `fetch...` öneki kullanın.

#### 1.2 Meaningful Names over Comments (Yorum Yerine İsimlendirme)
Kodun **ne** yaptığını yorumla değil, ismiyle anlatın. Yorumlar zamanla eskir ve yalan söyler, kod her zaman gerçektir.

```java
// KÖTÜ: 
// Check if user is eligible for discount
if (user.age > 65 && user.purchaseCount > 10) { ... }

// İYİ:
if (user.isEligibleForDiscount()) { ... }
```

#### 1.3 Keep Methods / Classes / Files Small (Küçük Tutun)
*   **Metotlar:** Tek bir iş yapmalı (Single Responsibility). 20 satırı geçiyorsa bölmeyi düşünün.
*   **Sınıflar:** Tek bir sorumluluğu olmalı. "God Class" (her şeyi yapan sınıf) anti-pattern'dir.
*   **Dosyalar:** Binlerce satırlık dosyalar yönetilemez.

#### 1.4 Indentation and Code Style (Girinti ve Stil)
Kodun görsel yapısı, okunabilirliği doğrudan etkiler.
*   Derin `if/else` bloklarından kaçının (**Guard Clauses** kullanın).
*   Otomatik formatter (Prettier, Google Java Format) kullanın ve CI/CD'de zorlayın.

```java
// KÖTÜ (Arrow Code)
if (user != null) {
    if (user.isActive()) {
        if (user.hasCredit()) {
            process();
        }
    }
}

// İYİ (Guard Clauses)
if (user == null || !user.isActive() || !user.hasCredit()) return;
process();
```

---

### 2. Fonksiyonel ve Yapısal Prensipler

#### 2.1 Pure Functions (Saf Fonksiyonlar)
Yan etkisi (side-effect) olmayan, aynı girdiyle her zaman aynı çıktıyı veren fonksiyonlar.
*   Test etmesi çok kolaydır.
*   Concurrency sorunu yaşatmaz.

#### 2.2 Minimize Cyclomatic Complexity (Karmaşıklığı Azaltın)
Kodun içindeki bağımsız yolların sayısıdır. `if`, `for`, `switch` complexity'i artırır.
*   **Hedef:** Metot başına < 10.
*   **Çözüm:** Polimorfizm, Strategy Pattern veya metotları bölmek.

#### 2.3 Avoid Passing Nulls & Booleans
*   **Null:** Parametre olarak `null` geçmek, içeride sürekli `if (arg != null)` kontrolü gerektirir. `Optional` veya `Null Object Pattern` kullanın.
*   **Boolean:** Bir metoda `true/false` geçiyorsanız (flag argument), o metot muhtemelen iki iş yapıyordur. İki ayrı metoda bölün.

```java
// KÖTÜ
public void render(boolean isSuite) {
    if (isSuite) { ... } else { ... }
}

// İYİ
public void renderSuite() { ... }
public void renderStandard() { ... }
```

#### 2.4 Use Correct Constructs (Doğru Yapıları Kullanın)
*   Sabit listeler için `List` yerine `Enum` kullanın.
*   Para birimi için `double` yerine `BigDecimal` kullanın.
*   Tarih için `Date` yerine `LocalDateTime` kullanın.
*   **WebClient Mocking:** WebFlux kullanıyorsanız `WebTestClient` kullanın.

---

### 3. Mimari Prensipler

#### 3.1 Keep Framework Code Distant (Framework'ü Uzak Tutun)
İş mantığınız (Domain Logic), kullandığınız framework'e (Spring, Hibernate) göbekten bağlı olmamalıdır.
*   **Hexagonal Architecture (Ports & Adapters):** Domain merkezde, framework dışarıda detaydır.
*   Annotation kirliliğini domain entity'lerinde minimumda tutun.

#### 3.2 Organize Code by Actor (Aktöre Göre Organizasyon)
Kodu teknik katmanlara (Controller, Service, Dao) göre değil, işlevsel modüllere (Order, User, Payment) göre paketleyin (**Package by Feature**).
*   Böylece bir özelliği silmek veya mikroservise ayırmak kolaylaşır.

#### 3.3 Command Query Separation (CQS)
Bir metot ya bir iş yapmalı (Command - state değiştirir, void döner) ya da bir cevap vermeli (Query - state değiştirmez, değer döner). İkisini aynı anda yapmamalı.

```java
// KÖTÜ (Hem değiştiriyor hem dönüyor)
User saveAndReturnUser(User u);

// İYİ
void save(User u);
User findById(Long id);
```

#### 3.4 Tests Should Be Fast and Independent
*   **Fast:** Unit testler milisaniyeler içinde çalışmalı. Yavaşsa kimse çalıştırmaz.
*   **Independent:** Bir testin sonucu diğerine bağlı olmamalı. Her test kendi verisini hazırlamalı.

#### 3.5 Keep It Simple (KISS) & Refactor Often
*   "Gelecekte lazım olur" diye kod yazmayın (**YAGNI** - You Ain't Gonna Need It).
*   **Boy Scout Rule:** Kodu bulduğunuzdan daha temiz bırakın. Her commit'te küçük bir refactoring yapın.

---

### 4. Kritik Mülakat Soruları 

#### Soru 1: "Composition over Inheritance" prensibini açıklayın. Neden tercih edilir?
**Cevap:** Kalıtım (Inheritance) sıkı bir bağ (tight coupling) yaratır ve "is-a" ilişkisi her zaman esnek değildir. Kompozisyon (Composition - "has-a") ise parçaları birleştirerek esneklik sağlar, çalışma zamanında davranış değiştirmeye izin verir (Strategy Pattern).

#### Soru 2: Anemic Domain Model vs Rich Domain Model farkı nedir?
**Cevap:**
*   **Anemic:** Entity'lerde sadece getter/setter var, tüm mantık Service katmanında. (Spring'de yaygın ama OOP'ye aykırı).
*   **Rich:** Entity'ler kendi verisi üzerindeki işlemleri (metotları) kendisi yapar. OOP prensiplerine daha uygundur.

#### Soru 3: Dependency Injection neden önemlidir?
**Cevap:** Sınıfların bağımlılıklarını kendilerinin oluşturması yerine dışarıdan almasını sağlar. Bu, **Loose Coupling** (gevşek bağlılık) sağlar ve bir sınıfı izole ederek **Unit Test** yazmayı mümkün kılar.

#### Soru 4 (Tricky): "Feature Envy" code smell nedir?
**Cevap:** Bir metodun, kendi sınıfından çok başka bir sınıfın verisiyle ilgilenmesidir.
*   **Örnek:** `Order` sınıfındaki bir metodun sürekli `Customer` sınıfının getter'larını çağırıp hesaplama yapması.
*   **Çözüm:** O metodu (veya mantığı) verinin olduğu yere (`Customer` sınıfına) taşıyın.

#### Soru 5 (Tricky): "Primitive Obsession" nedir?
**Cevap:** Domain kavramları yerine sürekli ilkel tipler (String, int) kullanmaktır.
*   **Kötü:** `public void saveUser(String email, String phone, String zipCode)`
*   **İyi:** `public void saveUser(Email email, Phone phone, ZipCode zipCode)`
*   **Yararı:** Validasyon mantığı kendi sınıflarında toplanır, tip güvenliği artar.

---

### 5. Geliştirici İpuçları

*   **Code Review Kültürü:** Code review sadece hata bulmak için değil, bilgi paylaşımı ve standartları korumak içindir. "Neden?" sorusunu sormaktan çekinmeyin.
*   **Tech Debt Yönetimi:** Teknik borç kaçınılmazdır. Ancak bunu görünür kılın (backlog'a ekleyin) ve düzenli olarak ödeyin (sprint'lerde %20 pay ayırın).
*   **Documentation:** Kodun "nasıl" çalıştığını kod anlatır, "neden" o şekilde yazıldığını (kararlar, trade-off'lar) dokümantasyon (ADR - Architecture Decision Records) anlatır.


---

