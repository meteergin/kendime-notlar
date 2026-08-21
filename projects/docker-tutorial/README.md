# Yazılım Geliştiricileri için Docker Rehberi

Bu rehber, "Docker nedir?" sorusunu soran yeni başlayanlar için değil, JVM'in derinliklerini bilen, Garbage Collection tuning yapmış, ancak konteyner dünyasında "best practice" arayan yazılım geliştiriciler için hazırlanmıştır.

Burada "Hello World" örnekleri yerine, Production ortamında karşılaşacağınız senaryoları, JVM'in konteyner içindeki davranışlarını ve modern CI/CD pipeline entegrasyonu konuşacağız.

---

## İçindekiler

1.  [Zihniyet Değişimi: Java vs Docker](#1-zihniyet-değişimi-java-vs-docker)
2.  [Docker Mimarisi ve Java Analojileri](#2-docker-mimarisi-ve-java-analojileri)
3.  [Java Uygulamalarını Dockerize Etmek (Profesyonel Yol)](#3-java-uygulamalarını-dockerize-etmek-profesyonel-yol)
    *   [Naive Yaklaşım (Yapılmaması Gereken)](#31-naive-yaklaşım)
    *   [Layered Jar Yaklaşımı (Spring Boot)](#32-layered-jar-yaklaşımı)
    *   [Multi-Stage Build](#33-multi-stage-build)
    *   [Base Image Seçimi (Alpine vs Distroless vs Temurin)](#34-base-image-seçimi)
4.  [JVM ve Konteyner Uyumu (Memory & CPU)](#4-jvm-ve-konteyner-uyumu)
5.  [Docker Compose ile Local Development Ortamı](#5-docker-compose-ile-local-development-ortamı)
6.  [İleri Seviye Senaryolar](#6-ileri-seviye-senaryolar)
    *   [Remote Debugging](#61-remote-debugging)
    *   [Security (Non-Root User)](#62-security-non-root-user)
    *   [Hot Reload (DevTools)](#63-hot-reload-devtools)
7.  [CI/CD Pipeline Entegrasyonu](#7-cicd-pipeline-entegrasyonu)

---

## 1. Zihniyet Değişimi: Java vs Docker

Geleneksel dünyada bir Java uygulaması dağıtmak şuna benzerdi:
1.  Sunucuya SSH ile bağlan.
2.  JDK'nın doğru versiyonunun kurulu olduğundan emin ol (`java -version`).
3.  Environment variable'ları `.bashrc` içine tanımla.
4.  Tomcat/Jetty kur veya `java -jar app.jar` ile uygulamayı başlat.

**Sorun:** "Benim makinemde çalışıyordu" (It works on my machine).
Sunucudaki JDK minor versiyonu farklı olabilir, OS kütüphaneleri eksik olabilir, encoding (UTF-8 vs CP1252) farklı olabilir.

**Docker Çözümü:** Immutable Infrastructure.
Uygulamanız sadece JAR dosyanız değil; OS, JDK, kütüphaneler ve konfigürasyonların bütünüdür. Docker image'ı, uygulamanızın **donmuş bir anıdır**.

---

## 2. Docker Mimarisi ve Java Analojileri

Kavramları hızlıca eşleştirelim:

| Java Kavramı | Docker Karşılığı | Açıklama |
| :--- | :--- | :--- |
| **Class** | **Image** | Image, çalıştırılabilir bir şablondur. Read-only'dir. |
| **Object (Instance)** | **Container** | Image'ın çalışan halidir. State tutar (istenirse). |
| **Maven Central** | **Docker Hub / Registry** | Image'ların depolandığı yer. |
| **pom.xml** | **Dockerfile** | Image'ın nasıl oluşturulacağının tarifi. |
| **Classpath** | **Layered Filesystem** | Bağımlılıkların üst üste binmesi. |

### Layer Mantığı (Kritik)
Docker image'ları katmanlardan (layers) oluşur. Bir `Dockerfile` içindeki her komut (`RUN`, `COPY`, `ADD`) yeni bir katman oluşturur.
**Java Geliştiricisi için Önemi:**
Spring Boot "Fat JAR"ınız 100MB olsun. Bunun 95MB'ı kütüphaneler (`dependencies`), 5MB'ı sizin kodunuzdur.
Eğer her build'de tüm JAR'ı tek bir katman olarak kopyalarsanız, kodunuzda 1 satır değiştirseniz bile Docker 100MB'lık yeni bir katman oluşturur ve bunu Registry'e push/pull etmeniz gerekir.
**Hedef:** Değişmeyen kütüphaneleri alt katmanlara, sık değişen kodu üst katmanlara koymak.

---

## 3. Java Uygulamalarını Dockerize Etmek (Profesyonel Yol)

Örnek bir Spring Boot uygulaması üzerinden gidelim.

### 3.1. Naive Yaklaşım (Yapılmaması Gereken)

En basit `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/myapp-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

**Neden Kötü?**
1.  **Cache Kullanımı Yok:** Kodunuzda bir boşluk bile değiştirseniz, 100MB'lık JAR yeniden kopyalanır.
2.  **Güvenlik:** Root kullanıcısı ile çalışır.
3.  **Build Context:** `target` klasörünü host makinede build etmeniz gerekir (`mvn clean package`).

### 3.2. Multi-Stage Build ve Layered JAR

Modern yöntem: Build işlemini de Docker içinde yapın, sadece artifact'i runtime image'ına taşıyın. Ayrıca Spring Boot'un `layertools` özelliğini kullanın.

**Adım 1: Spring Boot Maven Plugin Ayarı (`pom.xml`)**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <layers>
                    <enabled>true</enabled>
                </layers>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Adım 2: Profesyonel `Dockerfile`**

```dockerfile
# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
# Önce sadece pom.xml'i kopyala ve bağımlılıkları indir (Cache için kritik!)
COPY pom.xml .
RUN mvn dependency:go-offline

# Şimdi kaynak kodu kopyala ve build et
COPY src ./src
RUN mvn clean package -DskipTests

# JAR'ı aç (Extract layers)
WORKDIR /app/target
RUN java -Djarmode=layertools -jar *.jar extract

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /application

# Layer'ları en az değişenden en çok değişene doğru kopyala
COPY --from=builder /app/target/dependencies/ ./
COPY --from=builder /app/target/spring-boot-loader/ ./
COPY --from=builder /app/target/snapshot-dependencies/ ./
COPY --from=builder /app/target/application/ ./

# JVM Tuning ve Entrypoint
# "exec" formunu kullanın, shell formunu değil! (Signal handling için)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**Avantajları:**
*   **Hız:** Bağımlılıklarınız değişmediği sürece `mvn dependency:go-offline` katmanı cache'den gelir.
*   **Boyut:** Runtime image'ında Maven yok, sadece JRE var.
*   **Verimlilik:** Uygulama kodunuzu değiştirdiğinizde sadece `application` katmanı (birkaç KB) değişir.

### 3.4. Base Image Seçimi

*   **`eclipse-temurin:17-jre` (Ubuntu/Debian tabanlı):** Standart, güvenli, glibc kullanır. Boyut ~200MB.
*   **`eclipse-temurin:17-jre-alpine`:** Çok küçük (~50-100MB). `musl` libc kullanır.
    *   *Dikkat:* Bazı Java kütüphaneleri (örn: eski Netty versiyonları, JNI kullananlar) Alpine ile sorun çıkarabilir.
*   **`gcr.io/distroless/java17`:** Google'ın "Distroless" image'ı. İçinde shell (`/bin/sh`) bile yok.
    *   *Artı:* Maksimum güvenlik. Saldırgan konteynera girse bile komut çalıştıramaz.
    *   *Eksi:* Debug etmek zordur (shell yok).

**Tavsiye:** Production için `distroless` veya `alpine` (test ettiyseniz). Debugging ihtiyacı olan dev ortamları için standart Debian/Ubuntu tabanlılar.

---

## 4. JVM ve Konteyner Uyumu

Java 10 öncesinde JVM, bir konteyner içinde çalıştığını anlamazdı. Host makinenin 64GB RAM'i varsa, konteyner limiti 512MB olsa bile JVM 64GB var sanıp büyük bir Heap oluşturmaya çalışır ve OOM Kill (Out of Memory) yerdi.

Java 10+ (ve backport edilmiş Java 8u191+) ile bu çözüldü. Ancak yine de açıkça belirtmekte fayda var.

**Kritik JVM Parametreleri:**

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

*   **`-XX:+UseContainerSupport`**: JVM'in cgroup limitlerini okumasını sağlar (Default açık).
*   **`-XX:MaxRAMPercentage=75.0`**: Konteyner'a verilen RAM'in %75'ini Heap olarak kullan. Geriye kalan %25; Metaspace, Thread Stack'ler ve Native Memory (Direct Buffers) için ayrılır.
    *   *Hata:* Asla `-Xmx512m` gibi hard-coded değerler vermeyin. Docker Compose veya K8s limitlerini değiştirdiğinizde bunu da değiştirmeniz gerekir. Yüzde kullanın.

---

## 5. Docker Compose ile Local Development Ortamı

Bir Java geliştiricisi olarak, uygulamanızın çalışması için Postgres, Redis ve belki bir Kafka'ya ihtiyacınız var. Bunları tek tek kurmak yerine `docker-compose.yml` kullanın.

**Örnek Senaryo:** Spring Boot App + PostgreSQL + Redis.

`docker-compose.yml`:
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
      - "5005:5005" # Remote Debugging Port
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mydb
      - SPRING_DATA_REDIS_HOST=redis
      - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -XX:MaxRAMPercentage=75.0
    depends_on:
      db:
        condition: service_healthy # DB hazır olana kadar bekle
      redis:
        condition: service_started
    deploy:
      resources:
        limits:
          memory: 512M # JVM bunu görecek ve MaxRAMPercentage'ı buna göre ayarlayacak

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data # Veri kalıcılığı
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user -d mydb"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine

volumes:
  postgres_data:
```

**Önemli Noktalar:**
1.  **Service Discovery:** `SPRING_DATASOURCE_URL` içinde `localhost` yerine `db` yazdık. Docker'ın kendi DNS'i servis isimlerini çözümler.
2.  **Healthchecks & depends_on:** Spring Boot uygulamanızın, veritabanı tamamen ayağa kalkmadan başlamasını engellemek için `condition: service_healthy` kullanın.
3.  **Volumes:** `postgres_data` volume'ü sayesinde konteyneri silip tekrar başlatsanız bile verileriniz kaybolmaz.

---

## 6. İleri Seviye Senaryolar

### 6.1. Remote Debugging

Konteyner içindeki Java uygulamasını IDE'nizden (IntelliJ/Eclipse) debug edebilirsiniz.

1.  **JVM Argümanı:** `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
    *   `address=*:5005`: Tüm interface'lerden gelen bağlantıları kabul et (önemli, çünkü localhost değil, dışarıdan geliyoruz).
2.  **Port Mapping:** `docker-compose.yml` içinde `5005:5005`.
3.  **IDE Ayarı:** IntelliJ -> Edit Configurations -> Remote JVM Debug -> Host: `localhost`, Port: `5005`.

### 6.2. Security (Non-Root User)

Docker konteynerları varsayılan olarak `root` kullanıcısı ile çalışır. Bu bir güvenlik riskidir.

**Dockerfile'da Kullanıcı Değiştirme:**

```dockerfile
# ... (önceki adımlar)

# Bir grup ve kullanıcı oluştur
RUN addgroup -S spring && adduser -S spring -G spring

# Dosya sahipliğini değiştir
RUN chown -R spring:spring /application

# Kullanıcıya geç
USER spring:spring

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### 6.3. Hot Reload (DevTools)

Geliştirme yaparken her kod değişikliğinde `docker build` yapmak yavaştır.

**Çözüm:** Spring Boot DevTools ve Volume Mounting.
Ancak, Docker ile class reloading biraz karmaşıktır. Genellikle yerel geliştirmede IDE üzerinden çalıştırmak daha pratiktir. Eğer illa Docker içinde hot-reload istiyorsanız, **Jib** veya **Cloud Native Buildpacks** kullanabilirsiniz, ya da kaynak kodunuzu volume olarak mount edip container içinde Maven çalıştırabilirsiniz (tavsiye edilmez, yavaştır).

---

## 7. CI/CD Pipeline Entegrasyonu

Jenkins, GitLab CI veya GitHub Actions kullanırken dikkat etmeniz gerekenler:

1.  **Tagging Strategy:**
    *   `latest` tag'ini production için asla kullanmayın. Geri alamazsınız.
    *   Git Commit Hash (kısa) veya Semantic Version kullanın.
    *   Örn: `myapp:1.0.2`, `myapp:a1b2c3d`.

2.  **Docker-in-Docker (DinD) vs Kaniko:**
    *   CI ortamınızda Docker daemon'a erişiminiz varsa (örneğin GitHub Actions), standart `docker build` komutlarını kullanabilirsiniz.
    *   Kubernetes üzerinde çalışan bir Jenkins agent'ınız varsa, Docker daemon'a erişmek güvenlik riski olabilir. Bu durumda **Kaniko** veya **Buildah** gibi daemon-less build araçlarını kullanın.

3.  **Örnek GitHub Actions Adımı:**

```yaml
- name: Build and Push Docker Image
  uses: docker/build-push-action@v4
  with:
    context: .
    push: true
    tags: |
      myregistry/myapp:latest
      myregistry/myapp:${{ github.sha }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```
*   `cache-from/to`: GitHub Actions cache'ini kullanarak build sürelerini %80 azaltır.

---

## Sonuç

Docker, uygulama geliştiricileri için sadece bir "paketleme" aracı değil, tutarlı ve güvenilir bir "çalıştırma" ortamıdır.
*   **Layered JAR** yapısını kullanarak build sürelerini kısaltın.
*   **JVM Tuning** parametrelerini (`MaxRAMPercentage`) asla unutmayın.
*   **Docker Compose** ile production benzeri bir ortamı local'de kurun.

---

## 8. Gerçek Hayat Senaryoları (Hands-on Labs)

Bu dokümanın yanında, `scenarios` klasörü altında 11 adet çalıştırılabilir, production-ready örnek hazırladım. Her klasörün içinde kendi `README.md` dosyası ve açıklamaları mevcuttur.

1.  **[01-professional-builder](./scenarios/01-professional-builder):** En iyi pratiklere sahip Dockerfile (Multi-stage, Layered, Security).
2.  **[02-db-persistence-migration](./scenarios/02-db-persistence-migration):** PostgreSQL, Volumes, Init Scriptleri ve Healthcheck.
3.  **[03-cache-and-queue](./scenarios/03-cache-and-queue):** Redis ve RabbitMQ entegrasyonu.
4.  **[04-observability-stack](./scenarios/04-observability-stack):** Prometheus, Grafana ve Zipkin ile tam izlenebilirlik.
5.  **[05-centralized-logging-elk](./scenarios/05-centralized-logging-elk):** Filebeat, Elasticsearch ve Kibana ile log yönetimi.
6.  **[06-nginx-load-balancing](./scenarios/06-nginx-load-balancing):** Nginx ile Load Balancing ve High Availability.
7.  **[07-traefik-edge-router](./scenarios/07-traefik-edge-router):** Modern, auto-discovery özellikli Edge Router.
8.  **[08-kafka-kraft](./scenarios/08-kafka-kraft):** Zookeeper'sız Kafka (KRaft) ve Yönetim Paneli.
9.  **[09-minio-object-storage](./scenarios/09-minio-object-storage):** Local S3 uyumlu dosya depolama sunucusu.
10. **[10-keycloak-iam](./scenarios/10-keycloak-iam):** Merkezi Kimlik Yönetimi (OAuth2/OpenID Connect).
11. **[11-microservices-complete-stack](./scenarios/11-microservices-complete-stack):** **The Big Picture.** Tüm bileşenleri (Infra, Platform, Business) içeren devasa bir mimari şablonu.

