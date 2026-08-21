# Domain-Driven Design (DDD) — Stratejik ve Taktiksel Desenler

> **Analoji:** DDD, bir "şehir planlama" gibidir. Önce şehrin bölgelerini (Bounded Context) belirlersiniz — ticaret bölgesi, konut bölgesi, sanayi bölgesi. Her bölgenin kendi kuralları vardır. Bölgeler arası iletişim yollar (Integration) ile sağlanır. İçerideki yapılar ise taktiksel desenlerdir (Entity, Value Object, Aggregate).

---

## 1. Stratejik Desenler (Big Picture)

### Bounded Context

Aynı terim farklı bağlamlarda farklı anlam taşır. Her bağlamın sınırları nettir.

```
"Müşteri" Terimi:
  - Sales Context:     { isim, email, segmenti, satın alma geçmişi }
  - Billing Context:   { fatura adresi, vergi no, ödeme yöntemi }
  - Shipping Context:  { teslimat adresi, tercih edilen kargo }
  
→ Her context kendi "Müşteri" modelini tanımlar. Paylaşılan model (shared kernel) yerine her bağlam bağımsızdır.
```

### Context Map

Bounded Context'ler arası ilişkileri gösteren harita:

| Pattern | Açıklama | Örnek |
| :--- | :--- | :--- |
| **Shared Kernel** | İki context ortak bir modeli paylaşır | Authentication shared library |
| **Customer-Supplier** | Downstream talep eder, upstream sağlar | Order (customer) → Inventory (supplier) |
| **Anti-Corruption Layer (ACL)** | Legacy sisteme adapter | Eski ERP → Yeni sisteme çeviri katmanı |
| **Open Host Service** | Protokol yayınla, diğerleri kullanabilsin | REST API |
| **Published Language** | Ortak bir dil (JSON schema, Protobuf) | Event schema registry |

---

## 2. Taktiksel Desenler (Building Blocks)

### Entity vs Value Object

| Özellik | Entity | Value Object |
| :--- | :--- | :--- |
| **Kimlik** | Unique ID ile tanınır | Değerleri ile tanınır |
| **Eşitlik** | ID'ye göre | Tüm alanlara göre |
| **Değişebilirlik** | Mutable (state değişir) | Immutable (yeni nesne oluşturulur) |
| **Örnek** | `Order(id=123)` | `Money(100, "TRY")`, `Address(...)` |

```java
// Entity
@Entity
public class Order {
    @Id
    private UUID id;
    private OrderStatus status;
    
    // İş mantığı entity'nin kendisinde!
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
        registerEvent(new OrderConfirmedEvent(this.id));
    }
}

// Value Object (Java 17 record ile ideal)
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount must be non-negative");
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch");
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

### Aggregate

Tutarlılık sınırını belirleyen entity kümesi. Dışarıdan sadece **root entity** üzerinden erişilir.

```
Order Aggregate:
  ┌─── Order (Root Entity) ─────────────────┐
  │  id: UUID                                │
  │  status: OrderStatus                     │
  │  totalAmount: Money                      │
  │                                          │
  │  ┌── OrderItem (Entity) ──┐             │
  │  │  productId              │             │
  │  │  quantity               │             │
  │  │  price: Money           │             │
  │  └─────────────────────────┘             │
  │                                          │
  │  ┌── ShippingAddress (VO) ─┐             │
  │  │  street, city, zip      │             │
  │  └─────────────────────────┘             │
  └──────────────────────────────────────────┘

Kurallar:
  ✅ order.addItem(product, qty)    → Root üzerinden erişim
  ❌ orderItemRepository.save(item) → Direkt erişim YASAK
  ✅ Aggregate içi: Strong consistency (tek transaction)
  ✅ Aggregate arası: Eventual consistency (domain events)
```

### Domain Service

Entity veya Value Object'e ait olmayan iş mantığı:

```java
@Service
public class TransferService {
    // Transfer işlemi iki aggregate'i ilgilendiriyor
    // Hiçbir entity'ye ait değil → Domain Service
    @Transactional
    public void transfer(UUID fromAccountId, UUID toAccountId, Money amount) {
        Account source = accountRepository.findById(fromAccountId).orElseThrow();
        Account target = accountRepository.findById(toAccountId).orElseThrow();
        
        source.debit(amount);    // Source aggregate
        target.credit(amount);   // Target aggregate
        
        accountRepository.save(source);
        accountRepository.save(target);
    }
}
```

### Domain Event

Aggregate içinde iş kuralı tetiklendiğinde yayınlanan olay:

```java
public abstract class AggregateRoot {
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearEvents() {
        domainEvents.clear();
    }
}

// Spring'de @DomainEvents kullanımı
public class Order extends AggregateRoot {
    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        registerEvent(new OrderConfirmedEvent(this.id, this.totalAmount));
    }
}
```

---

## 3. Hexagonal Architecture (Ports & Adapters)

DDD ile sıklıkla birlikte kullanılır. Domain katmanı dış dünyadan izole edilir.

```
        ┌─── Driving Adapters ───┐
        │  REST Controller       │
        │  gRPC Server           │
        │  CLI                   │
        └──────┬─────────────────┘
               ▼ (Input Port)
        ┌─── Domain ─────────────┐
        │  Entities              │
        │  Value Objects         │
        │  Domain Services       │
        │  Use Cases             │
        └──────┬─────────────────┘
               ▼ (Output Port)
        ┌─── Driven Adapters ────┐
        │  JPA Repository        │
        │  Kafka Producer        │
        │  HTTP Client           │
        └────────────────────────┘
```

---

## 4. Kritik Mülakat Soruları

### Soru 1: DDD ne zaman kullanılmalı, ne zaman kullanılmamalı?
**Cevap:**
- **Kullanılmalı:** Karmaşık iş mantığı, çok sayıda iş kuralı, uzun ömürlü projeler
- **Kullanılmamalı:** Basit CRUD uygulamaları, kısa ömürlü projeler, prototip
- **Kural:** İş mantığının karmaşıklığı > teknik karmaşıklık ise DDD uygundur

### Soru 2: Aggregate boyutu nasıl belirlenir?
**Cevap:** **Küçük tutun!** Transaction boundary = aggregate boundary. Büyük aggregate = uzun transaction = yavaşlık + contention. Aggregate arası eventual consistency kullanın.

### Soru 3 (Tricky): Anemic vs Rich Domain Model?
**Cevap:**
- **Anemic:** Entity'de sadece getter/setter. İş mantığı Service'te. Anti-pattern!
- **Rich:** Entity kendi iş kurallarını bilir. `order.confirm()`, `account.withdraw()`. DDD'nin gerçek gücü.

---

## 5. Geliştirici İpuçları

- **Ubiquitous Language:** Domain expert'lerin kullandığı terimleri kodda kullanın. `calculatePrice()` değil `applyDiscount()`.
- **Package by Feature:** `com.example.order`, `com.example.payment` — her bounded context ayrı paket.
- **Event Storming:** Mimari keşif için büyük bir duvara turuncu post-it'ler yapıştırın: "OrderPlaced", "PaymentReceived". Domain expert'ler ile birlikte yapın.
