## Konu 17: Spring Cloud Ekosistemi (Gateway, Config, Circuit Breaker, Feign, Eureka, Micrometer)

Spring Cloud, mikroservis mimarilerini destekleyen bir araç setidir. Bir geliştirici olarak, dağıtık sistemlerin zorluklarını (Service Discovery, Configuration Management, Circuit Breaking, Distributed Tracing) nasıl çözeceğinizi bilmelisiniz.

---

### 1. Spring Cloud Nedir?

**Analoji:** Spring Cloud, bir "orkestra şefi" gibidir. Onlarca mikroservis (müzisyen) birbirinden bağımsız çalışır ama orkestra şefi (Spring Cloud), hepsini koordine eder, iletişimi sağlar ve sorunları yönetir.

#### Spring Cloud Bileşenleri

| Bileşen | Açıklama | Kullanım |
| :--- | :--- | :--- |
| **Eureka** | Service Discovery (Servis keşfi) | Mikroservislerin birbirini bulması |
| **Spring Cloud Gateway** | API Gateway | Tek giriş noktası, routing, filtering |
| **Spring Cloud Config** | Merkezi konfigürasyon yönetimi | Tüm servislerin config'i tek yerden |
| **Circuit Breaker (Resilience4j)** | Hata toleransı | Başarısız servislere istek göndermeyi durdurma |
| **OpenFeign** | Deklaratif HTTP Client | Mikroservisler arası iletişim |
| **Micrometer** | Metrics ve Monitoring | Prometheus, Grafana entegrasyonu |

---

### 2. Eureka (Service Discovery)

**Analoji:** Eureka, bir "telefon rehberi" gibidir. Mikroservisler kendilerini kaydeder (register), diğer servisler bu rehberden arama yapar (discover).

#### Neden Gerekli?
Mikroservis mimarilerinde, servisler dinamik olarak ölçeklenir (scale). IP adresleri ve portlar sürekli değişir. Hard-coded URL'ler kullanmak imkansızdır.

#### Eureka Server

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# application.yml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false # Kendini kaydetme
    fetch-registry: false # Registry'yi çekme
```

#### Eureka Client (Mikroservis)

```java
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

```yaml
spring:
  application:
    name: user-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Kullanım:**
```java
@Autowired
private DiscoveryClient discoveryClient;

public void callOrderService() {
    List<ServiceInstance> instances = discoveryClient.getInstances("order-service");
    ServiceInstance instance = instances.get(0);
    String url = instance.getUri() + "/api/orders";
    // HTTP isteği gönder
}
```

---

### 3. Spring Cloud Gateway

**Analoji:** Gateway, bir "resepsiyon" gibidir. Tüm dış istekler önce buraya gelir, sonra doğru servise yönlendirilir. Ayrıca güvenlik kontrolü, rate limiting gibi işlemler burada yapılır.

#### Temel Özellikler
*   **Routing:** İstekleri doğru mikroservise yönlendirir.
*   **Filtering:** İstek/cevap üzerinde manipülasyon (header ekleme, authentication).
*   **Load Balancing:** Aynı servisten birden fazla instance varsa yük dağıtır.
*   **Rate Limiting:** Belirli bir sürede maksimum istek sayısını sınırlar.

#### Konfigürasyon

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service # Eureka'dan load-balanced
          predicates:
            - Path=/api/users/**
          filters:
            - AddRequestHeader=X-Request-Source, Gateway
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/users
        
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - RewritePath=/api/orders/(?<segment>.*), /$\{segment}
```

#### Custom Filter

```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        if (token == null || !validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -1; // En önce çalışsın
    }
}
```

---

### 4. Spring Cloud Config

**Analoji:** Config Server, bir "merkezi kütüphane" gibidir. Tüm mikroservislerin ayarları (database URL, API keys) burada saklanır. Servisler başlarken buradan okur.

#### Config Server

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```yaml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/mycompany/config-repo
          default-label: main
```

**Git Repository Yapısı:**
```
config-repo/
  ├── application.yml (Tüm servisler için ortak)
  ├── user-service.yml
  ├── order-service.yml
  └── user-service-prod.yml (Production profili)
```

#### Config Client

```yaml
spring:
  application:
    name: user-service
  config:
    import: "optional:configserver:http://localhost:8888"
  profiles:
    active: dev
```

**Refresh Endpoint (Runtime'da config değiştirme):**
```java
@RestController
@RefreshScope // Config değişince bean yeniden oluşturulur
public class UserController {
    
    @Value("${app.message}")
    private String message;
    
    @GetMapping("/message")
    public String getMessage() {
        return message;
    }
}
```

Config değiştikten sonra:
```bash
curl -X POST http://localhost:8080/actuator/refresh
```

---

### 5. Circuit Breaker (Resilience4j)

**Analoji:** Circuit Breaker, bir "elektrik sigortası" gibidir. Bir servis sürekli hata veriyorsa (kısa devre), sigorta atar ve o servise istek göndermeyi durdurur. Sistem çökmez.

#### 3 Durum (State)
1.  **CLOSED:** Normal çalışma. İstekler gönderilir.
2.  **OPEN:** Çok fazla hata oldu. İstekler gönderilmez, direkt fallback döner.
3.  **HALF_OPEN:** Belirli süre sonra test istekleri gönderilir. Başarılıysa CLOSED'a döner.

#### Kullanım

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject("http://payment-service/api/pay", request, PaymentResponse.class);
    }
    
    // Fallback metodu
    public PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        return new PaymentResponse("PENDING", "Payment service is down, will retry later");
    }
}
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10 # Son 10 istek
        failure-rate-threshold: 50 # %50 hata oranı
        wait-duration-in-open-state: 10s # 10 saniye OPEN kalır
        permitted-number-of-calls-in-half-open-state: 3
```

---

### 6. OpenFeign (Deklaratif HTTP Client)

**Analoji:** Feign, bir "otomatik telefon çevirici" gibidir. Sadece arayacağınız kişinin adını söylersiniz (interface tanımlarsınız), numarayı çevirme işini (HTTP isteği) Feign halleder.

#### Kullanım

```java
@FeignClient(name = "user-service") // Eureka'dan bulur
public interface UserClient {
    
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
    
    @PostMapping("/api/users")
    UserDTO createUser(@RequestBody UserDTO user);
}

@Service
public class OrderService {
    
    @Autowired
    private UserClient userClient;
    
    public void createOrder(Long userId) {
        UserDTO user = userClient.getUserById(userId);
        // ...
    }
}
```

**Circuit Breaker ile Entegrasyon:**
```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {
    // ...
}

@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserDTO getUserById(Long id) {
        return new UserDTO(id, "Unknown User", "N/A");
    }
}
```

---

### 7. Micrometer (Metrics & Monitoring)

**Analoji:** Micrometer, bir "arabanın gösterge paneli" gibidir. CPU, bellek, istek sayısı, hata oranı gibi metrikleri toplar ve Prometheus, Grafana gibi araçlara gönderir.

#### Kullanım

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Custom Metrics:**
```java
@Service
public class OrderService {
    
    private final Counter orderCounter;
    private final Timer orderTimer;
    
    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .register(registry);
        
        this.orderTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(registry);
    }
    
    public void createOrder() {
        orderTimer.record(() -> {
            // Order işleme
            orderCounter.increment();
        });
    }
}
```

**Prometheus Endpoint:** `http://localhost:8080/actuator/prometheus`

---

### 8. Kritik Mülakat Soruları 

#### Soru 1: Eureka'da Self-Preservation Mode nedir?
**Cevap:** Eureka, ağ sorunları nedeniyle servislerin heartbeat göndermediğini düşünürse, tüm servisleri registry'den silmek yerine "self-preservation" moduna girer ve hiçbir servisi silmez.
*   **Amaç:** Geçici ağ sorunlarında tüm registry'nin boşalmasını önlemek.
*   **Devre Dışı Bırakma:** `eureka.server.enable-self-preservation=false` (Production'da önerilmez).

#### Soru 2: Spring Cloud Gateway ile Zuul farkı nedir?
**Cevap:**
*   **Zuul (1.x):** Servlet tabanlı, blocking I/O. Eski teknoloji.
*   **Spring Cloud Gateway:** WebFlux tabanlı, non-blocking, reactive. Daha performanslı. Spring ekibi tarafından önerilir.

#### Soru 3: Config Server'da şifreli değerler nasıl saklanır?
**Cevap:** `{cipher}` prefix'i ile şifrelenmiş değerler saklanır. Config Server otomatik deşifre eder.

```yaml
spring:
  datasource:
    password: '{cipher}AQA3fG7...' # Şifrelenmiş
```

**Şifreleme:**
```bash
curl http://localhost:8888/encrypt -d "mypassword"
```

#### Soru 4: Circuit Breaker'da Bulkhead Pattern nedir?
**Cevap:** Farklı servislere yapılan istekleri izole eder. Bir servis yavaşsa, diğer servislerin thread pool'unu tüketmez.

```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10
```

#### Soru 5: Distributed Tracing nedir? Spring Cloud Sleuth nasıl çalışır?
**Cevap:** Mikroservisler arası bir isteğin tüm yolculuğunu izlemek için kullanılır. Her istekte benzersiz bir **Trace ID** ve her serviste **Span ID** oluşturulur.

**Spring Cloud Sleuth + Zipkin:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

**Log Örneği:**
```
[user-service,a1b2c3d4,e5f6g7h8] User fetched
[order-service,a1b2c3d4,i9j0k1l2] Order created
```
[user-service,a1b2c3d4,e5f6g7h8] User fetched
[order-service,a1b2c3d4,i9j0k1l2] Order created
```
`a1b2c3d4` = Trace ID (aynı istek), `e5f6g7h8` = Span ID (farklı servis).

#### Soru 6 (Tricky): Client-Side Load Balancing (Ribbon/LoadBalancer) ile Server-Side Load Balancing (Nginx) farkı nedir?
**Cevap:**
*   **Server-Side (Nginx):** İstemci tek bir adresi bilir (Gateway). Yük dağıtımı sunucuda yapılır.
*   **Client-Side (Spring Cloud LoadBalancer):** İstemci (mikroservis), Eureka'dan tüm instance listesini alır ve kendisi birini seçer (Round Robin vb.).
*   **Avantaj:** Tek bir load balancer darboğazı (bottleneck) oluşmaz.

#### Soru 7 (Tricky): Circuit Breaker "Half-Open" durumunda ne yapar?
**Cevap:** Belirli bir süre bekledikten sonra (wait duration), sınırlı sayıda (örn: 3) test isteğinin geçmesine izin verir.
*   Eğer bu istekler başarılıysa -> **CLOSED** (Sistem düzeldi).
*   Eğer başarısızsa -> **OPEN** (Sistem hala bozuk, tekrar bekle).

---

### 9. Geliştirici İpuçları

*   **Eureka Cluster:** Production'da tek Eureka server kullanmayın. En az 3 instance cluster oluşturun (High Availability).

*   **Config Encryption:** Hassas bilgileri (DB şifreleri, API keys) mutlaka şifreleyin. Asla plain text saklamayın.

*   **Gateway Rate Limiting:** Redis tabanlı rate limiting kullanın:
    ```yaml
    spring:
      cloud:
        gateway:
          routes:
            - id: user-service
              filters:
                - name: RequestRateLimiter
                  args:
                    redis-rate-limiter.replenishRate: 10
                    redis-rate-limiter.burstCapacity: 20
    ```

*   **Feign Timeout:** Varsayılan timeout çok kısadır. Mutlaka özelleştirin:
    ```yaml
    feign:
      client:
        config:
          default:
            connectTimeout: 5000
            readTimeout: 10000
    ```

*   **Circuit Breaker Metrics:** Resilience4j metrikleri Prometheus'a export edin. Grafana'da dashboard oluşturun. OPEN state'e geçen circuit breaker'lar için alert kurun.

Bu konu ile **17 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

