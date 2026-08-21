# Spring WebFlux ve Reactive Programming

> **Analoji:** Geleneksel Spring MVC bir "banka kuyruğu" gibidir — her müşteri (request) bir veznedar (thread) tarafından baştan sona işlenir. Müşteri sayısı arttıkça veznedar sayısını artırmak gerekir (thread-per-request). WebFlux ise bir "fast-food sipariş sistemi" gibidir — siparişi verirsiniz, numaranız çağrılır. Sipariş hazırlanırken başkaları da sipariş verebilir (non-blocking).

---

## 1. Reactive Programming Temelleri

### Blocking vs Non-blocking

| Özellik | Spring MVC (Servlet) | Spring WebFlux (Reactive) |
| :--- | :--- | :--- |
| **Model** | Thread-per-request | Event loop |
| **Thread Sayısı** | Yüzlerce (Tomcat default: 200) | Az sayıda (CPU çekirdek kadar) |
| **I/O** | Blocking (thread bekler) | Non-blocking (thread serbest kalır) |
| **Uygun Senaryo** | CPU-intensive işlemler | I/O-intensive (DB, API çağrısı) |
| **Backpressure** | Yok | Var (Reactive Streams) |

### Reactive Streams Standardı

4 temel arayüz:
- **`Publisher<T>`:** Veri yayınlayan (0..N eleman)
- **`Subscriber<T>`:** Veriyi tüketen
- **`Subscription`:** Publisher-Subscriber arası bağlantı (request/cancel)
- **`Processor<T,R>`:** Hem Publisher hem Subscriber

### Project Reactor (Spring WebFlux'ın temeli)

| Tip | Açıklama | Analoji |
| :--- | :--- | :--- |
| **`Mono<T>`** | 0 veya 1 eleman | `Optional<T>` ama asenkron |
| **`Flux<T>`** | 0..N eleman | `Stream<T>` ama asenkron |

---

## 2. WebFlux ile REST API

### Functional Endpoint vs Annotation-based

```java
// Annotation-based (Spring MVC'ye benzer)
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Mono<UserDTO> getUser(@PathVariable String id) {
        return userService.findById(id);  // Mono döner, blocking yok
    }

    @GetMapping
    public Flux<UserDTO> getAllUsers() {
        return userService.findAll();  // Flux döner, streaming
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDTO> createUser(@RequestBody Mono<CreateUserRequest> request) {
        return request.flatMap(userService::create);
    }
}
```

```java
// Functional Endpoint (Router Functions)
@Configuration
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> routes(UserHandler handler) {
        return RouterFunctions.route()
            .GET("/api/users/{id}", handler::getUser)
            .GET("/api/users", handler::getAllUsers)
            .POST("/api/users", handler::createUser)
            .build();
    }
}

@Component
public class UserHandler {
    public Mono<ServerResponse> getUser(ServerRequest request) {
        String id = request.pathVariable("id");
        return userService.findById(id)
            .flatMap(user -> ServerResponse.ok().bodyValue(user))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
}
```

---

## 3. Reactive Data Access

### R2DBC (Reactive Relational Database Connectivity)

```java
// Repository
public interface UserRepository extends ReactiveCrudRepository<User, String> {
    Flux<User> findByStatus(UserStatus status);
    Mono<User> findByEmail(String email);
}

// Service
@Service
public class UserService {
    private final UserRepository userRepository;

    public Mono<User> findById(String id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)));
    }

    public Flux<User> findActiveUsers() {
        return userRepository.findByStatus(UserStatus.ACTIVE)
            .delayElements(Duration.ofMillis(100)); // Backpressure demo
    }
}
```

### Reactive MongoDB

```java
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
    Flux<Product> findByCategory(String category);
    
    @Tailable // MongoDB Capped Collection üzerinde canlı dinleme
    Flux<Product> findWithTailableCursorBy();
}
```

---

## 4. Operatörler (Mono/Flux)

### Sık Kullanılan Operatörler

```java
// map: Senkron dönüşüm (1:1)
Mono<String> upper = Mono.just("mete").map(String::toUpperCase);

// flatMap: Asenkron dönüşüm (1:1, Mono/Flux döner)
Mono<UserDTO> user = userRepo.findById(id)
    .flatMap(u -> orderRepo.findByUserId(u.getId())
        .collectList()
        .map(orders -> new UserDTO(u, orders)));

// zip: İki Mono'yu paralel çalıştır, sonuçları birleştir
Mono<Tuple2<User, Account>> combined = Mono.zip(
    userService.findById(id),
    accountService.findByUserId(id)
);

// switchIfEmpty: Boş sonuçta alternatif
Mono<User> user = cache.findById(id)
    .switchIfEmpty(database.findById(id));

// onErrorResume: Hata durumunda fallback
Mono<User> resilient = userService.findById(id)
    .onErrorResume(ex -> Mono.just(User.defaultUser()));

// retry: Otomatik yeniden deneme
Mono<Response> response = webClient.get()
    .uri("/external-api")
    .retrieve()
    .bodyToMono(Response.class)
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
```

---

## 5. WebClient (Reactive HTTP Client)

`RestTemplate` deprecated! WebClient kullanın:

```java
@Service
public class ExternalApiService {
    private final WebClient webClient;

    public ExternalApiService(WebClient.Builder builder) {
        this.webClient = builder
            .baseUrl("https://api.example.com")
            .defaultHeader("Authorization", "Bearer token")
            .build();
    }

    public Mono<Product> getProduct(String id) {
        return webClient.get()
            .uri("/products/{id}", id)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response ->
                Mono.error(new NotFoundException("Product not found")))
            .bodyToMono(Product.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofMillis(500)));
    }

    public Flux<Product> streamProducts() {
        return webClient.get()
            .uri("/products/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(Product.class);
    }
}
```

---

## 6. Kritik Mülakat Soruları

### Soru 1: Spring MVC mi yoksa WebFlux mı kullanmalıyım?
**Cevap:**
- **MVC:** Blocking I/O yeterli, ekip reactive deneyimsiz, CPU-intensive işlemler, JDBC kullanılıyor
- **WebFlux:** Yüksek eşzamanlılık (10K+ bağlantı), I/O-intensive (çok sayıda API çağrısı), streaming veri, SSE/WebSocket

**Trap:** "WebFlux her zaman daha hızlıdır" **yanlış**! Az sayıda eşzamanlı istek ve CPU-intensive işlemlerde MVC daha performanslıdır.

### Soru 2: Backpressure nedir?
**Cevap:** Consumer, Producer'dan daha yavaş veri tüketiyorsa, Producer'a "yavaşla" sinyali gönderir. Reactive Streams'in `request(n)` mekanizması ile yapılır.
- **Karşılaştırma:** Thread-per-request modelde backpressure yoktur; thread havuzu dolunca istekler kuyrukta bekler veya reddedilir.

### Soru 3: `block()` metodu ne yapar? Neden tehlikelidir?
**Cevap:** Reactive pipeline'ı bloklayarak senkron sonuç döndürür. **WebFlux'ta asla kullanmayın!** Event loop thread'ini bloklar ve tüm sistem kilitlenir.
```java
// ❌ WebFlux'ta YASAKLI
User user = userService.findById(id).block();

// ✅ Doğru - reactive chain devam etsin
return userService.findById(id)
    .map(user -> ResponseEntity.ok(user));
```

### Soru 4 (Tricky): WebFlux thread modeli nasıl çalışır?
**Cevap:** Netty event loop kullanır. CPU çekirdek sayısı kadar thread (varsayılan). Her thread binlerce bağlantıyı yönetir (non-blocking I/O). Bir I/O beklerken thread serbest kalır ve başka isteğe hizmet eder.

### Soru 5 (Tricky): Reactive programlamada debugging neden zordur?
**Cevap:** Stack trace'ler anlamsızdır (operator chain boyunca kaybolur). **Çözümler:**
1. `Hooks.onOperatorDebug()` (geliştirme ortamı)
2. `.checkpoint("açıklayıcı isim")` operatörü
3. `log()` operatörü ile akışı izleme
4. Reactor Tools (Java agent ile assembly trace)

---

## 7. Geliştirici İpuçları

- **Hibrit Mimari:** Aynı projede MVC ve WebFlux birlikte kullanılabilir. Ağır I/O endpoint'leri WebFlux, diğerleri MVC olsun.
- **Testing:** `StepVerifier` kullanın: `StepVerifier.create(flux).expectNext("a", "b").verifyComplete();`
- **JDBC Kullanıyorsanız:** R2DBC'ye geçiş yapın veya `Schedulers.boundedElastic()` ile blocking çağrıları sarın.
- **Timeout Her Yere:** Reactive chain'de `timeout()` kullanmadan asla production'a çıkmayın. Sonsuz bekleme riski var.
- **Cold vs Hot:** `Mono/Flux` varsayılan olarak **cold**'dur (subscribe olana kadar çalışmaz). `share()`, `cache()` ile **hot** yapabilirsiniz.
