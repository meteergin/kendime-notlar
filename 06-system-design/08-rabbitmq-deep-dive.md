# RabbitMQ — Derinlemesine Rehber

> **Analoji:** RabbitMQ, bir "akıllı merkez postanesi" gibidir. Mektuplar (mesajlar) vezneye (Exchange) teslim edilir. Posta müdürü (Exchange Tipi) mektubun üzerindeki adrese ve puldaki kurallara (Routing Key / Binding) bakarak mektubu doğru posta kutularına (Queues) dağıtır. Postacılar (Consumers) da kutularından mektupları alıp teslim eder. Mektup teslim edilip onaylanınca (Ack) kutudan silinir.

---

## 1. RabbitMQ Mimarisi ve Temel Kavramlar

RabbitMQ, **AMQP 0-9-1 (Advanced Message Queuing Protocol)** standardını temel alan, Erlang ile yazılmış yüksek güvenilirlikli (reliability) bir mesaj kuyruk sistemidir.

```
┌───────────────── RabbitMQ Broker ───────────────────────────────┐
│                                                                 │
│                 ┌───────────────┐                               │
│                 │   Exchange    │                               │
│  Producer ─────►│  (Yönlendirici│                               │
│                 └───────┬───────┘                               │
│                         │                                       │
│             Bindings / Routing Keys                             │
│             ┌───────────┼───────────┐                           │
│             ▼           ▼           ▼                           │
│        ┌─────────┐ ┌─────────┐ ┌─────────┐                      │
│        │ Queue A │ │ Queue B │ │ Queue C │                      │
│        └────┬────┘ └────┬────┘ └────┬────┘                      │
│             │           │           │                           │
└─────────────┼───────────┼───────────┼───────────────────────────┘
              ▼           ▼           ▼
          Consumer 1  Consumer 2  Consumer 3
```

### Temel Bileşenler ve Sözlük

| Bileşen | Açıklama | Analoji |
| :--- | :--- | :--- |
| **Producer** | Mesajı üreten ve Exchange'e gönderen istemci | Mektubu yazıp postaneye veren kişi |
| **Exchange** | Mesajları alıp kurallara göre kuyruklara dağıtan yönlendirici | Postanedeki ayrıştırma masası |
| **Queue** | Mesajların tüketilmeyi beklediği bellek/disk tamponu | Posta kutusu |
| **Binding** | Exchange ile Queue arasındaki bağlantı / kural ilişkisi | Adresleme & yönlendirme tabelası |
| **Routing Key** | Producer'ın mesaja eklediği adresleme etiketi | Zarfın üzerindeki adres/posta kodu |
| **Consumer** | Kuyruktan mesajı çekip işleyen servis | Mektubu kutudan alan kişi |
| **Virtual Host (vhost)** | Broker içinde mantıksal izolasyon (multi-tenancy) | Aynı binadaki farklı şirket ofisleri |
| **Channel** | Tek bir TCP bağlantısı üzerinde açılan hafif sanal bağlantı | Aynı otoyoldaki farklı şeritler |

---

## 2. Exchange Türleri ve Yönlendirme Algoritmaları

RabbitMQ'da Producer **asla doğrudan kuyruğa yazmaz**. Her zaman bir Exchange'e gönderir. 4 temel exchange tipi vardır:

### 1. Direct Exchange (Birebir Eşleşme)
Routing Key ile Binding Key **birebir eşitse** mesaj o kuyruğa gider.
```
Producer --(Routing Key: "payment.success")--> [Direct Exchange]
                                                     │
                             ┌───────────────────────┴───────────────────────┐
                             │ (Binding: "payment.success")                  │ (Binding: "payment.failed")
                             ▼                                               ▼
                      [Payment_OK_Queue]                             [Payment_Fail_Queue]
```

### 2. Fanout Exchange (Yayın / Broadcast)
Routing key'i **tamamen yok sayar**. Mesajı kendisine bağlı **tüm kuyruklara kopyalar**.
- **Kullanım:** Canlı skor bildirimleri, konfigürasyon güncellemeleri, log replikasyonu.
- **Performans:** En hızlı exchange türüdür (routing hesabı yapmaz).

### 3. Topic Exchange (Örüntü / Wildcard Eşleştirme)
Routing key noktalarla ayrılmış kelimelerden oluşur (`eur.stock.nyse`). Joker karakterler kullanılır:
- `*` (yıldız): Tam olarak **bir** kelime yerine geçer (`stock.*.nyse`).
- `#` (kare): **Sıfır veya daha fazla** kelime yerine geçer (`stock.#`).

```
Routing Key: "order.eu.express"

Bindings:
  - Queue 1: "order.*.express"  --> ✅ Mesajı alır
  - Queue 2: "order.eu.#"       --> ✅ Mesajı alır
  - Queue 3: "order.us.*"       --> ❌ Alamaz
```

### 4. Headers Exchange (Başlık Bazlı Yönlendirme)
Routing key yerine mesajın **Header** parametrelerine göre yönlendirir. `x-match` argümanı alır:
- `x-match: all` → Tüm header anahtar/değerleri eşleşmeli.
- `x-match: any` → Herhangi bir header eşleşmesi yeterli.

---

## 3. Güvenilirlik ve Mesaj Kaybını Önleme (Reliability)

Kurumsal mimaride "sıfır mesaj kaybı" için şu 4 katman eksiksiz kurulmalıdır:

```
[Producer] ──(1. Publisher Confirms)──► [Exchange]
                                             │
                                    (2. Durable Queue & Persistent Msg)
                                             │
                                             ▼
                                          [Queue]
                                             │
[Consumer] ◄──(4. Manual Ack)─────────(3. Prefetch Count)
```

### 1. Publisher Confirms (Producer Güvencesi)
Producer, mesajın broker tarafından diske yazıldığını veya kuyruğa ulaştığını `ack/nack` ile doğrular.
```java
// Spring Boot: application.yml
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
```

### 2. Message Persistence & Durable Queues (Broker Güvencesi)
- **Durable Queue:** Broker restart olsa bile kuyruk tanımı silinmez (`durable = true`).
- **Persistent Message:** Mesaj diske yazılır (`MessageProperties.PERSISTENT_TEXT_PLAIN` / `deliveryMode = 2`).

### 3. Consumer Acknowledgements (Manual Ack)
Mesaj kuyruktan çekildiği an değil, Consumer **başarıyla işledikten sonra** silinmelidir.
- `basicAck(deliveryTag, multiple)`: Başarılı, kuyruktan sil.
- `basicNack(deliveryTag, multiple, requeue)`: Başarısız, requeue=true ise tekrar kuyruğa koy, false ise DLX'e at.
- `basicReject(deliveryTag, requeue)`: Tek bir mesaj için nack.

```java
// Spring Boot Container Factory
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Manuel onay modu
    factory.setPrefetchCount(20); // Aynı anda unacked tutulabilecek max mesaj
    return factory;
}
```

### 4. Prefetch Count (QoS - Quality of Service)
RabbitMQ varsayılan olarak tüm mesajları hızlıca Consumer'a yığmaya çalışır (Round-Robin). Bu durum bir consumer'ın belleğinin şişmesine veya ağır işlerde darboğaza yol açar.
- `basicQos(prefetchCount = 1)` veya `prefetchCount = 20`: Consumer onay vermediği sürece yeni mesaj gönderme!

---

## 4. Gelişmiş Özellikler: DLX, TTL, RPC, Priority

### Dead Letter Exchange (DLX) & Retry Pattern

Bir mesaj şu 3 durumda "Dead Letter" olur:
1. `basicNack` veya `basicReject` ile `requeue = false` yapıldığında
2. Mesajın TTL (Time-to-Live) süresi dolduğunda
3. Kuyruğun maksimum uzunluk limiti (`x-max-length`) aşıldığında

```java
@Configuration
public class RabbitMqDlqConfig {

    public static final String MAIN_QUEUE = "orders.queue";
    public static final String DLX_NAME = "orders.dlx.exchange";
    public static final String DLQ_NAME = "orders.dlq.queue";

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("orders.dead");
    }

    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "orders.dead")
                .withArgument("x-message-ttl", 60000) // 60 sn işlenmezse DLQ'ya git
                .build();
    }
}
```

---

## 5. Spring Boot ile Tam Entegrasyon Örneği

### Producer Örneği (Correlation Data ile)

```java
@Service
@Slf4j
public class OrderMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.setupCallbacks();
    }

    private void setupCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Mesaj Broker'a başarıyla ulaştı. ID: {}", correlationData != null ? correlationData.getId() : "null");
            } else {
                log.error("Mesaj kayboldu! Sebep: {}, ID: {}", cause, correlationData != null ? correlationData.getId() : "null");
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            log.warn("Mesaj kuyruğa iletilemedi (Unrouted)! ReplyCode: {}, Exchange: {}, RoutingKey: {}",
                    returned.getReplyCode(), returned.getExchange(), returned.getRoutingKey());
        });
    }

    public void sendOrderCreatedEvent(OrderDto orderDto) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
                "orders.exchange",
                "order.created",
                orderDto,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("correlationId", correlationData.getId());
                    return message;
                },
                correlationData
        );
    }
}
```

### Consumer Örneği (Manuel Ack + Idempotency)

```java
@Service
@Slf4j
public class OrderMessageConsumer {

    @RabbitListener(queues = "orders.queue", ackMode = "MANUAL")
    public void handleOrderCreated(
            @Payload OrderDto order,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        try {
            log.info("Sipariş işleniyor: {}", order.getOrderId());
            
            // 1. Idempotency Kontrolü (DB veya Redis)
            if (isAlreadyProcessed(order.getOrderId())) {
                log.warn("Bu sipariş daha önce işlenmiş (Duplicate), onaylanıp geçiliyor.");
                channel.basicAck(tag, false);
                return;
            }

            // 2. Business Logic
            processBusinessLogic(order);

            // 3. Başarılı Onay
            channel.basicAck(tag, false);
            log.info("Sipariş başarıyla tamamlandı ve Ack gönderildi: {}", order.getOrderId());

        } catch (BusinessException be) {
            log.error("İş kuralı hatası, DLQ'ya gönderiliyor: {}", be.getMessage());
            // Tekrar deneme (requeue=false -> DLQ'ya düşer)
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Geçici altyapı hatası, tekrar kuyruğa alınıyor (Requeue): {}", ex.getMessage());
            // Geçici network/DB hatası ise tekrar denensin
            channel.basicNack(tag, false, true);
        }
    }

    private boolean isAlreadyProcessed(String orderId) {
        // Redis / DB kontrolü
        return false;
    }

    private void processBusinessLogic(OrderDto order) {
        // İş kuralları
    }
}
```

---

## 6. Clustering, High Availability ve Quorum Queues

### Classic Mirrored Queues (Deprecated) vs Quorum Queues (Modern)

RabbitMQ 3.8+ ile birlikte eski "Mirrored Queues" yapısının yerini **Raft Consensus tabanlı Quorum Queues** almıştır.

| Özellik | Classic Mirrored Queues | Quorum Queues |
| :--- | :--- | :--- |
| **Konsensüs Algoritması** | Özel lider-takipçi (senkronizasyon bloklayıcı) | **Raft** tabanlı güçlü konsensüs |
| **Veri Bütünlüğü** | Network partition (split-brain) durumunda veri kaybı riski | **Safety First** (CAP: CP modeli) |
| **Zehirlenmiş Mesaj Tespiti** | Yok | `x-delivery-count` ile otomatik tespit |
| **Performans** | Çok fazla kuyrukta yavaşlar | Yüksek throughput ve disk optimizasyonu |
| **Tavsiye** | RabbitMQ 4.0'da kaldırıldı | **Tüm yeni sistemlerde zorunlu** |

```java
// Quorum Queue Tanımlama
@Bean
public Queue paymentQuorumQueue() {
    return QueueBuilder.durable("payments.quorum.queue")
            .quorum() // Raft consensus aktif
            .withArgument("x-delivery-limit", 5) // 5 kez hata alırsa otomatik DLX'e at
            .build();
}
```

---

## 7. Kritik Mülakat Soruları (Architect Level)

### Soru 1: RabbitMQ vs Kafka — Hangi mimaride hangisini seçersiniz?
**Cevap (Mimari Karar Matrisi):**
- **RabbitMQ:**
  - İnce elenmiş, karmaşık yönlendirme kuralları (routing/topic bindings) gerekiyorsa.
  - Görev dağıtımı (Task Queue, RPC, Background Jobs) yapılacaksa.
  - Mesaj tüketildiği an kuyruktan silinmeli ve yüksek transactional güvenlik (AMQP ack) gerekiyorsa.
  - Düşük latency (milisaniyenin altında) ve anlık işlem öncelikliyse.
- **Kafka:**
  - Çok yüksek hacimli Event Streaming (100K+ msg/sn), Log toplama, CDC (Debezium) varsa.
  - Mesajların geçmişe dönük saklanması (retention) ve tekrar oynatılması (Event Replay / Event Sourcing) gerekiyorsa.
  - Partition bazlı mutlak sıralama ve Stream Processing (Kafka Streams, Flink) gerekiyorsa.

### Soru 2: RabbitMQ'da "Poison Message" (Zehirli Mesaj) nedir ve nasıl engellenir?
**Cevap:**
Sürekli hata fırlatan ve her `nack(requeue=true)` edildiğinde kuyruğun en başına gelip sistemi sonsuz döngüye sokan mesajdır.
**Çözümler:**
1. **Quorum Queue `x-delivery-limit`:** Belirli deneme (örn: 5) sonrasında otomatik DLQ'ya gönderir.
2. **Spring Retry Interceptor:** `StatefulRetryOperationsInterceptor` ile retry sayacı tutup DLQ'ya `requeue=false` ile yönlendirmek.
3. **Consumer tarafında try-catch:** `BusinessException` durumlarında `requeue=false` yapmak.

### Soru 3: RabbitMQ'da Channel Pool ve Connection yönetimi nasıl olmalıdır?
**Cevap:**
TCP Connection açıp kapatmak çok maliyetlidir. RabbitMQ'da **tek bir uzun ömürlü Connection** açılır ve bunun üzerinde hafif sanal kanallar (**Channel**) oluşturulur.
- Her thread için ayrı bir Channel kullanılmalıdır (Channel thread-safe DEĞİLDİR).
- Spring `CachingConnectionFactory` bu havuzlamayı otomatik ve optimize şekilde yönetir.

### Soru 4 (Tricky): Message Ordering garantisi RabbitMQ'da nasıl sağlanır?
**Cevap:**
RabbitMQ'da bir kuyruk için **tek bir consumer** varsa mesajlar kesinlikle FIFO sırasıyla tüketilir.
**Ancak:**
1. Birden fazla concurrent consumer varsa sıralama bozulabilir (bir iş uzun sürebilir).
2. Bir mesaj `requeue=true` ile kuyruğa geri atılırsa en başa veya farklı sıraya gelebilir.
3. **Çözüm:** Sıralama kritikse tek consumer (veya Single Active Consumer özelliği) kullanılmalı, ya da Partition mantığı sunan Kafka düşünülmelidir.

### Soru 5 (Tricky): "Flow Control" ve "Memory Alarm" durumunda ne olur?
**Cevap:**
RabbitMQ, host üzerindeki RAM kullanımını izler (varsayılan: kullanılabilir RAM'in %40'ı). Bellek eşiği aşılırsa RabbitMQ **tüm Producer bağlantılarını bloklar (TCP socket düzeyinde okumayı durdurur)**. Consumer'lar çalışmaya devam ederek belleği boşaltana kadar hiçbir producer mesaj gönderemez. Bu durum mimaride "Throttling" veya "Circuit Breaker" mekanizmalarını tetiklemelidir.

---

## 8. Geliştirici İpuçları ve Best Practices

- **Kuyruk Sayısı ve Şişmesi:** RabbitMQ kuyrukları in-memory çalışmaya meyillidir. Kuyrukta milyonlarca mesaj birikirse (consumer downtime) disk paging başlar ve performans dramatik düşer. Kuyrukları boş tutmaya çalışın; biriktirme gerekiyorsa Kafka kullanın.
- **Connection Leak:** Manuel AMQP istemcilerinde connection/channel nesnelerini `try-with-resources` ile kapatın.
- **Single Active Consumer:** Bir kuyrukta birden fazla consumer tanımlı olsa bile sadece birinin aktif olmasını (`x-single-active-consumer: true`) sağlayarak hem failover hem de sıralama garantisi elde edebilirsiniz.
- **JSON Serialization:** `Jackson2JsonMessageConverter` kullanarak DTO'ları doğrudan JSON'a çevirin, Java Serialization'dan (`Serializable`) güvenlik ve cross-language uyumluluk nedeniyle kesinlikle kaçının.
- **Queue Arguments Değişmezliği:** Bir kuyruk oluşturulduktan sonra parametreleri (`durable`, `x-dead-letter-exchange` vb.) değiştirilemez. Değiştirmek için kuyruk silinip yeniden oluşturulmalı veya yeni isimle tanımlanmalıdır (`v2`).
