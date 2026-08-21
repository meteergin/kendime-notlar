## Konu 4: Modern Java (Lambda & Optional) ve I/O İşlemleri

Java 8, dilin tarihindeki en büyük değişimlerden biridir. Fonksiyonel programlama özelliklerinin (Lambda) gelmesi, kodun yazılış şeklini kökten değiştirmiştir. Ayrıca I/O işlemleri de zamanla evrimleşmiştir (IO -> NIO -> NIO.2). Bir geliştirici olarak modern Java özelliklerini etkin kullanmalı ve eski/yeni I/O yöntemleri arasındaki farkları bilmelisiniz.

---

### 1. Lambda Expressions (Lambda İfadeleri)

**Analoji:** Lambda, bir işi yapacak olan "anonim bir taşeron işçi" gibidir. İsmi yoktur, sadece ne yapacağı (kod bloğu) bellidir. Eskiden bir iş için koca bir sınıf (Anonymous Inner Class) tanımlardık, şimdi sadece işin kendisini (fonksiyonu) gönderiyoruz.

#### Tanım ve Sözdizimi
Lambda, bir **Functional Interface**'in (tek bir abstract metodu olan interface) implementasyonunu kısa yoldan yazmaktır.

```java
// Eski Yöntem (Anonymous Inner Class)
Runnable r1 = new Runnable() {
    @Override
    public void run() {
        System.out.println("Eski usül çalışıyor");
    }
};

// Lambda (Java 8+)
Runnable r2 = () -> System.out.println("Lambda çalışıyor");
```

**Sözdizimi:** `(parametreler) -> { gövde }`
*   Tek parametre varsa parantez opsiyonel: `x -> x * 2`
*   Tek satırsa süslü parantez ve `return` opsiyonel: `(x, y) -> x + y`

#### Functional Interfaces
Lambda kullanabilmek için hedef tipin **Functional Interface** olması şarttır.
*   `@FunctionalInterface` anotasyonu ile işaretlenir (zorunlu değil ama iyi pratik).
*   **Java.util.function Paketi (Ezberlenmeli!):**
    1.  **`Predicate<T>`:** `T -> boolean`. Test eder. (`filter` metodunda kullanılır)
    2.  **`Function<T, R>`:** `T -> R`. Dönüştürür. (`map` metodunda kullanılır)
    3.  **`Consumer<T>`:** `T -> void`. Tüketir. (`forEach` metodunda kullanılır)
    4.  **`Supplier<T>`:** `() -> T`. Üretir. (Factory pattern, `Optional.orElseGet`)

#### Method References (:: Operatörü)
Lambda ifadesi sadece var olan bir metodu çağırıyorsa daha da kısaltılabilir.
*   `s -> System.out.println(s)`  ==> `System.out::println`
*   `s -> Integer.parseInt(s)`    ==> `Integer::parseInt`

---

### 2. Optional (Opsiyonel / Null Güvenliği)

**Analoji:** Optional, bir "kutu"dur. Kutu boş olabilir veya içinde bir değer olabilir. Değeri almadan önce "Kutu boş mu?" diye sormaya zorlar, böylece elinizi boş kutuya daldırıp (NullPointerException) yaralanmazsınız.

#### Amaç
`NullPointerException` (NPE) hatalarını azaltmak ve bir metodun değer döndürmeyebileceğini (return null yerine) açıkça belirtmek.

#### Kullanım Senaryoları

```java
// Oluşturma
Optional<String> bos = Optional.empty();
Optional<String> dolu = Optional.of("Merhaba"); // Null ise NPE atar!
Optional<String> guvenli = Optional.ofNullable(null); // Boş kutu döner

// Kullanım (Kötü - isPresent + get)
if (guvenli.isPresent()) {
    System.out.println(guvenli.get());
}

// Kullanım (İyi - Functional Style)
guvenli.ifPresent(System.out::println);

// Değer Döndürme veya Varsayılan
String deger = guvenli.orElse("Varsayılan");
String deger2 = guvenli.orElseGet(() -> "Hesaplanan Varsayılan"); // Lazy evaluation
String deger3 = guvenli.orElseThrow(() -> new RuntimeException("Değer yok!"));

// Dönüştürme (Map / FlatMap)
Optional<Integer> uzunluk = guvenli.map(String::length);
```

**Dikkat:** `Optional` bir **return type** olarak tasarlanmıştır. Parametre olarak veya sınıf field'ı olarak kullanılması önerilmez (Serializable değildir).

---

### 3. I/O Operations (Giriş/Çıkış)

Java'da I/O iki ana pakette toplanır: `java.io` (Eski) ve `java.nio` (Yeni).

#### java.io (Stream Oriented - Blocking)
*   Veriyi bayt bayt veya karakter karakter işler (Stream).
*   **Blocking:** Okuma/yazma işlemi bitene kadar thread bloklanır.
*   Sınıflar: `FileInputStream`, `BufferedReader`, `InputStream`.

#### java.nio (Buffer Oriented - Non-Blocking)
*   Java 1.4 ile geldi, Java 7 ile (NIO.2) güçlendi.
*   **Channel & Buffer:** Veri bir kanaldan (Channel) bir tampona (Buffer) okunur.
*   **Non-Blocking:** Thread, okuma isteği gönderip başka işe bakabilir.
*   **Selector:** Tek bir thread ile birden fazla kanalı yönetebilir (Netty gibi frameworklerin temeli).

---

### 4. File Operations (Dosya İşlemleri)

Modern Java'da (Java 7+) dosya işlemleri için `java.io.File` yerine **`java.nio.file.Path`** ve **`java.nio.file.Files`** kullanılmalıdır.

#### Path ve Files Sınıfı

```java
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class FileTest {
    public void modernFileOperations() throws IOException {
        Path path = Paths.get("data.txt");

        // 1. Dosya var mı?
        boolean exists = Files.exists(path);

        // 2. Dosya oluşturma
        if (!exists) {
            Files.createFile(path);
        }

        // 3. Yazma (Java 11+)
        Files.writeString(path, "Merhaba Dünya\nJava 11", StandardOpenOption.APPEND);

        // 4. Okuma (Tümünü okuma - Küçük dosyalar için)
        String content = Files.readString(path);
        List<String> lines = Files.readAllLines(path);

        // 5. Okuma (Stream ile - Büyük dosyalar için)
        // try-with-resources ile stream kapatılmalı!
        try (Stream<String> stream = Files.lines(path)) {
            stream.filter(line -> line.contains("Java"))
                  .forEach(System.out::println);
        }
    }
}
```

#### try-with-resources (Otomatik Kaynak Yönetimi)
Java 7 ile geldi. `AutoCloseable` interface'ini implement eden kaynaklar (Stream, Socket, DB Connection) `try` parantezi içinde tanımlanırsa, blok bitince otomatik `close()` edilir. `finally` bloğuna gerek kalmaz.

```java
// Eski
BufferedReader br = null;
try {
    br = new BufferedReader(new FileReader("test.txt"));
    // ...
} catch (IOException e) {
    // ...
} finally {
    if (br != null) try { br.close(); } catch (IOException e) { }
}

// Yeni
try (BufferedReader br = new BufferedReader(new FileReader("test.txt"))) {
    // ...
} catch (IOException e) {
    // ...
}
```

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: Lambda ifadeleri ile Anonymous Inner Class arasındaki fark nedir?
**Cevap:**
1.  **`this` anahtar kelimesi:** Inner class'ta `this` o inner class nesnesini işaret eder. Lambda'da `this`, lambda'yı kapsayan dış sınıfı (enclosing class) işaret eder.
2.  **Derleme (Compilation):** Inner class ayrı bir `.class` dosyası oluşturur (`Outer$1.class`). Lambda ise `invokedynamic` bytecode talimatını kullanır, ayrı dosya oluşturmaz, daha hafiftir.

#### Soru 2: `Optional` sınıfını neden field olarak kullanmamalıyız?
**Cevap:** `Optional` sınıfı `Serializable` interface'ini implement etmez. Eğer sınıfınızı serialize etmeye çalışırsanız (örneğin Redis'e yazarken veya RMI ile) `NotSerializableException` alırsınız. Ayrıca her field için ekstra bir nesne (Optional wrapper) oluşturmak bellek israfıdır. Getter metodlarında dönüş tipi olarak kullanmak en doğrusudur.

#### Soru 3: `Stream` API kullanırken `map()` ve `flatMap()` farkı nedir?
**Cevap:**
*   **`map()`:** Bire-bir dönüşüm yapar. `T -> R`.
    *   `List<String>` -> `map(s -> s.length())` -> `List<Integer>`
*   **`flatMap()`:** Bire-çok dönüşüm yapar ve yapıyı düzleştirir (flatten). `T -> Stream<R>`.
    *   `List<List<String>>` (İç içe liste) -> `flatMap(List::stream)` -> `List<String>` (Tek liste).

#### Soru 4: Büyük bir dosyayı (örneğin 5GB) Java'da nasıl okursunuz?
**Cevap:** Asla `Files.readAllLines()` veya `Files.readString()` kullanmam. Bu tüm dosyayı Heap'e yükler ve `OutOfMemoryError` verir.
*   **Çözüm 1:** `Files.lines(Path)` ile Stream olarak satır satır işlerim (Lazy loading).
*   **Çözüm 2:** `BufferedReader` ile satır satır okurum.
*   **Çözüm 3:** `FileChannel` ve `MappedByteBuffer` (Memory Mapped Files) kullanarak çok yüksek performanslı okuma yaparım (OS seviyesinde caching kullanır).

#### Soru 5 (Tricky): `Optional.of()` ile `Optional.ofNullable()` farkı nedir?
**Cevap:**
*   **`Optional.of(value)`:** Value **kesinlikle null olmamalı**. Eğer null ise `NullPointerException` fırlatır.
*   **`Optional.ofNullable(value)`:** Value null olabilir. Null ise `Optional.empty()` döner.
*   **Trap:** "Optional zaten null güvenli, of() kullansam fark etmez" sanılabilir ama `of(null)` patlar.

#### Soru 6 (Tricky): Lambda içinde kullanılan değişkenler neden `final` veya effectively final olmalıdır?
**Cevap:** Lambda, değişkeni **capture** (yakalama) eder. Eğer değişken değişebilirse, lambda çalıştığında hangi değeri göreceği belirsiz olur (timing issue).
*   **Trap:** "Sadece final işaretlersem çalışır" değil, değişkene tekrar atama yapılmamışsa (effectively final) çalışır.
```java
int x = 10;
// x = 20; // Bu satırı açarsanız compile error: x effectively final değil
Runnable r = () -> System.out.println(x); // x effectively final olmalı
```

#### Soru 7 (Tricky): Stream'de `forEach()` ile `forEachOrdered()` farkı nedir?
**Cevap:**
*   **`forEach()`:** Paralel stream'de sıralama garantisi **yoktur**.
*   **`forEachOrdered()`:** Paralel stream'de bile orijinal sırayı korur.
*   **Trap:** "Paralel stream her zaman daha hızlı" değildir. `forEachOrdered` sıralama için senkronizasyon yapar, bu da yavaşlatabilir.

---

### 6. Geliştirici İpuçları

*   **Lambda Debugging:** Lambda ifadeleri debug etmesi zordur (stack trace karmaşıktır). Karmaşık lojikler için lambda içine blok yazmak yerine, o işi yapan isimlendirilmiş bir metoda çıkarıp Method Reference (`this::myMethod`) kullanın. Okunabilirlik artar.
*   **Optional.get() Yasağı:** Kodunuzda `Optional.get()` kullanmayın. Eğer değerin kesin orada olduğunu biliyorsanız bile `orElseThrow()` kullanın ki niyetiniz belli olsun. `get()` kullanımı Optional'ın amacına aykırıdır.
*   **NIO vs IO:** Yüksek trafikli, binlerce eşzamanlı bağlantı gerektiren ağ uygulamaları (Chat server, Proxy) yazmıyorsanız NIO'nun karmaşıklığına girmeyin. Standart IO (veya modern `Files` sınıfı) çoğu dosya işlemi için yeterli ve daha okunaklıdır.

---

