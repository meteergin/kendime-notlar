# Event-Driven Architecture (EDA), Event Sourcing ve CQRS

> **Analoji:** EDA, bir "gazete abonelik sistemi" gibidir. Siz gazeteye abone olursunuz, gazeteci haber yazar ve matbaa bunu tüm abonelere dağıtır. Gazeteci her aboneyi tek tek aramaz; haber "yayınlanır" ve ilgilenen herkes alır. Bu, **loose coupling** (gevşek bağlılık) sağlar.

---

## 1. Event-Driven Architecture Temelleri

### Geleneksel vs Event-Driven

```
GELENEKSEL (Request/Response):
  OrderService → doğrudan çağırır → InventoryService
  OrderService → doğrudan çağırır → PaymentService  
  OrderService → doğrudan çağırır → NotificationService
  ⚠️ Tight coupling, single point of failure, zincirleme hata

EVENT-DRIVEN:
  OrderService → "OrderCreated" olayı yayınlar → Event Bus
      ├── InventoryService (dinler, stok düşer)
      ├── PaymentService (dinler, ödeme alınır)  
      └── NotificationService (dinler, email gönderir)
  ✅ Loose coupling, bağımsız ölçeklenme, hata izolasyonu
```

### Olay Türleri

| Tür | Açıklama | Örnek |
| :--- | :--- | :--- |
| **Domain Event** | İş alanında olan bir gerçek | `OrderPlaced`, `PaymentReceived` |
| **Integration Event** | Servisler arası iletişim | `InventoryReserved`, `ShipmentCreated` |
| **Notification Event** | Bilgilendirme amaçlı | `UserLoggedIn`, `ReportGenerated` |

### Temel Bileşenler

1. **Producer (Üretici):** Olayı yayınlayan servis
2. **Event Channel / Broker:** Olayı taşıyan altyapı (Kafka, RabbitMQ)
3. **Consumer (Tüketici):** Olayı dinleyen ve işleyen servis

---

## 2. Event Sourcing

> **Analoji:** Bankacılık sistemi düşünün. Hesap bakiyenizi sadece "mevcut bakiye: 1000 TL" şeklinde saklamak yerine, **tüm işlem geçmişini** saklarsınız: +5000, -2000, -1500, -500. Mevcut bakiyeyi bu olayları oynatarak (replay) hesaplarsınız.

### Geleneksel CRUD vs Event Sourcing

```
CRUD (State-based):
  UPDATE accounts SET balance = 1000 WHERE id = 42;
  → Geçmiş kaybolur. "Nasıl 1000 TL'ye geldi?" bilinmez.

EVENT SOURCING (Event-based):
  Event Store:
    1. AccountCreated   { id: 42, balance: 0 }
    2. MoneyDeposited   { amount: 5000 }
    3. MoneyWithdrawn   { amount: 2000 }
    4. MoneyWithdrawn   { amount: 1500 }
    5. MoneyWithdrawn   { amount: 500 }
  → Mevcut durum: 1000 TL (replay ile hesaplanır)
  → Her an geçmişe gidip "2. olaydan sonra bakiye neydi?" bilinir
```

### Event Store Yapısı

```java
@Entity
@Table(name = "event_store")
public class StoredEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String aggregateId;       // Hangi entity (Order#123)
    private String aggregateType;     // Hangi sınıf (Order)
    private String eventType;         // Olay tipi (OrderCreated)
    private int version;              // Sıra numarası
    
    @Column(columnDefinition = "jsonb")
    private String payload;           // Olay verisi (JSON)
    
    private Instant occurredAt;       // Ne zaman oldu
}
```

### Aggregate Rebuild

```java
public class OrderAggregate {
    private UUID id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items = new ArrayList<>();
    
    // Olayları oynatarak mevcut durumu oluştur
    public static OrderAggregate rebuild(List<DomainEvent> events) {
        OrderAggregate aggregate = new OrderAggregate();
        for (DomainEvent event : events) {
            aggregate.apply(event);
        }
        return aggregate;
    }
    
    private void apply(DomainEvent event) {
        switch (event) {
            case OrderCreated e -> {
                this.id = e.orderId();
                this.status = OrderStatus.CREATED;
            }
            case ItemAdded e -> {
                this.items.add(e.item());
                this.totalAmount = this.totalAmount.add(e.item().price());
            }
            case OrderConfirmed e -> this.status = OrderStatus.CONFIRMED;
            default -> throw new IllegalArgumentException("Unknown event: " + event);
        }
    }
}
```

---

## 3. CQRS (Command Query Responsibility Segregation)

> **Analoji:** Bir restoranda "sipariş almak" (Command) ve "menüyü göstermek" (Query) tamamen farklı süreçlerdir. Aşçı siparişleri işler, garson menüyü gösterir. İkisini aynı kişi yapmak zorunda değil. CQRS, okuma ve yazma taraflarını ayırarak her birini bağımsız optimize etmenizi sağlar.

### CQRS Yapısı

```
                   ┌─── Command Model (Yazma) ───┐
                   │  - Validasyon                │
 Kullanıcı ───────►│  - Business Logic            │──► Write DB (Normalized)
 (Command)         │  - Event Yayınlama           │
                   └──────────────────────────────┘
                              │
                              ▼ (Domain Events)
                   ┌─── Query Model (Okuma) ──────┐
 Kullanıcı ───────►│  - Denormalized View          │──► Read DB (Denormalized)
 (Query)           │  - Hızlı sorgular             │
                   │  - Elasticsearch / Redis       │
                   └──────────────────────────────┘
```

### Spring Boot ile CQRS

```java
// COMMAND tarafı
@Service
public class OrderCommandService {
    @Transactional
    public UUID createOrder(CreateOrderCommand cmd) {
        Order order = Order.create(cmd.customerId(), cmd.items());
        orderRepository.save(order);
        
        // Event yayınla → Query modelini güncelle
        eventPublisher.publish(new OrderCreatedEvent(order.getId(), order.getTotal()));
        return order.getId();
    }
}

// QUERY tarafı
@Service
public class OrderQueryService {
    // Okuma için optimize edilmiş, farklı bir model/db
    public OrderSummaryDTO getOrderSummary(UUID orderId) {
        return orderViewRepository.findSummaryById(orderId);
    }
    
    public List<OrderListDTO> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        return orderViewRepository.findByCustomerId(customerId, pageable);
    }
}
```

---

## 4. Eventual Consistency (Nihayetinde Tutarlılık)

EDA ve CQRS'de, yazma ve okuma modelleri anlık değil, **bir süre sonra** tutarlı olur. Bu süre genellikle milisaniyeler seviyesindedir ama garanti yoktur.

### Stratejiler

| Strateji | Açıklama | Kullanım |
| :--- | :--- | :--- |
| **Idempotent Consumer** | Aynı olay iki kez işlense bile sonuç aynı | Mesaj tekrarlarını tolere etme |
| **Outbox Pattern** | Event'i DB transaction'ı içinde yaz, ayrı süreç olarak yayınla | Atomik event yayınlama |
| **Compensating Transaction** | Hata durumunda geri alma olayı yayınla | Saga pattern |

### Outbox Pattern

```java
@Transactional
public void placeOrder(CreateOrderCommand cmd) {
    // 1. Order'ı kaydet
    Order order = orderRepository.save(Order.create(cmd));
    
    // 2. Event'i AYNI TRANSACTION içinde outbox tablosuna yaz
    OutboxEvent event = new OutboxEvent(
        "OrderCreated", 
        order.getId().toString(),
        objectMapper.writeValueAsString(new OrderCreatedPayload(order))
    );
    outboxRepository.save(event);
    
    // 3. Debezium veya Scheduler outbox tablosunu okuyup Kafka'ya yayınlar
    // → Transaction garantisi sağlanır
}
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: EDA'nın avantajları ve dezavantajları nelerdir?
**Cevap:**
**Avantajlar:**
- **Loose Coupling:** Servisler birbirini bilmez
- **Scalability:** Her servis bağımsız ölçeklenir
- **Resilience:** Bir servis çökse bile event kuyrukta bekler

**Dezavantajlar:**
- **Complexity:** Debug ve trace zorlaşır (distributed tracing gerekir)
- **Eventual Consistency:** Anlık tutarlılık garanti değildir
- **Event Schema Evolution:** Event yapısı değiştiğinde backward compatibility

### Soru 2: Event Sourcing ne zaman kullanılmalı, ne zaman kullanılmamalı?
**Cevap:**
- **Kullanılmalı:** Audit trail kritikse (finans), temporal query gerekiyorsa, undo/redo istiyorsanız
- **Kullanılmamalı:** Basit CRUD uygulamalarında, yüksek performanslı arama gerektiğinde (CQRS ile birleştirilmediyse)
- **Trap:** Her sisteme Event Sourcing uygulamak "over-engineering"dir

### Soru 3: CQRS'de read ve write model farklı veritabanları kullanabilir mi?
**Cevap:** Evet! Bu en güçlü yönlerinden biridir:
- **Write:** PostgreSQL (ACID, normalized, transactional)
- **Read:** Elasticsearch (full-text search), Redis (cache), MongoDB (denormalized views)
- Senkronizasyon Domain Event'ler aracılığıyla yapılır

### Soru 4 (Tricky): Idempotency neden EDA'da kritiktir?
**Cevap:** Mesaj broker'lar "at-least-once" delivery garantisi verir (en az bir kez). Aynı event iki kez gelebilir. Consumer idempotent değilse:
- Para iki kez çekilir
- Email iki kez gönderilir
- **Çözüm:** Her event'e unique `eventId` ekleyin, processed event'leri saklayın, tekrar gelirse görmezden gelin.

### Soru 5 (Tricky): Event Sourcing'de Snapshot nedir?
**Cevap:** Binlerce event'i her seferinde replay etmek yavaş olur. **Snapshot**, belirli aralıklarla aggregate'in mevcut durumunu "fotoğraflayıp" saklar. Rebuild yaparken son snapshot'tan başlayıp sadece sonraki event'leri oynatır.

---

## 6. Geliştirici İpuçları

- **Event İsimlendirme:** Past tense kullanın — `OrderCreated`, `PaymentProcessed`. "Create" değil "Created".
- **Event Versioning:** Event schema'sı değiştiğinde `v1`, `v2` ile versiyonlayın. Eski consumer'lar eski versiyonu okuyabilmeli.
- **Correlation ID:** Bir iş akışındaki tüm event'lere aynı `correlationId` verin. Distributed tracing ile akışı uçtan uca izleyin.
- **Dead Letter Queue (DLQ):** İşlenemeyen event'leri DLQ'ya yönlendirin. Sonra manuel veya otomatik retry yapın.
- **Domain Events vs Integration Events:** Domain event'ler sınırlı bağlam (bounded context) içinde kalır. Integration event'ler servisler arası iletişim için kullanılır. İkisini karıştırmayın.
