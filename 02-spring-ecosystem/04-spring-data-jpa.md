## Konu 16: Spring Data (JPA, MongoDB, JDBC)

Spring Data, farklı veri kaynaklarına (SQL, NoSQL) erişimi standartlaştıran ve basitleştiren bir projedir. Bir geliştirici olarak, Spring Data'nın sağladığı soyutlama katmanını etkili kullanmalı, Query Methods, Specifications ve Projections gibi ileri düzey özelliklere hakim olmalısınız.

---

### 1. Spring Data Nedir?

**Analoji:** Spring Data, farklı veritabanlarına erişmek için "evrensel bir uzaktan kumanda" gibidir. İster SQL (JPA), ister NoSQL (MongoDB, Redis) olsun, aynı programlama modeli ile çalışırsınız.

#### Spring Data Modülleri

| Modül | Veritabanı | Kullanım |
| :--- | :--- | :--- |
| **Spring Data JPA** | İlişkisel DB (PostgreSQL, MySQL) | Hibernate üzerine kurulu, ORM |
| **Spring Data MongoDB** | MongoDB (NoSQL) | Document-based database |
| **Spring Data Redis** | Redis | Key-Value store, Caching |
| **Spring Data JDBC** | İlişkisel DB | Basit, Hibernate olmadan |
| **Spring Data Elasticsearch** | Elasticsearch | Full-text search |
| **Spring Data Cassandra** | Cassandra | Wide-column store |

---

### 2. Spring Data JPA

Spring Data JPA, JPA/Hibernate üzerine kurulu bir soyutlama katmanıdır. Repository pattern'i kullanarak CRUD işlemlerini otomatize eder.

#### Repository Hiyerarşisi

```
Repository (Marker Interface)
    ↓
CrudRepository (CRUD metodları: save, findById, delete)
    ↓
PagingAndSortingRepository (Sayfalama ve sıralama)
    ↓
JpaRepository (JPA özel metodlar: flush, saveAndFlush)
```

**Kullanım:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Hiçbir kod yazmadan CRUD işlemleri hazır!
}
```

#### Query Methods (Metot İsimlendirme ile Sorgu)

Spring Data, metot isminden otomatik sorgu oluşturur:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // SELECT * FROM user WHERE name = ?
    List<User> findByName(String name);
    
    // SELECT * FROM user WHERE email = ?
    Optional<User> findByEmail(String email);
    
    // SELECT * FROM user WHERE age > ?
    List<User> findByAgeGreaterThan(int age);
    
    // SELECT * FROM user WHERE name = ? AND age = ?
    List<User> findByNameAndAge(String name, int age);
    
    // SELECT * FROM user WHERE name LIKE ?
    List<User> findByNameContaining(String keyword);
    
    // SELECT * FROM user WHERE created_at BETWEEN ? AND ?
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // SELECT * FROM user ORDER BY name ASC
    List<User> findByAgeGreaterThanOrderByNameAsc(int age);
}
```

**Keyword Tablosu:**

| Keyword | Örnek | JPQL |
| :--- | :--- | :--- |
| `And` | `findByNameAndAge` | `WHERE name = ? AND age = ?` |
| `Or` | `findByNameOrEmail` | `WHERE name = ? OR email = ?` |
| `Between` | `findByAgeBetween` | `WHERE age BETWEEN ? AND ?` |
| `LessThan` | `findByAgeLessThan` | `WHERE age < ?` |
| `GreaterThan` | `findByAgeGreaterThan` | `WHERE age > ?` |
| `Like` | `findByNameLike` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining` | `WHERE name LIKE %?%` |
| `StartingWith` | `findByNameStartingWith` | `WHERE name LIKE ?%` |
| `OrderBy` | `findByAgeOrderByNameAsc` | `ORDER BY name ASC` |

#### Custom Queries (@Query)

Karmaşık sorgular için JPQL veya Native SQL kullanın:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // JPQL
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailCustom(@Param("email") String email);
    
    // Native SQL
    @Query(value = "SELECT * FROM users WHERE age > :age", nativeQuery = true)
    List<User> findUsersOlderThan(@Param("age") int age);
    
    // DTO Projection
    @Query("SELECT new com.example.UserDTO(u.id, u.name, u.email) FROM User u")
    List<UserDTO> findAllUserDTOs();
    
    // Modifying Query (UPDATE/DELETE)
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.active = false WHERE u.lastLogin < :date")
    int deactivateInactiveUsers(@Param("date") LocalDateTime date);
}
```

#### Specifications (Dinamik Sorgular)

Criteria API kullanarak runtime'da dinamik sorgular oluşturun:

```java
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
}

// Specification oluşturma
public class UserSpecifications {
    
    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> cb.equal(root.get("name"), name);
    }
    
    public static Specification<User> ageGreaterThan(int age) {
        return (root, query, cb) -> cb.greaterThan(root.get("age"), age);
    }
}

// Kullanım
Specification<User> spec = Specification
    .where(UserSpecifications.hasName("Ali"))
    .and(UserSpecifications.ageGreaterThan(18));

List<User> users = userRepository.findAll(spec);
```

---

### 3. Spring Data MongoDB

MongoDB, document-based NoSQL veritabanıdır. Spring Data MongoDB, JPA'ya benzer bir API sunar.

#### Temel Kullanım

```java
@Document(collection = "users") // MongoDB collection
public class User {
    @Id
    private String id; // MongoDB ObjectId
    private String name;
    private String email;
    private List<String> roles;
}

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByName(String name);
    List<User> findByRolesContaining(String role);
}
```

#### MongoTemplate (Programmatic Queries)

Daha karmaşık sorgular için `MongoTemplate` kullanın:

```java
@Service
public class UserService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    public List<User> findActiveUsers() {
        Query query = new Query();
        query.addCriteria(Criteria.where("active").is(true));
        query.addCriteria(Criteria.where("age").gte(18));
        return mongoTemplate.find(query, User.class);
    }
    
    public void updateUserEmail(String userId, String newEmail) {
        Query query = new Query(Criteria.where("id").is(userId));
        Update update = new Update().set("email", newEmail);
        mongoTemplate.updateFirst(query, update, User.class);
    }
}
```

#### Aggregation Pipeline

MongoDB'nin güçlü aggregation özelliklerini kullanın:

```java
public List<UserStatistics> getUserStatistics() {
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("active").is(true)),
        Aggregation.group("department")
            .count().as("userCount")
            .avg("age").as("avgAge"),
        Aggregation.sort(Sort.Direction.DESC, "userCount")
    );
    
    return mongoTemplate.aggregate(aggregation, "users", UserStatistics.class).getMappedResults();
}
```

---

### 4. Spring Data JDBC

Spring Data JDBC, Hibernate olmadan, daha basit ve performanslı bir alternatiftir. Lazy loading, dirty checking gibi "sihirli" özellikler yoktur.

#### Temel Farklar (JPA vs JDBC)

| Özellik | Spring Data JPA | Spring Data JDBC |
| :--- | :--- | :--- |
| **ORM** | Evet (Hibernate) | Hayır (Basit mapping) |
| **Lazy Loading** | Evet | Hayır |
| **Dirty Checking** | Evet | Hayır |
| **Caching** | Evet (1st/2nd level) | Hayır |
| **Performans** | Orta (Overhead var) | Yüksek (Minimal overhead) |
| **Karmaşıklık** | Yüksek | Düşük |

**Kullanım:**
```java
@Table("users")
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
}

public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT * FROM users WHERE email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
```

**Ne Zaman Kullanılmalı?**
*   Basit CRUD işlemleri.
*   Performans kritik (Hibernate overhead'i istemiyorsanız).
*   Karmaşık ilişkiler yoksa.

---

### 5. Pagination ve Sorting

Büyük veri setlerini sayfalamak için:

```java
public interface UserRepository extends JpaRepository<User, Long> {
}

// Controller
@GetMapping("/users")
public Page<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
    return userRepository.findAll(pageable);
}

// Response
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

---

### 6. Kritik Mülakat Soruları 

#### Soru 1: `JpaRepository` ile `CrudRepository` farkı nedir?
**Cevap:**
*   **`CrudRepository`:** Temel CRUD metodları (`save`, `findById`, `delete`).
*   **`JpaRepository`:** `CrudRepository`'yi extend eder + JPA özel metodlar (`flush`, `saveAndFlush`, `deleteInBatch`).

**Tavsiye:** Genellikle `JpaRepository` kullanın (daha fazla özellik).

#### Soru 2: Query Methods'da metot ismi çok uzun olursa ne yapılmalı?
**Cevap:** `@Query` anotasyonu kullanın veya Specifications ile dinamik sorgu oluşturun.

```java
// Kötü
List<User> findByNameAndEmailAndAgeGreaterThanAndActiveTrueOrderByCreatedAtDesc(...);
```

#### Soru 3 (Tricky): `save()` metodu her zaman `INSERT` mü yapar?
**Cevap:** Hayır.
*   Eğer entity yeni ise (ID null) -> `persist` (INSERT).
*   Eğer entity ID'si varsa -> `merge` (SELECT + UPDATE/INSERT).
*   **Trap:** Performans için `Persistable` interface'ini implement edip `isNew()` metodunu optimize etmek gerekebilir (gereksiz SELECT'ten kaçınmak için).

#### Soru 4 (Tricky): `getOne()` (veya `getReferenceById()`) ile `findById()` farkı nedir?
**Cevap:**
*   **`findById()`:** DB'ye gider ve gerçek entity'yi (Optional) döner.
*   **`getOne()` / `getReferenceById()`:** DB'ye gitmez, sadece ID'si dolu bir **Proxy** döner (Lazy).
*   **Kullanım:** Sadece ilişki kurmak için (foreign key set etmek için) entity'ye ihtiyacınız varsa `getReferenceById` kullanın, boşuna SELECT atmaz.

---
// İyi
@Query("SELECT u FROM User u WHERE u.name = :name AND u.email = :email AND u.age > :age AND u.active = true ORDER BY u.createdAt DESC")
List<User> findActiveUsers(@Param("name") String name, @Param("email") String email, @Param("age") int age);
```

#### Soru 3: Spring Data JPA'da `@Modifying` ne zaman kullanılır?
**Cevap:** `UPDATE` veya `DELETE` sorguları için kullanılır. Ayrıca `@Transactional` ile birlikte kullanılmalıdır.

```java
@Modifying
@Transactional
@Query("DELETE FROM User u WHERE u.active = false")
int deleteInactiveUsers();
```

**Dikkat:** `@Modifying` kullanıldığında, Hibernate cache'i otomatik temizlenmez. `clearAutomatically = true` ekleyin:
```java
@Modifying(clearAutomatically = true)
```

#### Soru 4: MongoDB'de `@DBRef` ne işe yarar?
**Cevap:** İlişkisel veritabanlarındaki foreign key gibi, başka bir document'e referans verir.

```java
@Document
public class Order {
    @Id
    private String id;
    
    @DBRef
    private User user; // User document'ine referans
}
```

**Dikkat:** `@DBRef` kullanımı N+1 sorunu yaratabilir. Embedded document kullanmayı düşünün.

#### Soru 5: Spring Data'da Auditing nasıl yapılır?
**Cevap:** `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` anotasyonları ile otomatik audit alanları oluşturulur.

```java
@EntityListeners(AuditingEntityListener.class)
@Entity
public class User {
    @Id
    private Long id;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
}

// Configuration
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
```

---

### 7. Geliştirici İpuçları

*   **Repository Naming:** Repository interface'lerini domain entity'nin adıyla isimlendirin: `UserRepository`, `OrderRepository`. `UserDao` gibi eski isimlendirmelerden kaçının.

*   **Custom Repository Implementation:** Karmaşık iş mantığı için custom repository implementasyonu oluşturun:
    ```java
    public interface UserRepositoryCustom {
        List<User> complexQuery();
    }
    
    public class UserRepositoryImpl implements UserRepositoryCustom {
        @PersistenceContext
        private EntityManager entityManager;
        
        @Override
        public List<User> complexQuery() {
            // Karmaşık Criteria API veya Native SQL
        }
    }
    
    public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    }
    ```

*   **Projection Interfaces:** Tüm entity yerine sadece ihtiyacınız olan alanları çekin:
    ```java
    public interface UserNameOnly {
        String getName();
        String getEmail();
    }
    
    List<UserNameOnly> findByAgeGreaterThan(int age);
    ```

*   **MongoDB Index:** Sık sorgulanan alanlara index ekleyin:
    ```java
    @Document
    @CompoundIndex(name = "email_idx", def = "{'email': 1}", unique = true)
    public class User {
        @Indexed
        private String name;
    }
    ```

*   **Batch Operations:** Çok sayıda entity kaydederken `saveAll()` kullanın (tek tek `save()` yerine). Hibernate batch insert devreye girer.

Bu konu ile **16 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

