# Enterprise Patterns (Repository, CQRS, Specification, Unit of Work)

> **Analoji:** Enterprise Patterns, bir "şirketin departman yapısı" gibidir. Her departmanın (pattern) net bir sorumluluğu vardır — muhasebe (Repository), strateji (CQRS), kalite kontrol (Specification). Bu yapı büyüdükçe düzeni korur.

---

## 1. Repository Pattern

Veri erişim katmanını iş mantığından ayırır. Koleksiyon benzeri bir arayüz sunar.

```java
// Domain-driven Repository Interface
public interface OrderRepository {
    Order findById(UUID id);
    List<Order> findByCustomer(UUID customerId);
    void save(Order order);
    void delete(UUID id);
}

// JPA Implementation
@Repository
public class JpaOrderRepository implements OrderRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Order findById(UUID id) {
        return em.find(Order.class, id);
    }

    @Override
    public void save(Order order) {
        if (order.getId() == null) em.persist(order);
        else em.merge(order);
    }
}
```

**Spring Data:** `JpaRepository` zaten Repository pattern uygular. Custom sorgular için `@Query` veya `Specification` kullanın.

---

## 2. Specification Pattern

Karmaşık sorgu koşullarını composable (birleştirilebilir) nesneler olarak ifade eder. `WHERE` koşullarını programatik oluşturur.

```java
public class OrderSpecifications {
    
    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
    
    public static Specification<Order> createdAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThan(root.get("createdAt"), date);
    }
    
    public static Specification<Order> totalGreaterThan(BigDecimal amount) {
        return (root, query, cb) -> cb.greaterThan(root.get("total"), amount);
    }
}

// Kullanım: Koşulları birleştir
Specification<Order> spec = Specification
    .where(hasStatus(OrderStatus.DELIVERED))
    .and(createdAfter(LocalDate.now().minusDays(30)))
    .and(totalGreaterThan(new BigDecimal("100")));

List<Order> orders = orderRepository.findAll(spec, PageRequest.of(0, 20));
```

---

## 3. Unit of Work Pattern

Bir transaction içindeki tüm değişiklikleri takip eder ve toplu olarak commit eder. JPA `EntityManager` zaten bu pattern'i uygular (Persistence Context).

```java
// JPA's built-in Unit of Work
@Transactional
public void transferMoney(UUID from, UUID to, BigDecimal amount) {
    Account source = accountRepo.findById(from);  // Managed entity
    Account target = accountRepo.findById(to);     // Managed entity
    
    source.debit(amount);   // Dirty checking - otomatik takip
    target.credit(amount);  // Dirty checking - otomatik takip
    
    // save() çağırmaya gerek yok! Transaction commit'te otomatik flush
}
```

---

## 4. DTO Pattern ve MapStruct

```java
// Entity → DTO dönüşümü (MapStruct ile)
@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderDTO toDTO(Order order);
    Order toEntity(CreateOrderRequest request);
    
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "itemCount", expression = "java(order.getItems().size())")
    OrderSummaryDTO toSummary(Order order);
}
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: Repository ile DAO farkı nedir?
**Cevap:**
- **DAO:** Veri erişim odaklı. Tabloya karşılık gelir. SQL/CRUD operasyonları.
- **Repository:** Domain odaklı. Aggregate root'a karşılık gelir. İş mantığı dili kullanır.
- Pratikte Spring Data JPA'da ikisi birleşmiştir.

### Soru 2: Specification pattern ne zaman kullanılmalı?
**Cevap:** Dinamik filtreleme gereken UI'larda (e-ticaret ürün filtresi, admin paneli arama). Statik sorgular için `@Query` yeterlidir.

### Soru 3 (Tricky): Anemic Domain Model anti-pattern nedir?
**Cevap:** Entity sınıflarında sadece getter/setter olup, tüm iş mantığının Service katmanında olmasıdır. Domain nesneleri "kansız" (anemic) kalır. **Rich Domain Model**'da entity kendi iş kurallarını bilir: `order.cancel()`, `account.withdraw(amount)`.

---

## 6. Geliştirici İpuçları

- **DTO ≠ Entity:** Asla Entity'yi doğrudan API response olarak dönmeyin. Lazy loading, circular reference, güvenlik sorunları oluşur.
- **Aggregate Root:** Bir aggregate'e sadece root entity üzerinden erişin. OrderItem'ı doğrudan DB'den çekmeyin, Order üzerinden erişin.
- **Domain Events:** Entity'lerin iç durumu değiştiğinde event yayınlayın: `order.addItem() → DomainEvents.raise(new ItemAddedEvent())`.
