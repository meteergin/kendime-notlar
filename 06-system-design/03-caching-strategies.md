## Konu 18: Caching Stratejileri (Server-Side, Redis, Memcached, HTTP Caching)

Caching, performans optimizasyonunun en etkili yöntemlerinden biridir. Bir geliştirici olarak, farklı caching stratejilerini (Cache-Aside, Write-Through, Write-Behind), Redis ve Memcached arasındaki farkları ve HTTP caching mekanizmalarını (ETag, Cache-Control) derinlemesine bilmelisiniz.

---

### 1. Caching Nedir?

**Analoji:** Caching, bir "not defteri" gibidir. Sık kullandığınız bilgileri (telefon numaraları) not defterine yazarsınız. Her seferinde telefon rehberini (veritabanı) karıştırmak yerine, not defterinize (cache) bakarsınız.

#### Neden Caching?
*   **Performans:** Veritabanı sorguları yavaştır. Cache bellekte (RAM) çalışır, çok hızlıdır.
*   **Ölçeklenebilirlik:** Veritabanı yükünü azaltır, daha fazla kullanıcıya hizmet verebilirsiniz.
*   **Maliyet:** Daha az veritabanı kaynağı gerekir.

---

### 2. Caching Stratejileri

#### 1. Cache-Aside (Lazy Loading)

**En yaygın strateji.** Uygulama önce cache'e bakar, yoksa veritabanından çeker ve cache'e yazar.

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    public User getUserById(Long id) {
        String cacheKey = "user:" + id;
        
        // 1. Cache'e bak
        User user = redisTemplate.opsForValue().get(cacheKey);
        
        if (user != null) {
            return user; // Cache Hit
        }
        
        // 2. Cache Miss: DB'den çek
        user = userRepository.findById(id).orElseThrow();
        
        // 3. Cache'e yaz
        redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
        
        return user;
    }
}
```

**Avantajları:**
*   Sadece kullanılan veriler cache'lenir (bellek tasarrufu).
*   Cache çökerse, uygulama çalışmaya devam eder (DB'den okur).

**Dezavantajları:**
*   İlk istek yavaştır (Cache Miss).
*   Cache ve DB arasında tutarsızlık olabilir.

#### 2. Write-Through

Veri yazılırken hem DB'ye hem cache'e yazılır.

```java
public void updateUser(User user) {
    userRepository.save(user); // DB'ye yaz
    redisTemplate.opsForValue().set("user:" + user.getId(), user); // Cache'e yaz
}
```

**Avantajları:**
*   Cache her zaman günceldir.

**Dezavantajları:**
*   Yazma işlemi yavaşlar (iki yere yazılır).
*   Hiç okunmayan veriler de cache'lenir (bellek israfı).

#### 3. Write-Behind (Write-Back)

Veri önce cache'e yazılır, arka planda asenkron olarak DB'ye yazılır.

**Avantajları:**
*   Yazma işlemi çok hızlıdır.

**Dezavantajları:**
*   Cache çökerse veri kaybı riski vardır.
*   Karmaşık implementasyon.

---

### 3. Spring Cache Abstraction

Spring, farklı cache provider'larını (Redis, Caffeine, Ehcache) soyutlar.

#### Temel Kullanım

```java
@Configuration
@EnableCaching
public class CacheConfig {
}

@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id") // Cache'e ekle
    public User getUserById(Long id) {
        // Bu metot sadece Cache Miss'te çalışır
        return userRepository.findById(id).orElseThrow();
    }
    
    @CachePut(value = "users", key = "#user.id") // Cache'i güncelle
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id") // Cache'ten sil
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @CacheEvict(value = "users", allEntries = true) // Tüm cache'i temizle
    public void clearCache() {
    }
}
```

**SpEL (Spring Expression Language) ile Key:**
```java
@Cacheable(value = "users", key = "#user.email")
public User findByEmail(String email) { }

@Cacheable(value = "orders", key = "#userId + '_' + #year")
public List<Order> getOrdersByUserAndYear(Long userId, int year) { }
```

---

### 4. Redis (Remote Dictionary Server)

**Analoji:** Redis, bir "süper hızlı not defteri" gibidir. Sadece key-value saklamakla kalmaz, liste, set, sorted set gibi veri yapılarını da destekler.

#### Redis Özellikleri
*   **In-Memory:** Tüm veri RAM'de saklanır (çok hızlı).
*   **Persistence:** Snapshot (RDB) veya Append-Only File (AOF) ile disk'e yazılabilir.
*   **Data Structures:** String, List, Set, Sorted Set, Hash, Bitmap, HyperLogLog.
*   **Pub/Sub:** Mesajlaşma sistemi.
*   **Transactions:** MULTI/EXEC ile atomik işlemler.
*   **Replication:** Master-Slave replikasyon.
*   **Clustering:** Yatay ölçeklenebilirlik.

#### Spring Data Redis

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}

@Service
public class ProductService {
    
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    public void cacheProduct(Product product) {
        redisTemplate.opsForValue().set("product:" + product.getId(), product, 30, TimeUnit.MINUTES);
    }
    
    public Product getProduct(Long id) {
        return redisTemplate.opsForValue().get("product:" + id);
    }
    
    // List işlemleri
    public void addToCart(Long userId, Product product) {
        redisTemplate.opsForList().rightPush("cart:" + userId, product);
    }
    
    // Set işlemleri (Unique)
    public void addTag(Long productId, String tag) {
        redisTemplate.opsForSet().add("tags:" + productId, tag);
    }
    
    // Sorted Set (Leaderboard)
    public void updateScore(String username, double score) {
        redisTemplate.opsForZSet().add("leaderboard", username, score);
    }
}
```

---

### 5. Memcached

**Analoji:** Memcached, "basit bir not defteri" gibidir. Sadece key-value saklar, Redis kadar zengin özellikler sunmaz ama çok basit ve hızlıdır.

#### Redis vs Memcached

| Özellik | Redis | Memcached |
| :--- | :--- | :--- |
| **Veri Yapıları** | String, List, Set, Hash, Sorted Set | Sadece String (key-value) |
| **Persistence** | Evet (RDB, AOF) | Hayır (sadece RAM) |
| **Replication** | Evet (Master-Slave) | Hayır |
| **Transactions** | Evet | Hayır |
| **Pub/Sub** | Evet | Hayır |
| **Multi-Threading** | Single-threaded (Event loop) | Multi-threaded |
| **Kullanım** | Karmaşık senaryolar | Basit caching |

**Ne Zaman Memcached?**
*   Sadece basit key-value caching gerekiyorsa.
*   Çok yüksek throughput (multi-threading avantajı).

**Ne Zaman Redis?**
*   Karmaşık veri yapıları gerekiyorsa.
*   Persistence, replication, pub/sub gibi özellikler gerekiyorsa.

---

### 6. HTTP Caching

**Analoji:** HTTP caching, bir "kitap kütüphanesi" gibidir. Bir kitabı (web sayfası) bir kez ödünç alırsınız (download), sonra tekrar gitmek yerine evinizde (browser cache) okursunuz.

#### Cache-Control Header

```http
Cache-Control: public, max-age=3600
```

| Directive | Açıklama |
| :--- | :--- |
| `public` | Herkes (browser, CDN) cache'leyebilir |
| `private` | Sadece browser cache'leyebilir (CDN cache'lemez) |
| `no-cache` | Cache'leyebilir ama her seferinde sunucuya doğrulama yapmalı |
| `no-store` | Hiç cache'leme (hassas veriler için) |
| `max-age=3600` | 3600 saniye (1 saat) geçerli |
| `must-revalidate` | Süresi dolunca mutlaka sunucuya sor |

**Spring Boot'ta:**
```java
@GetMapping("/api/products/{id}")
public ResponseEntity<Product> getProduct(@PathVariable Long id) {
    Product product = productService.getById(id);
    
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(product);
}
```

#### ETag (Entity Tag)

**Analoji:** ETag, bir "parmak izi" gibidir. Sunucu, kaynağın bir hash'ini (ETag) gönderir. Browser bir sonraki istekte bu hash'i gönderir. Sunucu, kaynak değişmediyse `304 Not Modified` döner (veri gönderilmez).

```java
@GetMapping("/api/products/{id}")
public ResponseEntity<Product> getProduct(@PathVariable Long id, WebRequest request) {
    Product product = productService.getById(id);
    String etag = "\"" + product.getVersion() + "\"";
    
    // Browser'ın gönderdiği ETag ile karşılaştır
    if (request.checkNotModified(etag)) {
        return null; // 304 Not Modified
    }
    
    return ResponseEntity.ok()
        .eTag(etag)
        .body(product);
}
```

---

### 7. Kritik Mülakat Soruları 

#### Soru 1: Cache Stampede (Thundering Herd) nedir? Nasıl önlenir?
**Cevap:** Popüler bir cache key'in süresi dolduğunda, aynı anda binlerce istek DB'ye gider (stampede). 

**Çözümler:**
1.  **Lock Mekanizması:** İlk istek DB'ye gider, diğerleri bekler.
2.  **Probabilistic Early Expiration:** Süre dolmadan önce rastgele yenile.
3.  **Cache Warming:** Süre dolmadan önce arka planda yenile.

#### Soru 2: Redis'te Eviction Policy nedir?
**Cevap:** Bellek dolduğunda hangi key'lerin silineceğini belirler.

| Policy | Açıklama |
| :--- | :--- |
| `noeviction` | Yeni veri yazılamaz, hata döner |
| `allkeys-lru` | En az kullanılan (Least Recently Used) key silinir |
| `volatile-lru` | Expire time'ı olan key'ler arasından LRU |
| `allkeys-random` | Rastgele key silinir |
| `volatile-ttl` | En kısa TTL'ye sahip key silinir |

**Tavsiye:** `allkeys-lru` (en yaygın).

#### Soru 3: Redis Persistence: RDB vs AOF?
**Cevap:**
*   **RDB (Snapshot):** Belirli aralıklarla tüm veriyi disk'e yazar. Hızlı, ama son snapshot'tan sonraki veriler kaybolabilir.
*   **AOF (Append-Only File):** Her yazma işlemini log'lar. Veri kaybı riski düşük ama dosya büyür.

**Tavsiye:** İkisini birlikte kullanın (Hybrid).

#### Soru 4: `@Cacheable` ile `@CachePut` farkı nedir?
**Cevap:**
*   **`@Cacheable`:** Metodu çalıştırmadan önce cache'e bakar. Varsa metot çalışmaz.
*   **`@CachePut`:** Metot her zaman çalışır, sonucu cache'e yazar (güncelleme için).

#### Soru 5: CDN (Content Delivery Network) nedir?
**Cevap:** Statik içerikleri (resim, CSS, JS) kullanıcıya en yakın sunucudan (edge server) sunar. HTTP caching kullanır.

**Örnek:** CloudFlare, AWS CloudFront, Akamai.

---

### 8. Geliştirici İpuçları

*   **Cache Key Design:** Key'leri anlamlı yapın: `user:123`, `product:456:reviews`. Namespace kullanın.

*   **TTL (Time-To-Live) Stratejisi:** Farklı veriler için farklı TTL kullanın:
    *   Statik veriler (ülke listesi): 24 saat
    *   Kullanıcı profili: 1 saat
    *   Ürün fiyatı: 5 dakika

*   **Cache Invalidation:** "There are only two hard things in Computer Science: cache invalidation and naming things." Cache'i doğru zamanda temizlemek zordur. Event-driven invalidation kullanın (Kafka, RabbitMQ).

*   **Redis Clustering:** Production'da tek Redis instance kullanmayın. Redis Cluster veya Redis Sentinel (High Availability) kullanın.

*   **Monitoring:** Cache hit/miss oranını izleyin. %80+ hit rate idealdir. Düşükse cache stratejinizi gözden geçirin.

*   **Serialization:** Redis'e Java nesnesi yazarken JSON serialization kullanın (Jackson). Daha esnek ve okunabilir.

Bu konu ile **18 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

