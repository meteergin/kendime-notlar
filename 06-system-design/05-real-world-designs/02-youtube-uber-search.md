## Konu 34: Gerçek Dünya Sistem Tasarımları - Part 2

Bu bölümde Netflix, Uber ve Twitter sistemlerinin tasarımını detaylıca inceliyoruz.

---

### 4. Netflix - Video Streaming Platformu

**Kullanıcı:** 230+ milyon, günde milyarlarca saat içerik izleniyor.

#### Temel Gereksinimler
*   **Functional:** Video streaming, profil yönetimi, öneriler, indirme (offline), farklı cihaz desteği.
*   **Non-Functional:** Ultra-low latency, 99.99% availability, adaptive streaming, global scale.

#### Sistem Bileşenleri

**1. Content Delivery Strategy**
*   **Open Connect (Netflix CDN):** Netflix kendi CDN'ini kurmuştur. ISP'lerin (Internet Service Provider) içine sunucular yerleştirir.
*   **Pre-caching:** Popüler içerikler gece saatlerinde ISP sunucularına önceden yüklenir (peak saatlerinde bandwidth tasarrufu).

**2. Video Encoding ve Adaptive Streaming**
*   Her video 120+ farklı versiyonda kodlanır (çözünürlük, codec, bitrate kombinasyonları).
*   **Encoding Farm:** AWS EC2 Spot Instances ile masif paralel encoding.
*   **Per-Title Encoding:** Her içerik için optimal encoding profili belirlenir (aksiyon filmi vs. animasyon farklı sıkıştırma gerektirir).

**3. Personalization Engine**
*   **2 Tier Recommendation:**
    *   **Offline (Batch):** Günlük olarak tüm kullanıcılar için öneri listeleri Apache Spark ile hesaplanır.
    *   **Online (Real-time):** Kullanıcının son davranışlarına göre anlık düzeltme (Kafka Streams).
*   **A/B Testing:** Her zaman yüzlerce A/B testi çalışır. Kullanıcıların %1'i yeni algoritma ile test edilir.

**4. Database**
*   **EV Cache (Memcached/Redis):** Sık erişilen metadata (film detayları, kullanıcı profilleri) cache'lenir.
*   **Cassandra:** Kullanıcı viewing history, profil ayarları (multi-region replication).
*   **MySQL:** Subscription, billing bilgileri.

**5. Playback Architecture**
*   **Playback API:** Client hangi videoyu, hangi kalitede oynayacağını sunucuya sorar. Sunucu en yakın CDN URL'ini döner.
*   **Client-Side Adaptation:** Client, network bandwidth'ini her 10 saniyede bir ölçer ve segment kalitesini değiştirir.

#### Kritik Senaryolar

**Soru 1: Bir bölge (ör: Avrupa) tamamen çöktüğünde ne olur?**
*   **Cevap:** Netflix multi-region architecture kullanır. Chaos Monkey ile düzenli olarak sunucular kapatılır (resilience testi). Bir region çökerse trafik otomatik olarak başka region'a yönlendirilir (Route 53 Health Checks).

**Soru 2: Yeni bir sezon çıktığında milyonlarca kişi aynı anda izlemeye başlarsa?**
*   **Cevap:** 
    *   İçerik önceden tüm CDN edge server'lara push edilir (Strategic Pre-caching).
    *   Auto-scaling ile EC2 instance sayısı artırılır.
    *   Rate limiting: Aynı anda çok fazla istek gelirse queue'ya alınır.

---

### 5. Uber - Ride-Hailing Platformu

**Kullanıcı:** 130+ milyon, günde 23+ milyon yolculuk.

#### Temel Gereksinimler
*   **Functional:** Konum paylaşımı, sürücü eşleştirme, fiyat hesaplama, ödeme, rota optimizasyonu.
*   **Non-Functional:** Real-time (<1s matching), yüksek accuracy (konum), 99.99% availability.

#### Sistem Bileşenleri

**1. Location Service (Geo-Spatial)**
*   Her sürücü ve yolcu konumunu saniyede birkaç kez günceller.
*   **Google S2 Geometry:** Dünyayı hücreler (cells) halinde böler. Her hücrenin ID'si vardır. Yakındaki sürücüleri bulmak için aynı veya komşu hücrelerdeki sürücüler sorgulanır.
*   **Database:** Redis Geospatial (GEOADD, GEORADIUS komutları).

**2. Matching Service (DISCO)**
*   Yolcu istek gönderdiğinde, en yakın sürücülere push notification gider.
*   **Algorithm:** Çok faktörlü optimizasyon (mesafe, rating, fiyat, tahmini varış süresi).
*   **Supply/Demand Balance:** Surge pricing (talep yüksekse fiyat artar).

**3. ETA Prediction (Estimated Time of Arrival)**
*   **Machine Learning Model:** Geçmiş yolculuk verileri, trafik, hava durumu, zaman dilimi kullanılarak ETA tahmini yapılır.
*   **Real-time Updates:** Yolculuk sırasında ETA sürekli güncellenir.

**4. Trip Service**
*   Yolculuk başladığında trip kaydı oluşturulur.
*   **Real-time Tracking:** Sürücünün konumu her saniye güncellenir, yolcunun uygulamasında haritada görülür.
*   **Polyline Encoding:** Rota (lat/lng listesi) sıkıştırılarak saklanır (Google Polyline).

**5. Payment Service**
*   Ödeme bilgileri **PCI-DSS** uyumlu vault'ta saklanır.
*   **Asynchronous Payment:** Yolculuk bittiğinde ödeme kuyruğa alınır, asenkron işlenir. Kullanıcı hemen uygulama kapanabilir.

**6. Database**
*   **PostgreSQL:** Kullanıcı profilleri, trip history.
*   **Cassandra:** Real-time location data (zaman serisi).
*   **Redis:** Sürücü availability (online/offline), surge pricing cache.

#### Kritik Senaryolar

**Soru 1: Sürücü uygulamayı kapattı ama hala "online" görünüyor. Nasıl tespit edilir?**
*   **Cevap:** Heartbeat mechanism. Sürücü her 5 saniyede bir "alive" mesajı gönderir. Eğer 15 saniye mesaj gelmezse "offline" olarak işaretlenir.

**Soru 2: Aynı sürücü iki farklı yolcu tarafından seçilirse ne olur? (Race condition)**
*   **Cevap:** Distributed lock (Redis SETNX). İlk gelen istek lock'u alır, sürücüyü reserve eder. İkinci istek lock alamaz ve "sürücü başka bir yolcuyla eşleşti" hatası alır.

---

### 6. Twitter (X) - Mikroblog Platformu

**Kullanıcı:** 450+ milyon, günde 500+ milyon tweet.

#### Temel Gereksinimler
*   **Functional:** Tweet atma, retweet, like, reply, timeline, trend topic, DM (direct message).
*   **Non-Functional:** Read-heavy (1:100 yaz/oku oranı), eventual consistency, real-time feed.

#### Sistem Bileşenleri

**1. Tweet Service**
*   Kullanıcı tweet atar.
*   **Tweet ID:** Snowflake algoritması (time-based, unique, sortable).
*   **Tweet Storage (MySQL Sharded):** Partition key = User ID. Her kullanıcının tweetleri kendi shard'ında.

**2. Timeline Service (Fan-out)**
*   **Hybrid Approach:**
    *   **Normal Kullanıcılar (Fan-out on Write):** Tweet atıldığında, takipçilerinin "Home Timeline" cache'ine (Redis) tweet ID eklenir.
    *   **Ünlüler (Fan-out on Read):** Takipçi sayısı >1M olanlarda fan-out yapılmaz. Kullanıcı timeline'a girdiğinde takip ettiği ünlülerin tweetleri DB'den çekilir ve merge edilir.

**3. Timeline Merging**
*   Kullanıcı timeline'a girdiğinde:
    1.  Redis'ten pre-computed timeline alınır (normal kullanıcıların tweetleri).
    2.  Takip edilen ünlülerin son tweetleri DB'den çekilir.
    3.  İkisi timestamp'e göre merge edilir.

**4. Trend Detection Service**
*   **Stream Processing (Apache Storm/Flink):** Her tweet Kafka'ya yazılır. Hashtag'ler real-time olarak sayılır.
*   **Trending Algorithm:** Son 1 saatte en çok kullanılan hashtag'ler. Ani artış gösteren hashtag'ler öncelikli (spike detection).

**5. Search Service (Elasticsearch)**
*   Tweetler gerçek zamanlı olarak indexlenir.
*   **Inverted Index:** Kelime bazlı arama.

**6. Database**
*   **MySQL (Sharded):** Tweet verileri (text, user_id, timestamp).
*   **Redis:** Timeline cache (her kullanıcının feed'i).
*   **Graph Database (FlockDB):** Takip/takipçi ilişkileri (follower/following graph).

#### Kritik Senaryolar

**Soru 1: Bir tweet viral oldu (milyonlarca like). Database nasıl dayanır?**
*   **Cevap:** 
    *   Like eventi önce Kafka'ya yazılır. Asenkron olarak DB'ye batch update edilir.
    *   Kullanıcı like yaptığında UI'da hemen "+1" görür ama backend eventual consistency ile günceller.
    *   Hot tweet'lerin like/retweet sayıları Redis'te cache'lenir.

**Soru 2: Timeline'da kaç tweet gösterilir? Nasıl pagination yapılır?**
*   **Cevap:** İlk yüklemede son 50 tweet gösterilir. Kullanıcı scroll yaptıkça "Load More" ile 50'şer tweet daha çekilir. Cursor-based pagination (last_tweet_id kullanılır, OFFSET kullanılmaz çünkü performans sorunu yaratır).

#### Soru 3 (Tricky): Netflix neden Public CDN (Akamai, CloudFront) yerine kendi CDN'ini (Open Connect) kurdu?
**Cevap:**
*   **Maliyet:** Petabyte'larca veriyi Public CDN üzerinden göndermek çok pahalıdır.
*   **Kontrol:** ISP'lerin içine sunucu koyarak (Embedded Cache), kullanıcıya fiziksel olarak en yakın noktadan hizmet verir ve internet omurgasındaki trafiği azaltır.

#### Soru 4 (Tricky): Uber'de "Surge Pricing" (Fiyat Artışı) tutarlılığı nasıl sağlanır?
**Cevap:** Bir bölgedeki talep artışı (Surge) hesaplandığında, bu bilgi tüm kullanıcılara aynı anda yansımalıdır.
*   **Distributed Lock:** Fiyat hesaplama servisi, belirli bir bölge (S2 Cell) için lock alır, fiyatı günceller ve Redis'e yazar. Kullanıcılar Redis'ten okur (Eventual Consistency kabul edilir, ama lock sayesinde tutarsız yazma önlenir).

---

