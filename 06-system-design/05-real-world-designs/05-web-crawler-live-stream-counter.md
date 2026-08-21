# Gerçek Dünya Sistem Tasarımları — Bölüm 5: Dağıtık Web Crawler, Canlı Yayın (Twitch/Live Streaming) ve Dağıtık Sayaç (Distributed Counter / Metrics)

---

## 1. Dağıtık Web Crawler (Google / Search Engine Spider)

> **Analoji:** Web Crawler, kütüphanedeki tüm kitapları tek tek gezip içindeki çapraz referansları (linkleri) not eden ve yeni kitapları bulup getiren devasa bir "otonom robot ordusu" gibidir.

```
                  ┌────────────────────────┐
                  │    URL Frontier (Kuyruk)│◄────────────┐
                  │  (Öncelik + Nezaket)   │             │
                  └───────────┬────────────┘             │ (Yeni Bulunan Linkler)
                              │ URL Çek                  │
                              ▼                          │
                  ┌────────────────────────┐             │
                  │   DNS Resolver Cache   │             │
                  └───────────┬────────────┘             │
                              │ IP                       │
                              ▼                          │
                  ┌────────────────────────┐             │
                  │   Fetcher / Downloader │             │
                  └───────────┬────────────┘             │
                              │ HTML                     │
                              ▼                          │
                  ┌────────────────────────┐             │
                  │ Parser & Link Extractor├─────────────┘
                  └───────────┬────────────┘
                              │ Text / Doküman
                              ▼
                  ┌────────────────────────┐
                  │ Duplicate Checker      │ (Fingerprint / SimHash)
                  └───────────┬────────────┘
                              │ Unik İse
                              ▼
                  ┌────────────────────────┐
                  │ Document Storage / S3  │ ──► [Inverted Index Builder]
                  └────────────────────────┘
```

### Gereksinimler & Ölçek
- **Hacim:** Aylık 10 Milyar web sayfası tarama.
- **Throughput:** ~4,000 sayfa / saniye (ortalama), 10,000 sayfa / saniye (peak).
- **Depolama:** Sayfa başına ortalama 500 KB (HTML + Metin) → Aylık ~5 Petabyte veri.
- **Kritik Kurallar:**
  - **Politeness (Nezaket):** Aynı web sunucusuna (host) saniyede yüzlerce istek atıp sunucuyu çökertmemek (`robots.txt` uyumu + host bazlı rate limit).
  - **Freshness (Güncellik):** Sık güncellenen siteleri (haber siteleri) daha sık, statik siteleri daha seyrek taramak.
  - **Deduplication:** Aynı içeriği farklı URL'lerde tekrar saklamamak.

### Mimari Bileşenler

#### 1. URL Frontier (En Kritik Bileşen)
URL Frontier, taranacak URL'lerin tutulduğu ve iki kuralı dengede tutan akıllı kuyruk sistemidir:
- **Politeness (Nezaket Havuzu):** Her `host` (örn: `cnn.com`, `medium.com`) için ayrı bir FIFO kuyruğu ayrılır. Her host kuyruğuna iki istek arasında belirli bir gecikme (örn: 1 saniye) konur.
- **Priority (Öncelik Havuzu):** PageRank skoru veya domain otoritesi yüksek olan sayfalar öncelikli kuyruklara (`High Priority`) alınır.

#### 2. Duplicate Detection (İçerik Tekilleştirme)
- **URL Tekilleştirme:** Bloom Filter (In-Memory) kullanılarak bir URL'nin daha önce taranıp taranmadığı O(1) hızında kontrol edilir (False positive kabul edilebilir, false negative asla olmaz).
- **İçerik Tekilleştirme:** Sayfa metinleri **SimHash** veya **MinHash** algoritmaları ile özetlenir (Fingerprinting). İki sayfanın Hamming mesafesi < 3 ise içerik aynı kabul edilir ve diskte tekrar depolanmaz.

#### 3. DNS Cache & Fetcher Optimizasyonu
- Standart DNS sorguları 50-200ms gecikmeye yol açar. Crawler worker'lar içinde yerel **Custom DNS Cache** tutulur.
- Dağıtık worker'lar coğrafi olarak dünyaya yayılır (Geo-distributed Fetchers), hedefe en yakın worker sayfayı indirir.

---

## 2. Canlı Yayın Platformu Tasarımı (Twitch / YouTube Live)

> **Analoji:** Canlı yayın sistemi, dev bir "su şebekesi" gibidir. Yayıncı kaynaktan suyu basar (Ingest), arıtma ve basınç istasyonları suyu farklı boru çaplarına (Transcoding/ABR) ayırır ve devasa dağıtım vanaları (CDN Edge) milyonlarca evin musluğuna gecikmesiz (Low Latency) ulaştırır.

```
[Streamer (OBS)] 
      │ (RTMP / SRT - Yüksek Bitrate)
      ▼
┌──────────────────────────────────────┐
│       Live Ingest Server             │
└──────────────────┬───────────────────┘
                   │ Ham Video Akışı
                   ▼
┌──────────────────────────────────────┐
│  Transcoding Farm (GPU Cluster)      │ ──► 1080p, 720p, 480p, 360p Segmentleri
└──────────────────┬───────────────────┘
                   │ (HLS / LL-HLS / WebRTC Çıktısı)
                   ▼
┌──────────────────────────────────────┐
│      Origin Video Server             │
└──────────────────┬───────────────────┘
                   │ Segment Cache
                   ▼
┌──────────────────────────────────────┐
│      Live CDN Edge Ağı               │
└─────────┬──────────────────┬─────────┘
          │                  │
          ▼                  ▼
  [Viewer 1 (Web)]    [Viewer 2 (Mobile)]
```

### Gereksinimler & Ölçek
- **Eşzamanlı İzleyici:** Popüler bir yayında 1 Milyon+ eşzamanlı izleyici (Concurreny).
- **Uçtan Uca Gecikme (Latency):**
  - Standart HLS: 10-30 saniye (Chat etkileşimi için kabul edilemez).
  - **Low-Latency HLS (LL-HLS) / WebRTC:** < 2-3 saniye (Canlı chat senkronizasyonu).

### Mimari Bileşenler

#### 1. Ingest Katmanı (Video Girişi)
- Yayıncı `RTMP` (Real-Time Messaging Protocol) veya modern `SRT` (Secure Reliable Transport) ile ham video akışını Ingest sunucusuna gönderir (TCP veya UDP bazlı).

#### 2. Transcoding Farm (Gerçek Zamanlı Çevrim)
- Gelen 4K/1080p 60fps ham video, saniyeler içinde donanımsal GPU'lar (NVENC / QuickSync) ile farklı çözünürlük ve bitrate'lere (Adaptive Bitrate - ABR) dönüştürülür.
- Video 1-2 saniyelik mikro parçalara (TS / fMP4 chunks) bölünür ve Playlist (`.m3u8`) dosyası sürekli güncellenir.

#### 3. Dağıtım ve CDN Katmanı
- Segmentler **Origin Shield** sunucularından global **Live CDN** noktalarına (Cloudflare, Fastly, AWS CloudFront) akar.
- **Thundering Herd Problemi:** Milyonlarca izleyici aynı saniyede aynı `segment_1042.m4s` dosyasını isterse Origin Server çökebilir.
  - **Çözüm (Request Collapsing):** CDN Edge sunucusu Origin'e sadece **1** istek gönderir, gelen cevabı bekleyen tüm client'lara dağıtır.

#### 4. Canlı Chat Mimarisi (Pub/Sub & WebSocket)
- 1 milyon izleyicili bir yayında saniyede 50.000 chat mesajı yazılabilir.
- **Chat Dağıtımı:** WebSocket Gateway + Redis Cluster / Kafka Pub-Sub.
- **Chat Slow-Mode & Sampling:** İstemcilere her mesajı tek tek iletmek yerine mesajlar 100ms'lik pencerelerle batch'lenir (Client UI render darboğazını önler).

---

## 3. Dağıtık Yüksek Hacimli Sayaç Sistemi (Distributed Counter / Real-Time Metrics)

> **Analoji:** Çok yoğun bir mitingde tek bir görevlinin kapıda gelen herkesi tek tek sayması kuyruk oluşturur. Bunun yerine 50 kapıya 50 görevli konur, herkes kendi defterine sayar ve 5 dakikada bir toplamlar ana merkezde birleştirilir.

```
                     ┌─── Counter Shard 1 (Redis) ───┐
                     │   key: video_123_shard_0      │
                     └──────────────┬────────────────┘
                                    │
                                    │ (Scheduled Aggregator)
[Write Requests] ──► [Hash Router] ─┼─── Counter Shard 2 (Redis) ───► [Total Aggregator DB]
                                    │   key: video_123_shard_1        (Postgres / Cassandra)
                                    │
                     ┌──────────────┴────────────────┐
                     │   Counter Shard 3 (Redis)     │
                     │   key: video_123_shard_2      │
                     └───────────────────────────────┘
```

### Problem Tanımı
- YouTube videosu izlenme sayısı (View Count), Tweet beğeni sayısı veya Reklam tıklama sayısı (Ad Clicks).
- **Zorluk:** Aynı anda 1 milyon kullanıcı tek bir kaydı güncellemek isterse (`UPDATE videos SET views = views + 1 WHERE id = 123`), veritabanı satır kilitlemesi (Row Lock Contention) yüzünden sistem anında kilitlenir.

### Çözüm Mimarileri

#### 1. Sharded In-Memory Counters (Sharding Çözümü)
- Sayaç tek bir değişkende değil, N adet shard'da tutulur:
  - `video:123:view:shard_0`
  - `video:123:view:shard_1`
  - `...`
  - `video:123:view:shard_9`
- Gelen her istek `random(0, 9)` ile rastgele bir shard'a yönlendirilir ve `INCR` komutu çalıştırılır.
- Okuma yapılırken bu 10 shard toplanır (`SUM(shard_0..shard_9)`). Yazma kilitlemesi 1/N oranına düşer.

#### 2. Event Streaming & Batch Aggregation (Asenkron Çözüm)
- İzlenme olayları doğrudan DB'ye yazılmaz; Kafka'ya `ViewEvent` olarak basılır.
- **Flink / Spark Streaming:** 5 saniyelik pencerelerle (Tumbling Window) aynı ID'ye ait izlenmeleri toplar (örn: `video_123: +4500`).
- Veritabanına saniyede 1 milyon kez `+1` yazmak yerine, 5 saniyede bir tek bir `UPDATE ... SET views = views + 4500` sorgusu atılır.

---

## 4. Kritik Mülakat Soruları ve Tricky Senaryolar

### Soru 1: Web Crawler'da "Spider Trap" (Örümcek Tuzağı) nedir, nasıl engellenir?
**Cevap:** Sonsuz URL üreten dinamik sayfalardır (örn: sonsuz takvim linkleri `site.com/calendar?date=2026-08`, veya `dir1/dir2/dir1/dir2/...`).
- **Engelleme:**
  1. URL derinlik limiti koymak (Max Depth: 10).
  2. URL path uzunluk limiti koymak.
  3. Aynı domain'den çekilen maksimum sayfa sınırını aşınca URL Frontier'da o domain'i duraklatmak.

### Soru 2: Canlı yayında Adaptive Bitrate Streaming (ABR) nasıl çalışır?
**Cevap:** Client (video oynatıcı) her video parçasını (chunk) indirdikten sonra indirme süresini ve mevcut CPU/Buffer durumunu ölçer. Ağ yavaşladıysa bir sonraki parçayı daha düşük bitrate'li profilin manifest dosyasından (`720p.m3u8` yerine `480p.m3u8`) ister. Kullanıcı video donmadan izlemeye devam eder.

### Soru 3 (Tricky): Dağıtık sayaçta "Tam Kesinlik" (Exact Count) vs "Yaklaşık Değer" (Approximate Count) trade-off'u nedir?
**Cevap:**
- Milyonlarca izlenmesi olan bir YouTube videosunda izlenmenin `10,452,112` yerine `10.4M` görünmesi yeterlidir.
- Büyük ölçekte HyperLogLog (HLL) algoritması kullanılarak sadece **1.5 KB bellek** ile %1 hata payıyla benzersiz tekil sayma (Unique Visitors) yapılabilir. Kesinlikten ödün verilerek milyarlarca dolarlık bellek ve IO tasarrufu sağlanır.
