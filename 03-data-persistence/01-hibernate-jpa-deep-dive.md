## Konu 15: Hibernate, JPA Transactions, Entity Relationships ve Lifecycle

Hibernate, Java'nın en yaygın kullanılan ORM (Object-Relational Mapping) framework'üdür. Bir geliştirici olarak, N+1 sorunu, LazyInitializationException, Dirty Checking, Entity Lifecycle ve Transaction yönetimi gibi ileri düzey konulara hakim olmalısınız.

---

### 1. Hibernate vs JPA

**Analoji:** JPA bir "arayüz (interface)", Hibernate ise bu arayüzün bir "implementasyonu"dur. JPA standardı tanımlar, Hibernate bu standardı uygular (ve ekstra özellikler sunar).

| Kavram | Açıklama |
| :--- | :--- |
| **JPA (Java Persistence API)** | Java EE standardı. Sadece spesifikasyon (interface'ler ve anotasyonlar). |
| **Hibernate** | JPA'nın en popüler implementasyonudur. JPA'dan daha fazla özellik sunar (Criteria API, Caching). |

**Neden JPA Kullanmalı?**
*   Vendor-agnostic (Hibernate'ten EclipseLink'e geçiş kolay).
*   Standart API (Dokümantasyon ve topluluk desteği geniş).

---

### 2. Entity Lifecycle (Yaşam Döngüsü)

Bir Entity, 4 farklı durumda (state) olabilir:

| State | Açıklama | Özellikleri |
| :--- | :--- | :--- |
| **Transient** | Yeni oluşturulmuş, Hibernate bilmiyor | `new User()` - Henüz persist edilmemiş |
| **Persistent (Managed)** | Hibernate Session/EntityManager tarafından yönetiliyor | Değişiklikler otomatik DB'ye yansır (Dirty Checking) |
| **Detached** | Hibernate'ten ayrılmış, ama DB'de var | Session kapandıktan sonra. Değişiklikler DB'ye yansımaz. |
| **Removed** | Silinmek üzere işaretlenmiş | `remove()` çağrıldı, transaction commit'te silinecek |

```java
User user = new User("Ali"); // TRANSIENT

entityManager.persist(user); // PERSISTENT (Managed)
user.setName("Ece"); // Dirty Checking: DB'ye otomatik yansır

entityManager.detach(user); // DETACHED
user.setName("Ahmet"); // DB'ye YANSIMAZ

entityManager.merge(user); // Tekrar PERSISTENT
entityManager.remove(user); // REMOVED
```

---

### 3. Entity Relationships (İlişkiler)

#### 1. One-to-One (Bire-Bir)

Bir kullanıcının bir profili vardır.

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private Profile profile;
}

@Entity
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(mappedBy = "profile") // Bidirectional
    private User user;
}
```

#### 2. One-to-Many / Many-to-One (Bire-Çok / Çoka-Bir)

Bir departmanın birden fazla çalışanı vardır.

```java
@Entity
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();
}

@Entity
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}
```

**Önemli:** `@OneToMany` tarafında `mappedBy` kullanın (ilişkinin sahibi `@ManyToOne` tarafıdır).

#### 3. Many-to-Many (Çoka-Çok)

Bir öğrenci birden fazla derse kayıtlı, bir derste birden fazla öğrenci vardır.

```java
@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

---

### 4. Fetch Strategies (Yükleme Stratejileri)

#### Eager vs Lazy Loading

| Fetch Type | Açıklama | Kullanım |
| :--- | :--- | :--- |
| **EAGER** | İlişkili entity'ler anında yüklenir | `@ManyToOne`, `@OneToOne` (varsayılan) |
| **LAZY** | İlişkili entity'ler ihtiyaç duyulduğunda yüklenir | `@OneToMany`, `@ManyToMany` (varsayılan) |

**Önemli:** Lazy loading, Session/EntityManager açıkken çalışır. Session kapandıktan sonra lazy field'a erişirseniz `LazyInitializationException` alırsınız.

**Çözümler:**
1.  **Open Session in View (OSIV):** Spring Boot varsayılan olarak aktiftir. HTTP isteği boyunca session açık kalır (Performans sorunu yaratabilir).
2.  **JOIN FETCH:** JPQL ile ilişkili entity'leri tek sorguda çekin:
    ```java
    @Query("SELECT u FROM User u JOIN FETCH u.profile WHERE u.id = :id")
    User findByIdWithProfile(@Param("id") Long id);
    ```
3.  **Entity Graph:** JPA 2.1+ ile dinamik fetch stratejisi:
    ```java
    @EntityGraph(attributePaths = {"profile", "orders"})
    User findById(Long id);
    ```

---

### 5. N+1 Problem

**Sorun:** Bir sorgu ile N tane entity çekiyorsunuz, sonra her biri için ayrı sorgu atılıyor (1 + N sorgu).

```java
List<User> users = userRepository.findAll(); // 1 sorgu
for (User user : users) {
    user.getOrders().size(); // Her user için 1 sorgu (N sorgu)
}
// Toplam: 1 + N sorgu
```

**Çözümler:**
1.  **JOIN FETCH:**
    ```java
    @Query("SELECT u FROM User u JOIN FETCH u.orders")
    List<User> findAllWithOrders();
    ```
2.  **@EntityGraph:**
    ```java
    @EntityGraph(attributePaths = "orders")
    List<User> findAll();
    ```
3.  **Batch Fetching:**
    ```properties
    spring.jpa.properties.hibernate.default_batch_fetch_size=10
    ```

---

### 6. Transactions (İşlemler)

#### ACID Prensipleri

| Prensip | Açıklama |
| :--- | :--- |
| **Atomicity** | Tüm işlemler başarılı olur veya hiçbiri olmaz (All or Nothing) |
| **Consistency** | Veri tutarlılığı korunur (Constraints ihlal edilmez) |
| **Isolation** | Eşzamanlı işlemler birbirini etkilemez |
| **Durability** | Commit edilen değişiklikler kalıcıdır (Sistem çökse bile) |

#### Spring `@Transactional`

```java
@Service
public class UserService {
    
    @Transactional // Varsayılan: Propagation.REQUIRED, Isolation.DEFAULT
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        User from = userRepository.findById(fromId).orElseThrow();
        User to = userRepository.findById(toId).orElseThrow();
        
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        
        // Exception fırlatılırsa ROLLBACK
        // Başarılıysa COMMIT
    }
}
```

#### Transaction Propagation

| Propagation | Açıklama |
| :--- | :--- |
| **REQUIRED** (Default) | Mevcut transaction varsa kullan, yoksa yeni oluştur |
| **REQUIRES_NEW** | Her zaman yeni transaction oluştur (mevcut suspend edilir) |
| **NESTED** | İç içe transaction (savepoint kullanır) |
| **MANDATORY** | Transaction olmalı, yoksa exception |
| **SUPPORTS** | Transaction varsa kullan, yoksa transaction'sız çalış |
| **NOT_SUPPORTED** | Transaction'ı suspend et, transaction'sız çalış |
| **NEVER** | Transaction olmamalı, varsa exception |

#### Transaction Isolation Levels

| Isolation | Sorun | Açıklama |
| :--- | :--- | :--- |
| **READ_UNCOMMITTED** | Dirty Read, Non-repeatable Read, Phantom Read | En düşük izolasyon |
| **READ_COMMITTED** (Default) | Non-repeatable Read, Phantom Read | Commit edilmiş veriyi okur |
| **REPEATABLE_READ** | Phantom Read | Aynı sorgu aynı sonucu verir |
| **SERIALIZABLE** | Yok | En yüksek izolasyon (Performans düşer) |

---

### 7. Kritik Mülakat Soruları 

#### Soru 1: `persist()` ile `merge()` farkı nedir?
**Cevap:**
*   **`persist()`:** Transient entity'yi Persistent yapar. Entity yeni olmalıdır (ID yoksa).
*   **`merge()`:** Detached entity'yi Persistent yapar. Entity'nin bir kopyasını döner (orijinal detached kalır).

```java
User user = new User("Ali");
entityManager.persist(user); // user artık Persistent

entityManager.detach(user);
user.setName("Ece");
User merged = entityManager.merge(user); // merged Persistent, user hala Detached
```

#### Soru 2: Cascade Types nelerdir?
**Cevap:**
*   **`CascadeType.PERSIST`:** Parent persist edilince child da persist edilir.
*   **`CascadeType.MERGE`:** Parent merge edilince child da merge edilir.
*   **`CascadeType.REMOVE`:** Parent silinince child da silinir.
*   **`CascadeType.REFRESH`:** Parent refresh edilince child da refresh edilir.
*   **`CascadeType.DETACH`:** Parent detach edilince child da detach edilir.
*   **`CascadeType.ALL`:** Tüm cascade işlemleri.

**Dikkat:** `CascadeType.REMOVE` ile `orphanRemoval=true` farklıdır. `orphanRemoval`, parent'tan çıkarılan child'ı siler (parent silinmese bile).

#### Soru 3: `@Transactional` anotasyonu hangi durumlarda rollback yapar?
**Cevap:** Varsayılan olarak sadece **Unchecked Exception** (RuntimeException ve alt sınıfları) fırlatıldığında rollback yapar.
*   **Checked Exception** (IOException vb.) rollback yapmaz.
*   **Özelleştirme:**
    ```java
    @Transactional(rollbackFor = Exception.class) // Tüm exception'larda rollback
    @Transactional(noRollbackFor = CustomException.class) // Bu exception'da rollback yapma
    ```

#### Soru 4: Hibernate'de First Level Cache ve Second Level Cache nedir?
**Cevap:**
*   **First Level Cache (Session Cache):** Her Session/EntityManager'ın kendi cache'i vardır. Aynı session içinde aynı entity tekrar sorgulanırsa DB'ye gitmez, cache'ten gelir. Varsayılan olarak aktiftir, kapatılamaz.
*   **Second Level Cache:** Tüm session'lar arasında paylaşılan cache. Ehcache, Hazelcast gibi provider'lar kullanılır. Manuel olarak aktif edilmelidir.

#### Soru 5: `@Version` anotasyonu ne işe yarar?
**Cevap:** **Optimistic Locking** için kullanılır. Eşzamanlı güncellemelerde veri tutarlılığını sağlar.

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    @Version
    private Long version; // Her update'te otomatik artar
    
    private BigDecimal price;
}
```

**Senaryo:** İki kullanıcı aynı ürünü aynı anda güncelliyor.
1.  User A ve User B ürünü okur (version=1).
3.  User B güncellemeye çalışır → `OptimisticLockException` (version hala 1, ama DB'de 2).

#### Soru 6 (Tricky): N+1 Problemi nasıl tespit edilir?
**Cevap:**
*   **Hibernate Statistics:** `spring.jpa.properties.hibernate.generate_statistics=true` ile loglarda sorgu sayılarını izleyin.
*   **SQL Logları:** `spring.jpa.show-sql=true` ile konsolda ardışık `SELECT` sorgularını gözlemleyin.
*   **Araçlar:** P6Spy veya datasource-proxy gibi araçlar kullanın.

#### Soru 7 (Tricky): `FetchType.LAZY` nasıl çalışır?
**Cevap:** Hibernate, gerçek nesne yerine bir **Proxy** nesnesi döner. Bu proxy'nin bir metoduna (ID hariç) erişildiğinde gerçek SQL sorgusu atılır.
*   **Trap:** Eğer `toString()` metodunu override edip lazy field'ları yazdırırsanız, loglama sırasında bile DB sorgusu atılır (veya LazyInitException alırsınız).

---

### 8. Geliştirici İpuçları

*   **Bidirectional İlişkilerde Helper Metodlar:** İlişkinin her iki tarafını da güncelleyen helper metodlar yazın:
    ```java
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this); // Bidirectional sync
    }
    ```

*   **`equals()` ve `hashCode()`:** Entity sınıflarında business key (örn: email) üzerinden implement edin. ID kullanmayın (transient entity'lerde ID null olabilir).

*   **DTO Projection:** Tüm entity'yi çekmek yerine, sadece ihtiyacınız olan alanları DTO ile çekin:
    ```java
    @Query("SELECT new com.example.UserDTO(u.id, u.name) FROM User u")
    List<UserDTO> findAllUserDTOs();
    ```

*   **Batch Insert/Update:** Çok sayıda entity eklerken batch işlem kullanın:
    ```properties
    spring.jpa.properties.hibernate.jdbc.batch_size=50
    spring.jpa.properties.hibernate.order_inserts=true
    ```

*   **Read-Only Transactions:** Sadece okuma yapıyorsanız `@Transactional(readOnly = true)` kullanın. Dirty checking devre dışı kalır, performans artar.

Bu konu ile **15 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

