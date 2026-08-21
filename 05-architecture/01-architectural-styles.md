## Konu 26: Yazılım Mimari Stilleri (Architectural Styles)

Yazılım mimarisi, sistemin organizasyonunu, bileşenlerini ve bu bileşenlerin birbirleriyle olan ilişkilerini tanımlar. Bir geliştirici, projenin gereksinimlerine (ölçeklenebilirlik, bakım, dağıtım) en uygun mimariyi seçebilmelidir.

---

### 1. Temel Mimari Stiller

#### 1.1 Monolithic Architecture (Monolitik Mimari)
Tüm uygulamanın tek bir birim (JAR/WAR) olarak paketlendiği ve dağıtıldığı yapıdır.
*   **Avantaj:** Geliştirmesi, test etmesi ve deploy etmesi başlangıçta kolaydır. IDE dostudur.
*   **Dezavantaj:** Büyüdükçe yönetilemez hale gelir ("Big Ball of Mud"). Bir modüldeki hata tüm sistemi çökertebilir. Teknoloji bağımlılığı yaratır.
*   **Kullanım:** Küçük ekipler, MVP (Minimum Viable Product) projeleri.

#### 1.2 Layered Architecture (Katmanlı Mimari)
Sistemi yatay katmanlara ayırır. En yaygın olanı **N-Tier** mimarisidir.
*   **Katmanlar:** Presentation (UI) → Business (Service) → Persistence (DAO) → Database.
*   **Kural:** İstekler yukarıdan aşağıya akar. Bir katman sadece altındaki katmanı bilir.
*   **Avantaj:** Separation of Concerns (İlgi alanlarının ayrımı).
*   **Dezavantaj:** "Sinkhole anti-pattern" (İsteklerin katmanlardan hiçbir iş yapmadan sadece geçmesi).

#### 1.3 Client-Server Architecture
İş yükünün **Sunucu** (kaynak sağlayıcı) ve **İstemci** (hizmet talep eden) arasında paylaşıldığı modeldir.
*   **Örnek:** Web tarayıcısı (Client) ve Web Sunucusu (Server).
*   **Stateless:** HTTP gibi (REST).
*   **Stateful:** FTP veya Telnet gibi.

#### 1.4 Component-Based Architecture
Yazılımın, tekrar kullanılabilir ve değiştirilebilir **bileşenlerden (components)** oluştuğu yapıdır.
*   Her bileşen, iyi tanımlanmış bir arayüze (interface) sahiptir.
*   **Örnek:** Spring Bean'leri, React Component'leri.
*   **Avantaj:** Modülerlik, yeniden kullanılabilirlik.

---

### 2. Dağıtık ve Mesajlaşma Tabanlı Mimariler

#### 2.1 Distributed Architecture (Dağıtık Mimari)
Bileşenlerin ağ üzerinde farklı bilgisayarlarda çalıştığı ve mesajlaşarak iletişim kurduğu sistemlerdir.
*   **Örnek:** Microservices, SOA (Service Oriented Architecture).
*   **Zorluklar:** Network latency, data consistency (CAP teoremi), distributed tracing.

#### 2.2 Event-Driven Architecture (EDA)
Sistemin durum değişikliklerine (olaylara) tepki verdiği mimaridir.
*   **Yapı:** Event Producer → Event Channel → Event Consumer.
*   **Decoupling:** Producer, Consumer'ı bilmez.
*   **Örnek:** E-ticaret siparişi verildiğinde (Event), Stok servisi, Fatura servisi ve Kargo servisi bunu dinler ve işlem yapar.

#### 2.3 Publish-Subscribe (Pub/Sub)
Mesajlaşma tabanlı bir modeldir. Gönderici (Publisher) mesajı belirli bir alıcıya değil, bir konuya (Topic) gönderir.
*   **Subscriber:** O konuya abone olan herkes mesajı alır.
*   **Teknoloji:** Kafka, RabbitMQ (Topic Exchange), Redis Pub/Sub.
*   **Fark:** Message Queue (Point-to-Point) modelinde mesajı sadece bir tüketici alır; Pub/Sub'da hepsi alır.

#### 2.4 Peer-to-Peer (P2P)
Merkezi bir sunucunun olmadığı, her düğümün (node) hem istemci hem de sunucu gibi davrandığı yapıdır.
*   **Örnek:** BitTorrent, Blockchain ağları.
*   **Avantaj:** Tek bir başarısızlık noktası (Single Point of Failure) yoktur. Yüksek ölçeklenebilirlik.

---

### 3. Yapısal Desenler (Structural Patterns)

Mimari stiller sistemin genelini tanımlarken, yapısal desenler sınıfların ve nesnelerin nasıl birleştirileceğini tanımlar (GoF Patterns).

*   **Adapter:** Uyumsuz arayüzleri birbirine bağlar.
*   **Facade:** Karmaşık bir alt sistemi basitleştirilmiş bir arayüzle sunar.
*   **Proxy:** Bir nesneye erişimi kontrol eder (Lazy loading, Security, Logging).
*   **Composite:** Nesneleri ağaç yapısında düzenler (Dosya sistemi: Klasör-Dosya).

---

### 4. Kritik Mülakat Soruları 

#### Soru 1: Monolitik mimariden Mikroservis mimarisine ne zaman geçilmelidir?
**Cevap:**
1.  Uygulama çok büyüdüğünde ve derleme/başlatma süreleri verimliliği düşürdüğünde.
2.  Farklı modüllerin farklı ölçeklenme (scaling) ihtiyaçları olduğunda (örn: Resim işleme servisi çok CPU harcıyor, diğerleri harcamıyor).
3.  Ekip çok büyüdüğünde ve aynı kod tabanında çalışmak (merge conflict) zorlaştığında.
**Uyarı:** "Moda olduğu için" geçilmemelidir. Mikroservisler, dağıtık sistem karmaşıklığını getirir (Distributed Complexity).

#### Soru 2: Event-Driven mimaride "Eventual Consistency" nedir?
**Cevap:** Dağıtık sistemlerde verinin tüm düğümlerde anında tutarlı olması (Strong Consistency) zordur. Eventual Consistency, verinin bir süre sonra (milisaniyeler veya dakikalar) tüm sistemde tutarlı hale geleceğini garanti eder.
*   **Örnek:** Instagram'da birini takip ettiğinizde, takipçi sayısının hemen artmaması ama bir süre sonra güncellenmesi.

*   **Open Layer:** Bir katman atlanabilir. Örneğin, Business katmanı bazen gereksizse, Presentation doğrudan Persistence katmanına erişebilir (Performans için, ama bağımlılığı artırır).

#### Soru 4 (Tricky): Hexagonal, Onion ve Clean Architecture arasındaki temel fark nedir?
**Cevap:** Hepsi aynı amaca (Separation of Concerns, Domain-Centric) hizmet eder ama terminolojileri farklıdır.
*   **Hexagonal (Ports & Adapters):** Dış dünya ile etkileşimi Port ve Adapter'lar üzerinden yapar.
*   **Onion:** Katmanları soğan halkaları gibi dairesel düşünür. Bağımlılıklar hep içe (Domain'e) doğrudur.
*   **Clean:** Onion'un daha rafine halidir (Use Cases, Entities, Interface Adapters).
*   **Ortak Nokta:** Hepsi **Dependency Inversion** kullanır; Domain hiçbir şeye bağlı değildir.

#### Soru 5 (Tricky): Microkernel Architecture (Plugin Pattern) nerede kullanılır?
**Cevap:** Çekirdek bir sistemin (Core System) eklentilerle (Plugins) genişletilmesi gereken yerlerde.
*   **Örnek:** Eclipse IDE, VS Code, Browser Extensions.
*   **Avantaj:** Çekirdek sistem değişmeden yeni özellik eklenebilir.

---

### 5. Geliştirici İpuçları

*   **Mimari Kararlar Kalıcı Değildir:** Başlangıçta Monolitik başlayıp, ihtiyaç duyulduğunda modüler monolite, sonra mikroservislere evrilmek en sağlıklı yoldur (**MonolithFirst** stratejisi).
*   **Fallacies of Distributed Computing:** Dağıtık sistem tasarlarken şu yanılgılara düşmeyin:
    1.  Network güvenilirdir.
    2.  Latency sıfırdır.
    3.  Bandwidth sınırsızdır.
    4.  Network güvenlidir.
*   **Trade-off Analizi:** Mükemmel mimari yoktur. Her seçimin bir maliyeti vardır (Complexity vs Scalability). Senior mühendis bu takası (trade-off) yöneten kişidir.


---

## Konu 27: Yazılım Mimari Desenleri (Architectural Patterns)

Mimari stiller genel yapıyı belirlerken, mimari desenler belirli problemleri çözmek için kullanılan kanıtlanmış şablonlardır. Bir geliştirici, **DDD**, **CQRS**, **Event Sourcing** ve **Microservices** gibi modern desenlere hakim olmalıdır.

---

### 1. Domain-Driven Design (DDD)

Yazılımın karmaşıklığını, iş alanının (domain) karmaşıklığıyla eşleştirmeyi amaçlayan bir yaklaşımdır.

*   **Ubiquitous Language (Ortak Dil):** Yazılımcılar ve iş uzmanları (domain experts) aynı dili konuşmalıdır. Kodda geçen `Policy`, `Claim`, `Premium` terimleri iş dünyasındakiyle aynı olmalıdır.
*   **Bounded Context:** Büyük bir domaini, sınırları belli alt domainlere bölmektir. Her context'in kendi modeli ve veritabanı olabilir.
*   **Entity vs Value Object:**
    *   **Entity:** Kimliği (ID) olan nesneler (User, Order).
    *   **Value Object:** Kimliği olmayan, değerleriyle tanımlanan nesneler (Address, Money). Immutable olmalıdır.
*   **Aggregate Root:** Bir grup nesneyi (Aggregate) yöneten ana entity. Dış dünya sadece Aggregate Root ile konuşur. (Örn: `Order` root'tur, `OrderItem`'a sadece `Order` üzerinden erişilir).

---

### 2. CQRS (Command Query Responsibility Segregation)

Okuma (Query) ve Yazma (Command) işlemlerini farklı modellerle, hatta farklı veritabanlarıyla ayırma desenidir.

*   **Command Model:** Veriyi değiştirir, karmaşık iş kuralları ve validasyon içerir. (Write-heavy).
*   **Query Model:** Veriyi okur, join gerektirmeyen, okuma için optimize edilmiş (denormalize) DTO'lar döner. (Read-heavy).
*   **Avantaj:** Okuma ve yazma performansını bağımsız ölçekleyebilme.
*   **Dezavantaj:** Karmaşıklık artar, Eventual Consistency yönetimi gerekir.

---

### 3. Event Sourcing

Verinin son durumunu saklamak yerine, duruma etki eden **olayların (events)** sırasını saklama yöntemidir.

*   **Mantık:** `AccountBalance = 100` yerine `[AccountCreated, MoneyDeposited(50), MoneyDeposited(50)]` saklanır.
*   **Replay:** Olayları baştan oynatarak sistemin herhangi bir andaki durumu yeniden oluşturulabilir.
*   **Audit Log:** Doğal bir denetim izi (audit trail) sağlar.
*   **Kullanım:** Bankacılık, muhasebe sistemleri. Genellikle CQRS ile birlikte kullanılır.

---

### 4. Microservices Architecture

Uygulamanın küçük, bağımsız, kendi process'inde çalışan ve hafif mekanizmalarla (HTTP/REST, Messaging) iletişim kuran servisler olarak geliştirilmesidir.

*   **Özellikler:**
    *   **Bağımsız Deploy:** Bir servisi güncellemek diğerlerini etkilemez.
    *   **Teknoloji Bağımsızlığı:** Biri Java, diğeri Go ile yazılabilir.
    *   **Database per Service:** Her servisin kendi veritabanı vardır.
*   **Zorluklar:** Dağıtık transaction (Saga), servis keşfi (Eureka), hata toleransı (Circuit Breaker).

---

### 5. Serverless Architecture (FaaS)

Sunucu yönetimiyle uğraşmadan, sadece kodun (fonksiyonun) çalıştırıldığı modeldir (AWS Lambda, Google Cloud Functions).

*   **Özellikler:**
    *   **Event-Triggered:** HTTP isteği, DB değişikliği veya zamanlayıcı ile tetiklenir.
    *   **Stateless:** Fonksiyonlar durum tutmaz.
    *   **Pay-per-use:** Sadece çalıştığı süre kadar ödeme yapılır.
*   **Cold Start:** Fonksiyon uzun süre çalışmazsa, ilk istekte başlatma gecikmesi yaşanabilir.

---

### 6. Diğer Önemli Desenler

#### 6.1 Model-View-Controller (MVC)
Kullanıcı arayüzü (View), veri (Model) ve iş mantığını (Controller) ayırır. Web framework'lerinin (Spring MVC) temelidir.

#### 6.2 Blackboard Pattern
Çözüme ulaşmak için farklı uzmanlık alanlarına sahip bileşenlerin (Knowledge Sources) ortak bir veri alanını (Blackboard) kullanarak işbirliği yaptığı desendir.
*   **Kullanım:** Yapay zeka, ses tanıma, karmaşık problem çözme.

#### 6.3 Microkernel Architecture (Plug-in Architecture)
Çekirdek bir sistem (Core System) ve ona eklenebilen eklentilerden (Plugins) oluşur.
*   **Örnek:** Eclipse IDE, VS Code, Chrome Tarayıcı.
*   **Core:** Temel işlevleri sağlar.
*   **Plugins:** Ekstra özellikler katar, core'u değiştirmeden sistemi genişletir.

#### 6.4 Service Oriented Architecture (SOA)
Mikroservislerin atasıdır. Genellikle SOAP ve ESB (Enterprise Service Bus) kullanır. Servisler daha büyüktür ve ESB üzerinde akıllı borular (smart pipes) kullanılır. Mikroservislerde ise "Smart endpoints and dumb pipes" ilkesi geçerlidir.

---

### 7. Kritik Mülakat Soruları 

#### Soru 1: CQRS ve Event Sourcing her projede kullanılmalı mı?
**Cevap:** Hayır. Bu desenler ciddi bir karmaşıklık (complexity) getirir. Sadece yüksek performans gerektiren, karmaşık iş kuralları olan veya audit log'un kritik olduğu (finans) sistemlerde kullanılmalıdır. Basit CRUD uygulamaları için "Over-Engineering" olur.

#### Soru 2: "Database per Service" pattern'inde join işlemleri nasıl yapılır?
**Cevap:** Veritabanları ayrı olduğu için SQL JOIN yapılamaz.
1.  **API Composition:** Bir üst katman (API Gateway veya Aggregator Service) gerekli servisleri çağırıp veriyi bellekte birleştirir.
2.  **Data Replication (CQRS):** İhtiyaç duyulan veri, domain event'leri dinlenerek servisin kendi veritabanına (read model) kopyalanır.

#### Soru 3: Saga Pattern nedir?
**Cevap:** Mikroservislerde dağıtık transaction yönetimi için kullanılır.
*   **Choreography:** Servisler birbirine event fırlatır.
*   **Orchestration:** Merkezi bir orkestratör (State Machine) süreci yönetir.
*   Bir adım başarısız olursa, önceki adımları geri almak için **Compensating Transaction** (telafi edici işlem) çalıştırılır.

#### Soru 4 (Tricky): CQRS ve Event Sourcing kullanırken "Eventual Consistency" nasıl yönetilir?
**Cevap:** Kullanıcı bir işlem yaptığında (Command), arayüzde hemen güncel veriyi göstermek için **Optimistic UI** (sunucudan cevap beklemeden arayüzü güncelleme) kullanılabilir.
*   Ancak gerçek veri (Query Model) bir süre sonra güncellenecektir. Kullanıcı sayfayı yenilerse eski veriyi görebilir. Bu durum kullanıcıya "İşleminiz alındı, işleniyor" mesajı ile bildirilmelidir.

#### Soru 5 (Tricky): Microservices'de Distributed Transaction için 2PC (Two-Phase Commit) neden önerilmez?
**Cevap:** 2PC bloklayan bir protokoldür (Blocking). Koordinatör çökerse tüm servisler kilitli kalır. Performansı çok düşürür ve Availability'i (CAP teoremindeki A) azaltır. Saga Pattern (Asenkron) tercih edilmelidir.

---

### 8. Geliştirici İpuçları

*   **Context Mapping:** DDD'de Bounded Context'ler arasındaki ilişkiyi (Partnership, Shared Kernel, Customer-Supplier, Anti-Corruption Layer) haritalamak, entegrasyon stratejisi için kritiktir.
*   **Strangler Fig Pattern:** Monolitik bir uygulamayı mikroservislere dönüştürürken, yeni özellikleri mikroservis olarak yazıp, eski özellikleri yavaş yavaş boğarak (yerini alarak) ilerleyin. Big Bang rewrite'tan kaçının.
*   **Idempotency:** Dağıtık sistemlerde (Message Queues, Event Sourcing) aynı mesajın birden fazla kez işlenmesi (duplicate processing) kaçınılmazdır. İşlemleriniz **Idempotent** (tekrar çalıştırıldığında sonucu değiştirmeyen) olmalıdır.


---

