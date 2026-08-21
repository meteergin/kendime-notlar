## Konu 33: Gerçek Dünya Sistem Tasarımları - Part 1

Bu bölümde, dünya çapında en çok kullanılan uygulamaların sistem tasarımlarını detaylıca inceliyoruz. Her uygulama için mimari bileşenler, teknoloji seçimleri ve olası mülakat soruları ele alınmaktadır.

---

### 1. WhatsApp - Mesajlaşma Sistemi

**Kullanıcı:** 2+ milyar aktif kullanıcı, günde 100 milyar mesaj.

#### Temel Gereksinimler
*   **Functional:** 1-1 mesajlaşma, grup chat, multimedya (resim/video), end-to-end encryption, son görülme, çevrimiçi durum.
*   **Non-Functional:** Düşük latency (<100ms), yüksek availability (99.99%), horizontal scalability.

#### Sistem Bileşenleri

**1. Client (Mobile App)**
*   **Persistent Connection:** WebSocket üzerinden sunucuya kalıcı bağlantı. Mesaj anında iletilir.
*   **Local Storage:** SQLite ile mesaj geçmişi saklanır (offline erişim için).

**2. Load Balancer (Layer 4)**
*   Gelen bağlantıları **Chat Server**'lara dağıtır.
*   **Sticky Session:** Bir kullanıcı hep aynı Chat Server'a bağlanır (IP Hash algoritması).

**3. Chat Server (WebSocket Gateway)**
*   Kullanıcıdan mesajı alır, hedef kullanıcının hangi sunucuda olduğunu bulur (User-Server Mapping).
*   Eğer hedef kullanıcı çevrimiçiyse mesajı direkt WebSocket ile gönderir.
*   **Technology:** Erlang/Elixir (yüksek concurrency için) veya Go.

**4. Message Queue (Kafka/RabbitMQ)**
*   Alıcı çevrimdışıysa, mesaj kuyruğa alınır.
*   **Reliability:** Mesaj kaybını önlemek için persistence ve replication.

**5. Message Service (Backend)**
*   Mesajı veritabanına yazar.
*   **Message ID:** Snowflake algoritması ile unique ID üretir.
*   Push Notification servisiyle haberleşir.

**6. Database**
*   **User DB (PostgreSQL):** Kullanıcı profilleri, telefon numarası, profile photo.
*   **Message DB (Cassandra/HBase):** Mesajlar. Wide-column store (zaman serisi veri). Partition key: User ID + Timestamp.
*   **Group DB (MongoDB):** Grup bilgileri, üyeleri.

**7. Media Storage (S3/Cloud Storage)**
*   Resim, video, ses dosyaları object storage'da saklanır.
*   CDN (CloudFront) ile hızlı erişim.

**8. End-to-End Encryption**
*   **Signal Protocol:** Her cihazda anahtar çifti (public/private key). Mesaj, alıcının public key'i ile şifrelenir.
*   Sunucu mesajın içeriğini göremez.

#### Kritik Senaryolar ve Çözümler

**Soru 1: Bir kullanıcı çevrimdışı iken 1000 mesaj aldı. Tekrar çevrimiçi olduğunda ne olur?**
*   **Cevap:** Client, son aldığı mesajın ID'sini (last_message_id) sunucuya gönderir. Sunucu, bu ID'den sonraki tüm mesajları toplu (batch) olarak döner. Pagination ile yüklenir.

**Soru 2: Grup mesajlaşmasında fan-out nasıl yapılır?**
*   **Cevap:** 
    *   **Küçük Gruplar (<100 kişi):** Mesaj her grup üyesine ayrı ayrı gönderilir (Fan-out on Write).
    *   **Büyük Gruplar:** Mesaj bir kere yazılır, grup üyeleri lazy-loading ile okur (Fan-out on Read).

**Soru 3: Kullanıcıların "son görülme" bilgisi nasıl güncellenir?**
*   **Cevap:** Her kullanıcı bağlantı kestiğinde timestamp Redis'e yazılır (TTL ile). Heartbeat mesajı ile bağlantı kontrolü yapılır. Eğer 30 saniye heartbeat gelmezse "çevrimdışı" kabul edilir.

---

### 2. Instagram - Fotoğraf Paylaşım Platformu

**Kullanıcı:** 2+ milyar, günde 95 milyon fotoğraf paylaşılıyor.

#### Temel Gereksinimler
*   **Functional:** Fotoğraf/video yükleme, timeline (feed), takip et/takipten çık, beğeni, yorum, story, keşfet sayfası.
*   **Non-Functional:** Yüksek okuma/yazma oranı (100:1), eventual consistency, CDN ile düşük latency.

#### Sistem Bileşenleri

**1. Upload Service**
*   Kullanıcı fotoğrafı yükler.
*   **Image Processing Pipeline:**
    *   Orijinal dosya S3'e yüklenir.
    *   Asenkron olarak farklı boyutlarda thumbnail oluşturulur (Lambda/Fargate).
    *   Metadata (boyut, format, konum) veritabanına yazılır.
*   **Technology:** Multipart upload (chunk'lara bölerek), Pre-signed URL (direkt S3'e yükleme).

**2. Feed Generation Service**
*   **Fan-out on Write (Push Model):** Birisi fotoğraf paylaştığında, takipçilerinin "hazır feed"ine eklenir (Redis List).
    *   **Sorun:** Ünlüler için takipçi sayısı çok fazla (milyonlarca), fan-out maliyeti yüksek.
    *   **Çözüm:** Ünlüler için **Fan-out on Read (Pull Model)** kullanılır. Kullanıcı feed'e girdiğinde, takip ettiği ünlülerin son gönderileri DB'den çekilir.
*   **Hybrid Approach:** Normal kullanıcılar Push, ünlüler Pull.

**3. Timeline Service**
*   Redis'te saklanan feed ID listesini alır.
*   Her ID için post detaylarını (fotoğraf URL, beğeni sayısı, yorumlar) DB'den çeker.
*   **Caching:** Hot posts (çok beğenilen) Redis'te cache'lenir.

**4. Database**
*   **User DB (MySQL):** Kullanıcı bilgileri, takipçi/takip edilen listesi (Graph DB de kullanılabilir).
*   **Post DB (Cassandra):** Partition key = User ID. Her kullanıcının postları hızlıca çekilebilir.
*   **Feed DB (Redis):** Her kullanıcının feed'i (post ID listesi). Sorted Set kullanılır (timestamp ile sıralı).
*   **Activity DB (Cassandra):** Beğeniler, yorumlar (zaman serisi veri).

**5. Recommendation Engine (Keşfet / Explore)**
*   **Collaborative Filtering:** Benzer kullanıcıların beğendiği postlar önerilir.
*   **Content-Based:** Kullanıcının geçmişte beğendiği içeriklere benzer postlar.
*   **Technology:** Apache Spark (offline batch processing), Redis (online serving).

**6. Search Service (Elasticsearch)**
*   Hashtag, kullanıcı adı araması.

#### Kritik Senaryolar

**Soru 1: Bir post aniden viral oldu (milyon beğeni). Sistemi nasıl korursunuz?**
*   **Cevap:** 
    *   **Rate Limiting:** Aynı kullanıcının saniyede birden fazla beğeni yapmasını engelle.
    *   **Async Write:** Beğeni önce Kafka'ya yazılır, asenkron olarak DB'ye. Kullanıcı anında "+1 beğeni" görür ama DB eventual consistency ile güncellenir.
    *   **Caching:** Viral postun detayları CDN ve Redis'te cache'lenir.

**Soru 2: Story özelliği nasıl tasarlanır?**
*   **Cevap:** Story'ler 24 saat sonra silinir.
    *   **Redis ile TTL:** Story verisi Redis'te saklanır, TTL=24h. Süre dolunca otomatik silinir.
    *   **Object Storage:** Video/resim S3'te saklanır ama 24 saat sonra lifecycle policy ile otomatik silinir.

---

### 3. YouTube - Video Paylaşım ve Streaming Platformu

**Kullanıcı:** 2+ milyar, günde 1+ milyar saat video izleniyor.

#### Temel Gereksinimler
*   **Functional:** Video yükleme, streaming, arama, öneri, yorum, beğeni, abonelik.
*   **Non-Functional:** Düşük latency (<2s başlatma), adaptive bitrate streaming, global CDN.

#### Sistem Bileşenleri

**1. Video Upload Service**
*   Kullanıcı videoyu yükler.
*   **Chunked Upload:** Büyük dosyalar (GB'lar) parçalara bölünerek yüklenir. Resume capability (kesintide kaldığı yerden devam).
*   **Pre-processing:**
    *   Video geçici depolama alanına (S3 Glacier) yüklenir.
    *   Asenkron olarak **Video Processing Pipeline** tetiklenir.

**2. Video Processing Pipeline (Transcoding)**
*   **Amaç:** Videoyu farklı çözünürlük ve formatlarda kodlamak (1080p, 720p, 480p, 360p).
*   **Technology:** FFmpeg, AWS Elemental MediaConvert.
*   **Adaptive Bitrate Streaming (HLS/DASH):** Video küçük segmentlere (2-10 saniye) bölünür. Her segment farklı kalitede hazırlanır. Client, network hızına göre segment kalitesini değiştirir.
*   **Thumbnail Generation:** Video'dan birkaç frame thumbnail olarak kaydedilir.

**3. Video Storage**
*   **Hot Storage (Sık izlenen):** SSD tabanlı storage, CDN edge server'lara yakın.
*   **Cold Storage (Eski, az izlenen):** S3 Glacier, Archival storage (maliyeti düşük).

**4. CDN (Content Delivery Network)**
*   Video segmentleri CDN edge server'larda cache'lenir.
*   Kullanıcı, en yakın edge server'dan videoyu çeker.
*   **Origin Server:** Video yoksa, CDN, YouTube'un ana storage'ından (origin) çeker.

**5. Metadata Service**
*   Video başlığı, açıklama, kanal, upload tarihi, izlenme sayısı.
*   **Database:** MySQL/PostgreSQL (ilişkisel veri için).

**6. Search and Recommendation**
*   **Search (Elasticsearch):** Video başlığı, açıklama, tag'lere göre arama.
*   **Recommendation Engine:**
    *   **Collaborative Filtering:** Benzer izleme geçmişine sahip kullanıcıların izlediği videolar.
    *   **Deep Learning (TensorFlow):** Video içeriği analizi (görüntü, ses).
    *   **Real-time Signals:** Kullanıcının son izlediği videolar, kanal abonelikleri.

**7. View Counter & Analytics**
*   **Stream Processing (Apache Flink/Kafka Streams):** Her "play" eventi Kafka'ya yazılır. Real-time olarak izlenme sayısı güncellenir.
*   **Dedüplication:** Aynı kullanıcının 30 saniye içindeki tekrar izlemeleri tek sayılır.

**8. Comment & Like Service**
*   **NoSQL (Cassandra):** Yorumlar ve beğeniler zaman serisi veri. Partition key = Video ID.

#### Kritik Senaryolar

**Soru 1: Yeni yüklenen bir video hemen izlenmeye başlarsa transcoding tamamlanmadan ne olur?**
*   **Cevap:** 
    *   Video yüklenirken en düşük kalitede (360p) hemen transcoding yapılır ve yayına alınır.
    *   Diğer kaliteler (720p, 1080p) arka planda işlenir ve hazır oldukça kullanıma sunulur.
    *   Kullanıcı başlangıçta düşük kalite izler, daha sonra yüksek kaliteye geçebilir.

**Soru 2: Canlı yayın (Live Streaming) nasıl çalışır?**
*   **Cevap:** 
    *   **RTMP/WebRTC:** Yayıncı, RTMP protokolü ile videoyu YouTube sunucusuna gönderir.
    *   **Real-time Transcoding:** Video gerçek zamanlı olarak farklı kalitelere dönüştürülür.
    *   **HLS/DASH:** İzleyiciler HLS segmentleri ile izler. Latency ~5-10 saniye.
    *   **Low-Latency Mode:** WebRTC ile latency <1 saniyeye düşürülebilir (Ultra Low Latency HLS).

**Soru 3: Copyright kontrolü nasıl yapılır?**
*   **Cevap:** 
    *   **Content ID System:** Her video upload edildiğinde, parmak izi (fingerprint) çıkarılır (audio/video).
    *   Veritabanında telif hakkı korumalı içeriklerle karşılaştırılır.
    *   Eşleşme varsa, video engellenir veya sahibine bildirim gönderilir.

#### Soru 4 (Tricky): WhatsApp'ta "End-to-End Encryption" anahtar değişimi (Key Exchange) nasıl yapılır?
**Cevap:** **Signal Protocol** (X3DH - Extended Triple Diffie-Hellman) kullanılır.
*   Kullanıcı A, sunucudan Kullanıcı B'nin "Pre-Key Bundle"ını (Public Key'ler) indirir.
*   Kendi Private Key'i ile ortak bir "Shared Secret" oluşturur ve mesajı şifreler.
*   Sunucu bu anahtarı göremez, sadece şifreli mesajı iletir.

#### Soru 5 (Tricky): YouTube izlenme sayısı (View Count) neden 301'de donardı (eskiden)?
**Cevap:** İzlenme sayıları dağıtık sunuculardan toplanır. Düşük sayılar için (örn: <300) doğrulama yapılmaz. Ancak 300'ü geçince, "bot/spam" kontrolü için sayım durdurulur ve merkezi doğrulama yapılır.
*   Günümüzde: Stream Processing (Kafka/Flink) ile daha hızlı ve yaklaşık (approximate) sayım yapılır, donma olmaz.

---

