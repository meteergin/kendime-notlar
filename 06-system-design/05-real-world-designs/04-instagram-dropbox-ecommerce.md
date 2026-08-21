## Konu 36: Gerçek Dünya Sistem Tasarımları - Part 4 & 5

Bu final bölümde Amazon, Google Search, Spotify, Zoom, Dropbox ve Airbnb sistemlerinin tasarımlarını inceliyoruz.

---

### 10. Amazon - E-ticaret Platformu

**Kullanıcı:** 300+ milyon aktif müşteri.

#### Temel Gereksinimler
*   **Functional:** Ürün arama, sepet, checkout, ödeme, sipariş takibi, öneri sistemi, kullanıcı yorumları.
*   **Non-Functional:** %99.99 availability (downtime = revenue loss), low latency, massive inventory.

#### Sistem Bileşenleri

**1. Product Catalog Service**
*   **Database Sharding:** Ürünler kategori ve coğrafi konum bazlı sharding yapılır.
*   **Search (Elasticsearch):** Ürün adı, marka, kategori bazlı full-text search.

**2. Recommendation Engine**
*   **Collaborative Filtering:** "Bunu satın alanlar şunları da aldı."
*   **Content-Based:** Kullanıcının geçmişte baktığı/satın aldığı ürünlere benzer ürünler.

**3. Shopping Cart Service**
*   **Database:** DynamoDB (Key-Value). Cart data = User ID.
*   **Stateless:** Cart sunucuda saklanır (client değil). Kullanıcı farklı cihazdan girdiğinde aynı sepeti görür.

**4. Order Service & Inventory**
*   **2-Phase Commit (veya Saga):** Sipariş → Stok düşme → Ödeme → Kargo kaydı.
*   **Eventual Consistency:** Stok miktarı real-time güncel olmayabilir. "Stokta kalmadı" hatası checkout'ta verilir.

**5. Payment Service**
*   **PCI-DSS Compliance:** Kredi kartı bilgileri vault'ta şifrelenmiş saklanır.
*   **Retry Logic:** Ödeme gateway timeout verirse 3 kere tekrar denenir (idempotency key ile).

#### Kritik Soru

**Soru: Black Friday'de trafik 100x arttığında nasıl ölçeklenir?**
*   **Cevap:**
    *   **Auto Scaling:** EC2 instance'lar otomatik scaling group ile artar.
    *   **CDN (CloudFront):** Statik içerikler (resim, JS) CDN'den sunulur.
    *   **Queue-Based Checkout:** Yüksek yük anında checkout işlemi SQS kuyruğuna alınır, asenkron işlenir.

---

### 11. Google Search - Arama Motoru

**Kullanıcı:** 8.5+ milyar arama/gün.

#### Sistem Bileşenleri

**1. Web Crawling (Googlebot)**

#### Soru 2 (Tricky): Amazon Sepet (Shopping Cart) verisi neden Redis yerine DynamoDB'de tutulur?
**Cevap:** Redis in-memory'dir ve kalıcılık (persistence) konusunda DynamoDB kadar güvenilir değildir (özellikle büyük ölçekte). Sepet verisi "finansal değeri olan" veridir, kaybolmamalıdır. DynamoDB, yüksek yazma hızı ve kalıcılık sağlar.

#### Soru 3 (Tricky): Google Crawler "Spider Trap" (Sonsuz Döngü) tuzağına nasıl düşer?
**Cevap:** Dinamik URL üreten siteler (örn: takvim sayfaları `next_month=...`) sonsuz sayıda benzersiz URL oluşturabilir.
*   **Çözüm:** URL uzunluğunu sınırlamak, path derinliğini sınırlamak ve aynı domain için crawl bütçesi (crawl budget) koymak.

#### Soru 4 (Tricky): Spotify Offline Mode senkronizasyonu nasıl çalışır?
**Cevap:** Kullanıcı offline iken playlist düzenlerse, bu değişiklikler yerel veritabanında saklanır. Online olunca sunucuya gönderilir.
*   **Conflict:** Eğer sunucudaki playlist de değişmişse, genellikle "son yazan kazanır" (Last Write Wins) veya birleştirme (Merge) stratejisi uygulanır.

---
*   **Crawler:** Web'deki milyarlarca sayfayı periyodik olarak tarar.
*   **Scheduler:** Hangi URL'lerin ne sıklıkla taranacağını belirler (popüler siteler daha sık).

**2. Indexing**
*   **Inverted Index:** Her kelime için, o kelimeyi içeren dokümanların listesi.
    *   Örnek: "apple" → [doc1, doc5, doc9...]
*   **Distributed Storage:** Bigtable (Google'ın NoSQL DB'si).

**3. Ranking (PageRank & RankBrain)**
*   **PageRank:** Sayfanın önem skoru (backlink sayısı ve kalitesine göre).
*   **RankBrain (ML):** Kullanıcı davranışı (tıklama, sayfada geçirilen süre) ile ranking dinamik güncellenir.

**4. Query Processing**
*   Query gelir → Spell check → Synonym expansion → Index query → Ranking → Results.
*   **Latency:** <200ms (ağırlıklı cache ile).

#### Kritik Soru

**Soru: Yeni bir web sitesi yayınlandı. Google kaç gün sonra bulur?**
*   **Cevap:** Sitemap gönderilirse birkaç saat içinde. Yoksa, crawler'ın rastgele bulması haftalar sürebilir. Backlink sayısı artarsa öncelik verilir.

---

### 12. Spotify - Müzik Streaming Platformu

**Kullanıcı:** 500+ milyon.

#### Sistem Bileşenleri

**1. Music Catalog & Metadata**
*   **Database:** Cassandra. Şarkı metadata (başlık, sanatçı, albüm, süre).

**2. Audio Streaming**
*   **Format:** Ogg Vorbis (320 kbps, 160 kbps, 96 kbps).
*   **Adaptive Bitrate:** Network hızına göre kalite değişir.
*   **CDN:** Audio dosyaları Google/Amazon CDN'de cache'lenir.

**3. Recommendation (Discover Weekly)**
*   **Collaborative Filtering:** Benzer kullanıcıların dinledği şarkılar.
*   **Natural Language Processing:** Blog/sosyal medya'da şarkıdan bahseden metinler analiz edilir.

**4. Offline Mode**
*   Kullanıcı şarkıları indirebilir. Şifrelenmiş olarak cihazda saklanır (DRM).

#### Kritik Soru

**Soru: 10 milyon kullanıcı aynı şarkıyı dinlerse CDN bandwidth maliyeti çok artar. Nasıl optimize edilir?**
*   **Cevap:** P2P teknolojisi. Kullanıcılar birbirlerinden de chunk alır (BitTorrent benzeri). Spotify Desktop client eskiden bunu kullanıyordu.

---

### 13. Zoom - Video Conferencing

**Kullanıcı:** 300+ milyon daily meeting participants.

#### Sistem Bileşenleri

**1. Video/Audio Encoding**
*   **Codec:** H.264/H.265 (video), Opus (audio).
*   **Adaptive:** Bandwidth düşükse video kalitesi düşürülür, ekran paylaşımına öncelik verilir.

**2. Media Server (SFU - Selective Forwarding Unit)**
*   Her participant video stream'ini sunucuya gönderir.
*   Sunucu her kullanıcıya diğer katılımcıların stream'lerini gönderir (mesh yerine star topology).

**3. WebRTC**
*   Browser-based client için WebRTC kullanılır.
*   **STUN/TURN:** NAT traversal için.

**4. Recording & Storage**
*   Toplantı kayıtları S3'te saklanır. Video transcoding ile farklı kaliteler üretilir.

#### Kritik Soru

**Soru: 1000 kişilik meeting'de herkes herkesi görürse bandwidth nasıl dayanır?**
*   **Cevap:** Gallery view'de maksimum 49 video gösterilir. Geri kalanlar thumbnail veya sadece isim. Konuşan kişinin videosu vurgulanır (active speaker detection).

---

### 14. Dropbox - Dosya Depolama ve Senkronizasyon

**Kullanıcı:** 700+ milyon.

#### Sistem Bileşenleri

**1. File Storage (Block-Level Storage)**
*   Dosya chunk'lara (4MB) bölünür.
*   **Deduplication:** Aynı chunk birden fazla dosyada varsa tek kere saklanır (hash bazlı).
*   **Storage:** S3 + Dropbox'un kendi Magic Pocket storage.

**2. Metadata Service**
*   Dosya adı, boyut, timestamp, hash, chunk ID'leri MySQL'de saklanır.

**3. Sync Protocol**
*   Client bir dosyayı değiştirdiğinde sadece değişen chunk'lar upload edilir (delta sync).
*   **Conflict Resolution:** İki cihazda aynı dosya değiştirilirse "conflicted copy" oluşturulur.

**4. Notification Service**
*   WebSocket ile diğer cihazlara "dosya değişti" notification gönderilir, otomatik sync başlar.

#### Kritik Soru

**Soru: 10GB dosya upload edilirken bağlantı koptu. Baştan mı başlar?**
*   **Cevap:** Hayır. Chunked upload sayesinde kaldığı chunk'tan devam eder (resume capability).

---

### 15. Airbnb - Konaklama Rezervasyon Platformu

**Kullanıcı:** 150+ milyon.

#### Sistem Bileşenleri

**1. Search Service**
*   **Geo-Spatial Search:** Kullanıcı konum ve tarih giriyor. Elasticsearch Geo-Query ile yakındaki evler bulunur.
*   **Filters:** Fiyat, misafir sayısı, özellikler (havuz, WiFi).

**2. Availability & Pricing**
*   Her ev için availability calendar (tarih bazlı müsait mi?).
*   **Dynamic Pricing:** Talep yüksekse fiyat artar (ML model).

**3. Booking Service**
*   Rezervasyon isteği → Müsaitlik kontrolü → Ödeme → Onay.
*   **Concurrency:** İki kişi aynı tarihi aynı anda reserve etmeye çalışırsa distributed lock (Redlock pattern).

**4. Review & Rating System**
*  Misafir checkout sonrası ev sahibini, ev sahibi de misafiri değerlendirir.
*   **Reputation Score:** Geçmiş yorumlara göre güven skoru.

**5. Photo Service**
*   Ev fotoğrafları S3'te, farklı boyutlarda thumbnail (CDN ile cache).

#### Kritik Soru

**Soru: Tatil sezonunda milyonlarca arama gelirse nasıl ölçeklenir?**
*   **Cevap:**
    *   **Caching:** Popüler lokasyonlar (Örn: Paris, NY) için arama sonuçları Redis'te cache'lenir (TTL: 10 dk).
    *   **Read Replicas:** Elasticsearch read replicas ile arama yükünü dağıt.

---

