# Performans Optimizasyonu (Profiling, JMH, Bottleneck Analizi)

> **Analoji:** Performans optimizasyonu, bir doktora gitmek gibidir. Önce teşhis (profiling), sonra tedavi (optimization). Teşhis koymadan "ilaç" vermek (optimize etmek) genellikle zararlıdır.

---

## 1. Altın Kural: "Ölçmeden Optimize Etme"

> *"Premature optimization is the root of all evil."* — Donald Knuth

### Doğru Sıra

1. **Çalışır Hale Getir** → Doğru çalışan kodu yaz
2. **Ölç** → Darboğazı bul (profile)
3. **Optimize Et** → Sadece darboğazı düzelt
4. **Tekrar Ölç** → İyileşme olduğunu doğrula

---

## 2. JMH (Java Microbenchmark Harness)

Mikro benchmark yazmak için OpenJDK'nın resmi aracı. `System.currentTimeMillis()` ile ölçüm yapmayın!

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class StringConcatBenchmark {

    private List<String> words;

    @Setup
    public void setup() {
        words = List.of("Hello", "World", "Java", "Spring", "Boot");
    }

    @Benchmark
    public String concatenation() {
        String result = "";
        for (String word : words) {
            result += word;  // Her + yeni String nesnesi oluşturur
        }
        return result;
    }

    @Benchmark
    public String stringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word);
        }
        return sb.toString();
    }

    @Benchmark
    public String stringJoin() {
        return String.join("", words);
    }
}
```

**Sonuç:**
```
Benchmark                      Mode  Cnt    Score   Units
concatenation                  avgt   20   152.3 ± 5.2  ns/op
stringBuilder                  avgt   20    38.7 ± 1.1  ns/op
stringJoin                     avgt   20    42.1 ± 1.5  ns/op
```

---

## 3. Profiling Araçları

| Araç | Tür | Avantaj |
| :--- | :--- | :--- |
| **VisualVM** | GUI, sampling/instrumentation | Ücretsiz, kolay |
| **JFR (Flight Recorder)** | Continuous recording | Production-safe, düşük overhead |
| **async-profiler** | Sampling (native) | Çok düşük overhead, flame graph |
| **YourKit** | Commercial profiler | Güçlü, detaylı |
| **IntelliJ Profiler** | IDE entegre | Geliştirme ortamı için ideal |

### JFR Kullanımı

```bash
# Recording başlat
java -XX:StartFlightRecording=duration=60s,filename=app.jfr -jar app.jar

# veya çalışan JVM'e bağlan
jcmd <PID> JFR.start duration=60s filename=app.jfr

# Analiz: JDK Mission Control (JMC) ile aç
```

---

## 4. Yaygın Darboğazlar ve Çözümleri

### CPU Darboğazları

| Problem | Belirti | Çözüm |
| :--- | :--- | :--- |
| Hot loop | CPU %100 | Algoritma optimizasyonu, caching |
| Regex derlemesi | Her çağrıda yeniden compile | `Pattern.compile()` cache'le |
| XML/JSON parse | Yoğun serialization | Jackson streaming, Protocol Buffers |
| Sorting | Büyük liste sıralaması | Veritabanında sırala, limit kullan |

### Memory Darboğazları

| Problem | Belirti | Çözüm |
| :--- | :--- | :--- |
| Büyük koleksiyonlar | Heap dolması | Pagination, lazy loading |
| String duplicate | Bellek israfı | `String.intern()`, `UseStringDeduplication` |
| Autoboxing | GC pressure | Primitive streams, `int[]` yerine `List<Integer>` |
| Buffer kopyalama | Gereksiz bellek | Direct ByteBuffer, zero-copy |

### I/O Darboğazları

| Problem | Belirti | Çözüm |
| :--- | :--- | :--- |
| N+1 sorgusu | Çok sayıda DB çağrısı | JOIN, EntityGraph, batch fetching |
| Büyük result set | Yavaş sorgu | Pagination, projeksiyon, index |
| Sync HTTP çağrısı | Thread bloklanması | WebClient (async), CompletableFuture |
| Disk I/O | Yavaş dosya okuma | BufferedReader, Memory-mapped files |

---

## 5. Connection Pool Optimizasyonu (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20     # max_connections / (instance sayısı)
      connection-timeout: 30000  # 30 saniye
      idle-timeout: 600000       # 10 dakika
      max-lifetime: 1800000      # 30 dakika
      leak-detection-threshold: 60000  # 60 saniye (connection leak tespiti)
```

### Pool Boyutu Formülü

```
Optimal Pool Size = (Core Sayısı * 2) + Disk Spindle Sayısı
```

Örnek: 4 core, SSD → Pool Size = (4 * 2) + 1 = ~10

---

## 6. Caching ile Performans

```java
@Service
public class ProductService {

    // L1 Cache: Application-level (Caffeine)
    private final Cache<String, Product> localCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build();

    // L2 Cache: Distributed (Redis)
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Product findById(String id) {
        return localCache.get(id, key -> productRepository.findById(key).orElse(null));
    }
}
```

---

## 7. Kritik Mülakat Soruları

### Soru 1: Uygulamanız yavaş. Nereden başlarsınız?
**Cevap:** Aşama aşama:
1. **Metrics:** Response time, throughput, error rate bakın (Prometheus/Grafana)
2. **APM:** Hangi endpoint yavaş? (Spring Boot Actuator, Micrometer)
3. **Tracing:** Yavaş endpoint'te hangi adım (DB, API, logic) darboğaz? (Zipkin/Jaeger)
4. **Profiling:** CPU mu? Memory mi? I/O mu? (JFR, async-profiler)
5. **Fix & Measure:** Düzelt ve mutlaka tekrar ölç

### Soru 2: N+1 sorgu problemi nedir?
**Cevap:** 1 ana sorgu + N adet lazy-loaded ilişki sorgusu. Örneğin 100 Order çekerken her birinin Customer'ını ayrı sorguyla getirmek = 101 sorgu.
**Çözüm:** `@EntityGraph`, `JOIN FETCH`, `@BatchSize`

### Soru 3 (Tricky): JIT Compilation nasıl performans artırır?
**Cevap:** JVM, sık çağrılan metodları (hot methods) runtime'da native makine koduna derler. İlk çağrılar yorumlayıcı (interpreter) ile yavaş, sonraki çağrılar native hızında. Bu yüzden JMH'da **warmup** iterasyonları gereklidir.

---

## 8. Geliştirici İpuçları

- **Flame Graph:** async-profiler ile flame graph oluşturun. CPU zamanının nereye harcandığını görsel olarak görün.
- **Lazy Initialization:** Pahalı nesneleri (DB connection, HTTP client) startup'ta değil, ilk kullanımda oluşturun.
- **Virtual Threads (Java 21):** I/O-bound işlemlerde platform thread yerine virtual thread kullanın. Thread havuzu yönetimi gereksizleşir.
- **Batch Processing:** Tek tek DB insert yerine `saveAll()` ile batch insert yapın. Hibernate `batch_size` ayarını açın.
- **Asenkron İşleme:** Kullanıcıyı bekletmeyin. Email gönderme, rapor oluşturma gibi işleri `@Async` veya mesaj kuyruğuyla arka plana alın.
