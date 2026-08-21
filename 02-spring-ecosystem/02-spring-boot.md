## Konu 14: Spring Boot Starters ve Auto-Configuration

Spring Boot'un en büyük gücü, "Convention over Configuration" (Yapılandırma yerine Konvansiyon) prensibidir. Starters ve Auto-Configuration mekanizması sayesinde, minimal konfigürasyonla production-ready uygulamalar oluşturabilirsiniz. Bir geliştirici olarak, bu "sihrin" nasıl çalıştığını ve gerektiğinde nasıl özelleştireceğinizi bilmelisiniz.

---

### 1. Spring Boot Starters Nedir?

**Analoji:** Starters, bir "başlangıç paketi" gibidir. Pizza yapmak için un, maya, domates, peynir ayrı ayrı almak yerine, "Pizza Yapım Kiti" alırsınız. İçinde ihtiyacınız olan her şey vardır.

Starter, belirli bir özellik için gerekli tüm bağımlılıkları (dependencies) tek bir dependency olarak sunar.

#### Popüler Starters

| Starter | Açıklama | İçerdiği Bağımlılıklar |
| :--- | :--- | :--- |
| `spring-boot-starter-web` | Web uygulamaları (REST API) | Spring MVC, Tomcat, Jackson (JSON) |
| `spring-boot-starter-data-jpa` | JPA/Hibernate ile database işlemleri | Hibernate, Spring Data JPA, JDBC |
| `spring-boot-starter-security` | Güvenlik | Spring Security |
| `spring-boot-starter-test` | Test | JUnit 5, Mockito, AssertJ, Hamcrest |
| `spring-boot-starter-actuator` | Monitoring ve Health Check | Micrometer, Actuator endpoints |
| `spring-boot-starter-validation` | Bean Validation | Hibernate Validator |
| `spring-boot-starter-cache` | Caching | Spring Cache abstraction |
| `spring-boot-starter-amqp` | RabbitMQ | Spring AMQP |
| `spring-boot-starter-data-redis` | Redis | Lettuce, Spring Data Redis |

**Kullanım:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Avantajı:** Versiyon uyumsuzluğu riski yoktur. Spring Boot, tüm bağımlılıkların uyumlu versiyonlarını yönetir (Dependency Management).

---

### 2. Auto-Configuration (Otomatik Yapılandırma)

**Analoji:** Akıllı bir asistan gibidir. Evinize gelir, neye ihtiyacınız olduğunu anlar (classpath'e bakar) ve otomatik olarak ayarlar (bean'leri oluşturur). Eğer siz zaten bir şey ayarlamışsanız (custom bean), o karışmaz.

#### Nasıl Çalışır?

1.  **`@SpringBootApplication`:** Bu anotasyon aslında 3 anotasyonun birleşimidir:
    *   `@Configuration`: Bu sınıf bir konfigürasyon sınıfıdır.
    *   `@EnableAutoConfiguration`: Auto-configuration'ı aktif eder.
    *   `@ComponentScan`: Bu paketin altındaki `@Component`, `@Service` vb. sınıfları tarar.

2.  **Classpath Tarama:** Spring Boot, classpath'teki JAR dosyalarına bakar.
    *   Örn: `spring-boot-starter-data-jpa` varsa → Hibernate ve DataSource bean'leri oluşturur.

3.  **Conditional Annotations:** Auto-configuration sınıfları, koşullu bean oluşturur:
    *   `@ConditionalOnClass`: Belirli bir sınıf classpath'te varsa.
    *   `@ConditionalOnMissingBean`: Belirli bir bean tanımlanmamışsa.
    *   `@ConditionalOnProperty`: `application.properties`'de belirli bir property varsa.

**Örnek Auto-Configuration Sınıfı:**
```java
@Configuration
@ConditionalOnClass(DataSource.class) // DataSource sınıfı varsa
@ConditionalOnMissingBean(DataSource.class) // Kullanıcı DataSource tanımlamamışsa
public class DataSourceAutoConfiguration {
    
    @Bean
    public DataSource dataSource() {
        // Varsayılan DataSource oluştur (H2, HikariCP)
        return new HikariDataSource();
    }
}
```

#### Auto-Configuration Raporu

Hangi auto-configuration'ların aktif olduğunu görmek için:
```bash
java -jar myapp.jar --debug
```
veya `application.properties`:
```properties
debug=true
```

**Çıktı:**
```
Positive matches: (Aktif olanlar)
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.sql.DataSource'

Negative matches: (Aktif olmayanlar)
   RabbitAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'org.springframework.amqp.rabbit.core.RabbitTemplate'
```

---

### 3. Auto-Configuration'ı Özelleştirme

#### 1. application.properties ile Yapılandırma

Spring Boot, binlerce özelleştirilebilir property sunar:

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Logging
logging.level.org.springframework=INFO
logging.level.com.example=DEBUG
```

#### 2. Custom Bean ile Override

Auto-configuration'ın oluşturduğu bean'i değiştirmek için kendi bean'inizi tanımlayın:

```java
@Configuration
public class CustomDataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        // Kendi DataSource yapılandırmanız
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/customdb");
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setMaximumPoolSize(20);
        return ds;
    }
}
```

Spring Boot, `@ConditionalOnMissingBean` sayesinde kendi DataSource'unu oluşturmaz.

#### 3. Auto-Configuration'ı Devre Dışı Bırakma

Belirli bir auto-configuration'ı kapatmak için:

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

veya `application.properties`:
```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

### 4. Custom Starter Oluşturma

Kendi starter'ınızı oluşturarak ortak konfigürasyonları paylaşabilirsiniz (örn: Şirket içi ortak kütüphaneler).

#### Adımlar:

1.  **Starter Modülü:** Sadece dependency'leri içerir (kod yok).
    ```xml
    <!-- my-custom-starter/pom.xml -->
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-custom-autoconfigure</artifactId>
        </dependency>
        <!-- Diğer gerekli bağımlılıklar -->
    </dependencies>
    ```

2.  **Auto-Configuration Modülü:** Gerçek konfigürasyon kodunu içerir.
    ```java
    @Configuration
    @ConditionalOnClass(MyService.class)
    public class MyServiceAutoConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public MyService myService() {
            return new MyService();
        }
    }
    ```

3.  **META-INF/spring.factories:** Auto-configuration sınıfını kaydedin.
    ```properties
    org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
    com.example.autoconfigure.MyServiceAutoConfiguration
    ```

4.  **Kullanım:** Diğer projelerde sadece starter'ı dependency olarak ekleyin:
    ```xml
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>my-custom-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    ```

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: `@SpringBootApplication` anotasyonu hangi 3 anotasyonu içerir?
**Cevap:**
1.  **`@Configuration`:** Sınıfın bir konfigürasyon sınıfı olduğunu belirtir.
2.  **`@EnableAutoConfiguration`:** Auto-configuration mekanizmasını aktif eder.
3.  **`@ComponentScan`:** Mevcut paketin altındaki component'leri tarar.

#### Soru 2: `@ConditionalOnMissingBean` ne işe yarar?
**Cevap:** Belirli bir bean tanımlanmamışsa (kullanıcı custom bean oluşturmamışsa) auto-configuration bean'ini oluşturur. Bu sayede kullanıcı kendi bean'ini tanımlarsa, auto-configuration devreye girmez.

**Örnek:**
```java
@Bean
@ConditionalOnMissingBean(DataSource.class)
public DataSource defaultDataSource() {
    // Kullanıcı DataSource tanımlamadıysa bu çalışır
}
```

#### Soru 3: Spring Boot'ta Embedded Server nasıl çalışır?
**Cevap:** `spring-boot-starter-web` dependency'si, varsayılan olarak **Tomcat**'i embedded (gömülü) olarak içerir. Uygulama bir JAR dosyası olarak paketlenir ve `java -jar app.jar` ile çalıştırılır. Ayrı bir Tomcat kurulumuna gerek yoktur.
*   **Alternatifler:** Tomcat yerine Jetty veya Undertow kullanılabilir (dependency değiştirerek).

#### Soru 4: `spring-boot-starter-parent` nedir? Kullanmak zorunlu mudur?
**Cevap:** Maven parent POM'dur. Dependency versiyonlarını, plugin konfigürasyonlarını ve Java versiyonunu yönetir.
*   **Zorunlu değildir.** Eğer başka bir parent POM kullanmanız gerekiyorsa, `spring-boot-dependencies`'i `<dependencyManagement>` bloğunda import edebilirsiniz:
    ```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.2.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    ```

#### Soru 5: Spring Boot'ta profiller nasıl kullanılır?
**Cevap:** Farklı ortamlar (dev, test, prod) için farklı konfigürasyonlar tanımlamak için kullanılır.

**Dosya yapısı:**
```
application.properties (Ortak)
application-dev.properties (Development)
application-prod.properties (Production)
```

**Aktif profil:**
```bash
java -jar app.jar --spring.profiles.active=prod
```

veya `application.properties`:
```properties
spring.profiles.active=dev
```

**Kod seviyesinde:**
```java
@Configuration
@Profile("prod")
public class ProdConfig {
    // Sadece prod profilinde aktif
}
```

#### Soru 6 (Tricky): `CommandLineRunner` ile `ApplicationRunner` farkı nedir?
**Cevap:** İkisi de uygulama başladığında kod çalıştırmak içindir.
*   **`CommandLineRunner`:** Argümanları `String[] args` (ham dizi) olarak alır.
*   **`ApplicationRunner`:** Argümanları `ApplicationArguments` nesnesi olarak alır (option ve non-option argümanları ayrıştırır, daha kullanışlıdır).

#### Soru 7 (Tricky): `spring-boot-devtools` production'da aktif midir?
**Cevap:** Varsayılan olarak **Hayır**. Uygulama `java -jar` ile veya production modunda çalıştırıldığında devtools otomatik olarak devre dışı kalır.
*   **Trap:** "Güvenlik riski oluşturur mu?" sorusuna "Otomatik kapanır ama dependency'yi production build'den çıkarmak (`<optional>true</optional>`) en iyisidir" denmeli.

---

### 6. Geliştirici İpuçları

*   **Starter Seçimi:** Gereksiz starter eklemeyin. Her starter, classpath'e onlarca JAR ekler. Uygulama başlangıç süresi ve bellek kullanımı artar.

*   **Auto-Configuration Debug:** Uygulama beklenmedik davranıyorsa, `--debug` flag'i ile hangi auto-configuration'ların aktif olduğunu kontrol edin. Bazen istemediğiniz bir bean otomatik oluşturulmuş olabilir.

*   **Lazy Initialization:** Spring Boot 2.2+ ile `spring.main.lazy-initialization=true` yaparak bean'lerin lazy (ihtiyaç duyulduğunda) oluşturulmasını sağlayabilirsiniz. Başlangıç süresi azalır ama ilk istek yavaş olur.

*   **Custom Starter Naming:** Kendi starter'ınıza `spring-boot-starter-*` adı vermeyin (Spring'in resmi naming convention'ı). `*-spring-boot-starter` formatını kullanın (örn: `acme-spring-boot-starter`).

*   **Configuration Properties:** Kendi konfigürasyon sınıflarınızı `@ConfigurationProperties` ile tanımlayın. IDE auto-complete desteği ve tip güvenliği sağlar:
    ```java
    @ConfigurationProperties(prefix = "myapp")
    public class MyAppProperties {
        private String apiKey;
        private int timeout;
        // Getters/Setters
    }
    ```
    ```properties
    myapp.api-key=secret123
    myapp.timeout=5000
    ```

Bu konu ile **14 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

