## Konu 12: Spring Framework Core & Spring Boot

Spring Framework, Java dünyasının de facto standardıdır. Bir geliştirici olarak, Spring'in "sihir" gibi görünen kısımlarının (IoC, AOP, Proxy) altında yatan mekanizmaları derinlemesine anlamanız ve Spring Boot'un bu mekanizmaları nasıl otomatize ettiğini bilmeniz gerekir.

---

### 1. IoC (Inversion of Control) ve DI (Dependency Injection)

**Analoji:**
*   **Geleneksel Yöntem:** Bir pizzacıya gidip "Bana pizza yap" dersiniz ama malzemeleri (un, peynir, domates) siz alıp pizzacıya verirsiniz. Kontrol sizdedir.
*   **IoC (Hollywood Prensibi):** Pizzacıya sipariş verirsiniz. Pizzacı malzemeleri nereden alacağını, nasıl yapacağını bilir. Size sadece pizzayı verir. "Bizi arama, biz seni ararız."

#### IoC Container (ApplicationContext)
Spring'in kalbidir. Nesnelerin (Bean) oluşturulmasından, yaşam döngüsünden ve bağımlılıklarının enjekte edilmesinden sorumludur.
*   **BeanFactory:** En temel container. Lazy loading yapar.
*   **ApplicationContext:** BeanFactory'nin gelişmiş halidir (AOP, i18n, Event publication destekler). Eager loading yapar (ayağa kalkarken tüm singleton beanleri oluşturur).

#### Dependency Injection (DI) Türleri

1.  **Constructor Injection (Önerilen):**
    *   Zorunlu bağımlılıklar için kullanılır.
    *   Nesne oluşturulduğunda tüm bağımlılıkların hazır olduğunu garanti eder.
    *   `final` field'lar kullanılabilir (Immutability).
    *   Test etmesi kolaydır (Mock'lar constructor'dan geçilebilir).

    ```java
    @Service
    public class UserService {
        private final UserRepository userRepository;

        // @Autowired (Spring 4.3+ ile tek constructor varsa opsiyonel)
        public UserService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }
    }
    ```

2.  **Setter Injection:**
    *   Opsiyonel bağımlılıklar için kullanılır.
    *   Bağımlılıklar sonradan değiştirilebilir.

3.  **Field Injection (Önerilmez):**
    *   `@Autowired private UserRepository userRepository;`
    *   Kodu Spring'e sıkı sıkıya bağlar.
    *   Test etmesi zordur (Reflection gerekir).
    *   NullPointerException riski vardır.

---

### 2. Spring Bean Scopes

Spring container'ın bean'leri nasıl yönettiğini belirler.

| Scope | Açıklama |
| :--- | :--- |
| **Singleton** (Default) | Container başına **tek bir** instance oluşturulur. Stateless olmalıdır. |
| **Prototype** | Her istekte **yeni bir** instance oluşturulur. Stateful beanler için. |
| **Request** (Web) | Her HTTP isteği için bir instance. |
| **Session** (Web) | Her HTTP oturumu (session) için bir instance. |
| **Application** (Web) | ServletContext başına bir instance. |
| **WebSocket** (Web) | WebSocket yaşam döngüsü boyunca. |

**Dikkat:** Singleton bir bean içine Prototype bir bean enjekte ederseniz, Prototype bean **sadece bir kez** (Singleton oluşturulurken) enjekte edilir. Her çağrıda yeni prototype gelmez! Çözüm: `ObjectProvider<PrototypeBean>` veya `@Lookup` anotasyonu.

---

### 3. Spring AOP (Aspect Oriented Programming)

**Analoji:** Bir banka uygulamasında para transferi, kredi başvurusu, hesap açma gibi farklı modüller vardır. Ama hepsinde ortak olan "Loglama", "Güvenlik" ve "Transaction" işlemleri vardır. Bu ortak işleri (Cross-cutting concerns) her metoda kopyala-yapıştır yapmak yerine, ayrı bir katmanda (Aspect) toplayıp, çalışma zamanında kodun arasına "dokumaktır" (weaving).

#### Temel Kavramlar
*   **Aspect:** Ortak işin kendisi (örn: LoggingAspect).
*   **Advice:** Ne zaman çalışacağı (Before, After, Around, AfterThrowing).
*   **Pointcut:** Hangi metodlarda çalışacağı (Regex benzeri ifade).
*   **JoinPoint:** Aspect'in uygulandığı an (Metot çağrısı).

#### Proxy Pattern
Spring AOP, **Proxy Pattern** kullanır. Siz bir Service'i çağırdığınızda aslında Spring'in oluşturduğu bir Proxy nesnesini çağırırsınız.
1.  **JDK Dynamic Proxy:** Eğer sınıf bir Interface implement ediyorsa kullanılır.
2.  **CGLIB:** Interface yoksa, sınıfın alt sınıfını (subclass) oluşturarak proxy yapar.

```java
@Aspect
@Component
public class LoggingAspect {
    
    // com.example.service paketindeki tüm metodlar
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("Metot çağrılıyor: " + joinPoint.getSignature().getName());
    }
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // Gerçek metodu çalıştır
        long end = System.currentTimeMillis();
        System.out.println("Süre: " + (end - start) + "ms");
        return result;
    }
}
```

---

### 4. Spring MVC ve Annotations

#### Spring MVC Mimarisi (Request Lifecycle)
1.  **Request:** İstemciden gelir.
2.  **DispatcherServlet (Front Controller):** Tüm istekleri karşılar.
3.  **HandlerMapping:** İsteğin hangi Controller'a gideceğini belirler.
4.  **Controller:** İş mantığını çalıştırır (Service'i çağırır).
5.  **ViewResolver:** (Eğer JSP/Thymeleaf kullanılıyorsa) Hangi view'ın döneceğini belirler. REST API'lerde (JSON) bu adım atlanır (`@ResponseBody`).
6.  **Response:** İstemciye döner.

#### Önemli Anotasyonlar

**Stereotype Annotations (Bean Tanımlama):**
*   **`@Component`:** Genel amaçlı bean.
*   **`@Service`:** İş mantığı katmanı.
*   **`@Repository`:** Veri erişim katmanı (Database exception'larını Spring exception'larına çevirir).
*   **`@Controller`:** MVC Controller.
*   **`@RestController`:** `@Controller` + `@ResponseBody`. JSON/XML döner.

**Configuration & Injection:**
*   **`@Configuration`:** Bean tanımları içeren sınıf.
*   **`@Bean`:** Metot seviyesinde bean tanımı (3. parti kütüphaneler için).
*   **`@Autowired`:** Bağımlılık enjeksiyonu.
*   **`@Qualifier`:** Aynı türden birden fazla bean varsa hangisinin seçileceğini belirtir.
*   **`@Primary`:** Varsayılan bean'i belirler.
*   **`@Value`:** `application.properties`'den değer okur.

**Spring Boot:**
*   **`@SpringBootApplication`:** `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: Circular Dependency (Döngüsel Bağımlılık) nedir? Spring nasıl yönetir?
**Cevap:** A Bean'i B'ye, B Bean'i A'ya ihtiyaç duyarsa oluşur.
*   **Constructor Injection** kullanıyorsanız uygulama **başlatılamaz** (`BeanCurrentlyInCreationException`).
*   **Setter/Field Injection** kullanıyorsanız Spring bunu çözebilir (önce beanleri oluşturur, sonra set eder).
*   **Çözüm:** Tasarımı düzeltmek (ortak kodu üçüncü bir servise taşımak) veya `@Lazy` anotasyonu kullanmak (biri diğerini ihtiyaç duyduğunda yüklesin).

#### Soru 2: Spring Bean'ler Thread-Safe midir?
**Cevap:** Varsayılan olarak **HAYIR**. Spring Bean'leri Singleton'dır (tek instance). Eğer bean içinde durum (state) tutan bir instance variable varsa (örn: `private int counter;`) ve birden fazla thread bu değişkeni değiştirirse **Race Condition** oluşur.
*   **Çözüm:** Beanleri **Stateless** tasarlayın. Durum tutmanız gerekiyorsa `ThreadLocal` kullanın veya Scope'u `Prototype`/`Request` yapın.

#### Soru 3: `@Transactional` anotasyonu nasıl çalışır? Aynı sınıf içindeki metod çağrısında neden çalışmaz?
**Cevap:** AOP Proxy mekanizması ile çalışır. Transaction yönetimi için metodun başında transaction açan, sonunda commit/rollback yapan bir proxy kodu çalışır.
*   **Sorun:** `methodA()` (transactional olmayan), aynı sınıf içindeki `methodB()` (transactional) metodunu çağırırsa (`this.methodB()`), çağrı proxy üzerinden değil, doğrudan nesne üzerinden (this) yapılır. Bu yüzden AOP devreye girmez ve transaction çalışmaz.
*   **Çözüm:** `methodB`'yi başka bir servise taşıyın veya self-injection kullanın.

#### Soru 4: `BeanFactory` ile `ApplicationContext` farkı nedir?
**Cevap:**
*   **BeanFactory:** Temel IoC container. Bean'leri **Lazy** (istendiğinde) yükler. Hafif kaynaklıdır (Mobil/Applet).
*   **ApplicationContext:** BeanFactory'yi extend eder. Bean'leri **Eager** (başlangıçta) yükler. AOP, Event Handling, i18n, Annotation desteği gibi kurumsal özellikler sunar. Spring Boot varsayılan olarak bunu kullanır.

#### Soru 5: Spring Boot'ta "Auto Configuration" nasıl çalışır?
**Cevap:** Classpath'teki kütüphanelere (JAR) ve tanımlı bean'lere bakarak uygulamanın ihtiyacı olan bean'leri otomatik oluşturur.
*   Örn: Classpath'te `H2` driver varsa ve `DataSource` bean'i tanımlanmamışsa, Spring Boot otomatik olarak bir in-memory H2 veritabanı yapılandırır.
*   `@ConditionalOnClass`, `@ConditionalOnMissingBean` gibi anotasyonlarla yönetilir.

#### Soru 6 (Tricky): Singleton bir Bean içine Prototype bir Bean inject edersek ne olur?
**Cevap:** Prototype bean **sadece bir kez** (Singleton oluşturulurken) inject edilir. Yani pratikte Singleton gibi davranır!
*   **Çözüm:** Her seferinde yeni instance almak için `ObjectProvider<PrototypeBean>` veya `@Lookup` anotasyonu kullanılmalıdır.
*   **Trap:** "Prototype her zaman yeni instance verir" kuralı injection anında geçerlidir, kullanım anında değil.

#### Soru 7 (Tricky): `BeanPostProcessor` nedir? Ne zaman kullanılır?
**Cevap:** Bean oluşturulduktan hemen önce (`postProcessBeforeInitialization`) ve hemen sonra (`postProcessAfterInitialization`) araya giren bir hook mekanizmasıdır.
*   **Kullanım:** Custom anotasyon işlemek, bean'i proxy ile sarmalamak (AOP böyle çalışır).

---

### 6. Geliştirici İpuçları

*   **Lombok Kullanımı:** Boilerplate kodu (Getter, Setter, Constructor) azaltmak için harikadır ama `@Data` anotasyonunu Entity sınıflarında dikkatli kullanın (`hashCode` sonsuz döngüye girebilir). `@Getter`, `@Setter` ve `@ToString` ayrı ayrı kullanmak daha güvenlidir.
*   **Profile Yönetimi:** `application-dev.properties`, `application-prod.properties` kullanarak ortam bazlı konfigürasyon yapın. Asla prod şifrelerini kodda tutmayın (Environment Variable veya Vault kullanın).
*   **Constructor Injection Israrı:** Field injection (`@Autowired` private...) kullanmayın. IDE uyarı verse bile Constructor injection kullanın. Bu, kodunuzun test edilebilirliğini ve kalitesini artırır.
*   **Bean İsimlendirme:** Aynı interface'i implement eden birden fazla sınıf varsa, bean isimlerini net verin ve `@Qualifier` ile yönetin.

---

