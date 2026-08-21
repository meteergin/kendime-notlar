## Konu 30: Sistem Tasarımı - Temeller ve Ölçeklenebilirlik (Part 1)

Sistem tasarımı (System Design), büyük ölçekli, güvenilir ve bakımı kolay sistemler oluşturma sanatıdır. Bu bölüm, performans, ölçeklenebilirlik, CAP teoremi ve yük dengeleme gibi temel kavramları kapsar.

---

### 1. Temel Kavramlar ve Metrikler

#### 1.1 Performance vs Scalability
*   **Performance:** Bir işlemin ne kadar hızlı yapıldığıdır (Response Time). "Sistem tek bir kullanıcı için ne kadar hızlı?"
*   **Scalability:** Sistemin artan yük altında performansını koruyabilme yeteneğidir. "Sistem 1 milyon kullanıcı varken de hızlı mı?"

#### 1.2 Latency vs Throughput
*   **Latency (Gecikme):** Bir işlemin başlaması ile bitmesi arasındaki süre (ms). Düşük olması iyidir.
*   **Throughput (İşlem Hacmi):** Birim zamanda yapılan işlem sayısı (RPS - Requests Per Second). Yüksek olması iyidir.
*   **İlişki:** Genellikle throughput artırmak için (batch processing) latency'den ödün verilir.

#### 1.3 Availability vs Consistency (CAP Theorem)
Dağıtık bir veri deposu, şu üç özellikten sadece ikisini garanti edebilir:
1.  **Consistency (Tutarlılık):** Her okuma işlemi en son yazılan veriyi veya hata döner.
2.  **Availability (Erişilebilirlik):** Her istek (hata olsa bile) bir yanıt alır.
3.  **Partition Tolerance (Bölünme Toleransı):** Ağ kopması durumunda bile sistem çalışmaya devam eder.

**Dağıtık sistemlerde P zorunludur.** Seçim CP veya AP arasındadır:
*   **CP (Consistency + Partition Tolerance):** Ağ koparsa, tutarlılığı korumak için bazı düğümler yanıt vermeyi durdurur (MongoDB, HBase).
*   **AP (Availability + Partition Tolerance):** Ağ koparsa, eski veri (stale data) dönse bile yanıt verir (Cassandra, DynamoDB).

---

### 2. Consistency Patterns (Tutarlılık Desenleri)

*   **Strong Consistency:** Yazma işleminden hemen sonraki okuma, güncel veriyi görür. (RDBMS).
*   **Eventual Consistency:** Yazma işleminden sonra, veri bir süre sonra tüm düğümlere yayılır. O ana kadar eski veri okunabilir. (DNS, Email, NoSQL).
*   **Weak Consistency:** Verinin güncelleneceği garanti edilmez. (Video streaming, VoIP).

---

### 3. Load Balancers (Yük Dengeleyiciler)

Gelen trafiği birden fazla sunucuya dağıtarak tek bir sunucunun aşırı yüklenmesini önler.

#### 3.1 LB vs Reverse Proxy
*   **Load Balancer:** Trafiği dağıtır (Nginx, HAProxy, AWS ELB).
*   **Reverse Proxy:** Sunucuların önünde durur; güvenlik, SSL sonlandırma, caching ve sıkıştırma yapar. (Genellikle LB aynı zamanda Reverse Proxy'dir).

#### 3.2 Layer 4 vs Layer 7 Load Balancing
*   **Layer 4 (Transport Layer):** IP ve Port bilgisine göre yönlendirir. İçeriğe (HTTP header, URL) bakmaz. Çok hızlıdır.
*   **Layer 7 (Application Layer):** HTTP içeriğine bakar. `/images` isteklerini resim sunucusuna, `/api` isteklerini API sunucusuna yönlendirebilir. Daha akıllıdır ama daha yavaştır.

#### 3.3 Load Balancing Algoritmaları
*   **Round Robin:** Sırayla dağıtır.
*   **Least Connections:** En az bağlantısı olan sunucuya gönderir.
*   **IP Hash:** Client IP'sine göre hep aynı sunucuya gönderir (Sticky Session için).

#### 3.4 Horizontal Scaling (Yatay Ölçekleme)
Daha fazla sunucu ekleyerek kapasiteyi artırmak. Load Balancer gerektirir. (Vertical Scaling: Mevcut sunucunun RAM/CPU'sunu artırmak).

---

### 4. DNS ve CDN

#### 4.1 Domain Name System (DNS)
İnternetin telefon rehberidir. `google.com` → `142.250.185.78`.
*   **Record Types:** A (IPv4), AAAA (IPv6), CNAME (Alias), MX (Mail), NS (Name Server).

#### 4.2 Content Delivery Networks (CDN)
Statik içeriği (resim, video, CSS) kullanıcılara coğrafi olarak en yakın sunuculardan (Edge Server) sunar.
*   **Pull CDN:** İlk istekte içeriği ana sunucudan (Origin) çeker ve cache'ler. Sonraki istekler cache'ten döner. (Cloudflare).
*   **Push CDN:** İçerik, geliştirici tarafından CDN'e yüklenir. (Amazon CloudFront).

---

### 5. Kritik Mülakat Sorusu 

**Soru:** Bir e-ticaret sitesi "Black Friday" indiriminde. Trafik 100 katına çıkacak. Sistemi nasıl ölçeklersiniz? (High Level Design)

**Cevap:**
1.  **CDN:** Tüm statik içerikleri (ürün resimleri, JS/CSS) CDN'e taşıyarak sunucu yükünü %80 azaltırım.
2.  **Load Balancer:** Uygulama sunucularının önüne Auto-Scaling Group ile çalışan bir LB koyarım. Trafik arttıkça otomatik yeni sunucu açılır.
3.  **Caching:** Ürün detayları, kategoriler gibi sık okunan verileri Redis'e (Distributed Cache) alırım. DB yükünü azaltırım.
4.  **Database:** Okuma işlemlerini Read Replica'lara dağıtırım. Yazma işlemleri Master DB'ye gider.
5.  **Asenkron İşlem:** Sipariş alındıktan sonra fatura, e-posta gibi işlemleri Message Queue (RabbitMQ/Kafka) ile asenkron yaparım. Kullanıcıyı bekletmem.

#### Soru 2 (Tricky): Load Balancer'da "Sticky Session" kullanmanın dezavantajı nedir?
**Cevap:**
*   **Yük Dengesizliği:** Bir sunucuya bağlı kullanıcılar çok aktifse, o sunucu aşırı yüklenir (diğerleri boş olsa bile).
*   **Failover Sorunu:** Sunucu çökerse, session bilgisi kaybolur (kullanıcı logout olur).
*   **Çözüm:** Stateless mimari (JWT) veya Distributed Session (Redis) kullanın.

#### Soru 3 (Tricky): CDN Cache Invalidation neden zordur?
**Cevap:** Dünyanın her yerindeki binlerce edge server'dan veriyi anında silmek (purge) zaman alır ve maliyetlidir.
*   **Çözüm:** Dosya ismine versiyon ekleyin (`style_v2.css`). Yeni versiyon yeni URL demektir, cache sorunu olmaz (Cache Busting).

---

