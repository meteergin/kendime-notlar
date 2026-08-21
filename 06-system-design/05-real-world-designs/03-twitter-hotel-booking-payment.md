## Konu 35: Gerçek Dünya Sistem Tasarımları - Part 3

Bu bölümde Facebook, TikTok ve LinkedIn sistemlerinin tasarımını detaylıca inceliyoruz.

---

### 7. Facebook - Sosyal Ağ Platformu

**Kullan**ıcı:** 3+ milyar aktif kullanıcı.

#### Temel Gereksinimler
*   **Functional:** Profil, arkadaşlık, post/yorum/beğeni, fotoğraf/video paylaşımı, mesajlaşma, gruplar, sayfalar.
*   **Non-Functional:** Ultra-low latency, massive scale, eventual consistency, personalized feed.

#### Sistem Bileşenleri

**1. News Feed Generation**
*   **EdgeRank Algorithm:** Post'ların feed'de sıralanması için scoring algoritması.
    *   **Affinity Score:** Kullanıcının post sahibiyle etkileşim geçmişi.
    *   **Edge Weight:** Post tipi (video > resim > metin).
    *   **Time Decay:** Eski postlar alt sıralara düşer.
*   **Fan-out Hybrid Model:**
    *   Push: Normal kullanıcılar için fan-out on write.
    *   Pull: Ünlüler/sayfalar için fan-out on read.

**2. TAO (The Associations and Objects)**
*   Facebook'un kendi graph database'i.
*   **Nodes:** Kullanıcılar, postlar, yorumlar.
*   **Edges:** Arkadaşlık, beğeni, yorum ilişkileri.
*   **Caching Layer (Memcached):** Graph sorguları cache'lenir.

**3. Photos & Videos (Haystack)**
*   **Haystack:** Facebook'un custom object storage sistemi.
*   **Problem:** Milyarlarca küçük dosya, geleneksel file system'ler yetersiz kalır.
*   **Çözüm:** Binlerce küçük resmi tek bir büyük dosyada (haystack store file) saklar. Index ile hızlı erişim.

**4. Messaging (WhatsApp/Messenger Backend)**
*   **Asynchronous Messaging:** Mesaj önce Kafka'ya yazılır, alıcıya asenkron iletilir.
*   **Offline Messages:** Alıcı çevrimdışıysa mesaj Cassandra'da saklanır, sonra push notification ile bildirim.

**5. Database**
*   **MySQL (Sharded):** User data, posts, comments.
*   **Cassandra:** Time-series data (activity logs, analytics).
*   **Memcached:** Massive caching layer (trilyon request/day).

####Kritik Senaryolar

**Soru 1: Bir kullanıcının 5000 arkadaşı var. Feed nasıl oluşturulur?**
*   **Cevap:** Tüm arkadaşların postları çekilmez. Ranking algoritması ile "interaksiyon olasılığı yüksek" arkadaşlar önce çekilir. Top 1000 post arasından en iyi 50 tanesi gösterilir.

**Soru 2: Bir post 1 milyon beğeni aldı. Tüm beğenenlerin listesi nasıl gösterilir?**
*   **Cevap:** İlk 100 beğeniyi hemen göster. Kullanıcı "Tümünü Gör" derse, scroll'da pagination ile lazy-load yap. Beğeniler Cassandra'da saklanır (partition key = post_id, sort key = timestamp).

---

### 8. TikTok - Kısa Video Platformu

**Kullanıcı:** 1+ milyar, günde 1+ milyar video izleniyor.

#### Temel Gereksinimler
*   **Functional:** Video yükleme/izleme, swipe feed (For You Page), keşfet, takip, beğeni, yorum.
*   **Non-Functional:** Addictive recommendation, low-latency video delivery, real-time.

#### Sistem Bileşenleri

**1. Video Upload & Processing**
*   Kullanıcı videoyu çeker ve yükler (max 60 saniye).
*   **Compression:** Client-side ilk sıkıştırma yapılır (bandwidth tasarrufu).
*   **Server-Side Processing:**
    *   Farklı formatlar ve çözünürlükler (480p, 720p, 1080p).
    *   **Watermark:** TikTok logosu eklenir.
    *   **Audio Extraction:** Müzik/ses ayrılır (copyright kontrolü için).

**2. For You Page (FYP) - Recommendation Engine**
TikTok'un en kritik bileşeni. Kullanıcı tercihlerini millisaniyeler içinde öğrenir.
*   **Signals:**
    *   **Explicit:** Beğeni, yorum, paylaşma, takip.
    *   **Implicit:** Videoyu sonuna kadar izleme (completion rate), tekrar izleme, video üzerinde geçirilen süre.
    *   **Video Metadata:** Hashtag, ses, kategori.
*   **Real-time ML Model:** Her kullanıcı için personalized video listesi gerçek zamanlı üretilir (TensorFlow Serving).
*   **A/B Testing:** Yeni videolar rastgele küçük bir kullanıcı grubuna gösterilir (cold start problem). Eğer etkileşim yüksekse daha geniş kitleye yayılır.

**3. Content Delivery**
*   **CDN (ByteDance CDN):** Video segmentleri en yakın edge server'dan sunulur.
*   **Pre-loading:** Kullanıcı bir video izlerken, arkada sonraki 3-4 video pre-load edilir (swipe latency ~0ms).

**4. Database**
*   **MySQL/PostgreSQL:** User data, video metadata.
*   **Cassandra:** Sosyal etkileşimler (like, comment), zaman serisi data.
*   **Redis:** Hot videos cache, trending hashtags.

**5. Moderation & Safety**
*   **AI Content Moderation:** Her video upload edildiğinde, zararlı içerik (şiddet, pornografi) tespiti için ML model çalıştırılır.
*   **Human Moderation:** AI flaglediği içerikler insan moderatörler tarafından incelenir.

#### Kritik Senaryolar

**Soru 1: Bir video viral oldu. Milyonlarca kişi aynı anda izliyor. Sistem nasıl dayanır?**
*   **Cevap:**
    *   Video tüm CDN edge server'lara push edilir.
    *   View counter asenkron güncellenir (Kafka Streams). Gerçek zamanlı değil, ~1 dakika gecikmeli.
    *   Rate limiting: Aynı user'ın saniyede 10+ video request atması engellenir.

**Soru 2: FYP algoritması nasıl güncellenirse kullanıcı anında fark eder?**
*   **Cevap:** Online Learning. Model sürekli güncellenir. Kullanıcının son 10 videodaki davranışı hemen modele eklenir. Geleneksel batch learning yerine streaming ML kullanılır (Kafka Streams + Flink).

---

### 9. LinkedIn - Profesyonel Sosyal Ağ

**Kullanıcı:** 900+ milyon.

#### Temel Gereksinimler
*   **Functional:** Profil, bağlantı istekleri, iş ilanları, mesajlaşma, post/makale paylaşımı, öneri sistemi (People You May Know).
*   **Non-Functional:** Business-critical (yavaş olsa kabul edilir), veri bütünlüğü önemli.

#### Sistem Bileşenleri

**1. People You May Know (PYMK)**
*   **Graph-Based Recommendation:**
    *   1. derece bağlantılar: Doğrudan bağlantılarınız.
    *   2. derece bağlantılar: Arkadaşınızın arkadaşları (önerilebilir).
*   **Common Connections:** Ortak bağlantı sayısına göre sıralama.
*   **Machine Learning:** Aynı şirkette çalışma, aynı okul, benzer beceriler (skills overlap).

**2. Job Recommendation Engine**
*   **Candidate-Job Matching:**
    *   Kullanıcının profili (beceriler, deneyim, eğitim).
    *   İş ilanının gereksinimleri.
    *   Cosine similarity ile eşleştirme.
*   **Aplikasyon Geçmişi:** Daha önce başvurulan ilan tipleri.

**3. Feed Ranking**
*   **Engagement Prediction:** Bir post'un kullanıcı tarafından beğenilme/yorum alma/tıklanma olasılığı tahmin edilir.
*   **Content Diversity:** Sadece bir kaynaktan post gösterilmez, farklı bağlantılardan içerik mix edilir.

**4. Messaging (LinkedIn Messaging)**
*   **Real-time Delivery:** WebSocket ile anlık mesajlaşma.
*   **Message Storage:** Cassandra (zaman serisi). Mesajlar şifrelenmez (end-to-end encryption yok).

**5. Database**
*   **Espresso (LinkedIn'in NoSQL DB'si):** Profil verileri, bağlantı grafiği.
*   **Voldemort (Distributed Key-Value Store):** Read-heavy data cache.
*   **Kafka:** Event streaming (profile update, post publish).

#### Kritik Senaryolar

**Soru 1: Bir kullanıcı profilini update etti (yeni beceriler ekledi). PYMK önerileri hemen değişir mi?**
*   **Cevap:** Hayır, eventual consistency. Profil update eventi Kafka'ya düşer. Batch job (Apache Spark) her gece çalışır ve PYMK önerilerini yeniden hesaplar. Değişiklik 24 saat içinde yansır.

**Soru 2: İş ilanına 10,000 başvuru geldi. Recruiter nasıl filtreleyecek?**
*   **Cevap:** **Applicant Ranking System:** ML modeli başvuranları scorer. "İlanla uyum skoru" yüksek olanlar üst sırada listelenir. Recruiter en uygun 50 adayı gösterir.

#### Soru 3 (Tricky): Facebook TAO (The Associations and Objects) neden SQL yerine Graph API kullanır?
**Cevap:** Sosyal ağ verisi (arkadaşlık, beğeni) yoğun ilişkiseldir. SQL JOIN işlemleri milyarlarca satırda çok yavaştır. TAO, graph sorgularını optimize eder ve veriyi kenar (edge) ve düğüm (node) olarak saklayıp, cache'ler.

#### Soru 4 (Tricky): TikTok'ta "Cold Start" problemi (yeni kullanıcı/yeni video) nasıl çözülür?
**Cevap:**
*   **Yeni Video:** Rastgele küçük bir kitleye gösterilir (Exploration). Etkileşim alırsa daha büyük kitleye yayılır (Exploitation).
*   **Yeni Kullanıcı:** Popüler (genel geçer) videolar gösterilir. Kullanıcı ilgi alanlarını seçmeye teşvik edilir.

---
Bu bölüm ile Part 3 tamamlandı! 🎉

---

