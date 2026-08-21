## Konu 31: Sistem Tasarımı - Veritabanları, Caching ve İletişim (Part 2)

Sistem tasarımının ikinci bölümünde, doğru veritabanı seçimi, caching stratejileri, iletişim protokolleri ve kaçınılması gereken anti-pattern'leri inceliyoruz.

---

### 1. Veritabanı Seçimi ve Ölçekleme

#### 1.1 SQL (RDBMS) vs NoSQL
*   **RDBMS (MySQL, PostgreSQL):** Yapısal veri, ACID transaction, karmaşık JOIN'ler. (Finans, E-ticaret).
*   **NoSQL:** Esnek şema, yüksek ölçeklenebilirlik, BASE prensibi.
    *   **Key-Value (Redis, DynamoDB):** Basit okuma/yazma, Caching, Session.
    *   **Document (MongoDB):** JSON benzeri veri, esnek şema. (CMS, Katalog).
    *   **Wide Column (Cassandra):** Devasa veri yazma, zaman serisi. (Loglar, IoT).
    *   **Graph (Neo4j):** İlişkisel veri. (Sosyal ağlar, Öneri motorları).

#### 1.2 Veritabanı Ölçekleme Teknikleri
*   **Replication (Master-Slave):** Yazma Master'a, okuma Slave'lere. Okuma performansını artırır.
*   **Sharding (Partitioning):** Veriyi yatay olarak böler (örn: User ID % 10). Yazma kapasitesini artırır ama JOIN ve Transaction zorlaşır.
*   **Federation:** Veritabanını fonksiyonel olarak böler (User DB, Order DB, Product DB).
*   **Denormalization:** Okuma performansını artırmak için veri tekrarı yapılır (JOIN'den kaçınmak için).

---

### 2. Caching Stratejileri

Önbellekleme, performansı artırmanın en etkili yoludur. "En hızlı istek, hiç yapılmayan istektir."

#### 2.1 Caching Patterns
*   **Cache Aside (Lazy Loading):** Uygulama önce Cache'e bakar. Yoksa DB'den çeker ve Cache'e yazar. (En yaygın).
*   **Write-Through:** Uygulama Cache'e yazar, Cache de DB'ye yazar. (Veri tutarlılığı yüksek, yazma yavaş).
*   **Write-Behind (Write-Back):** Uygulama Cache'e yazar, Cache asenkron olarak DB'ye yazar. (Yazma çok hızlı, veri kaybı riski var).
*   **Refresh-Ahead:** Cache süresi dolmadan otomatik olarak yenilenir. (Sıcak veriler için).

#### 2.2 Caching Seviyeleri
*   **Client Caching:** Browser cache (HTTP Headers).
*   **CDN Caching:** Statik içerik.
*   **Web Server Caching:** Reverse Proxy (Nginx) cache.
*   **Application Caching:** In-memory (Ehcache) veya Distributed (Redis).
*   **Database Caching:** DB'nin kendi buffer pool'u.

---

### 3. İletişim Protokolleri

*   **HTTP/REST:** Standart, metin tabanlı, stateless. (Public API'ler).
*   **gRPC:** Google tarafından geliştirilen, Protocol Buffers (binary) kullanan, yüksek performanslı framework. (Mikroservisler arası iletişim).
*   **GraphQL:** İstemcinin tam olarak ne istediğini sorgulayabildiği sorgu dili. (Mobile, Frontend).
*   **TCP vs UDP:**
    *   **TCP:** Güvenilir, sıralı, bağlantı tabanlı. (Web, Email, Dosya).
    *   **UDP:** Hızlı, güvensiz, bağlantısız. (Video streaming, Oyun, DNS).

---

### 4. Background Jobs (Arka Plan İşleri)

Kullanıcıyı bekletmemek için uzun süren işler asenkron yapılmalıdır.

*   **Event-Driven:** Bir olay olduğunda tetiklenir (Sipariş verildi -> Fatura oluştur).
*   **Schedule-Driven:** Belirli zamanlarda çalışır (Her gece rapor oluştur).
*   **Idempotency:** Arka plan işleri tekrar çalıştırılabilir olmalıdır. (Aynı mesaj iki kere gelirse, iki fatura kesilmemeli).

---

### 5. Performance Anti-Patterns (Kaçınılması Gerekenler)

1.  **N+1 Query Problem (Extraneous Fetching):** Gereksiz yere döngü içinde DB sorgusu atmak.
2.  **Synchronous I/O:** I/O işlemini beklerken thread'i bloklamak. (Non-blocking I/O kullanın).
3.  **Chatty I/O:** Çok sayıda küçük istek atmak yerine, tek seferde toplu veri çekin (Batching).
4.  **Noisy Neighbor:** Bulut ortamında, kaynakları sömüren başka bir servisin sizin performansınızı etkilemesi. (Resource Isolation / Quotas).
5.  **Retry Storm:** Bir servis çöktüğünde, herkesin aynı anda tekrar denemesi ve servisi daha da batırması. (Exponential Backoff kullanın).
6.  **Monolithic Persistence:** Tüm servislerin tek bir devasa veritabanına bağlanması. (Database per Service kullanın).

#### Soru 7 (Tricky): Cache Aside pattern kullanırken "Stale Data" (Eski Veri) riski nasıl yönetilir?
**Cevap:** Veri DB'de güncellendiğinde cache'teki veri eski kalır.
*   **Çözüm:** DB'ye yazarken cache'i silin (Cache Invalidation) veya TTL (Time-To-Live) süresini kısa tutun.
*   **Trap:** Cache'i güncellemeye çalışmayın (Race condition olabilir), silmek daha güvenlidir.

#### Soru 8 (Tricky): gRPC tarayıcılar (Browser) tarafından doğrudan desteklenir mi?
**Cevap:** Hayır. gRPC HTTP/2 ve binary protokol kullanır, tarayıcılar ise HTTP/1.1 ve text tabanlıdır (veya tam gRPC desteği yoktur).
*   **Çözüm:** **gRPC-Web** proxy (Envoy) kullanılarak tarayıcıdan gelen istekler gRPC'ye çevrilir.

---

### 6. Kritik Mülakat Sorusu 

**Soru:** "Twitter (X) Timeline" tasarımını nasıl yaparsınız? (System Design)

**Cevap:**
*   **Okuma/Yazma Oranı:** Okuma çok daha fazladır (Read-heavy).
*   **Pull Model (Fan-out on Load):** Kullanıcı timeline'a girdiğinde, takip ettiği kişilerin son tweetlerini DB'den çeker ve birleştirir. (Takipçi sayısı az olanlar için uygun).
*   **Push Model (Fan-out on Write):** Birisi tweet attığında, takipçilerinin "hazır timeline" listesine (Redis List) bu tweet ID'si eklenir. Kullanıcı girdiğinde direkt Redis'ten okur. (Okuma çok hızlıdır).

---
