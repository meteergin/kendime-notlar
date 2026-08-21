# NoSQL Veritabanları (MongoDB, Redis, Cassandra, Elasticsearch)

> **Analoji:** RDBMS (SQL) bir "kütüphane" gibidir — her kitap standart bir formatta (tablo), rafta belirli bir yerde (şema), kataloğa (index) kayıtlı. NoSQL ise bir "depo" gibidir — kutuların boyutu, şekli, içeriği farklı olabilir. Esneklik kazanır ama düzen sizin sorumluluğunuzdur.

---

## 1. SQL vs NoSQL — Ne Zaman Hangisi?

| Kriter | SQL (RDBMS) | NoSQL |
| :--- | :--- | :--- |
| **Veri Yapısı** | Tablo, satır, sütun (rigid schema) | Belge, key-value, column, graph (flexible) |
| **İlişkiler** | JOIN ile güçlü ilişki | Denormalizasyon, embed, reference |
| **Ölçeklenme** | Dikey (Scale Up) | Yatay (Scale Out, sharding) |
| **ACID** | Tam destek | Bazıları kısmi destek (eventual consistency) |
| **Sorgu Dili** | SQL (standart) | Her DB'nin kendi API/sorgu dili |
| **Uygun Senaryo** | Finans, ERP, ilişkisel veri | IoT, sosyal medya, real-time, log, cache |

---

## 2. MongoDB (Document Database)

### Yapı

```
RDBMS Karşılığı:
  Database  → Database
  Table     → Collection
  Row       → Document (JSON/BSON)
  Column    → Field
  JOIN      → Embedding veya $lookup
```

### CRUD Operasyonları

```javascript
// Ekleme
db.users.insertOne({
  name: "Mete",
  email: "mete@example.com",
  address: {
    city: "İstanbul",
    district: "Kadıköy"
  },
  skills: ["Java", "Spring", "MongoDB"]
});

// Sorgulama
db.users.find({ "address.city": "İstanbul" })
        .sort({ name: 1 })
        .limit(10);

// Güncelleme
db.users.updateOne(
  { email: "mete@example.com" },
  { $push: { skills: "Kubernetes" } }
);

// Aggregation Pipeline (SQL GROUP BY benzeri)
db.orders.aggregate([
  { $match: { status: "DELIVERED" } },
  { $group: { _id: "$customerId", total: { $sum: "$amount" } } },
  { $sort: { total: -1 } },
  { $limit: 10 }
]);
```

### Spring Data MongoDB

```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    private Address address;  // Embedded document
    private List<String> skills;
}

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByAddressCity(String city);
    
    @Query("{ 'skills': { $in: ?0 } }")
    List<User> findBySkills(List<String> skills);
}
```

### Embedding vs Referencing

| Strateji | Ne Zaman | Örnek |
| :--- | :--- | :--- |
| **Embedding** | 1:1 veya 1:Few ilişki, birlikte okunur | User → Address |
| **Referencing** | 1:Many veya Many:Many, bağımsız güncellenebilir | User → Orders (referans) |

---

## 3. Redis (In-Memory Key-Value Store)

### Kullanım Senaryoları

| Senaryo | Veri Yapısı | Örnek |
| :--- | :--- | :--- |
| **Cache** | String | Session, API response cache |
| **Session Store** | Hash | Kullanıcı oturumu |
| **Rate Limiting** | String + TTL | API istek limiti |
| **Leaderboard** | Sorted Set | Oyun sıralaması |
| **Pub/Sub** | Channels | Real-time bildirimler |
| **Queue** | List | İş kuyruğu |
| **Distributed Lock** | String + NX | Kritik bölge kilitleme |

### Spring Boot ile Redis

```java
// Cache olarak
@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }
}

@Service
public class ProductService {
    @Cacheable(value = "products", key = "#id")
    public Product findById(String id) {
        return productRepository.findById(id).orElseThrow();
    }

    @CacheEvict(value = "products", key = "#id")
    public void update(String id, Product product) {
        productRepository.save(product);
    }
}

// Distributed Lock
@Service
public class OrderService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean processOrder(String orderId) {
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent("lock:order:" + orderId, "1", Duration.ofSeconds(30));
        
        if (Boolean.TRUE.equals(locked)) {
            try {
                // Kritik bölge
                return doProcess(orderId);
            } finally {
                redisTemplate.delete("lock:order:" + orderId);
            }
        }
        return false; // Zaten işleniyor
    }
}
```

---

## 4. Cassandra (Wide-Column Store)

### Ne Zaman Cassandra?

- **Yazma ağırlıklı** iş yükleri (IoT, log, zaman serisi)
- **Yüksek erişilebilirlik** (AP sistemi, tek node çökse bile çalışır)
- **Coğrafi dağıtım** (Multi-datacenter replication)

### Veri Modelleme Prensibi

> **Cassandra Kuralı:** Sorgunuzu biliyorsanız tablonuzu tasarlayın. "Query-first design."

```cql
-- Her sorgu için ayrı tablo (denormalizasyon)
CREATE TABLE orders_by_customer (
    customer_id UUID,
    order_date TIMESTAMP,
    order_id UUID,
    total DECIMAL,
    PRIMARY KEY (customer_id, order_date)
) WITH CLUSTERING ORDER BY (order_date DESC);

-- Partition Key: customer_id (veriyi node'lara dağıtır)
-- Clustering Key: order_date (sıralama)
```

---

## 5. Elasticsearch (Search Engine)

### Ne Zaman Elasticsearch?

- Full-text search (ürün arama, log arama)
- Log analizi (ELK Stack: Elasticsearch + Logstash + Kibana)
- Faceted search (e-ticaret filtreleri: renk, beden, fiyat)
- Auto-complete / suggestion

### Spring Data Elasticsearch

```java
@Document(indexName = "products")
public class Product {
    @Id
    private String id;
    @Field(type = FieldType.Text, analyzer = "turkish")
    private String name;
    @Field(type = FieldType.Keyword)
    private String category;
    @Field(type = FieldType.Double)
    private double price;
}

public interface ProductSearchRepository extends ElasticsearchRepository<Product, String> {
    List<Product> findByNameContaining(String keyword);
}
```

---

## 6. Kritik Mülakat Soruları

### Soru 1: MongoDB'de JOIN yapmak gerekirse ne yaparsınız?
**Cevap:**
1. **Embedding:** İlişkili veriyi belge içine gömün (tercih edilen)
2. **`$lookup`:** Aggregation pipeline'da LEFT JOIN benzeri (performans düşüktür)
3. **Application-level join:** Uygulama kodunda iki sorgu yapıp birleştirme

### Soru 2: Redis'te veri kaybı riski var mıdır?
**Cevap:** Evet! Redis in-memory'dir. Çözümler:
- **RDB Snapshot:** Belirli aralıklarla disk'e yazma (veri kaybı riski var)
- **AOF (Append-Only File):** Her yazma loglanır (daha güvenli, daha yavaş)
- **Redis Cluster:** Replication ile yedeklilik

### Soru 3 (Tricky): Cassandra'da secondary index neden tehlikelidir?
**Cevap:** Secondary index, tüm node'lara scatter-gather sorgusu yapar. Partition key olmadan yapılan sorgular **tüm cluster'ı** tarar. Performans çöker. Çözüm: Sorgunuza uygun denormalize tablo oluşturun.

### Soru 4 (Tricky): Polyglot Persistence nedir?
**Cevap:** Farklı iş ihtiyaçlarına farklı veritabanları kullanmak:
- **PostgreSQL:** Sipariş yönetimi (ACID)
- **MongoDB:** Ürün kataloğu (esnek şema)
- **Redis:** Session ve cache
- **Elasticsearch:** Ürün arama
- **Kafka:** Event streaming

---

## 7. Geliştirici İpuçları

- **Schema Evolution:** MongoDB'de şema değişikliğinde `@Version` field ekleyin. Eski dokümanları lazy migration ile güncelleyin.
- **Redis Memory:** `maxmemory-policy allkeys-lru` ile bellekte yer açmak için en az kullanılan anahtarları silin.
- **Cassandra Tombstones:** `DELETE` çok tehlikeli. Silinen veri yerine tombstone marker yazılır. Çok fazla birikirse read performansı çöker.
- **Elasticsearch Mapping:** Dynamic mapping kapatın. Field tiplerini explicit tanımlayın. Yoksa string alanlar hem `text` hem `keyword` olarak indexlenir (disk israfı).
