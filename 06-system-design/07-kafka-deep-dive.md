# Apache Kafka — Derinlemesine Rehber

> **Analoji:** Kafka, bir "devasa commit log" gibidir. Bir gazete arşivi düşünün: her gün yeni bir sayı basılır, eski sayılar silinmez, herkes istediği sayıdan okumaya başlayabilir, birden fazla abone aynı anda farklı sayfaları okuyabilir. Kafka da veriyi bu şekilde sıralı, kalıcı ve paralel okunabilir şekilde saklar.

---

## 1. Kafka Mimarisi

### Temel Bileşenler

```
┌────────── Kafka Cluster ──────────────────────────────────────┐
│                                                                │
│  ┌─── Broker 1 ───┐  ┌─── Broker 2 ───┐  ┌─── Broker 3 ───┐ │
│  │  Topic-A P0 (L) │  │  Topic-A P1 (L) │  │  Topic-A P2 (L) │ │
│  │  Topic-A P1 (R) │  │  Topic-A P2 (R) │  │  Topic-A P0 (R) │ │
│  │  Topic-B P0 (L) │  │  Topic-B P1 (L) │  │  Topic-B P0 (R) │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                │
│  (L) = Leader Replica   (R) = Follower Replica                │
│                                                                │
│  ┌─── Controller ──────────────────────────────────────────┐   │
│  │  KRaft (Kafka 3.x+) veya ZooKeeper (legacy)            │   │
│  │  Broker liderlik seçimi, metadata yönetimi              │   │
│  └─────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
         ▲                           │
    Producer'lar                Consumer'lar
  (veri yazar)               (veri okur)
```

### Kavram Haritası

| Kavram | Açıklama | Analoji |
| :--- | :--- | :--- |
| **Broker** | Kafka sunucu instance'ı | Posta şubesi |
| **Topic** | Mesajların kategorize edildiği kanal | Gazete adı (Hürriyet, Milliyet) |
| **Partition** | Topic'in paralel bölümü | Gazetenin farklı sayfaları |
| **Offset** | Partition içindeki mesaj sıra numarası | Gazete sayfa numarası |
| **Producer** | Mesaj üreten uygulama | Gazeteci (haber yazar) |
| **Consumer** | Mesaj tüketen uygulama | Okuyucu (gazete okur) |
| **Consumer Group** | Birlikte çalışan consumer kümesi | Haber bürosu ekibi |
| **Replica** | Partition'ın kopyası (fault tolerance) | Gazetenin yedek baskısı |
| **Leader** | Okuma/yazma yapılan asıl replica | Asıl baskı |
| **Follower** | Leader'dan veri kopyalayan replica | Yedek baskı |

---

## 2. Topic ve Partition Detayları

### Partition Neden Gerekli?

```
Topic: order-events (3 partition)

  Partition 0: [msg0, msg3, msg6, msg9, ...]   → Consumer A okur
  Partition 1: [msg1, msg4, msg7, msg10, ...]   → Consumer B okur  
  Partition 2: [msg2, msg5, msg8, msg11, ...]   → Consumer C okur

→ 3 consumer paralel çalışır → throughput 3 katına çıkar
```

**Partition sayısı belirleme:**
- Hedef throughput / tek partition throughput = minimum partition sayısı
- Örnek: 100K msg/sn hedef, tek partition 10K msg/sn → en az 10 partition
- **Tavsiye:** Başlangıçta 6-12 partition. Sonra artırılabilir ama **azaltılamaz!**

### Partition Key ve Ordering

```java
// Key ile gönderim → Aynı key hep aynı partition'a gider
producer.send(new ProducerRecord<>("orders", orderId, orderEvent));

// Key: orderId="ORD-42" → hash(ORD-42) % partitionCount = Partition 1
// ORD-42'nin tüm event'leri Partition 1'de, sıralı olarak saklanır
```

**Ordering Garantisi:**
- ✅ **Partition içinde** mesaj sırası garanti
- ❌ **Partition'lar arasında** global sıralama garanti **DEĞİL**
- İlgili mesajları aynı partition'a yönlendirmek için **aynı key** kullanın

### Retention ve Compaction

| Strateji | Açıklama | Kullanım |
| :--- | :--- | :--- |
| **Time-based** | `retention.ms=604800000` (7 gün) | Event streaming, log |
| **Size-based** | `retention.bytes=1073741824` (1 GB) | Disk yönetimi |
| **Log Compaction** | Her key için sadece son değeri sakla | Changelog, state store |

```
Log Compaction Örneği:
  Offset 0: user-42 → { name: "Mete", city: "Ankara" }
  Offset 1: user-42 → { name: "Mete", city: "İstanbul" }  ← SADECE BU KALIR
  Offset 2: user-99 → { name: "Ali", city: "İzmir" }

Compaction sonrası:
  user-42 → { name: "Mete", city: "İstanbul" }
  user-99 → { name: "Ali", city: "İzmir" }
```

---

## 3. Producer Detayları

### Acknowledgment (acks) Stratejisi

| acks | Açıklama | Güvenlik | Performans |
| :--- | :--- | :--- | :--- |
| `acks=0` | Fire-and-forget, cevap bekleme | ❌ Veri kaybı riski yüksek | 🚀 En hızlı |
| `acks=1` | Leader yazınca onayla | ⚠️ Leader çökerse kayıp | ⚡ Hızlı |
| `acks=all` | Tüm in-sync replica'lar yazınca onayla | ✅ Güvenli | 🐢 En yavaş |

**Production tavsiyesi:** `acks=all` + `min.insync.replicas=2` + `replication.factor=3`

### Idempotent Producer

```java
Properties props = new Properties();
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // Kafka 0.11+
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

// Aynı mesaj ağ hatası nedeniyle iki kez gönderilirse,
// Kafka duplicate'ı tespit eder ve reddeder.
// Producer ID + Sequence Number ile izlenir.
```

### Transactional Producer (Exactly-Once)

```java
producer.initTransactions();

try {
    producer.beginTransaction();
    producer.send(new ProducerRecord<>("topic-a", key, value1));
    producer.send(new ProducerRecord<>("topic-b", key, value2));
    producer.commitTransaction();
} catch (ProducerFencedException | OutOfOrderSequenceException e) {
    producer.close(); // Fatal, yeniden oluşturulmalı
} catch (KafkaException e) {
    producer.abortTransaction(); // Geri al
}
```

### Batching ve Compression

```java
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);        // 32 KB batch
props.put(ProducerConfig.LINGER_MS_CONFIG, 20);             // 20 ms bekle, batch doldur
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");   // lz4, snappy, gzip, zstd
props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);   // 64 MB buffer
```

**Compression Karşılaştırma:**

| Algoritma | Sıkıştırma Oranı | Hız | Tavsiye |
| :--- | :--- | :--- | :--- |
| `lz4` | Orta | Çok Hızlı | **Genel kullanım için ideal** |
| `snappy` | Orta | Hızlı | Real-time düşük latency |
| `zstd` | Yüksek | Orta | Disk tasarrufu öncelikli |
| `gzip` | En Yüksek | Yavaş | Bandwidth kısıtlı ortamlar |

---

## 4. Consumer Detayları

### Consumer Group Mekanizması

```
Topic: payments (4 partition)

Consumer Group: payment-processor
  Consumer A → Partition 0, Partition 1
  Consumer B → Partition 2, Partition 3

Consumer Group: audit-logger  (bağımsız, aynı veriyi tekrar okur)
  Consumer X → Partition 0, Partition 1, Partition 2, Partition 3
```

**Kurallar:**
- Bir partition, aynı group içinde **sadece bir** consumer'a atanır
- Consumer sayısı > partition sayısı ise fazla consumer'lar **idle** kalır
- Farklı group'lar aynı topic'i bağımsız okur (pub/sub modeli)

### Rebalancing

Consumer eklenir/çıkarsa veya çökerse partition'lar yeniden dağıtılır:

| Strateji | Açıklama | Avantaj | Dezavantaj |
| :--- | :--- | :--- | :--- |
| **Eager (Range/RoundRobin)** | Tüm partition'ları durdur, yeniden ata | Basit | **Stop-the-world**, tüm consumer'lar durur |
| **Cooperative Sticky** | Sadece etkilenen partition'ları taşı | İncremental, kesintisiz | Biraz daha karmaşık |
| **Static Membership** | `group.instance.id` ile sabit atama | Gereksiz rebalance'ı önler | Manuel yönetim |

```java
props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
    CooperativeStickyAssignor.class.getName()); // Kafka 2.4+

// Static membership (rolling restart'larda rebalance'ı önler)
props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "consumer-pod-1");
```

### Offset Yönetimi

```java
// Otomatik commit (varsayılan, riskli)
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);

// Manuel commit (güvenli, tavsiye edilen)
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record); // İşle
    }
    consumer.commitSync(); // Başarılı işlem sonrası commit
}
```

**Auto Commit Tehlikesi:**
- Mesaj işlenmeden commit edilebilir → veri kaybı
- Mesaj işlenip commit edilemeden crash → tekrar işleme (at-least-once)

### Consumer Lag

```
Lag = Latest Offset (Producer'ın yazdığı son offset) - Consumer Offset (Consumer'ın okuduğu offset)

Partition 0: Latest=1000, Consumer=950 → Lag = 50
Partition 1: Latest=1200, Consumer=800 → Lag = 400 ⚠️ (Consumer yavaş!)
```

**İzleme:** Kafka Lag Exporter + Prometheus + Grafana ile alarm kurulmalı.

---

## 5. Spring Boot ile Kafka

### Producer

```java
@Configuration
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, OrderEvent> producerFactory() {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
            ProducerConfig.ACKS_CONFIG, "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

@Service
public class OrderEventPublisher {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        kafkaTemplate.send("order-events", event.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka gönderim başarısız: {}", ex.getMessage());
                } else {
                    log.info("Gönderildi: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
```

### Consumer

```java
@Service
public class OrderEventConsumer {

    @KafkaListener(
        topics = "order-events",
        groupId = "order-processor",
        concurrency = "3"  // 3 consumer thread
    )
    public void consume(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        
        try {
            log.info("Alındı: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);
            
            orderService.processOrder(event);
            
            ack.acknowledge(); // Manuel commit
        } catch (Exception e) {
            log.error("İşlem başarısız: {}", e.getMessage());
            // DLQ'ya yönlendir veya retry
        }
    }
}
```

### Error Handling & Retry

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
    
    // Retry: 3 kez dene, 1 saniye arayla
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate), // DLQ'ya gönder
        new FixedBackOff(1000L, 3L)  // 3 retry, 1s interval
    ));
    
    return factory;
}
```

---

## 6. Kafka Streams

> **Analoji:** Kafka Streams, Kafka'nın üzerine kurulmuş bir "gerçek zamanlı veri işleme motoru"dur. Veriyi topic'ten okur, işler ve başka bir topic'e yazar — hepsi stream olarak.

### KStream vs KTable

| Tip | Açıklama | Analoji |
| :--- | :--- | :--- |
| **KStream** | Sonsuz olay akışı (her kayıt bağımsız) | Akan nehir (her damla bir olay) |
| **KTable** | Son değeri tutan tablo (upsert) | Veritabanı tablosu (son durum) |
| **GlobalKTable** | Tüm partition'ları tek consumer'da tutan KTable | Lookup tablosu |

```java
StreamsBuilder builder = new StreamsBuilder();

// KStream: Her sipariş olayını oku
KStream<String, OrderEvent> orders = builder.stream("order-events");

// Filtreleme ve dönüştürme
KStream<String, OrderEvent> confirmedOrders = orders
    .filter((key, order) -> order.getStatus() == OrderStatus.CONFIRMED)
    .mapValues(order -> enrichOrder(order));

// KTable: Müşteri bazlı toplam harcama (aggregation)
KTable<String, BigDecimal> customerSpending = confirmedOrders
    .groupBy((key, order) -> order.getCustomerId())
    .aggregate(
        () -> BigDecimal.ZERO,
        (customerId, order, total) -> total.add(order.getTotal()),
        Materialized.as("customer-spending-store")
    );

// Sonucu başka bir topic'e yaz
confirmedOrders.to("confirmed-orders");
customerSpending.toStream().to("customer-spending");
```

### Windowed Aggregation

```java
// Son 5 dakikadaki sipariş sayısı (tumbling window)
KTable<Windowed<String>, Long> orderCounts = orders
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count(Materialized.as("order-count-store"));
```

---

## 7. Kafka Connect

Harici sistemlerle (DB, Elasticsearch, S3) entegrasyon. Kod yazmadan veri akışı sağlar.

```json
// Debezium PostgreSQL Source Connector
{
  "name": "postgres-source",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.dbname": "orderdb",
    "database.user": "debezium",
    "table.include.list": "public.orders",
    "topic.prefix": "cdc",
    "plugin.name": "pgoutput"
  }
}
// → orders tablosundaki her değişiklik "cdc.public.orders" topic'ine yazılır
```

### Yaygın Connector'lar

| Connector | Yön | Kullanım |
| :--- | :--- | :--- |
| **Debezium** | Source | CDC (Change Data Capture) — DB → Kafka |
| **JDBC Sink** | Sink | Kafka → DB |
| **Elasticsearch Sink** | Sink | Kafka → Elasticsearch |
| **S3 Sink** | Sink | Kafka → AWS S3 (archival) |
| **MongoDB Source/Sink** | Her ikisi | MongoDB ↔ Kafka |

---

## 8. Production Best Practices

### Cluster Sizing

| Parametre | Tavsiye |
| :--- | :--- |
| **Broker sayısı** | Minimum 3 (replication factor 3 için) |
| **Replication factor** | 3 (2 broker çökse bile veri kaybolmaz) |
| **min.insync.replicas** | 2 (acks=all ile birlikte) |
| **Disk** | SSD tercih, JBOD veya RAID-10 |
| **JVM Heap** | 6-8 GB (fazlası zararlı, GC baskısı) |
| **OS Page Cache** | Mümkün olduğunca fazla RAM |

### Monitoring Metrikleri

| Metrik | Alarm Eşiği | Açıklama |
| :--- | :--- | :--- |
| **Consumer Lag** | > 10,000 | Consumer yetişemiyor |
| **Under Replicated Partitions** | > 0 | Replica senkron değil |
| **ISR Shrink Rate** | > 0/dk | Broker'lar senkrondan çıkıyor |
| **Request Queue Size** | > 100 | Broker aşırı yüklü |
| **Disk Usage** | > %80 | Retention veya compaction gerekebilir |
| **Network I/O** | NIC kapasitesinin %70'i | Bandwidth darboğazı |

---

## 9. Kritik Mülakat Soruları

### Soru 1: Kafka neden bu kadar hızlı?
**Cevap:** 5 temel neden:
1. **Sequential I/O:** Disk'e sıralı yazma, random write'tan 6000x hızlı
2. **Zero-Copy:** `sendfile()` system call ile kernel → network buffer doğrudan transfer
3. **Page Cache:** OS seviyesinde caching, JVM heap kullanmaz
4. **Batching:** Mesajlar batch halinde gönderilir/yazılır
5. **Partitioning:** Paralel okuma/yazma

### Soru 2: Exactly-once semantics gerçekten mümkün mü?
**Cevap:** Kafka içinde **evet** (Kafka 0.11+):
- **Idempotent Producer:** Duplicate mesajı engeller
- **Transactional Producer:** Birden fazla topic/partition'a atomik yazma
- **read_committed Consumer:** Sadece commit edilmiş mesajları okur
- **Ancak:** End-to-end exactly-once (Kafka → external system) için consumer tarafında idempotency **ek olarak** gereklidir.

### Soru 3: Kafka'da mesaj sırası nasıl garanti edilir?
**Cevap:**
- **Partition içinde:** Doğal olarak garanti. Mesajlar offset sırasıyla okunur.
- **Partition'lar arasında:** Garanti **yoktur**.
- **Çözüm:** İlişkili mesajları aynı partition key ile gönderin. Örneğin: `orderId` key yapın → aynı siparişin tüm event'leri aynı partition'da sıralı kalır.

### Soru 4: Consumer Group'ta bir consumer çökerse ne olur?
**Cevap:**
1. `session.timeout.ms` (default 45s) boyunca heartbeat gelmezse consumer "dead" sayılır
2. Group Coordinator **rebalance** tetikler
3. Çöken consumer'ın partition'ları diğer consumer'lara atanır
4. **Sorun:** Son commit edilen offset'ten itibaren yeniden okunur → at-least-once delivery (tekrar işleme olabilir)

### Soru 5: Kafka ile RabbitMQ ne zaman hangisi seçilir?
**Cevap:**

| Kriter | Kafka | RabbitMQ |
| :--- | :--- | :--- |
| **Throughput** | Çok yüksek (milyon msg/sn) | Orta (onbinler msg/sn) |
| **Retention** | Günlerce/haftalarca saklar | Tüketilince silinir |
| **Ordering** | Partition bazlı garanti | Queue bazlı garanti |
| **Replay** | ✅ Geçmiş event'leri tekrar okuyabilir | ❌ Tüketilen mesaj gider |
| **Routing** | Topic + partition | Exchange + binding (esnek routing) |
| **Protokol** | Kafka Protocol (binary) | AMQP, MQTT, STOMP |
| **Use Case** | Event streaming, CDC, log aggregation | Task queue, RPC, request-reply |

### Soru 6 (Tricky): Partition sayısı sonradan artırılırsa ne olur?
**Cevap:** Mevcut mesajlar yerinde kalır, yeni mesajlar yeni partition'lara dağılır. **ANCAK:** Key-based partitioning kullanılıyorsa, aynı key farklı bir partition'a gidebilir → sıralama bozulur. **Tavsiye:** Başlangıçta yeterli partition belirleyin. Artırma yapılacaksa consumer'ları da güncelleyin.

### Soru 7 (Tricky): Kafka'da mesaj boyutu limiti nedir?
**Cevap:** Varsayılan `max.message.bytes = 1MB`. Artırılabilir ama **tavsiye edilmez**. Büyük mesajlar:
- Broker belleğini şişirir
- Network throughput'u düşürür
- GC pressure oluşturur
- **Çözüm:** Büyük payload'ı S3'e yükleyin, Kafka'ya sadece referansı (URL) gönderin — "Claim Check Pattern".

### Soru 8 (Tricky): KRaft nedir? ZooKeeper neden kaldırıldı?
**Cevap:** KRaft (Kafka Raft), Kafka 3.3+'te ZooKeeper'ı tamamen ortadan kaldırır:
- ZooKeeper ayrı bir cluster yönetimi gerektiriyordu (operasyonel yük)
- KRaft, controller quorum'u Kafka broker'ları içinde çalıştırır
- Metadata yönetimi daha hızlı ve basit
- Kafka 4.0'da ZooKeeper desteği tamamen kaldırıldı

---

## 10. Geliştirici İpuçları

- **Schema Registry:** Avro/Protobuf ile şema versiyonlama yapın. JSON kullanıyorsanız en azından JSON Schema ile validate edin. Confluent Schema Registry veya Apicurio kullanın.
- **Consumer Idempotency:** Her mesajı idempotent işleyin. `eventId`'yi DB'de unique index ile saklayın, tekrar gelirse skip edin.
- **Tombstone Message:** Key'e karşılık `null` value göndermek, log compaction'da o key'in silinmesini sağlar.
- **Headers:** Mesaj metadata'sı (correlationId, traceId, eventType) için Kafka headers kullanın, payload'ı kirletmeyin.
- **Testcontainers:** Integration test'lerde `@Testcontainers` + `KafkaContainer` ile gerçek Kafka kullanın. Embedded Kafka yerine tercih edin.
- **Backpressure:** Consumer yetişemiyorsa `max.poll.records`'ı düşürün (varsayılan 500). İşleme süresi `max.poll.interval.ms`'yi (varsayılan 5 dk) aşmasın, yoksa rebalance tetiklenir!
- **Dead Letter Topic (DLT):** İşlenemeyen mesajları `<topic>.DLT`'ye yönlendirin. Sonra analiz edip manuel/otomatik retry yapın.
