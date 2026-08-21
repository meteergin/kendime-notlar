## Konu 25: Yazılım Tasarım Prensipleri (SOLID & Diğerleri)

Bir geliştirici, kodu sadece yazmaz; onu **yönetilebilir**, **esnek** ve **sağlam** kılan prensiplere göre tasarlar. Bu bölüm, SOLID ve diğer kritik tasarım prensiplerini derinlemesine inceler.

---

### 1. SOLID Prensipleri

Robert C. Martin (Uncle Bob) tarafından derlenen bu 5 prensip, OOP tasarımının temel taşıdır.

#### 1.1 Single Responsibility Principle (SRP)
**"Bir sınıfın değişmesi için tek bir sebebi olmalıdır."**
Bir sınıf hem veritabanına kayıt yapıp, hem e-posta gönderip, hem de iş mantığını yürütmemelidir.

*   **Kötü:** `OrderService` içinde `sendEmail()`, `saveToDb()`, `calculateTotal()`.
*   **İyi:** `OrderRepository` (DB), `EmailSender` (Notification), `OrderCalculator` (Logic).

#### 1.2 Open/Closed Principle (OCP)
**"Yazılım varlıkları genişlemeye açık, değişime kapalı olmalıdır."**
Yeni bir özellik eklemek için mevcut kodu değiştirmek yerine, yeni kod eklemelisiniz.

*   **Örnek:** İndirim hesaplama. `if (type == STUDENT) ... else if (type == SENIOR)` yerine, `DiscountStrategy` interface'i ve `StudentDiscount`, `SeniorDiscount` sınıfları. Yeni indirim tipi için sadece yeni sınıf eklenir.

#### 1.3 Liskov Substitution Principle (LSP)
**"Alt sınıflar, üst sınıfların yerine kullanılabilmelidir."**
Bir alt sınıf, miras aldığı üst sınıfın davranışını bozmamalıdır.

*   **İhlal Örneği:** `Kare` sınıfı `Dikdörtgen` sınıfından türerse ve `setYukseklik()` metodunda genişliği de değiştirirse, `Dikdörtgen` bekleyen kod bozulur.

#### 1.4 Interface Segregation Principle (ISP)
**"İstemciler, kullanmadıkları metotlara bağımlı olmaya zorlanmamalıdır."**
Devasa tek bir interface yerine, özelleşmiş küçük interface'ler kullanın.

*   **Kötü:** `SmartDevice` interface'inde `print()`, `fax()`, `scan()`. Sadece `print` yapan bir cihaz `fax` metodunu boş implemente etmek zorunda kalır.
*   **İyi:** `Printer`, `FaxMachine`, `Scanner` interface'leri.

#### 1.5 Dependency Inversion Principle (DIP)
**"Yüksek seviyeli modüller, düşük seviyeli modüllere bağlı olmamalıdır. Her ikisi de soyutlamalara (abstraction) bağlı olmalıdır."**

*   **Kötü:** `OrderService` sınıfı doğrudan `MySQLDatabase` sınıfına bağlı.
*   **İyi:** `OrderService` sınıfı `DatabaseRepository` interface'ine bağlı. `MySQLDatabase` bu interface'i implemente eder.

---

### 2. Temel Prensipler (DRY, YAGNI, KISS)

*   **DRY (Don't Repeat Yourself):** Bilgi ve mantık tekrarından kaçının. Tekrar eden kod, bakım maliyetini ikiye katlar ve hata riskini artırır.
*   **YAGNI (You Ain't Gonna Need It):** İhtiyacınız olmayan özellikleri "belki lazım olur" diye eklemeyin.
*   **KISS (Keep It Simple, Stupid):** Çözümü olabildiğince basit tutun. Karmaşıklık hatayı davet eder.

---

### 3. İleri Seviye Tasarım Prensipleri

#### 3.1 Law of Demeter (LoD) - "Don't Talk to Strangers"
Bir nesne, sadece doğrudan ilişkili olduğu nesnelerle konuşmalıdır. Zincirleme çağrılardan kaçının.

*   **Kötü:** `order.getCustomer().getAddress().getCity()` (Tren kazası kodu).
*   **İyi:** `order.getCustomerCity()` (Order sınıfı, Customer üzerinden City bilgisini getirmeli).

#### 3.2 Tell, Don't Ask
Nesnelerin durumunu (state) sormak ve ona göre işlem yapmak yerine, nesneye ne yapması gerektiğini söyleyin.

*   **Kötü:**
    ```java
    if (account.getBalance() > amount) {
        account.setBalance(account.getBalance() - amount);
    }
    ```
*   **İyi:**
    ```java
    account.withdraw(amount); // Mantık Account sınıfının içinde
    ```

#### 3.3 Hollywood Principle
**"Don't call us, we'll call you."**
Inversion of Control (IoC) temelidir. Framework (Spring), sizin kodunuzu çağırır; siz framework'ü değil. Kontrol akışı framework'ün elindedir.

#### 3.4 Composition over Inheritance
Kalıtım (Inheritance) yerine Kompozisyonu (Composition) tercih edin.
*   **Neden?** Kalıtım statiktir (compile-time), kompozisyon dinamiktir (runtime). Kalıtım "is-a", kompozisyon "has-a" ilişkisidir. Kompozisyon daha esnek ve gevşek bağlı (loosely coupled) sistemler sağlar.

#### 3.5 Encapsulate What Varies
Değişen kısımları, değişmeyen kısımlardan ayırın ve kapsülleyin.
*   **Örnek:** Bir oyunda karakterin hareketi değişiyorsa, `MovementBehavior` interface'i oluşturun ve karakter sınıfında bunu kullanın (Strategy Pattern).

#### 3.6 Program Against Abstractions
Somut sınıflara (implementation) değil, soyutlamalara (interface/abstract class) kod yazın.
*   **Yararı:** Bağımlılıkları azaltır, test edilebilirliği artırır ve implementasyonu değiştirmeyi kolaylaştırır.

```java
// KÖTÜ
ArrayList<String> list = new ArrayList<>();

// İYİ
List<String> list = new ArrayList<>();
```

---

### 4. Kritik Mülakat Soruları 

#### Soru 1: Liskov Substitution Principle ihlali nasıl tespit edilir?
**Cevap:** Eğer bir alt sınıf, üst sınıfın metodunu implemente ederken boş bırakıyorsa (`throw new UnsupportedOperationException()`) veya üst sınıfın beklediği ön koşulları (pre-conditions) sıkılaştırıp, son koşulları (post-conditions) gevşetiyorsa LSP ihlali vardır.

#### Soru 2: Dependency Injection ile Dependency Inversion arasındaki ilişki nedir?
**Cevap:** Dependency Inversion bir **prensip**tir (yüksek seviye modüller detaylara bağlı olmamalı). Dependency Injection ise bu prensibi uygulamak için kullanılan bir **teknik**tir (bağımlılıkların dışarıdan verilmesi).

#### Soru 3: "Tell, Don't Ask" prensibi neden önemlidir?
**Cevap:** Bu prensip, **Encapsulation** (kapsülleme) ilkesini güçlendirir. Veriyi ve o veri üzerindeki işlemi aynı yerde (sınıfta) tutmayı sağlar (Cohesion). Böylece mantık kodun her yerine dağılmaz.

#### Soru 4 (Tricky): "Fragile Base Class" problemi nedir?
**Cevap:** Kalıtım (Inheritance) kullanıldığında, üst sınıfta (Base Class) yapılan küçük bir değişikliğin, tüm alt sınıflarda beklenmedik hatalara yol açmasıdır.
*   **Çözüm:** Kalıtım yerine **Composition** kullanın veya Base Class'ı `final` yapın (değişime kapatın).

#### Soru 5 (Tricky): Interface Pollution (Interface Segregation ihlali) nasıl anlaşılır?
**Cevap:** Bir sınıf, implemente ettiği interface'in bazı metodlarını boş bırakıyorsa (`throw new UnsupportedOperationException()`), o interface "kirletilmiş" demektir.
*   **Çözüm:** Interface'i daha küçük parçalara bölün (Role Interfaces).

---

### 5. Geliştirici İpuçları

*   **Prensipler Dogma Değildir:** Her prensibi her yerde uygulamaya çalışmak "Over-Engineering"e yol açabilir. Bağlam (Context) her şeydir. Basit bir CRUD uygulamasında CQRS veya Hexagonal Architecture kullanmak gereksiz karmaşıklıktır.
*   **Refactoring:** Bu prensipleri baştan mükemmel uygulamak zordur. Kod geliştikçe "Code Smells" (kötü kokular) fark edip refactoring yaparak prensiplere uygun hale getirin.
*   **Interface Kirliliği:** Sadece bir tane implementasyonu olacaksa, her sınıf için interface oluşturmak (Header Interface pattern) gereksizdir. Interface'ler, çoklu implementasyon veya test edilebilirlik (mocking) gerektiğinde anlamlıdır.


---

