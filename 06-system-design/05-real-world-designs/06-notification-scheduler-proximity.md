# Gerçek Dünya Sistem Tasarımları — Bölüm 6: Bildirim Sistemi (Notification System), Dağıtık Görev Zamanlayıcı (Distributed Job Scheduler) ve Yakındaki Yerler (Proximity Service / Yelp)

---

## 1. Ölçeklenebilir Bildirim Sistemi (Push, SMS, E-Mail)

> **Analoji:** Bildirim sistemi devasa bir "kurye lojistik merkezi" gibidir. Müşterilerden (Mikroservisler) paketler (bildirimler) gelir; merkez paketleri boyutuna ve aciliyetine göre sınıflandırır (Priority Queue), alıcının tercihine bakar (Gece rahatsız etme, E-posta mı SMS mi?), üçüncü parti kargo firmalarına (APNs, FCM, Twilio, SendGrid) teslim eder.

```
[Business Services] ──(gRPC/REST)──► [Notification Ingest API]
                                             │
                                     (Rate Limit & Deduplication)
                                             │
                                             ▼
                                     [User Preference DB] ──► (Hangi kanallar açık?)
                                             │
                       ┌─────────────────────┼─────────────────────┐
                       │                     │                     │
                       ▼                     ▼                     ▼
               [Push Kafka Topic]    [SMS Kafka Topic]     [Email Kafka Topic]
                       │                     │                     │
                       ▼                     ▼                     ▼
               [Push Workers]        [SMS Workers]         [Email Workers]
                       │                     │                     │
                       ▼                     ▼                     ▼
                   (APNs/FCM)            (Twilio)             (SendGrid)
```

### Gereksinimler & Ölçek
- **Hacim:** Günde 100 Milyon bildirim (Push: %60, E-posta: %30, SMS: %10).
- **Gecikme:** İşlemsel bildirimler (OTP SMS, Şifre Sıfırlama) < 5 saniye; Pazarlama/Promosyon bildirimleri < 30 dakika.
- **Kritik Kurallar:**
  - **Deduplication:** Aynı bildirim kullanıcıya birkaç saniye içinde iki kez gitmemeli (Örn: Çift tıklama sonucu çift SMS).
  - **User Opt-out & Gece Modu:** Kullanıcının bildirim tercihleri ve yerel saat dilimine göre sessiz saatler uygulanmalı.
  - **Third-Party Failover:** Bir SMS sağlayıcısı (Twilio) çökerse otomatik olarak alternatif sağlayıcıya (Infobip/MessageBird) geçilmeli.

### Mimari Bileşenler

#### 1. Rate Limiting & Tekilleştirme (Idempotency)
- Redis üzerinde `SET notification:hash(userId, content) "1" EX 30 NX` komutu ile 30 saniye içinde gelen birebir aynı içerikli bildirimler bastırılır.

#### 2. Önceliklendirme ve İzolasyon (Priority Queues)
- **High Priority Queue:** OTP, 2FA kodları, güvenlik alarmları, para transferi bildirimleri (Ayrı worker havuzu).
- **Low Priority Queue:** Pazarlama kampanyaları, haftalık bültenler (Bulk worker havuzu).
- Kampanya bildirimlerinin OTP kodlarının önüne geçip gecikme yaratması engellenir.

#### 3. Third-Party Provider Entegrasyonu ve Circuit Breaker
- APNs (Apple), FCM (Google), Twilio, SendGrid API çağrıları Resilience4j Circuit Breaker ile sarılır. Hata oranı %50'yi aşarsa devre açılır ve fallback sağlayıcıya yönlendirilir.

---

## 2. Dağıtık Görev Zamanlayıcı (Distributed Job Scheduler / Cron Service)

> **Analoji:** Dağıtık zamanlayıcı, havaalanındaki "uçuş kontrol kulesi" gibidir. Binlerce uçağın (işlerin) kalkış saatleri bellidir. Kule, pistlerin durumuna ve uçakların hazır olma anına göre işleri doğru hangarlara (Worker'lara) sırayla ve çakışmasız dağıtır.

```
                    ┌──────────────────────────────┐
                    │    Scheduler Leader (Master) │ ──► (Zookeeper / Raft Election)
                    └──────────────┬───────────────┘
                                   │ Time-Wheel / DB Polling
                                   ▼
                    ┌──────────────────────────────┐
                    │   Execution Queue (Kafka/RMQ)│
                    └──────────────┬───────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                    ▼
       [Worker Node 1]      [Worker Node 2]      [Worker Node 3]
       (Kendi Lock'unu alır)
```

### Gereksinimler & Ölçek
- **Kapasite:** Milyonlarca zamanlanmış görev (Örn: 10 dakika sonra fatura kontrolü, her gece saat 03:00'da veri temizliği).
- **Garantiler:** At-least-once veya Exactly-once çalışma; sistem restart olsa bile zamanı gelen hiçbir işin atlanmaması.

### Çözüm Mimarileri

#### 1. Dağıtık Gecikmeli Kuyruk (Redis Sorted Set / ZSET)
- Her görev için `score = execution_timestamp` (Unix epoch milisaniye), `value = jobId` olarak Redis ZSET'e eklenir:
  `ZADD scheduled_jobs 1724250000000 "job:send_invoice:9876"`
- **Scheduler Worker:** Sürekli olarak `ZRANGEBYSCORE scheduled_jobs 0 {current_timestamp} LIMIT 0 100` sorgusu atarak zamanı dolmuş işleri çeker.
- `ZREM` ile işi atomik olarak kuyruktan siler (veya Lua Script kullanılır) ve işlem kuyruğuna (Kafka/RabbitMQ) basar.

#### 2. Hashed Timing Wheel Algoritması (Bellek İçi Yüksek Performans)
- Saat kadranı gibi dairesel bir dizi (Array) tutulur (Örn: 60 saniyelik 60 dilim).
- Her saniye ibre 1 adım ilerler ve o dilimdeki bağlı listeyi (LinkedList) çalıştırır. Milyonlarca görevi O(1) karmaşıklığında yönetir (Netty ve Kafka dahili zamanlayıcılarında bu algoritmayı kullanır).

---

## 3. Yakındaki Yerler Servisi (Proximity Service / Yelp / Google Maps Places)

> **Analoji:** Dünya haritasını kare parçalara veya altıgen peteklere (Geohash / H3) böldüğünüzü düşünün. Bir restorana bakarken tüm dünyayı değil, sadece bulunduğunuz altıgeni ve komşu 6 altıgeni sorgularsınız.

```
[User App] (Lat: 41.0082, Lon: 28.9784, Radius: 2km)
    │
    ▼
[API Gateway] ──► [Geohash Converter] (Lat/Lon ──► "sxk9e")
                        │
                        ▼
            [Elasticsearch / PostGIS / Redis GEO]
            Query: Geohash prefix "sxk9e" + Komşu 8 kutu
                        │
                        ▼
            [Place Metadata DB (PostgreSQL)] ──► Detaylı Restoran Bilgisi
```

### Gereksinimler & Ölçek
- **Kullanım:** 100 Milyon arama / gün.
- **Sorgu:** "Bulunduğum konuma 5 km mesafedeki en iyi kafeler".
- **Gecikme:** < 100 ms.

### Mekansal İndeksleme (Spatial Indexing) Yöntemleri

| Yöntem | Açıklama | Avantaj | Dezavantaj |
| :--- | :--- | :--- | :--- |
| **PostGIS (R-Tree)** | Geleneksel RDBMS uzantısı (B-Tree'nin 2D hali) | Tam ACID, zengin coğrafi fonksiyonlar | Çok yüksek ölçekte sharding zorluğu |
| **Geohash** | Dünyayı hiyerarşik dikdörtgen string'lere böler (`sxk9e2`) | Prefix aramasıyla komşuluk kolaydır, B-Tree uyumlu | Kutu kenarlarında sınır atlama problemi |
| **Uber H3** | Dünyayı altıgen (Hexagonal) hücrelere böler | Tüm komşular eşit mesafededir (düzgün yarıçap hesabı) | Matematiksel dönüşüm maliyeti |
| **Google S2** | Dünyayı Hilbert eğrisi ile küresel hücrelere böler | Kutuplarda distorsiyonu minimuma indirir | Anlaşılması ve debug edilmesi karmaşık |

### Veritabanı ve Caching Stratejisi
1. **Durağan Veri (Static Places):** Restoranların konumları nadiren değişir. Konumlar Geohash string'i ile Redis Key veya Elasticsearch'te indexlenir.
2. **Komşu Hücre Araması (Edge Case):** Kullanıcı kutunun tam sınırında duruyorsa sadece kendi kutusu sorgulanırsa hemen 10 metre yanındaki restoran kaçabilir. Bu yüzden hedef Geohash + etrafındaki **8 komşu Geohash kutusu** daima paralel sorgulanır.

---

## 4. Kritik Mülakat Soruları ve Tricky Senaryolar

### Soru 1: Bildirim sisteminde kullanıcının cihaz token'ı (APNs/FCM) geçersizleştiğinde (Unregistered) ne yapılmalıdır?
**Cevap:** APNs/FCM hata kodlarında `BadDeviceToken` veya `Unregistered` dönerse, bu cihaz token'ı derhal kullanıcının profilinden temizlenmeli/pasife alınmalıdır. Aksi halde geçersiz token'lara sürekli istek atılması Apple/Google tarafından IP bazlı rate limit cezası almanıza sebep olur.

### Soru 2: Dağıtık Görev Zamanlayıcıda (Scheduler) aynı işin 2 farklı sunucuda aynı anda çalışması (Double Execution) nasıl engellenir?
**Cevap:**
1. **Distributed Lock (Redis Redlock / DB Optimistic Lock):** İş çalıştırılmadan önce `job_id` ile kilit alınır.
2. **Leader Election:** Sadece Leader olan Scheduler instance'ı görevleri tetikler, Follower'lar beklemede kalır.
3. **Idempotent Job Handler:** Çalışan işin kendisi idempotent tasarlanır (Örn: Faturayı 2 kez kesse bile `fatura_no` unique index'e takılır).

### Soru 3 (Tricky): Uber H3 altıgenlerinin Geohash dikdörtgenlerine göre en büyük üstünlüğü nedir?
**Cevap:** Geohash'te (dikdörtgen) merkezden kenarlara olan mesafe ile köşelere olan mesafe eşit değildir (köşeler daha uzaktır). Uber H3 altıgenlerinde ise merkezin 6 komşu altıgene olan mesafesi **tamamen eşittir**. Bu sayede yarıçap bazlı çevre aramalarında (Radius / Circle Search) altıgen modelleme mükemmel bir dairesel kapsama alanı sağlar ve sınır distorsiyonunu ortadan kaldırır.
