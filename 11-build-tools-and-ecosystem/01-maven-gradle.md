## Konu 7: Build Tools (Maven & Gradle)

Build toolları, Java projelerinin derlenmesi, test edilmesi, paketlenmesi ve dağıtımı için kullanılan otomasyon araçlarıdır. Bir geliştirici olarak sadece `mvn clean install` veya `gradle build` komutlarını bilmek yetmez; dependency management, lifecycle, plugin yapıları ve multi-module proje yönetimini derinlemesine anlamanız gerekir.

---

### 1. Maven (Apache Maven)

**Analoji:** Maven, bir "tarif kitabı" (cookbook) gibidir. Standart bir yol haritası (Convention over Configuration) sunar. Projenin yapısını (`src/main/java`, `src/test/java`) ve yaşam döngüsünü (compile, test, package) önceden belirler. Siz sadece "malzemeleri" (dependencies) ve "özel talimatları" (plugins) eklersiniz.

#### Temel Kavramlar

1.  **POM (Project Object Model):** `pom.xml` dosyası projenin kalbidir. Tüm metadata, bağımlılıklar ve build konfigürasyonu burada tanımlanır.

2.  **Coordinates (Koordinatlar):** Bir projeyi benzersiz şekilde tanımlayan üçlü:
    *   **groupId:** Organizasyon/şirket (örn: `com.example`)
    *   **artifactId:** Proje adı (örn: `my-app`)
    *   **version:** Versiyon (örn: `1.0.0-SNAPSHOT`)

3.  **Dependencies (Bağımlılıklar):** Projenin ihtiyaç duyduğu harici kütüphaneler.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.0</version>
</dependency>
```

**Scope Türleri:**
*   `compile` (varsayılan): Her yerde kullanılır (compile, runtime, test).
*   `provided`: Derleme zamanında var ama runtime'da uygulama sunucusu sağlar (örn: `servlet-api`).
*   `runtime`: Sadece runtime'da gerekli (örn: JDBC driver).
*   `test`: Sadece test kodunda kullanılır (örn: JUnit).

#### Maven Lifecycle (Yaşam Döngüsü)

Maven'in 3 ana lifecycle'ı vardır:
1.  **Clean:** Eski build dosyalarını temizler.
2.  **Default:** Asıl build sürecidir.
3.  **Site:** Proje dokümantasyonu oluşturur.

**Default Lifecycle'daki Önemli Fazlar:**
*   `validate`: Projenin doğru ve gerekli bilgilerin mevcut olup olmadığını kontrol eder.
*   `compile`: Kaynak kodu derler.
*   `test`: Derlenmiş test kodlarını çalıştırır.
*   `package`: Kodu JAR/WAR dosyasına paketler.
*   `verify`: Entegrasyon testlerini çalıştırır.
*   `install`: Paketi local Maven repository'ye (`.m2/repository`) yükler.
*   `deploy`: Paketi uzak (remote) repository'ye yükler (örn: Nexus, Artifactory).

**Komut:** `mvn clean install` → Önce temizler, sonra compile, test, package, install fazlarını sırayla çalıştırır.

#### Dependency Management ve Transitive Dependencies

**Transitive Dependencies:** Bir kütüphane eklendiğinde, o kütüphanenin bağımlı olduğu diğer kütüphaneler de otomatik olarak indirilir.
*   **Sorun:** Versiyon çakışmaları (Dependency Hell).
*   **Çözüm:** `<dependencyManagement>` bloğu ile üst seviye (parent POM) versiyonları merkezi olarak yönetin.

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

---

### 2. Gradle

**Analoji:** Gradle, bir "programlanabilir otomasyon robotu" gibidir. Maven'in aksine, sadece deklaratif değil, **imperative** (programatik) yapılandırma da yapabilirsiniz. Groovy veya Kotlin DSL kullanarak karmaşık build lojikleri yazabilirsiniz.

#### Temel Kavramlar

1.  **Build Script:** `build.gradle` (Groovy) veya `build.gradle.kts` (Kotlin) dosyasıdır.

2.  **Plugins:** Gradle'ın gücü plugin sistemindedir. Her plugin yeni task'lar ve yapılandırmalar ekler.
    *   `java`: Java derleme desteği.
    *   `application`: Çalıştırılabilir uygulama oluşturma.
    *   `spring-boot`: Spring Boot projeleri için.

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.0'
}
```

#### Gradle Build Phases

Gradle build'i 3 aşamada çalışır:
1.  **Initialization:** Hangi projelerin build'e dahil olacağını belirler (`settings.gradle`).
2.  **Configuration:** Tüm build script'leri çalıştırılır ve task grafiği (DAG - Directed Acyclic Graph) oluşturulur.
3.  **Execution:** İstenen task'lar çalıştırılır.

**Komutlar:**
*   `gradle build`: Test eder, derler, paketler.
*   `gradle clean build`: Temizler ve build eder.
*   `gradle bootRun`: Spring Boot uygulamasını çalıştırır.

#### Gradle Daemon

Gradle, performans için **daemon** (arka plan süreci) kullanır. İlk build yavaş, sonrakiler çok daha hızlıdır çünkü JVM ısınmış ve sınıflar cache'lenmiştir.

---

### 3. Maven vs Gradle

| Özellik | Maven | Gradle |
| :--- | :--- | :--- |
| **Yapılandırma** | XML (verbose, okuması zor) | Groovy/Kotlin (özlü, okunabilir) |
| **Performans** | Yavaş (incremental build yok) | Çok hızlı (incremental build, caching) |
| **Esneklik** | Katı kurallara bağlı (Convention) | Çok esnek (programlanabilir) |
| **Learning Curve** | Kolay (standart yapı) | Zor (DSL öğrenme gerekir) |
| **Topluluk** | Çok olgun, geniş plugin ekosistemi | Hızla büyüyen, Android'in resmi build tool'u |
| **Multi-Module Projeler** | İyi destekler (`<modules>`) | Mükemmel destekler (daha hızlı) |

**Ne Zaman Hangisi?**
*   **Maven:** Kurumsal, standart yapılara bağlı kalmak istiyorsanız. Spring ekosistemi çok iyi entegre.
*   **Gradle:** Performans kritikse (büyük projeler), özelleştirilmiş build süreçleri gerekiyorsa veya Android geliştiriyorsanız.

---

### 4. Kritik Mülakat Soruları 

#### Soru 1: Maven'de `mvn install` ile `mvn deploy` farkı nedir?
**Cevap:**
*   **`mvn install`:** Paketi **local repository**'ye (`.m2/repository`) yükler. Sadece kendi makinenizdeki diğer projeler kullanabilir.
*   **`mvn deploy`:** Paketi **remote/central repository**'ye (Nexus, Artifactory) yükler. Tüm takım veya kuruluş kullanabilir. CI/CD pipeline'larında kullanılır.

#### Soru 2: Gradle'da `implementation` ile `api` farkı nedir?
**Cevap:**
*   **`implementation`:** Bağımlılık sadece o modülde kalır. Transitive dependency olarak dışarı sızmaz (encapsulation). **Önerilir.**
*   **`api`:** Bağımlılık, o modülü kullanan diğer modüllere de geçer (leaked). Sadece gerçekten public API'nin bir parçasıysa kullanılmalı.

**Örnek:** A modülü B kütüphanesini kullanıyor. C modülü A'yı kullanıyor.
*   `implementation 'B'` → C, B'yi görmez.
*   `api 'B'` → C, B'yi görür ve kullanabilir.

#### Soru 3: Maven'de SNAPSHOT versiyonu ne demektir?
**Cevap:** Geliştirme aşamasında olan, henüz stabil olmayan bir versiyondur (örn: `1.0.0-SNAPSHOT`).
*   Maven her build'de SNAPSHOT bağımlılıklarını günceller (remote repository'den yeniden indirir).
*   Release versiyonları (`1.0.0`) ise immutable'dır, bir kez yüklendikten sonra değişmez.

#### Soru 4: Gradle'ın incremental build özelliği nasıl çalışır?
**Cevap:** Gradle, her task'ın input ve output'larını izler. Eğer bir task'ın input'ları değişmediyse, o task atlanır (UP-TO-DATE). Bu, büyük projelerde build süresini dramatik şekilde kısaltır.
*   Maven'de böyle bir mekanizma yoktur, her seferinde sıfırdan derler.

#### Soru 5: Multi-module Maven projesinde parent POM'un rolü nedir?
**Cevap:** Parent POM, ortak yapılandırmaları (plugin versiyonları, dependency management, properties) merkezi olarak yönetir. Child modüller parent'tan inherit eder.
*   Children sadece ihtiyaç duydukları dependency'leri ekler, version belirtmez (parent'tan alır).
*   Code duplication azalır, versiyon tutarlılığı sağlanır.

#### Soru 6 (Tricky): Maven'de `<dependencyManagement>` ile `<dependencies>` farkı nedir?
**Cevap:**
*   **`<dependencies>`:** Direkt bağımlılık ekler. O modüldeki tüm kodlar bu dependency'yi kullanabilir.
*   **`<dependencyManagement>`:** Sadece versiyon ve scope'u tanımlar, ekleme yapmaz. Child modüller bu dependency'yi kullanmak isterse `<version>` belirtmeden ekleyebilir.
*   **Trap:** "Her ikisi de dependency ekler" yanlış! `dependencyManagement` sadece yönetim amaçlıdır.

#### Soru 7 (Tricky): Gradle'da `compileOnly` ile `runtimeOnly` farkı nedir?
**Cevap:**
*   **`compileOnly`:** Sadece compile time'da gereklidir, runtime'a eklenmez (örn: Lombok annotations).
*   **`runtimeOnly`:** Sadece runtime'da gereklidir, compile time'da değil (örn: JDBC drivers).
*   **Trap:** "implementation her ikisini de kapsar" doğru ama gereksiz dependency ekler, optimizasyon için bunlar kullanılmalı.

---

### 5. Geliştirici İpuçları

*   **Dependency Analizi:** `mvn dependency:tree` (Maven) veya `gradle dependencies` (Gradle) ile transitive dependency grafiğini görüntüleyin. Versiyon çakışmalarını tespit edin.
*   **Offline Mode:** İnternetsiz ortamlarda çalışırken `mvn -o` (Maven) veya `gradle --offline` (Gradle) kullanın. Sadece local cache'ten okuır.
*   **Gradle Wrapper:** Projeye özel Gradle versiyonu kullanmak için `./gradlew` kullanın. Tüm team üyeleri aynı Gradle versiyonu ile çalışır (version mismatch önlenir).
*   **Maven Enforcer Plugin:** Minimum Java versiyonu, yasak dependency'ler gibi kuralları zorlamak için kullanın. CI/CD'de build kalitesini artırır.
*   **Build Profilleri:** Maven'de `-P` ile (örn: `mvn clean install -Pproduction`), Gradle'da `-P` property'leri ile farklı ortamlara (dev, test, prod) özel build yapabilirsiniz.

---

