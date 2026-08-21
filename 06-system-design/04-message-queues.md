## Konu 32: Mesaj Kuyrukları ve İleri Seviye Veri Yapıları (Kafka, RabbitMQ, Redis)

Modern dağıtık sistemlerde, servisler arası asenkron iletişim (Message Brokers) ve yüksek performanslı veri erişimi (Redis) kritik öneme sahiptir. Bu bölüm, Kafka, RabbitMQ ve Redis'in derinlemesine mimarisini, kullanım senaryolarını ve mülakatlarda sorulan zorlayıcı detayları kapsar.

---

### 1. Message Brokers (Mesaj Kuyrukları)

#### Neden Kullanılır?
1.  **Decoupling (Bağımlılığı Azaltma):** Üretici (Producer) ve Tüketici (Consumer) birbirini bilmek zorunda değildir.
2.  **Asynchronous Processing (Asenkron İşleme):** Uzun süren işlemleri (örn: PDF oluşturma) arka plana atarak kullanıcıya hemen cevap dönebilmek.
3.  **Buffering (Yük Dengeleme):** Ani trafik artışlarında (Spike), mesajlar kuyrukta birikir, sistem çökmez (Consumer kendi hızında işler).
4.  **Reliability (Güvenilirlik):** Consumer çökerse mesaj kaybolmaz, kuyrukta bekler.

---

### 2. Apache Kafka

Kafka, yüksek throughput (işlem hacmi) ve düşük latency için tasarlanmış, **dağıtık bir event streaming platformudur**. Bir "kuyruk"tan ziyade, dağıtık bir "log" dosyası gibi çalışır.

#### Temel Bileşenler
*   **Broker:** Kafka sunucusu.
*   **Topic:** Mesajların kategorize edildiği yer (Veritabanı tablosu gibi).
*   **Partition:** Bir topic, paralellik ve ölçeklenebilirlik için birden fazla partition'a bölünür.
    *   *Önemli:* Sıralama (Ordering) sadece partition içinde garantidir, tüm topic genelinde değil!
*   **Offset:** Bir partition içindeki mesajın benzersiz ID'sidir (Sıra numarası).
*   **Producer:** Mesajı üreten ve topic'e gönderen.
*   **Consumer Group:** Bir topic'i dinleyen consumer grubu.
    *   Her partition, gruptaki **sadece bir** consumer tarafından okunabilir. (Aynı mesajı iki kez işlememek için).
    *   Ölçeklemek için consumer sayısını partition sayısına kadar artırabilirsiniz.

#### Kafka Mimarisi ve Özellikleri
*   **Log-Based Storage:** Mesajlar diskte sıralı (sequential) olarak saklanır. Okunduktan sonra silinmez (Retention süresi bitene kadar).
*   **Pull Model:** Consumer, mesajları kendi hızında çeker (Pull).
*   **Dumb Broker, Smart Client:** Broker sadece mesajı saklar, kimin neyi okuduğunu takip etmez (Offset'i consumer yönetir).
*   **Zookeeper / KRaft:** Eskiden metadata yönetimi (broker listesi, partition lideri) için Zookeeper zorunluydu. Kafka 3.3+ ile **KRaft (Kafka Raft Metadata mode)** geldi ve Zookeeper bağımlılığı kalktı.

---

### 3. RabbitMQ

RabbitMQ, **AMQP (Advanced Message Queuing Protocol)** standardını uygulayan, geleneksel bir mesaj kuyruğudur. Karmaşık routing (yönlendirme) senaryoları için idealdir.

#### Temel Bileşenler
*   **Exchange:** Producer mesajı direkt kuyruğa atmaz, Exchange'e atar. Exchange, mesajı hangi kuyruğa yönlendireceğine karar verir.
*   **Queue:** Mesajların beklediği yer.
*   **Binding:** Exchange ile Queue arasındaki kural (köprü).

#### Exchange Tipleri
1.  **Direct:** Routing key tam eşleşirse kuyruğa gider. (Log seviyesi: `error` -> ErrorQueue).
2.  **Fanout:** Mesajı bağlı olan **tüm** kuyruklara kopyalar (Broadcasting). (Spor skoru -> MobileAppQueue, WebAppQueue).
3.  **Topic:** Wildcard (`*`, `#`) kullanarak desen eşleştirme yapar. (`stock.usd.*` -> USDQueue).
4.  **Headers:** Routing key yerine header özelliklerine bakar.

#### RabbitMQ Özellikleri
*   **Push Model:** Broker, mesajı consumer'a iter (Push). Consumer'ın "prefetch limit" ayarı ile kontrol edilir.
*   **Smart Broker, Dumb Client:** Routing mantığı broker üzerindedir.
*   **Message Acknowledgement (Ack):** Mesaj işlendikten sonra consumer "Ack" gönderir, broker mesajı siler.

---

### 4. Kafka vs RabbitMQ Karşılaştırması

| Özellik | Apache Kafka | RabbitMQ |
| :--- | :--- | :--- |
| **Model** | Log-based (Mesaj silinmez) | Queue-based (İşlenen silinir) |
| **İletişim** | Pull (Consumer çeker) | Push (Broker iter) |
| **Throughput** | Çok Yüksek (Milyonlarca/sn) | Yüksek (Binlerce/sn) |
| **Latency** | Çok Düşük (<10ms) | Çok Düşük (<1ms) |
| **Routing** | Basit (Partition key ile) | Gelişmiş (Exchange, Routing Key) |
| **Sıralama** | Partition bazında garanti | Queue bazında garanti |
| **Kullanım** | Event Streaming, Log Aggregation, Metrics | Complex Routing, Task Queues, Legacy |

**Hangisini Seçmeli?**
*   **Kafka:** Büyük veri akışı, loglama, event sourcing, veriyi saklayıp tekrar işleme (replay) gerekiyorsa.
*   **RabbitMQ:** Karmaşık routing kuralları, önceliklendirme (priority queue) gerekiyorsa veya veri hacmi çok devasa değilse.

---

### 5. Redis (Remote Dictionary Server) - Deep Dive

Redis sadece bir cache değildir; yüksek performanslı, in-memory bir veri yapısı sunucusudur.

#### Veri Yapıları ve Kullanım Alanları
1.  **String:** Cache, Session, Counter (`INCR`).
2.  **List:** Mesaj kuyruğu (`LPUSH`, `RPOP`), Son hareketler (Timeline).
3.  **Set:** Unique elemanlar (Takipçiler, IP whitelist). Kesişim/Birleşim (`SINTER`, `SUNION`) çok hızlıdır.
4.  **Sorted Set (ZSet):** Sıralı veriler. Leaderboard (Oyun skorları), Rate Limiting.
5.  **Hash:** Nesne saklama (User profili: ad, soyad, yaş).
6.  **HyperLogLog:** Çok az bellekle tahmini sayım (Unique visitor count). (%0.81 hata payı).
7.  **Geo:** Coğrafi konum verisi ve mesafe sorguları (`GEORADIUS`).
8.  **Stream:** Kafka benzeri log veri yapısı (Redis 5.0+).

#### Persistence (Kalıcılık)
Redis RAM'de çalışır ama veriyi diske yazabilir:
*   **RDB (Snapshot):** Belirli aralıklarla (örn: 5 dakikada bir) tüm verinin fotoğrafını çeker.
    *   *Pros:* Hızlı başlatma, kompakt dosya.
    *   *Cons:* Son snapshot'tan sonraki veriler kaybolabilir.
*   **AOF (Append Only File):** Her yazma komutunu loglar.
    *   *Pros:* Veri kaybı minimum (1 saniye).
    *   *Cons:* Dosya boyutu büyür, başlatma yavaştır.

#### High Availability & Scaling
*   **Redis Sentinel:** Master-Slave izleme ve otomatik failover sağlar. Master çökerse Slave'i Master yapar.
*   **Redis Cluster:** Veriyi birden fazla node'a dağıtır (Sharding). 16384 hash slot kullanır.

#### Single-Threaded Architecture
Redis, komutları işlemek için **tek bir thread** kullanır (Event Loop).
*   **Neden Hızlı?** Context switch maliyeti yoktur, lock (kilit) derdi yoktur, RAM erişimi çok hızlıdır.
*   **Dikkat:** `KEYS *` gibi O(N) karmaşıklığındaki komutlar tüm sunucuyu bloklar! Asla production'da kullanmayın (`SCAN` kullanın).

---

### 6. Gerçek Hayat Senaryoları ve İleri Seviye Mülakat Soruları (20 Case Study)

Bu bölüm, "Kafka nedir?" gibi teorik soruların ötesine geçerek, production ortamlarında karşılaşacağınız karmaşık senaryoları, failure mode'ları ve mimari kararları içerir.

#### A. Redis Senaryoları (Deep Dive)

**Senaryo 1: Session Management ve Güvenlik**
*   **Soru:** "Milyonlarca kullanıcının oturumunu Redis'te tutuyoruz. Nasıl bir key yapısı kullanırsın ve güvenliği nasıl sağlarsın?"
*   **Çözüm:**
    *   **Key Pattern:** `session:{userId}:{deviceId}` formatı kullanırım. Bu sayede bir kullanıcının tüm cihazlardaki oturumlarını bulup (`SCAN` ile) tek seferde "Tüm cihazlardan çıkış yap" özelliği sunabilirim.
    *   **TTL:** Mutlaka TTL (Time-To-Live) set ederim (örn: 30 dk). Her işlemde TTL'i uzatırım (Sliding Expiration).
    *   **Güvenlik:** Session verisini (JSON) şifreleyerek (AES) Redis'e yazarım. Redis çalınsa bile tokenlar işe yaramaz.

**Senaryo 2: Distributed Lock (Redlock) ve "Double Booking"**
*   **Soru:** "Bir e-ticaret sitesinde son kalan ürünü aynı anda iki kişi almaya çalışıyor. Redlock ile nasıl çözersin? Sunucu lock aldıktan sonra çökerse ne olur?"
*   **Çözüm:**
    *   **Lock Alma:** `SET resource_key my_random_token NX PX 30000` komutuyla atomik lock alırım.
    *   **Failure Mode:** Sunucu lock'ı aldı ama işi bitiremeden çöktü (Crash). `PX 30000` (30 sn) sayesinde Redis lock'ı otomatik düşürür (Deadlock önlenir).
    *   **Fencing Token:** Eğer GC pause yüzünden işlem 30 saniyeden uzun sürerse, lock düşer ve başkası alır. Bunu önlemek için veritabanına yazarken artan bir versiyon numarası (fencing token) kullanırım.

**Senaryo 3: Rate Limiting (API Kotası)**
*   **Soru:** "Bir IP adresinden dakikada en fazla 100 istek gelmesine izin veren sistemi Redis ile tasarla."
*   **Çözüm:**
    *   **Basit (Fixed Window):** `INCR ip:127.0.0.1:2023-10-27-14-05` ve `EXPIRE 60`. Ancak pencere geçişlerinde (14:05:59 ile 14:06:01 arası) 200 istek kaçabilir.
    *   **İleri (Sliding Window Log):** Redis `Sorted Set` kullanırım. Her isteğin timestamp'ini `ZADD` ile eklerim. `ZREMRANGEBYSCORE` ile 1 dakika öncesini silerim. `ZCARD` ile sayıyı kontrol ederim. Atomik olması için bu mantığı **Lua Script** içine gömerim.

**Senaryo 4: Real-time Leaderboard (Oyun Skorları)**
*   **Soru:** "Milyonlarca oyuncunun olduğu bir oyunda anlık sıralamayı (Top 10) nasıl gösterirsin?"
*   **Çözüm:** Redis **Sorted Set (ZSet)** veri yapısı bunun için biçilmiş kaftandır.
    *   `ZADD leaderboard 1500 "user1"`: Puan ekle/güncelle (O(logN)).
    *   `ZREVRANGE leaderboard 0 9 WITHSCORES`: En yüksek 10 kişiyi getir.
    *   `ZRANK leaderboard "user1"`: Kullanıcının kaçıncı olduğunu bul.
    *   İlişkisel veritabanında `ORDER BY score DESC LIMIT 10` yapmak milyonlarca satırda çok yavaştır, Redis'te milisaniyeler sürer.

**Senaryo 5: Thundering Herd Problemi**
*   **Soru:** "Çok popüler bir cache key'in süresi (TTL) dolduğu anda 10.000 istek aynı anda gelirse ne olur? Nasıl önlenir?"
*   **Çözüm:** Hepsi aynı anda "Cache Miss" alır ve hepsi aynı anda DB'ye saldırır. DB çöker.
    *   **Çözüm 1 (Mutex):** Cache boşsa, sadece bir thread lock alıp DB'ye gider, diğerleri bekler.
    *   **Çözüm 2 (Probabilistic Early Expiration):** TTL dolmadan (örn: son %10'luk dilimde) rastgele bir ihtimalle cache'i yenilerim. Böylece herkes aynı anda süresi dolmuş görmez.

**Senaryo 6: Redis Streams vs Pub/Sub**
*   **Soru:** "Chat uygulaması için hangisini seçersin?"
*   **Cevap:**
    *   **Pub/Sub:** "Fire and Forget" mantığıdır. Mesajı gönderirsin, o an dinleyen alır. Dinlemeyen (offline) kullanıcı mesajı kaybeder.
    *   **Streams:** Kafka gibidir. Mesajlar saklanır. Kullanıcı offline olsa bile geri geldiğinde kaldığı yerden (`last_id`) okuyabilir. Chat geçmişi için **Streams** daha doğrudur.

**Senaryo 7: Geo-Spatial Indexing (Yakındaki Sürücüler)**
*   **Soru:** "Uber gibi, bana en yakın 5 taksiyi bulan servisi nasıl yazarsın?"
*   **Çözüm:** Redis `GEO` komutları.
    *   `GEOADD taxis 41.0082 28.9784 "taxi_1"`: Konum güncelleme.
    *   `GEORADIUS taxis 41.0082 28.9784 1 km ASC COUNT 5`: 1 km çapındaki en yakın 5 taksiyi getir.
    *   PostgreSQL (PostGIS) de yapabilir ama Redis çok daha hızlıdır (High Throughput).

#### B. Apache Kafka Senaryoları (Architecture & Failure)

**Senaryo 8: Consumer Rebalancing Storm**
*   **Soru:** "Consumer grubuna yeni bir instance eklendiğinde sistem duruyor. Neden?"
*   **Çözüm:** Varsayılan "Eager Rebalancing" stratejisinde, grup üyeleri değişince herkes durur (Stop-the-world), partitionlar yeniden dağıtılır.
    *   **Çözüm:** **Cooperative Sticky Assignor** kullanırım. Sadece gereken partitionlar el değiştirir, diğerleri çalışmaya devam eder. Kesinti olmaz.

**Senaryo 9: Exactly-Once Semantics (Ödeme İşlemi)**
*   **Soru:** "Bir ödeme mesajını Kafka'dan okuyup işliyoruz. Sunucu çökerse ve mesaj tekrar gelirse (At-least-once), iki kere para çekmemeyi nasıl garantilersin?"
*   **Çözüm:**
    *   **Idempotency Key:** Mesajın içinde benzersiz bir `transactionId` olmalı. DB'de `processed_transactions` tablosunda bu ID var mı diye bakarım.
    *   **Transactional Outbox Pattern:** Kafka'ya mesaj atmakla DB'ye yazmayı aynı transaction içinde yaparım (Debezium gibi CDC araçlarıyla).

**Senaryo 10: Message Ordering (Sıralama Garantisi)**
*   **Soru:** "Bir kullanıcının 'Sipariş Verildi' -> 'Ödeme Alındı' -> 'Kargolandı' eventlerinin sırası bozulursa sistem çöker. Nasıl garanti edersin?"
*   **Çözüm:** Kafka sadece **Partition** içinde sıra garanti eder.
    *   Producer tarafında mesajları gönderirken Key olarak `userId` veya `orderId` kullanırım.
    *   Aynı Key'e sahip tüm mesajlar (hash algoritmasıyla) her zaman **aynı partition'a** düşer. Böylece o partition'ı okuyan consumer, olayları sırasıyla işler.

**Senaryo 11: Poison Pill (Zehirli Mesaj)**
*   **Soru:** "Kuyruğa bozuk formatta bir mesaj geldi. Consumer bunu parse ederken exception fırlatıyor, çöküyor, restart oluyor, aynı mesajı tekrar alıyor (Infinite Loop). Ne yaparsın?"
*   **Çözüm:**
    *   **Dead Letter Queue (DLQ):** Belirli bir deneme sayısından (örn: 3) sonra hata almaya devam ediyorsam, mesajı ana topic'ten alıp `error-topic` (DLQ) kuyruğuna atarım ve offset'i ilerletirim (commit).
    *   Sistem akmaya devam eder. DLQ'daki mesajları sonra incelerim.

**Senaryo 12: Consumer Lag Monitoring**
*   **Soru:** "Sistemin yavaşladığını nasıl anlarsın?"
*   **Çözüm:** En kritik metrik **Consumer Lag**'dir. (Producer Offset - Consumer Offset).
    *   Eğer Lag sürekli artıyorsa, consumerlar yetişemiyor demektir.
    *   **Action:** Yeni consumer instance'ları ekleyerek (scale out) partitionları paylaştırırım.

**Senaryo 13: Log Compaction (State Store)**
*   **Soru:** "Müşteri adreslerini Kafka'da tutuyoruz. Müşteri 50 kere adres değiştirdi. 50 mesajı da saklamak zorunda mıyız?"
*   **Çözüm:** Hayır. Topic için `cleanup.policy=compact` açarım.
    *   Kafka, arka planda aynı Key'e (`customerId`) sahip mesajlardan sadece **en sonuncusunu** tutar, eskileri siler. Topic boyutu küçülür, restore süresi kısalır.

**Senaryo 14: High Availability ve Broker Çökmesi**
*   **Soru:** "Kafka cluster'da bir broker çöktü ve veri kaybettik. Nasıl önlerdik?"
*   **Çözüm:**
    *   `replication.factor`: En az 3 olmalı. (Veri 3 brokerda kopyalanır).
    *   `min.insync.replicas`: En az 2 olmalı. (Producer yazarken en az 2 kopyaya yazıldığından emin olur).
    *   `acks=all`: Producer, lider ve replikalar yazana kadar bekler.

#### C. RabbitMQ Senaryoları (Routing & Reliability)

**Senaryo 15: Dead Letter Exchange (DLX) ile Retry Mekanizması**
*   **Soru:** "RabbitMQ'da başarısız olan mesajı 5 dakika sonra tekrar denemek istiyorum. Nasıl yaparım?"
*   **Çözüm:** RabbitMQ'da yerleşik "delayed retry" yoktur.
    *   Mesajı reddederim (`nack`) ve bir DLX'e yönlendiririm.
    *   DLX, mesajı `retry_queue`'ya atar.
    *   `retry_queue` üzerinde `x-message-ttl=300000` (5 dk) ayarı vardır ve consumer'ı yoktur.
    *   Süre dolunca (TTL), mesaj "ölür" ve bu kuyruğun DLX'i olan ana kuyruğa geri döner.

**Senaryo 16: Priority Queues (VIP Kullanıcılar)**
*   **Soru:** "Premium üyelerin işlemleri standart üyelerden önce yapılmalı. Nasıl tasarlarsın?"
*   **Çözüm:** RabbitMQ **Priority Queue** özelliğini kullanırım.
    *   Kuyruğu tanımlarken `x-max-priority=10` derim.
    *   Premium kullanıcı mesajlarına `priority=10`, standartlara `priority=1` veririm.
    *   Consumer, kuyrukta mesaj varsa önce yüksek önceliklileri çeker.

**Senaryo 17: Lazy Queues (RAM Yönetimi)**
*   **Soru:** "Kuyrukta milyonlarca mesaj birikti ve RabbitMQ RAM yetersizliğinden çöktü (OOM). Ne yapmalıydık?"
*   **Çözüm:** Kuyrukları **Lazy Queue** modunda tanımlamalıydık.
    *   Lazy Queue, mesajları RAM yerine direkt diske yazar. Sadece ihtiyaç olduğunda RAM'e yükler. Performans biraz düşer ama sistemin çökmesi engellenir.

**Senaryo 18: Complex Routing (Topic Exchange)**
*   **Soru:** "Loglama sistemi yapıyoruz. `app.error`, `db.info`, `auth.warn` gibi routing keyler var. Sadece 'error'ları bir servise, 'db' ile ilgili her şeyi başka servise nasıl yönlendirirsin?"
*   **Çözüm:** **Topic Exchange** kullanırım.
    *   Queue A (Error Service): Binding key `*.error`.
    *   Queue B (DB Service): Binding key `db.#` (`#` birden çok kelimeyi kapsar).

**Senaryo 19: TTL (Time-To-Live) ile Otomatik İptal**
*   **Soru:** "Kullanıcı sipariş verdi ama 15 dakika içinde ödeme yapmadı. Siparişi otomatik iptal et."
*   **Çözüm:**
    *   Sipariş oluşunca RabbitMQ'ya bir mesaj atarım ve `expiration=900000` (15 dk) veririm.
    *   Bu mesajın gideceği kuyruğun bir DLX'i (Dead Letter Exchange) olur: `cancel_order_queue`.
    *   15 dakika boyunca kimse mesajı okumaz. Süre dolunca mesaj DLX üzerinden iptal kuyruğuna düşer. İptal servisi bu kuyruğu dinler ve siparişi iptal eder.

**Senaryo 20: Clustering & Quorum Queues**
*   **Soru:** "RabbitMQ master node çöktü, kuyruk verileri gitti. Mirroring kullanıyorduk ama yine de sorun oldu (Split-brain)."
*   **Çözüm:** Klasik Mirroring (HA) artık önerilmiyor. **Quorum Queues** kullanılmalı.
    *   Raft konsensüs algoritmasını kullanır (Kafka gibi). Veri tutarlılığı ve güvenliği çok daha yüksektir. Network partition durumlarında daha dayanıklıdır.

---

### 7. Geliştirici İpuçları

*   **Idempotency:** Mesaj kuyruklarında "At-least-once" delivery yaygındır. Yani aynı mesaj iki kere gelebilir. Consumer'larınız mutlaka **Idempotent** olmalıdır (Aynı mesajı 10 kere işlese de sonuç değişmemeli).
*   **Redis Keys:** Key isimlerinizde `:` kullanarak hiyerarşi oluşturun (`user:1000:profile`). Çok uzun key'ler bellek israfıdır.
*   **Monitoring:** Kafka'da "Consumer Lag" (Consumer'ın ne kadar geriden geldiği) en kritik metriktir. Lag artıyorsa consumer sayısını artırın.

---

