## Konu 28: Tasarım Desenleri (GoF & PoSA)

Tasarım desenleri (Design Patterns), yazılım tasarımında sıkça karşılaşılan problemlere getirilen yeniden kullanılabilir çözümlerdir. Bir geliştirici, bu desenleri ezberlemekten ziyade **nerede ve neden** kullanılacağını (ve kullanılmayacağını) bilmelidir.

---

### 1. Yaratımsal Desenler (Creational Patterns)

Nesne oluşturma süreçlerini soyutlar ve esneklik sağlar.

#### 1.1 Singleton
Bir sınıfın sadece bir örneğinin (instance) olmasını garanti eder.
*   **Best Practice:** Java'da `enum` kullanarak thread-safe Singleton oluşturmak en güvenli yoldur.
*   **Anti-Pattern:** Global state yarattığı ve test etmeyi zorlaştırdığı için dikkatli kullanılmalıdır. Spring Bean'leri varsayılan olarak Singleton scope'tadır (ama IoC container yönetir, kod değil).

#### 1.2 Builder
Karmaşık nesnelerin adım adım oluşturulmasını sağlar.
*   **Kullanım:** Çok fazla parametresi olan constructor'lardan (Telescoping Constructor) kurtulmak için.
*   **Lombok:** `@Builder` anotasyonu ile otomatik üretilir.

#### 1.3 Factory Method vs Abstract Factory
*   **Factory Method:** Nesne oluşturma işini alt sınıflara bırakır. (Tek bir ürün ailesi).
*   **Abstract Factory:** Birbiriyle ilişkili veya bağımlı nesne ailelerini oluşturmak için bir arayüz sağlar. (Birden fazla ürün ailesi).

#### 1.4 Prototype
Mevcut bir nesneyi kopyalayarak (clone) yeni nesne oluşturur. Nesne oluşturma maliyeti yüksekse kullanılır.

---

### 2. Yapısal Desenler (Structural Patterns)

Sınıfların ve nesnelerin nasıl bir araya getirilerek daha büyük yapılar oluşturulacağını tanımlar.

#### 2.1 Adapter (Wrapper)
Uyumsuz arayüzleri birbirine bağlar.
*   **Örnek:** `Arrays.asList()` (Array'i List arayüzüne uydurur).
*   **Senaryo:** Eski bir XML servisini, yeni sistemin JSON bekleyen arayüzüne bağlamak.

#### 2.2 Decorator
Nesnelere çalışma zamanında dinamik olarak yeni davranışlar ekler.
*   **Örnek:** `new BufferedInputStream(new FileInputStream("file.txt"))`.
*   **Fark:** Kalıtım (Inheritance) statiktir, Decorator dinamiktir.

#### 2.3 Facade
Karmaşık bir alt sistemi basitleştirilmiş bir arayüzle sunar.
*   **Örnek:** Bir `VideoConverter` kütüphanesinde Codec, AudioMixer, Bitrate gibi detayları gizleyip sadece `convert(file, format)` metodu sunmak.

#### 2.4 Proxy
Bir nesneye erişimi kontrol eden bir vekil nesne sağlar.
*   **Tipleri:** Lazy Loading (Hibernate), Security, Logging, Caching.
*   **Spring:** `@Transactional`, `@Cacheable` gibi anotasyonlar AOP ve Proxy deseni kullanır.

#### 2.5 Composite
Nesneleri ağaç yapısında düzenler (Parça-Bütün hiyerarşisi).
*   **Örnek:** Dosya sistemi (Klasör içinde Dosya veya Klasör olabilir). Tekil nesne ve grup aynı arayüzü kullanır.

---

### 3. Davranışsal Desenler (Behavioral Patterns)

Nesneler arasındaki iletişim ve sorumluluk atamasıyla ilgilenir.

#### 3.1 Strategy
Bir algoritma ailesi tanımlar ve bunları değiştirilebilir kılar.
*   **Örnek:** `Collections.sort(list, comparator)`. Comparator bir stratejidir.
*   **Kullanım:** `if (type == A) ... else if (type == B)` bloklarından kurtulmak için (Open/Closed Principle).

#### 3.2 Observer (Publish-Subscribe)
Bir nesnenin durumu değiştiğinde, ona bağımlı diğer nesnelerin otomatik uyarılmasını sağlar.
*   **Örnek:** Java `PropertyChangeListener`, Spring `ApplicationEventPublisher`.

#### 3.3 Template Method
Bir algoritmanın iskeletini bir metotta tanımlar, bazı adımları alt sınıflara bırakır.
*   **Örnek:** Spring `JdbcTemplate`. Bağlantı açma/kapama, hata yönetimi şablondur; SQL çalıştırma kısmı kullanıcıya bırakılır.

#### 3.4 Chain of Responsibility
Bir isteği işleyebilecek nesneler zinciri oluşturur.
*   **Örnek:** Spring Security Filter Chain, Servlet Filters. İstek zincir boyunca akar, uygun olan işler veya bir sonrakine devreder.

#### 3.5 Command
Bir isteği nesneye dönüştürür (encapsulate).
*   **Örnek:** `Runnable` interface'i. İşlemi kuyruğa atmak, geri almak (undo) veya loglamak için kullanılır.

---

### 4. PoSA (Pattern-Oriented Software Architecture)

GoF desenleri daha çok sınıf/nesne seviyesindeyken, PoSA desenleri daha geniş mimari seviyededir.

*   **Layers (Katmanlar):** Sistemi katmanlara ayırır (Bkz: Mimari Stiller).
*   **Pipes and Filters:** Veri işleme adımlarını boru hattı gibi bağlar. (Unix pipe `|`, Java Stream API).
*   **Broker:** Dağıtık sistemlerde bileşenlerin iletişimini koordine eder (CORBA, RMI, gRPC).
*   **Model-View-Controller (MVC):** UI ve iş mantığını ayırır.

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: Strategy Pattern ile State Pattern arasındaki fark nedir?
**Cevap:** Yapıları çok benzerdir (ikisi de composition kullanır).
*   **Strategy:** Algoritmayı değiştirir. İstemci (Client) hangi stratejiyi kullanacağını genellikle baştan seçer. "Nasıl" yapıldığı değişir.
*   **State:** Nesnenin iç durumuna göre davranışını değiştirir. Durum geçişleri (State transition) genellikle otomatiktir. "Ne" yapıldığı değişir.

#### Soru 2: Decorator Pattern ile Proxy Pattern arasındaki fark nedir?
**Cevap:**
*   **Decorator:** Nesneye **yeni özellik/davranış** ekler.
*   **Proxy:** Nesneye **erişimi kontrol eder**. Davranışı değiştirmez, sadece araya girer (yetki kontrolü, lazy load).

#### Soru 3: Double-Checked Locking (Singleton) nedir?
**Cevap:** Multi-threaded ortamda Lazy Singleton oluştururken performans için kullanılan bir yöntemdir.
```java
if (instance == null) {
    synchronized (Singleton.class) {
        if (instance == null) {
            instance = new Singleton();
        }
    }
}
```
Java 5+ ve `volatile` anahtar kelimesi ile güvenlidir. Ancak Enum Singleton daha basittir.

#### Soru 4 (Tricky): Observer Pattern kullanırken Memory Leak riski nedir?
**Cevap:** Subject (Gözlenen), Observer'ları (Gözlemcileri) bir listede tutar. Eğer Observer işi bittiğinde listeden çıkarılmazsa (unsubscribe), Garbage Collector tarafından temizlenemez (Lapsed Listener Problem).
*   **Çözüm:** `WeakReference` kullanmak veya explicit `unsubscribe` metodu çağırmak.

#### Soru 5 (Tricky): Singleton Pattern'i Enum ile implement etmenin avantajı nedir?
**Cevap:**
*   **Serialization:** Enum'lar otomatik olarak Serializable'dır ve deserialization sırasında yeni instance oluşturulmaz (Singleton garantisi bozulmaz).
*   **Reflection:** Reflection ile private constructor çağrılarak ikinci bir instance oluşturulamaz (Enum korumalıdır).

---

### 6. Geliştirici İpuçları

*   **Pattern İsimlerini Kullanın:** Ekip içi iletişimde "Burada bir Factory kullanalım" veya "Bu bir Observer olsun" demek, sayfalarca dokümandan daha hızlı anlaşılır (Ubiquitous Language).
*   **Anti-Pattern:** Her soruna bir desen uygulamaya çalışmak (Golden Hammer). Basit bir `if-else` yeterliyken Strategy Pattern kullanmak karmaşıklığı artırır.
*   **Refactoring to Patterns:** Desenleri baştan tasarlamak yerine, kod karmaşıklaştıkça refactoring sırasında uygulayın.


---

## Konu 29: 14 Temel Yazılım Mimari Deseni

Bu bölüm, modern dağıtık sistemlerde ve bulut mimarilerinde sıkça kullanılan 14 temel mimari deseni özetler. Bir geliştirici, bu desenlerin her birinin **ne zaman** ve **neden** kullanılacağını bilmelidir.

---

### 1. Circuit Breaker (Devre Kesici)
Bir servisin sürekli hata vermesi durumunda, o servise giden trafiği geçici olarak keserek sistemin geri kalanını korur.
*   **States:** Closed (Normal), Open (Hata eşiği aşıldı, istekler reddedilir), Half-Open (Test amaçlı birkaç isteğe izin verilir).
*   **Kullanım:** Mikroservis iletişiminde (Resilience4j, Hystrix).

### 2. Client-Server (İstemci-Sunucu)
İş yükünün kaynak sağlayıcı (Server) ve hizmet talep eden (Client) arasında paylaşıldığı temel modeldir.
*   **Kullanım:** Web uygulamaları, E-posta sistemleri.

### 3. CQRS (Command Query Responsibility Segregation)
Veri okuma (Query) ve yazma (Command) işlemlerini farklı modellerle ve potansiyel olarak farklı veritabanlarıyla ayırır.
*   **Kullanım:** Okuma/Yazma oranları çok farklı olan, yüksek performanslı sistemler.

### 4. Controller-Responder
Bir bileşenin (Controller) veriyi dağıttığı ve diğer bileşenlerin (Responders) bu veriyi işleyip sonuç döndürdüğü yapıdır.
*   **Kullanım:** Arama motorları (Controller sorguyu dağıtır, Responder'lar indekslerde arar).

### 5. Event Sourcing
Uygulamanın durumunu saklamak yerine, duruma etki eden tüm olayları (events) bir günlük (log) olarak saklar.
*   **Kullanım:** Bankacılık, muhasebe, audit log gerektiren sistemler.

### 6. Layered (Katmanlı)
Sistemi hiyerarşik katmanlara (Presentation, Business, Persistence, Database) ayırır.
*   **Kullanım:** Geleneksel kurumsal uygulamalar, Monolitik yapılar.

### 7. Microservices
Uygulamanın küçük, bağımsız, kendi veritabanına sahip ve ağ üzerinden haberleşen servisler olarak geliştirilmesidir.
*   **Kullanım:** Büyük, karmaşık, hızlı değişen ve ölçeklenebilirlik gerektiren sistemler.

### 8. Model-View-Controller (MVC)
Uygulamayı Veri (Model), Arayüz (View) ve İş Mantığı (Controller) olarak üçe ayırır.
*   **Kullanım:** Web framework'leri (Spring MVC, Django, Ruby on Rails).

### 9. Pub-Sub (Publish-Subscribe)
Mesaj gönderenlerin (Publishers) mesajları belirli alıcılara değil, konulara (Topics) gönderdiği; alıcıların (Subscribers) ise ilgilendikleri konulara abone olduğu asenkron iletişim modelidir.
*   **Kullanım:** Kafka, RabbitMQ, Event-Driven sistemler.

### 10. Saga
Dağıtık sistemlerde transaction yönetimini sağlayan desendir. Uzun süren işlemleri bir dizi yerel transaction'a böler.
*   **Kullanım:** Mikroservislerde sipariş süreci (Stok düş -> Ödeme al -> Kargo oluştur).

### 11. Sharding (Database Partitioning)
Büyük bir veritabanını, veriyi belirli bir anahtara (Shard Key) göre bölerek birden fazla sunucuya dağıtma işlemidir.
*   **Kullanım:** Büyük ölçekli veritabanları (MongoDB, Cassandra), Multi-tenant sistemler.

### 12. Static Content Hosting
Statik içeriklerin (HTML, CSS, JS, Resim) doğrudan bir depolama servisinden (CDN, S3) sunulmasıdır. Sunucu yükünü azaltır.
*   **Kullanım:** SPA (Single Page Application) hosting, Medya sunucuları.

### 13. Strangler (Boğma)
Eski (Legacy) bir sistemi, yeni özellikler ekleyerek ve eski özellikleri yavaş yavaş yeni sisteme taşıyarak zamanla devre dışı bırakma stratejisidir.
*   **Kullanım:** Monolitik sistemden mikroservislere geçiş.

### 14. Throttling (Rate Limiting)
Bir servise gelen istek sayısını belirli bir süre içinde sınırlayarak aşırı yüklenmeyi önler.
*   **Kullanım:** API Gateway, Public API'ler, DDoS koruması.

---

### Kritik Mülakat Sorusu 

**Soru:** Bir e-ticaret sitesinde "Sipariş Ver" butonu tıklandığında arka planda stok düşülüyor, ödeme alınıyor ve kargo kaydı oluşturuluyor. Bu sistem mikroservis mimarisinde ise, transaction bütünlüğünü nasıl sağlarsınız? Hangi desenleri kullanırsınız?

**Cevap:**
Bu senaryoda **Distributed Transaction** problemi vardır. Çözüm için **Saga Pattern** kullanılmalıdır.
1.  **Saga:** İşlem adımlara bölünür (Stok Servisi, Ödeme Servisi, Kargo Servisi).
2.  **Choreography veya Orchestration:** Servisler event fırlatarak (Choreography) veya merkezi bir yönetici ile (Orchestration) haberleşir.
3.  **Compensating Transaction:** Eğer ödeme alınamazsa, daha önce düşülen stoğu iade etmek için bir "telafi işlemi" (compensating transaction) çalıştırılır.
4.  **Circuit Breaker:** Ödeme servisi yanıt vermiyorsa, sistemin geri kalanını korumak için devre kesici açılır ve kullanıcıya "Şu an işlem yapılamıyor" denir.
5.  **Event Sourcing:** Siparişin tüm durum değişiklikleri (OrderCreated, StockReserved, PaymentFailed, StockReleased) event olarak saklanırsa, hata durumunda iz sürmek kolaylaşır.

#### Soru 2 (Tricky): Sharding (Database Partitioning) yaparken "Re-balancing" sorunu nedir?
**Cevap:** Yeni bir shard (sunucu) eklendiğinde, verilerin yeniden dağıtılması gerekir.
*   **Hash Modulo:** `id % n` kullanırsanız, `n` değiştiğinde neredeyse tüm verilerin yeri değişir (Massive Data Migration).
*   **Çözüm:** **Consistent Hashing** kullanın. Sadece komşu düğümler etkilenir, veri taşıma minimuma iner.

#### Soru 3 (Tricky): Strangler Pattern uygularken en büyük risk nedir?
**Cevap:** Eski ve yeni sistemin aynı anda çalışması sırasında **Veri Senkronizasyonu**.
*   Eski sistem DB'ye yazarken, yeni sistemin de haberdar olması gerekir (veya tam tersi). Çift yazma (Dual Write) veya CDC (Change Data Capture) kullanılmalıdır.

---

