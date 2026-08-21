## Konu 21: Veritabanı Tasarımı, Normalizasyon, ACID, Failure Modes ve Performans Profiling

Veritabanı katmanı, büyük ölçekli sistemlerde performans, tutarlılık ve bakım açısından kritik bir rol oynar. Bir geliştirici olarak, **normalizasyon**, **ACID** prensipleri, **ORM** kullanımındaki tuzaklar (N+1, batch), **failure mode** senaryoları ve **profiling** tekniklerini iyi bilmelisiniz.

---

### 1. ACID Prensipleri (Atomicity, Consistency, Isolation, Durability)

| Prensip | Açıklama | Örnek Senaryo |
| :--- | :--- | :--- |
| **Atomicity** | İşlem ya tamamen başarılı olur ya da hiçbiri. | `INSERT` + `UPDATE` aynı transaction içinde; bir hata olduğunda tümü rollback edilir. |
| **Consistency** | İşlem sonunda veri bütünlüğü kuralları (FK, CHECK) korunur. | Bir sipariş oluşturulurken stok miktarı negatif olmamalı. |
| **Isolation** | Eşzamanlı işlemler birbirini etkilemez. | `READ COMMITTED`, `REPEATABLE READ`, `SERIALIZABLE` seviyeleri. |
| **Durability** | Commit sonrası veri kalıcıdır; çökme sonrası kaybolmaz. | WAL (Write-Ahead Log) sayesinde crash sonrası recovery. |

**Spring @Transactional** varsayılan olarak `Propagation.REQUIRED`, `Isolation.DEFAULT` (genellikle `READ_COMMITTED`). Gerekli olduğunda `@Transactional(isolation = Isolation.SERIALIZABLE)` gibi ayarlamalar yapılabilir.

---

### 2. Normalizasyon ve Denormalizasyon

#### 2.1 Normalizasyon (1NF, 2NF, 3NF, BCNF)

| Normal Form | Kural | Örnek (Sipariş‑Ürün) |
| :--- | :--- | :--- |
| **1NF** | Tekil hücre, tekrar eden grup olmamalı. | `order_items` tablosunda her satır tek bir ürün. |
| **2NF** | 1NF + tüm non‑key sütunlar tam fonksiyonel bağımlı. | `order_items.quantity` sadece `order_id`+`product_id` kombinasyonuna bağlı. |
| **3NF** | 2NF + transitif bağımlılık yok. | `product.price` ayrı `products` tablosunda, `order_items` sadece `product_id` tutar. |
| **BCNF** | Her determinant bir aday anahtar olmalı. | Çoklu aday anahtarlar varsa, her determinant anahtar olmalı. |

**Denormalization**: Performans için kontrollü olarak normalizasyon kurallarını gevşetmek. Örneğin, rapor amaçlı `order_total` alanını `orders` tablosuna eklemek, `JOIN` maliyetini azaltır. Ancak veri tutarlılığı için trigger veya uygulama düzeyinde güncelleme gerekir.

---

### 3. ORM (Hibernate / JPA) Yaygın Tuzakları ve Çözümleri

#### 3.1 N+1 Sorunu

**Senaryo:** `SELECT * FROM user` → ardından her kullanıcı için `SELECT * FROM orders WHERE user_id = ?`.

**Çözüm:** `JOIN FETCH`, `@EntityGraph`, `batch-size`.

```java
// JOIN FETCH örneği
@Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
User findByIdWithOrders(@Param("id") Long id);

// Batch fetching (hibernate.cfg.xml)
<property name="hibernate.default_batch_fetch_size">16</property>
```

#### 3.2 LazyInitializationException

**Neden:** Lazy ilişki, session kapandıktan sonra erişildi.

**Çözüm:**
*   `@Transactional` içinde erişim.
*   `EntityGraph`/`JOIN FETCH` ile eager fetch.
*   Open Session in View (OSIV) **kullanmayın** prod ortamda; performans ve security riski.

#### 3.3 Batch Inserts / Updates

Hibernate `jdbc.batch_size` ile toplu insert yapılabilir. Özellikle CSV import gibi büyük veri yüklemelerinde kritiktir.

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

#### 3.4 Flush & Clear

Büyük batch işleminde `entityManager.flush(); entityManager.clear();` ile 1. seviye cache temizlenir, memory overflow önlenir.

---

### 4. Failure Modes (Hata Senaryoları)

| Senaryo | Etki | Çözüm / Önlem |
| :--- | :--- | :--- |
| **Deadlock** | İki transaction birbirini bekler, timeout. | Transaction isolation `READ_COMMITTED` tercih edin, lock ordering tutarlı olsun, `@Transactional(timeout = 30)` ile timeout ayarlayın. |
| **Connection Pool Exhaustion** | Yeni istekler DB'ye bağlanamaz, 503. | HikariCP `maximumPoolSize`, `connectionTimeout` ayarları; `leakDetectionThreshold` ile sızıntı tespit. |
| **Long‑Running Queries** | CPU ve I/O baskısı, diğer istekleri yavaşlatır. | `EXPLAIN ANALYZE` ile plan incele, indeks ekle, pagination (`LIMIT/OFFSET`) kullan. |
| **Stale Data / Lost Updates** | İki transaction aynı satırı günceller, biri kaybolur. | Optimistic Locking (`@Version`), veya pessimistic lock (`SELECT ... FOR UPDATE`). |
| **Schema Migration Failures** | Migration sırasında veri kaybı. | Flyway/Liquibase ile version‑controlled migration, `dry‑run` ve `rollback` script'leri. |

---

### 5. Profiling & Performance Tuning

#### 5.1 Query Profiling (EXPLAIN, pgAdmin, MySQL EXPLAIN)
```sql
EXPLAIN ANALYZE SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id WHERE c.country = 'TR';
```
*   **Seq Scan** → indeks ekle.
*   **Index Scan** → doğru indeks kullanılıyor.

#### 5.2 Hibernate Statistics
```java
entityManagerFactory.unwrap(SessionFactory.class).getStatistics().setStatisticsEnabled(true);
Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
System.out.println("Entity fetch count: " + stats.getEntityFetchCount());
```
*   `entityFetchCount`, `queryExecutionCount`, `secondLevelCacheHitCount` gibi metrikler izlenir.

#### 5.3 JPA / Spring Data Pagination
```java
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Page<User> users = userRepository.findAll(pageable);
```
*   `OFFSET` büyük olduğunda performans düşer → **keyset pagination** (`WHERE id > :lastId`).

#### 5.4 Indexing Strategy
*   **Single‑column index**: Sık sorgulanan `WHERE` sütunları.
*   **Composite index**: `WHERE a = ? AND b = ?` gibi çoklu koşullar.
*   **Covering index**: `SELECT col1, col2 FROM tbl` → `INCLUDE (col1, col2)` (PostgreSQL `INCLUDE`).

#### 5.5 Monitoring Tools
*   **Spring Actuator + Micrometer** → `datasource.hikari.connections` ve `datasource.hikari.active` metrikleri.
*   **pg_stat_activity**, **MySQL Performance Schema**.
*   **Grafana dashboards** (query latency, cache hit ratio).

---

### 6. Kritik Mülakat Soruları 

#### Soru 1: ACID vs BASE – NoSQL sistemlerde BASE (Basically Available, Soft state, Eventual consistency) ne zaman tercih edilir?
**Cevap:** BASE, yüksek ölçeklenebilirlik ve availability gereken sistemlerde tercih edilir.
*   **Kullanım:** Sosyal medya feed'leri, öneri sistemleri, analytics. Örneğin, Twitter'da bir tweet'in beğeni sayısının 1-2 saniye gecikmeyle güncellenmesi kabul edilebilir.
*   **ACID ise:** Finansal işlemler, sipariş sistemleri gibi tutarlılık kritik olan yerlerde kullanılır.

#### Soru 2: N+1 Problem – Hibernate'da N+1 sorunu nasıl tespit eder ve iki farklı çözüm (JOIN FETCH vs EntityGraph) örnekleyiniz.
**Cevap:** N+1 problemi, bir entity listesi çekerken her bir entity için ayrı sorgu atılmasıdır.
*   **Tespit:** `hibernate.generate_statistics=true` ve loglarda `select` sayısına bakın.
*   **Çözüm 1 (JOIN FETCH):** 
    ```java
    @Query("SELECT u FROM User u JOIN FETCH u.orders")
    List<User> findAllWithOrders();
    ```
*   **Çözüm 2 (EntityGraph):**
    ```java
    @EntityGraph(attributePaths = {"orders"})
    List<User> findAll();
    ```

#### Soru 3: Deadlock – PostgreSQL'de deadlock nasıl görülür (`pg_locks`) ve uygulama seviyesinde nasıl önlenir?
**Cevap:** 
*   **Tespit:** `SELECT * FROM pg_locks;` ve `SELECT * FROM pg_stat_activity WHERE wait_event_type = 'Lock';`
*   **Uygulama Seviyesinde Önleme:**
    *   Her zaman **aynı sırada** lock alın (User önce, Order sonra).
    *   Lock sürelerini minimumda tutun.
    *   `@Transactional(timeout = 5)` ile timeout belirleyin.

#### Soru 4: Optimistic vs Pessimistic Locking – `@Version` ile optimistic lock nasıl çalışır, `SELECT … FOR UPDATE` ile pessimistic lock farkı nedir?
**Cevap:**
*   **Optimistic (`@Version`):** Conflict nadir olduğunda kullanılır. Entity'de `@Version Long version` alanı olur. Update sırasında version kontrolü yapılır. Conflict varsa `OptimisticLockException` fırlatılır.
*   **Pessimistic (`SELECT FOR UPDATE`):** Conflict sık olduğunda kullanılır. Satırı okurken kilitleme yapar, başkaları bekler. Deadlock riski vardır.

#### Soru 5: Denormalization Trade‑off – Rapor tablosu (materialized view) kullanmanın avantajları ve dezavantajları nelerdir?
**Cevap:**
*   **Avantajlar:** Karmaşık JOIN'li sorguları hızlandırır. Raporlama için önceden hesaplanmış veri.
*   **Dezavantajlar:** Veri tutarsızlığı riski (Eventual consistency). Ekstra depolama alanı. Refresh maliyeti (Scheduled job veya trigger ile güncelleme gerekir).

#### Soru 6 (Tricky): Soft Delete kullanırken Unique Constraint sorunu nasıl çözülür?
**Cevap:** Eğer `email` alanı unique ise ve bir kullanıcıyı soft delete (`deleted=true`) yaptıysanız, aynı email ile yeni kullanıcı ekleyemezsiniz (DB constraint hatası).
*   **Çözüm 1:** Unique index'e `deleted` kolonunu da ekleyin (Partial Index): `CREATE UNIQUE INDEX idx_email ON users (email) WHERE deleted = false;`
*   **Çözüm 2:** Silinen email'in sonuna timestamp ekleyin (`ali@example.com_deleted_123`).

#### Soru 7 (Tricky): UUID vs Long (Auto Increment) ID performans farkı nedir?
**Cevap:**
*   **Long:** Sıralıdır, B-Tree index'e ekleme çok hızlıdır (sayfa bölünmesi az olur).
*   **UUID:** Rastgeledir, index'e ekleme yaparken rastgele sayfalara yazar (Random I/O), index fragmentasyonu yaratır ve insert performansı düşer.
*   **Tavsiye:** TSID veya ULID gibi sıralı (time-sorted) UUID alternatifleri kullanın.

---

### 7. Geliştirici İpuçları

*   **Schema‑First vs Code‑First:** Büyük ekiplerde `Flyway`/`Liquibase` ile **schema‑first** yaklaşımı, migration tarihçesini tutar ve CI/CD entegrasyonu kolaydır.
*   **Index Maintenance:** `ANALYZE` ve `VACUUM` (PostgreSQL) periyodik çalıştırın; indeks fragmentasyonu performansı düşürür.
*   **Batch Size Tuning:** `hibernate.jdbc.batch_size` 20‑50 arası iyi bir başlangıç; aşırı büyük batch memory overflow yaratabilir.
*   **Connection Leak Detection:** HikariCP `leakDetectionThreshold=2000` ms ayarı, uzun süre açık kalan connection'ları loglar.
*   **Read‑Only Transactions:** Sadece okuma yapan servislerde `@Transactional(readOnly = true)` kullanın; Hibernate dirty‑checking devre dışı kalır.
*   **Profiling First:** Önce `EXPLAIN` ve `Hibernate Statistics` ile darboğazı bulun, ardından indeks/denormalization ekleyin – **ölç, sonra iyileştir** prensibi.

Bu ek bölüm ile **21 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

