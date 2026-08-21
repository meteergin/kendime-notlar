## Konu 22: Transaction Yönetimi, Entity İlişkileri, Lifecycle ve ORM Derinliği

Bu bölüm, **transaction yönetimi**, **entity ilişkileri**, **entity lifecycle** ve **ORM** (Hibernate/JPA) konularını senior seviyede ele alır. Mülakatlarda sık sorulan senaryolar ve performans ipuçları burada bulunur.

---

### 1. Transaction Yönetimi ve Propagation

| Özellik | Açıklama | Örnek Kullanım |
| :--- | :--- | :--- |
| **Propagation.REQUIRED** | Mevcut transaction varsa ona katılır, yoksa yeni oluşturur. | `@Transactional(propagation = Propagation.REQUIRED)` |
| **Propagation.REQUIRES_NEW** | Her zaman yeni transaction başlatır, mevcut transaction'ı askıya alır. | `@Transactional(propagation = Propagation.REQUIRES_NEW)` |
| **Propagation.NESTED** | Savepoint kullanarak iç içe transaction oluşturur (JDBC destekli). | `@Transactional(propagation = Propagation.NESTED)` |
| **Isolation Levels** | `READ_COMMITTED`, `REPEATABLE_READ`, `SERIALIZABLE`, `READ_UNCOMMITTED`. | `@Transactional(isolation = Isolation.SERIALIZABLE)` |
| **Rollback Rules** | `rollbackFor`, `noRollbackFor` ile istisna bazlı geri dönüş. | `@Transactional(rollbackFor = Exception.class)` |

**Pratik İpucu:** Mikroservislerde **saga** pattern'i ile distributed transaction yerine eventual consistency tercih edin.

---

### 2. Entity İlişkileri (Relationships)

| İlişki | JPA Anotasyonu | DB Katkısı | Tipik Kullanım |
| :--- | :--- | :--- | :--- |
| **One‑To‑One** | `@OneToOne` | UNIQUE FK | Profil‑Kullanıcı, Detay‑Sipariş |
| **One‑To‑Many** | `@OneToMany` + `@ManyToOne` | FK (child) | Kullanıcı‑Siparişler |
| **Many‑To‑Many** | `@ManyToMany` (join table) | Join Table | Öğrenci‑Kurs, Tag‑Post |
| **Bidirectional** | `mappedBy` ile sahte taraf | Tek FK | `User` ↔ `Profile` |

**Fetch Types:** `EAGER` (varsayılan for `@ManyToOne`), `LAZY` (default for collections). `EAGER` çoklu ilişkilerde **N+1** ve **Cartesian Product** riskine yol açar.

**Cascade Types:** `PERSIST`, `MERGE`, `REMOVE`, `REFRESH`, `DETACH`. Örneğin, `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` ile child entity'leri otomatik siler.

---

### 3. Entity Lifecycle (Durumlar)

| Durum | Açıklama | Tipik Event |
| :--- | :--- | :--- |
| **Transient** | Henüz `EntityManager`'a eklenmemiş. | `new User()` |
| **Persistent** | `EntityManager` içinde, DB ile senkron. | `entityManager.persist(user)` |
| **Detached** | Session kapandı, değişiklik otomatik yansıtılmaz. | `entityManager.detach(user)` |
| **Removed** | Silinmek üzere işaretli. | `entityManager.remove(user)` |

**Lifecycle Callbacks:** `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostLoad`, `@PreRemove` – audit, timestamp, soft‑delete gibi işlemler için kullanılır.

---

### 4. ORM Derinliği ve Performans İpuçları

1. **Batch Fetching** – `hibernate.default_batch_fetch_size` ile koleksiyonları toplu getir.
2. **Second‑Level Cache** – Ehcache/Redis ile sık okunan entity'leri cache'le. `@Cacheable` ve `@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)`.
3. **Stateless Session** – Büyük veri import'larında `StatelessSession` kullanarak 1. seviye cache'i devre dışı bırak.
4. **DTO Projection** – `SELECT new com.example.dto.UserDto(u.id, u.name) FROM User u` ile sadece gerekli alanları çek.
5. **Optimistic Locking** – `@Version` alanı ekleyerek concurrent update çakışmalarını önle.
6. **Pessimistic Locking** – `entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE)` ile satır‑kilidi al.
7. **Entity Graphs** – `EntityGraph<User> graph = entityManager.createEntityGraph(User.class); graph.addAttributeNodes("orders");` ile dinamik fetch planı.

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: Propagation.REQUIRES_NEW kullanmanın avantajları ve dezavantajları nelerdir?
**Cevap:**
*   **Avantajlar:** Ana transaction rollback olsa bile yeni transaction commit olabilir (örn: audit log kaydetmek).
*   **Dezavantajlar:** İki ayrı connection gerekir. Connection pool dolarsa deadlock riski. Transaction koordinasyonu zorlaşır.

#### Soru 2: Lazy vs Eager fetch stratejileri arasındaki trade‑off nedir? N+1 problemi nasıl tespit edilir?
**Cevap:**
*   **Lazy:** İhtiyaç duyulana kadar child entity'ler yüklenmez. Memory tasarrufu ama N+1 riski.
*   **Eager:** Her zaman child'ları yükler. N+1 olmaz ama gereksiz veri yükleme (Cartesian Product riski).
*   **Tespit:** Hibernate Statistics açın ve sorgu sayısını izleyin.

#### Soru 3: @Version ile optimistic locking nasıl çalışır? Bir conflict durumunda ne olur?
**Cevap:** Her update'te version artırılır. Eğer iki kullanıcı aynı entity'yi aynı anda güncellerse, ikinci update `OptimisticLockException` fırlatır.
*   **Çözüm:** Exception yakalayıp kullanıcıya "Veri başkası tarafından değiştirildi, lütfen tekrar deneyin" mesajı gösterin.

#### Soru 4: CascadeType.ALL ve orphanRemoval arasındaki fark nedir? Bir örnek senaryo veriniz.
**Cevap:**
*   **CascadeType.ALL:** Parent'a yapılan işlemler (persist, merge, remove) child'a da uygulanır.
*   **orphanRemoval=true:** Parent'tan çıkarılan (orphan) child otomatik silinir.
*   **Örnek:** `user.getOrders().remove(order);` → orphanRemoval=true ise order DB'den silinir, false ise sadece ilişki kopar.

#### Soru 5: StatelessSession ne zaman tercih edilir? Performans etkisi nasıldır?
**Cevap:** Büyük veri import/export işlemlerinde kullanılır.
*   **Avantaj:** 1st level cache ve dirty checking yok, çok hızlı.
*   **Dezavantaj:** Lazy loading çalışmaz, cascade ve versioning desteği yok.

#### Soru 6 (Tricky): `Propagation.REQUIRES_NEW` kullanmanın riski nedir?
**Cevap:** Deadlock ve Connection Pool Exhaustion riski vardır.
*   Ana transaction bir connection tutar ve beklemeye geçer. Yeni transaction için ikinci bir connection alınır. Eğer pool dolarsa, ana transaction beklerken yeni transaction connection alamaz ve sistem kilitlenir.

#### Soru 7 (Tricky): Transaction commit olduktan sonra bir işlem yapmak için ne kullanılır?
**Cevap:** `TransactionSynchronizationManager` kullanılır.
*   `afterCommit()` hook'u ile transaction başarılı olduktan sonra (örn: email göndermek, cache temizlemek) işlem yapılır. Transaction içinde email atarsanız ve transaction rollback olursa, email geri alınamaz!

---

### 6. Geliştirici İpuçları

* **Transaction Boundaries:** Service katmanında `@Transactional` tutun, repository katmanında **transaction** açmayın.
* **Read‑Only Transactions:** Sorgu‑ağırlıklı metodlarda `@Transactional(readOnly = true)` kullanarak Hibernate'in dirty‑checking maliyetini ortadan kaldırın.
* **Batch Size Tuning:** `hibernate.jdbc.batch_size` 20‑50 arası, `hibernate.order_inserts`/`order_updates` true yapın.
* **Fetch Plan Auditi:** `entityManagerFactory.getStatistics().logSummary()` ile fetch sayısını izleyin; yüksek `entityFetchCount` N+1 uyarısıdır.
* **Cache Invalidation:** 2nd‑level cache kullanıyorsanız, veri değiştiğinde `@CacheEvict` ile ilgili region'ı temizleyin.
* **Profiling First:** `EXPLAIN ANALYZE` ve Hibernate `Statistics` ile darboğazı bulun, ardından fetch/locking/batch ayarlarını iyileştirin.


---

