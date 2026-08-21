## Konu 41: Mikroservis Mimarisi ve Dağıtık Sistem Senaryoları (30 Real-World Cases)

Bu bölüm, mikroservis mimarisinin en zorlu konularını (Distributed Transactions, Failure Modes, Consistency) 30 farklı gerçek hayat senaryosu üzerinden ele alır. Mülakatlarda "X olursa ne yaparsın?" sorularına verilecek en profesyonel cevaplar buradadır.

---

### A. Distributed Transactions & Data Consistency

**Senaryo 1: Saga Pattern (Orchestration) - E-Ticaret Siparişi**
*   **Soru:** "Sipariş -> Ödeme -> Stok servisleri var. Ödeme başarılı ama stok düşerken hata aldık. Ödemeyi nasıl geri alırız?"
*   **Cevap:** **Orchestrator** (Merkezi Yönetici) tabanlı Saga kullanırım.
    *   Sipariş Servisi (Orchestrator), işlemleri sırayla çağırır.
    *   Stok servisi hata dönerse, Orchestrator "Compensating Transaction" (Telafi İşlemi) başlatır ve Ödeme Servisine "İade Yap" (Refund) komutu gönderir.

**Senaryo 2: Saga Pattern (Choreography) - Event Bazlı Akış**
*   **Soru:** "Merkezi bir yönetici olmadan servisleri nasıl konuşturursun?"
*   **Cevap:** **Choreography** (Koreografi) kullanırım. Servisler birbirini dinler.
    *   Sipariş Servisi `OrderCreated` eventi atar.
    *   Ödeme Servisi bunu dinler, ödemeyi alır ve `PaymentProcessed` atar.
    *   Stok Servisi bunu dinler. Hata olursa `StockFailed` atar.
    *   Ödeme Servisi `StockFailed` dinler ve iade yapar. (Daha karmaşık ama daha az bağımlı).

**Senaryo 3: Two-Phase Commit (2PC/XA) - Neden Kullanılmaz?**
*   **Soru:** "Neden basitçe veritabanı seviyesinde distributed transaction (XA) yapmıyoruz?"
*   **Cevap:** Mikroservislerde **2PC Anti-Pattern**'dir.
    *   **Blocking:** Koordinatör onay verene kadar tüm servisler DB kilidini tutar. Performans yerle bir olur.
    *   **SPOF:** Koordinatör çökerse sistem kilitlenir.
    *   Sadece aynı DB instance içindeki tablolarda transaction kullanırım, servisler arası Saga tercih ederim.

**Senaryo 4: TCC (Try-Confirm-Cancel) - Rezervasyon Sistemi**
*   **Soru:** "Uçak ve Otel rezervasyonu yapıyoruz. İkisi de kesinleşmeden ödeme almak istemiyoruz."
*   **Cevap:** TCC pattern kullanırım.
    *   **Try:** Uçak ve Otel'den "geçici rezervasyon" (soft lock) yaparım.
    *   **Confirm:** İkisi de "OK" derse, ödemeyi alır ve rezervasyonu kesinleştiririm.
    *   **Cancel:** Biri hata verirse, diğerindeki geçici rezervasyonu iptal ederim.

**Senaryo 5: Dual Write Problem - DB ve Kafka Tutarsızlığı**
*   **Soru:** "Kullanıcıyı DB'ye kaydettim, sonra Kafka'ya 'UserCreated' eventi atarken network koptu. DB'de var ama Kafka'da yok. Ne olur?"
*   **Cevap:** Veri tutarsızlığı olur. Çözüm **Transactional Outbox Pattern**.
    *   User tablosuna ve `Outbox` tablosuna **aynı transaction** içinde yazarım. (Atomik).
    *   Debezium (CDC) veya bir job, Outbox tablosunu okuyup Kafka'ya atar.

**Senaryo 6: Eventual Consistency - Kullanıcı Deneyimi (UX)**
*   **Soru:** "Kullanıcı profilini güncelledi ama arama sayfasında hala eski ismini görüyor (Replikasyon gecikmesi). Şikayet ediyor."
*   **Cevap:** Bu **Eventual Consistency**'dir.
    *   **UX Çözümü:** Kullanıcıya "Güncelleme alındı, yansıması zaman alabilir" uyarısı gösteririm.
    *   **Teknik Çözüm:** Kullanıcının kendi profilini gördüğü endpoint'te "Read-Your-Writes" consistency uygularım (Master'dan okuturum veya versiyon kontrolü yaparım).

**Senaryo 7: Distributed ID Generation - Snowflake**
*   **Soru:** "Sharding yapılmış 10 veritabanımız var. Auto-increment ID kullanamıyoruz (çakışır). UUID çok yer kaplıyor. Ne yapalım?"
*   **Cevap:** **Twitter Snowflake** algoritması (veya benzeri TSID) kullanırım.
    *   64-bit Long: Timestamp + Machine ID + Sequence No.
    *   Hem benzersizdir, hem zamana göre sıralıdır (Index performansı için), hem de Long olduğu için az yer kaplar.

---

### B. Resiliency & Failure Modes

**Senaryo 8: Circuit Breaker - Hızlı Hata**
*   **Soru:** "Ödeme servisi yanıt vermiyor (Timeout). Sipariş servisi her istekte 30 saniye bekliyor, tüm threadler doldu ve sistem çöktü."
*   **Cevap:** **Circuit Breaker** (Sigorta) kullanırım (Resilience4j).
    *   Hata oranı %50'yi geçerse devre "AÇIK" konuma gelir.
    *   Gelen isteklere beklemeden anında hata dönerim (Fail Fast). Sistem kaynakları tükenmez.

**Senaryo 9: Bulkhead Pattern - Gemi Bölmeleri**
*   **Soru:** "Resim yükleme servisi çok yavaşladı. Bu yüzden Login servisi de cevap veremiyor. Alakasız servisler neden etkileniyor?"
*   **Cevap:** Çünkü Tomcat thread havuzu ortaktır. Resim yükleme tüm threadleri bitirmiştir.
    *   **Çözüm:** **Bulkhead Pattern**. Her servis client'ı için ayrı thread havuzu (veya semaphore) tanımlarım. Resim servisi ölse bile Login threadleri ayrı olduğu için çalışmaya devam eder.

**Senaryo 10: Retry Storm - Tekrar Deneme Fırtınası**
*   **Soru:** "Bir servis anlık gitti. Diğer servisler sürekli Retry yaptı. Servis ayağa kalkmaya çalışırken milyonlarca istek altında tekrar ezildi."
*   **Cevap:**
    *   **Exponential Backoff:** Bekleme süresini artırarak dene (1sn, 2sn, 4sn...).
    *   **Jitter:** Herkes aynı anda denemesin diye süreye rastgelelik ekle (1.1sn, 2.3sn...).

**Senaryo 11: Rate Limiting (Distributed) - Token Bucket**
*   **Soru:** "10 tane API Gateway sunucumuz var. Toplamda saniyede 1000 isteğe izin vermeliyiz. Local rate limit işe yaramaz."
*   **Cevap:** **Distributed Rate Limiting** (Redis ile).
    *   Token Bucket algoritması kullanırım. Redis'te ortak bir sayaç tutarım (Lua script ile atomik azaltırım).

**Senaryo 12: Fallback Strategies - Graceful Degradation**
*   **Soru:** "Öneri (Recommendation) servisi çöktü. Ana sayfa boş mu gelsin?"
*   **Cevap:** Hayır. **Fallback** devreye girer.
    *   Kişiye özel öneri yerine, Redis'ten "En Çok Satanlar" listesini (statik cache) döndürürüm. Kullanıcı hata görmez, sadece deneyim biraz düşer.

**Senaryo 13: Cascading Failures - Domino Etkisi**
*   **Soru:** "Servis A -> B -> C'yi çağırıyor. C çöktü. B bekliyor, A bekliyor. Tüm zincir kilitlendi."
*   **Cevap:** Her çağrıda mutlaka **Timeout** olmalı. Ayrıca A ve B'de Circuit Breaker olmalı. C çöktüyse B bunu hemen anlamalı ve A'ya hata dönmeli.

---

### C. Communication & API Gateway

**Senaryo 14: BFF (Backend for Frontend)**
*   **Soru:** "Mobil uygulama ekibi, Web için dönülen devasa JSON'ı parse etmekte zorlanıyor ve gereksiz data (bandwidth) harcıyor."
*   **Cevap:** **BFF Pattern**.
    *   Web için ayrı, Mobil için ayrı API Gateway (veya servis) yazarım. Mobil BFF sadece mobilin ihtiyacı olan küçük datayı döner.

**Senaryo 15: Sync vs Async - Ne Zaman Hangisi?**
*   **Soru:** "Kullanıcı 'Kayıt Ol'a bastı. Hoşgeldin emaili atmalıyız. Email servisini REST ile mi çağıralım?"
*   **Cevap:** Hayır, **Async** (RabbitMQ/Kafka) kullanırım.
    *   Kullanıcıyı emailin gitmesini bekletmek (latency) yanlıştır.
    *   Kayıt işlemini yapıp kuyruğa mesaj atarım. Email servisi ne zaman müsaitse o zaman atar.

**Senaryo 16: API Versioning - Breaking Change**
*   **Soru:** "User API'de `name` alanını `firstName` ve `lastName` diye ikiye böldük. Eski mobil uygulamalar patladı."
*   **Cevap:** Asla var olan alanı silmem.
    *   **Versioning:** `/v1/users` (eski) ve `/v2/users` (yeni) endpointleri sunarım.
    *   Veya Header bazlı versiyonlama yaparım (`Accept-Version: v2`).

**Senaryo 17: Service Discovery - Adres Defteri**
*   **Soru:** "Bulut ortamında servislerin IP'si sürekli değişiyor. Hard-code IP yazamayız."
*   **Cevap:** **Service Discovery** (Eureka, Consul, K8s Service).
    *   Servis ayağa kalkınca kendini Registry'ye kaydeder ("Ben Payment, IP'm bu").
    *   Diğer servisler Registry'den adresi sorar.

**Senaryo 18: gRPC vs REST Performance**
*   **Soru:** "Mikroservisler arası iletişim çok yavaşladı. JSON parse maliyeti yüksek."
*   **Cevap:** İç iletişimde **gRPC** (Protobuf) geçerim.
    *   Binary format olduğu için JSON'dan çok daha küçüktür ve parse etmesi (serialization/deserialization) çok daha hızlıdır.

---

### D. Data Management & Querying

**Senaryo 19: CQRS (Command Query Responsibility Segregation)**
*   **Soru:** "Sipariş veritabanı çok yoğun. Rapor almak isteyenler sistemi kilitliyor."
*   **Cevap:** **CQRS** ile Okuma ve Yazma modellerini ayırırım.
    *   **Write DB (MySQL):** Sadece sipariş oluşturma/güncelleme. (Normalize).
    *   **Read DB (Elasticsearch/Mongo):** Raporlama için optimize edilmiş, denormalize veri.
    *   İkisi arasındaki senkronizasyonu Kafka (Eventual Consistency) ile sağlarım.

**Senaryo 20: API Composition - Data Aggregation**
*   **Soru:** "Sipariş detay sayfasında User, Order ve Payment bilgisini göstermeliyiz. 3 ayrı servise istek atmak client'ı yoruyor."
*   **Cevap:** **API Gateway Aggregation**.
    *   Client tek istek atar. Gateway arkada 3 servise paralel istek atar, sonuçları birleştirir (Aggregate) ve client'a tek JSON döner.

**Senaryo 21: Database per Service - Join Sorunu**
*   **Soru:** "Her servisin kendi DB'si var. Peki 'İstanbul'daki kullanıcıların son siparişlerini' nasıl joinleyip çekeceğiz?"
*   **Cevap:** Distributed Join yapamayız.
    *   **Çözüm 1 (Data Replication):** Sipariş servisinde, kullanıcı şehir bilgisini de (redundant) tutarım.
    *   **Çözüm 2 (Data Warehouse):** Analitik sorgular için verileri ETL ile ortak bir ambara (BigQuery, Snowflake) taşırım.

**Senaryo 22: Shared Database Anti-Pattern**
*   **Soru:** "Neden tüm servisleri tek bir büyük Oracle DB'ye bağlamıyoruz? Transaction yönetimi kolay olurdu."
*   **Cevap:** **Tight Coupling** (Sıkı Bağımlılık) yaratır.
    *   Tabloda bir kolon değiştirirseniz 10 servis birden patlayabilir.
    *   DB darboğaz (Bottleneck) olur.
    *   Her servis kendi teknolojisini (Mongo, Postgres, Neo4j) seçemez.

---

### E. Deployment & Observability

**Senaryo 23: Blue-Green Deployment - Sıfır Kesinti**
*   **Soru:** "Yeni versiyonu deploy ederken sistemin 1 saniye bile durmaması lazım. Ve hata varsa anında geri almalıyız."
*   **Cevap:** **Blue-Green**.
    *   Blue (Eski) çalışırken, Green (Yeni) ortamı hazırlarım.
    *   Load Balancer'ı anında Green'e yönlendiririm. Hata varsa anında Blue'ya geri çekerim.

**Senaryo 24: Canary Release - Risk Azaltma**
*   **Soru:** "Yeni ödeme altyapısını tüm kullanıcılara açmaya korkuyoruz."
*   **Cevap:** **Canary Deployment**.
    *   Trafiğin sadece %1'ini yeni versiyona yönlendiririm.
    *   Hata yoksa %5, %10, %50 diye artırırım.

**Senaryo 25: Distributed Tracing - Samanlıkta İğne**
*   **Soru:** "Kullanıcı hata aldı ama loglarda 10 servis var. Hatanın hangisinde olduğunu nasıl bulacağız?"
*   **Cevap:** **Distributed Tracing** (Zipkin/Jaeger).
    *   Gateway'de bir `TraceID` üretirim. Bu ID tüm servis çağrılarında header ile taşınır.
    *   Logları bu ID ile arattığımda tüm akışı (Flow) görürüm.

**Senaryo 26: Log Aggregation - Merkezi Log**
*   **Soru:** "100 tane container var. Hangi makineye girip log bakacağız?"
*   **Cevap:** Hiçbirine. Loglar **ELK Stack** (Elasticsearch, Logstash, Kibana) veya Graylog gibi merkezi bir yere akmalı.

**Senaryo 27: Centralized Configuration**
*   **Soru:** "50 servisin log seviyesini DEBUG yapmak için hepsini yeniden mi başlatacağız?"
*   **Cevap:** Hayır. **Spring Cloud Config** veya **Consul** kullanırım.
    *   Konfigürasyonu git/consul üzerinden değiştiririm. `@RefreshScope` ile servisler restart olmadan yeni ayarı alır.

---

### F. Security

**Senaryo 28: AuthZ/AuthN - Token Relay**
*   **Soru:** "Her mikroserviste tekrar tekrar kullanıcı adı/şifre mi soracağız?"
*   **Cevap:** Hayır. Kimlik doğrulama (AuthN) **Gateway** veya **Auth Service**'te yapılır.
    *   Gateway, geçerli istekler için bir **JWT** (veya Opaque Token) üretir.
    *   Arka plandaki servislere bu token iletilir (Token Relay). Servisler sadece token imzasını/yetkisini (AuthZ) kontrol eder.

**Senaryo 29: Service-to-Service Security - mTLS**
*   **Soru:** "Bir hacker iç ağımıza sızdı. Servisleri direkt çağırabilir mi?"
*   **Cevap:** Evet, eğer HTTP ise çağırır.
    *   **Çözüm:** **mTLS** (Mutual TLS). Servisler birbirini sertifika ile doğrular. (Genelde Istio/Linkerd gibi Service Mesh araçları bunu otomatik yapar).

**Senaryo 30: Sensitive Data (PII) Masking**
*   **Soru:** "Loglarda müşterinin kredi kartı numarası açık görünüyor!"
*   **Cevap:** Büyük güvenlik açığı.
    *   Log kütüphanesinde (Logback/Log4j) **Masking Pattern** tanımlarım. Kredi kartı, TCKN gibi regex'e uyan verileri `****` ile maskelerim.

---

