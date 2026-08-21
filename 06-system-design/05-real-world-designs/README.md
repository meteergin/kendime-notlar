# 🏗️ Gerçek Dünya Sistem Tasarımları (Real-World System Designs)

> Mülakatlarda en sık karşılaşılan, mimari vizyonunuzu ve ölçeklenebilirlik yetkinliğinizi test eden gerçek dünya sistem tasarımı problemleri, mimari şemaları ve çözümleri.

---

## 📚 Tasarım Rehberleri

| Bölüm | Başlık | Kapsanan Sistemler | Bağlantı |
|---|---|---|---|
| **Bölüm 1** | URL Shortener, Rate Limiter & Chat | TinyURL (Base62/Hashing), Distributed Rate Limiter (Token Bucket/Leaky Bucket), WhatsApp/Discord Real-Time Chat (WebSocket/Presence) | [01-url-shortener-rate-limiter-chat.md](01-url-shortener-rate-limiter-chat.md) |
| **Bölüm 2** | Video Streaming, Ride-Sharing & Search | YouTube/Netflix Video Processing & CDN, Uber/Grab Konum Takibi & Eşleştirme (QuadTree), Google Typeahead Search (Trie) | [02-youtube-uber-search.md](02-youtube-uber-search.md) |
| **Bölüm 3** | Social Media Feed, Booking & Payments | Twitter/X News Feed (Fan-out on write/read), Hotel/Flight Booking (Optimistic/Pessimistic Lock), Stripe Payment Gateway (2PC/Idempotency) | [03-twitter-hotel-booking-payment.md](03-twitter-hotel-booking-payment.md) |
| **Bölüm 4** | Media Sharing, Cloud Storage & E-Commerce | Instagram Fotoğraf Akışı & Keşfet, Dropbox Dosya Senkronizasyonu & Chunking, Amazon/E-Ticaret Flash Sale & Sepet Mimarisi | [04-instagram-dropbox-ecommerce.md](04-instagram-dropbox-ecommerce.md) |
| **Bölüm 5** | Web Crawler, Live Streaming & High-Volume Metrics | Google Web Crawler & URL Frontier, Twitch/YouTube Canlı Yayın (LL-HLS, ABR, Transcoding), Dağıtık Sayaç (Sharded Counters / Aggregation) | [05-web-crawler-live-stream-counter.md](05-web-crawler-live-stream-counter.md) |
| **Bölüm 6** | Notifications, Job Scheduling & Proximity Service | Apple/Google Push, SMS & E-posta Bildirim Motoru, Dağıtık Zamanlayıcı (Timing Wheel / Redis ZSET), Yelp / Harita Yakındaki Yerler (Geohash / Uber H3) | [06-notification-scheduler-proximity.md](06-notification-scheduler-proximity.md) |

---

## 📐 Sistem Tasarımı Mülakatlarında İzlenecek 4 Aşamalı Çerçeve (Framework)

1. **Gereksinimleri Netleştirme (Scope & Estimation - 5 dk):**
   - Fonksiyonel ve Fonksiyonel Olmayan (Non-functional: Latency, HA, Consistency) gereksinimler.
   - Trafik (RPS, QPS), Depolama (Storage) ve Bandwidth hesaplamaları.
2. **Üst Seviye Tasarım (High-Level Architecture - 10-15 dk):**
   - API Tanımları, Veritabanı Şeması (SQL vs NoSQL).
   - Temel bloklar: Load Balancer, Gateway, Services, DB, Cache, Message Broker.
3. **Detaylı Tasarım ve Darboğazlar (Deep Dive - 15-20 dk):**
   - Kritik bileşenlerin derinlemesine analizi (Veri sharding, replikasyon, concurrency, failover).
4. **Darboğazları Çözme & Ölçekleme (Bottlenecks & Trade-offs - 5 dk):**
   - Single Point of Failure (SPOF) analizi, Caching stratejileri, Rate limiting, Monitoring & Metrics.
