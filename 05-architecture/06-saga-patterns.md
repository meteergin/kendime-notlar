# Saga Patterns (Choreography vs Orchestration)

> **Analoji:** Bir düğün organizasyonu düşünün. **Choreography**, herkesin kendi sorumluluğunu bildiği ve müzik duyunca dans ettiği bir partidir — aşçı, DJ, fotoğrafçı kendi işini yapar, birbirlerine "olaylar" (sinyaller) ile haber verir. **Orchestration** ise bir düğün planlayıcısının (koordinatör) herkesi yönettiği bir organizasyondur — planlayıcı aşçıya "hazırla", DJ'e "çal" der.

---

## 1. Dağıtık Transaction Problemi

Mikroservis mimarisinde her servis kendi veritabanına sahiptir. Bir iş akışı birden fazla servisi kapsarsa, "ya hep ya hiç" (atomik) garanti nasıl sağlanır?

```
Sipariş Akışı:
  1. OrderService  → Sipariş oluştur
  2. PaymentService → Ödeme al
  3. InventoryService → Stok düş
  4. ShippingService → Kargo oluştur

❌ 3. adımda stok yetersiz! 1 ve 2 geri alınmalı.
```

**2PC neden uygun değil?** Blocking, SPOF (coordinator), farklı DB'ler arası uyumsuzluk.

**Çözüm: Saga Pattern** — Her adım local transaction yapar. Hata durumunda önceki adımlar **compensating transaction** ile geri alınır.

---

## 2. Choreography-Based Saga

Her servis olay yayınlar, diğer servisler dinler ve tepki verir. Merkezi koordinatör yoktur.

```
Başarılı Akış:
  OrderService → "OrderCreated" → 
    PaymentService → "PaymentCompleted" → 
      InventoryService → "StockReserved" → 
        ShippingService → "ShipmentCreated"

Hata Akışı (Stok Yetersiz):
  InventoryService → "StockReservationFailed" → 
    PaymentService → "PaymentRefunded" → 
      OrderService → "OrderCancelled"
```

### Spring Boot + Kafka İmplementasyonu

```java
// OrderService - Olayı yayınla
@Service
public class OrderService {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Transactional
    public Order createOrder(CreateOrderCommand cmd) {
        Order order = orderRepository.save(Order.create(cmd));
        kafkaTemplate.send("order-events", 
            new OrderCreatedEvent(order.getId(), order.getItems(), order.getTotal()));
        return order;
    }

    // Compensating: Ödeme başarısız olursa siparişi iptal et
    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.cancel("Ödeme başarısız: " + event.getReason());
        orderRepository.save(order);
    }
}

// PaymentService - Olayı dinle ve tepki ver
@Service
public class PaymentService {
    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            Payment payment = processPayment(event.getOrderId(), event.getTotal());
            kafkaTemplate.send("payment-events", 
                new PaymentCompletedEvent(event.getOrderId(), payment.getId()));
        } catch (InsufficientFundsException e) {
            kafkaTemplate.send("payment-events", 
                new PaymentFailedEvent(event.getOrderId(), e.getMessage()));
        }
    }
}
```

### Avantajlar & Dezavantajlar

| ✅ Avantaj | ❌ Dezavantaj |
| :--- | :--- |
| Loose coupling | Akışı takip etmek zor (debug) |
| Her servis bağımsız | Döngüsel olay bağımlılığı riski |
| SPOF yok | Karmaşık hata yönetimi |

---

## 3. Orchestration-Based Saga

Merkezi bir **Saga Orchestrator** tüm adımları yönetir. Her servise ne yapacağını söyler ve yanıtına göre sonraki adıma geçer veya geri alır.

```
Saga Orchestrator:
  1. → OrderService: "Sipariş oluştur"     ← "Tamam"
  2. → PaymentService: "Ödeme al"          ← "Tamam"
  3. → InventoryService: "Stok düş"        ← "HATA! Stok yok"
  4. → PaymentService: "Ödemeyi iade et"   ← "Tamam" (Compensate)
  5. → OrderService: "Siparişi iptal et"   ← "Tamam" (Compensate)
```

### Spring Boot İmplementasyonu

```java
@Service
public class OrderSagaOrchestrator {
    
    public void executeSaga(CreateOrderCommand cmd) {
        SagaState state = new SagaState(cmd);
        
        try {
            // Forward steps
            Order order = orderService.createOrder(cmd);
            state.setOrderId(order.getId());
            
            Payment payment = paymentService.processPayment(order.getId(), cmd.getTotal());
            state.setPaymentId(payment.getId());
            
            inventoryService.reserveStock(order.getId(), cmd.getItems());
            state.setStockReserved(true);
            
            shippingService.createShipment(order.getId());
            
        } catch (Exception e) {
            // Compensating transactions (ters sıra)
            compensate(state, e);
        }
    }
    
    private void compensate(SagaState state, Exception originalError) {
        log.error("Saga başarısız, compensating: {}", originalError.getMessage());
        
        if (state.isStockReserved()) {
            inventoryService.releaseStock(state.getOrderId());
        }
        if (state.getPaymentId() != null) {
            paymentService.refund(state.getPaymentId());
        }
        if (state.getOrderId() != null) {
            orderService.cancelOrder(state.getOrderId());
        }
    }
}
```

### Avantajlar & Dezavantajlar

| ✅ Avantaj | ❌ Dezavantaj |
| :--- | :--- |
| Akış takibi kolay | Orchestrator SPOF olabilir |
| Merkezi hata yönetimi | Servisler orchestrator'a bağımlı |
| Karmaşık akışlarda ideal | Daha fazla kod |

---

## 4. Karşılaştırma

| Kriter | Choreography | Orchestration |
| :--- | :--- | :--- |
| **Bağımlılık** | Loose coupling | Orchestrator'a bağımlı |
| **Karmaşıklık** | 3-4 adıma kadar uygun | 5+ adımda ideal |
| **Debugging** | Zor (distributed tracing gerekir) | Kolay (merkezi log) |
| **Esneklik** | Yeni servis eklemek kolay | Orchestrator güncellenmeli |

---

## 5. Kritik Mülakat Soruları

### Soru 1: Compensating transaction nedir?
**Cevap:** Bir adım başarısız olduğunda, önceki başarılı adımları geri almak için çalıştırılan ters işlemlerdir. Örneğin: Ödeme → Refund, Stok Düşme → Stok Geri Ekleme.

### Soru 2: Saga'da idempotency neden kritiktir?
**Cevap:** Mesaj broker'lar "at-least-once" delivery yapar. Aynı compensating transaction iki kez tetiklenebilir. Idempotent olmayan bir refund işlemi iki kez para iadesi yapar.

### Soru 3 (Tricky): Choreography'de "semantic coupling" nedir?
**Cevap:** Servisler fiziksel olarak bağımsız olsa da, event yapısına (schema) ve sırasına bağımlıdır. Event şeması değişince tüm consumer'lar etkilenir. Bu gizli bağımlılık (semantic coupling) olarak adlandırılır.

---

## 6. Geliştirici İpuçları

- **Saga Status Tracking:** Her saga instance'ının durumunu (STARTED, PAYMENT_DONE, FAILED) bir tabloda saklayın. Monitoring ve debug için kritiktir.
- **Timeout Handling:** Bir adım yanıt vermezse ne olacak? Timeout sonrası otomatik compensate veya retry stratejisi belirleyin.
- **Dead Letter Queue:** İşlenemeyen compensating event'leri DLQ'ya yönlendirin. Manuel müdahale gerekebilir.
- **Process Manager Pattern:** Orchestration'ın gelişmiş halidir. State machine ile adımları yönetir (Axon Framework, Temporal.io).
