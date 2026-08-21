## Konu 6: Functional Programming ve Stream API

Java 8 ile birlikte Java, sadece Nesne Yönelimli (OOP) bir dil olmaktan çıkıp, Fonksiyonel Programlama (FP) paradigmalarını da destekleyen hibrit bir dil haline gelmiştir. Bir geliştirici olarak, FP'nin temel prensiplerini (Immutability, Pure Functions) ve Stream API'nin deklaratif gücünü performansı gözeterek kullanabilmelisiniz.

---

### 1. Functional Programming Temelleri

**Analoji:**
*   **Imperative (Emir kipi):** Birine "Git dolabı aç, yumurtayı al, kaseye kır, çırp" demek (Adım adım NASIL yapılacağını söylemek).
*   **Declarative (Bildirimsel/Fonksiyonel):** "Bana çırpılmış yumurta ver" demek (NE istediğini söylemek, nasılına karışmamak).

#### Temel Kavramlar
1.  **Pure Functions (Saf Fonksiyonlar):**
    *   Aynı girdi için her zaman aynı çıktıyı üretir.
    *   **Side-effect (Yan etki) yoktur:** Dış dünyadaki bir durumu (global değişken, DB, dosya) değiştirmez.
    *   Thread-safe'dir çünkü durum (state) paylaşmaz.

2.  **Immutability (Değişmezlik):**
    *   Veri değiştirilmez, değiştirilmek istendiğinde yeni bir kopyası oluşturulur.
    *   Java'da `final` keyword'ü ve `Record` (Java 14+) yapıları bunu destekler.

3.  **High Order Functions (Yüksek Mertebeli Fonksiyonlar):**
    *   Parametre olarak fonksiyon alan veya geriye fonksiyon döndüren fonksiyonlardır.
    *   Java'da metodlara parametre olarak `Function`, `Predicate` vb. geçmek buna örnektir.

```java
// High Order Function Örneği
public void process(List<String> list, Consumer<String> action) {
    for (String s : list) {
        action.accept(s); // Parametre olarak gelen fonksiyonu çalıştır
    }
}
```

---

### 2. Functional Composition (Fonksiyonel Bileşim)

Küçük ve bağımsız fonksiyonları birleştirerek daha karmaşık iş mantıkları oluşturmaktır. Java'daki Functional Interface'ler bunu `default` metodlarla (`andThen`, `compose`, `and`, `or`) sağlar.

#### Function Composition (`Function<T, R>`)
*   `f1.andThen(f2)`: Önce f1 çalışır, çıktısı f2'ye girdi olur.
*   `f1.compose(f2)`: Önce f2 çalışır, çıktısı f1'e girdi olur.

```java
Function<Integer, Integer> multiplyBy2 = x -> x * 2;
Function<Integer, Integer> add10 = x -> x + 10;

// (5 * 2) + 10 = 20
Function<Integer, Integer> pipeline = multiplyBy2.andThen(add10);
System.out.println(pipeline.apply(5)); 
```

#### Predicate Composition (`Predicate<T>`)
Mantıksal operatörler gibi çalışır (`and`, `or`, `negate`).

```java
Predicate<String> startsWithA = s -> s.startsWith("A");
Predicate<String> endsWithZ = s -> s.endsWith("Z");

// A ile başlayıp Z ile bitmeyenler
Predicate<String> complexRule = startsWithA.and(endsWithZ.negate());
```

---

### 3. Stream API (Akışlar)

**Analoji:** Stream, bir "fabrika montaj hattı" (assembly line) gibidir. Hammadde (veri kaynağı) banda girer, çeşitli istasyonlardan (intermediate operations) geçer (boyanır, kesilir) ve sonunda paketlenir (terminal operation).

#### Özellikleri
1.  **Veri Saklamaz:** Veri yapısı değildir, veri üzerinde işlem yapma aracıdır.
2.  **Functional:** Kaynak veriyi değiştirmez (Immutable).
3.  **Lazy Evaluation (Tembel Değerlendirme):** Terminal operasyonu (örn: `collect`, `forEach`) çağrılana kadar ara işlemler (örn: `filter`, `map`) ÇALIŞTIRILMAZ.
4.  **Short-circuiting:** `findFirst`, `limit` gibi işlemlerde tüm veriyi işlemeye gerek kalmadan sonuç dönebilir.

#### Operasyon Türleri

| Tip | Açıklama | Örnekler |
| :--- | :--- | :--- |
| **Intermediate** (Ara) | Stream döndürür, zincirleme yapılır. Lazy'dir. | `filter`, `map`, `flatMap`, `sorted`, `distinct`, `limit`, `peek` |
| **Terminal** (Sonlandırıcı) | Stream'i tüketir ve sonuç (List, int, void) döner. | `collect`, `forEach`, `reduce`, `count`, `min`, `max`, `anyMatch` |

#### Örnek Senaryo

```java
List<String> names = Arrays.asList("Ali", "Ayşe", "Mehmet", "Ahmet", "Ece");

// A ile başlayan isimleri uzunluklarına göre sıralayıp büyük harfe çevir
List<String> result = names.stream()
    .filter(name -> name.startsWith("A")) // 1. Filtrele
    .map(String::toUpperCase)             // 2. Dönüştür
    .sorted()                             // 3. Sırala
    .collect(Collectors.toList());        // 4. Topla (Terminal)
```

#### `map` vs `flatMap` (Tekrar)
*   `map`: `Stream<T>` -> `Stream<R>` (1'e 1 dönüşüm)
*   `flatMap`: `Stream<T>` -> `Stream<R>` (1'e N dönüşüm, iç içe yapıları düzleştirir).

```java
// Dosyadaki kelimeleri unique olarak listeleme
Files.lines(path) // Stream<String> (Satırlar)
     .flatMap(line -> Arrays.stream(line.split(" "))) // Stream<String> (Kelimeler)
     .distinct()
     .collect(Collectors.toList());
```

---

### 4. Parallel Streams

Stream API, çok çekirdekli işlemcileri kullanmayı çok kolaylaştırır. `stream()` yerine `parallelStream()` demek yeterlidir.

**Nasıl Çalışır?**
Veriyi parçalara böler (ForkJoinPool kullanarak), her parçayı ayrı thread'de işler ve sonuçları birleştirir.

**Ne Zaman Kullanılmalı?**
*   Veri miktarı ÇOK büyükse.
*   Her bir eleman için yapılan işlem (CPU maliyeti) yüksekse.
*   Sıralama (ordering) önemli değilse.

**Riskleri:**
*   Thread-safety sorunu (eğer side-effect varsa).
*   Common ForkJoinPool'u tıkama riski (tüm uygulama aynı havuzu kullanır).
*   Küçük veri setlerinde, thread yönetim maliyeti işlemden daha pahalıya patlar ve daha YAVAŞ çalışır.

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: Stream API'de `reduce` metodu ne işe yarar?
**Cevap:** Stream'deki elemanları tek bir sonuca indirger (reduction). `sum`, `min`, `max` aslında özelleşmiş reduce işlemleridir.
*   `T reduce(T identity, BinaryOperator<T> accumulator)`
*   Örnek: `int sum = numbers.stream().reduce(0, (a, b) -> a + b);`
*   Burada `0` başlangıç değeridir, `(a, b) -> a + b` ise birleştirme kuralıdır.

#### Soru 2: `findFirst()` ile `findAny()` arasındaki fark nedir?
**Cevap:**
*   **`findFirst()`:** Stream'deki (encounter order'a göre) **ilk** elemanı döner. Sıralı streamlerde deterministiktir.
*   **`findAny()`:** Stream'deki **herhangi bir** elemanı döner. Özellikle **Parallel Stream**'lerde performans için kullanılır, çünkü hangisini önce bulursa onu getirir, sırayı beklemez.

#### Soru 3: Bir Stream tekrar kullanılabilir mi?
**Cevap:** Hayır. Bir Stream terminal operasyonu ile tüketildikten sonra kapanır. Tekrar kullanmaya çalışırsanız `IllegalStateException: stream has already been operated upon or closed` hatası alırsınız. Veri kaynağını (Collection) tekrar stream'e dönüştürmeniz gerekir.

#### Soru 4: `peek()` metodu ne için kullanılır?
**Cevap:** `peek`, stream akarken araya girip elemanları "gözlemlemek" (loglamak) için kullanılır. Stream'i değiştirmez.
*   **Dikkat:** Intermediate bir operasyondur. Eğer terminal operasyonu yoksa `peek` içindeki kod ASLA çalışmaz (Lazy evaluation).
*   `stream.peek(System.out::println).collect(...)` -> Debug için harikadır.

#### Soru 5: Parallel Stream kullanırken nelere dikkat etmeliyiz?
**Cevap:**
1.  **Stateful Lambda:** Lambda ifadeleri durum tutmamalıdır (stateless).
2.  **Thread-Safety:** Paylaşılan bir listeye `parallelStream().forEach(list::add)` yaparsanız veri kaybı veya hata olur. `collect()` veya thread-safe koleksiyonlar kullanılmalı.
3.  **Blocking I/O:** Parallel stream varsayılan olarak işlemci çekirdek sayısı kadar thread kullanır. Eğer I/O işlemi yapıp bu threadleri bloklarsanız, tüm uygulamamın parallel stream performansı çöker.

#### Soru 6 (Tricky): `Optional.map()` ile `Optional.flatMap()` farkı nedir?
**Cevap:**
*   **`map()`:** Değeri dönüştürür ve `Optional` içine sarar.
*   **`flatMap()`:** Zaten `Optional` dönen bir fonksiyonu kullanır, iç içe `Optional` oluşmasini önler.
```java
Optional<String> opt = Optional.of("abc");
opt.map(s -> s.toUpperCase()); // Optional<String>
opt.flatMap(s -> Optional.of(s.toUpperCase())); // İç içe Optional olmasın
```

#### Soru 7 (Tricky): Stream'de `collect(Collectors.toList())` ile `.toList()` (Java 16+) farkı nedir?
**Cevap:**
*   **`collect(Collectors.toList())`:** Değiştirilebilir (mutable) bir liste döner.
*   **`.toList()`:** **Immutable** (değiştirilemez) bir liste döner.
*   **Trap:** `.toList()` ile dönen listeye `add()` yapmaya çalışırsanız `UnsupportedOperationException` alırsınız.

#### Soru 8 (Tricky): `Stream.generate()` ile `Stream.iterate()` farkı nedir?
**Cevap:**
*   **`generate(Supplier)`:** Her eleman bağımsızdır. Örn: `Stream.generate(Math::random)` - her seferinde rastgele sayı.
*   **`iterate(seed, UnaryOperator)`:** Her eleman bir önceki elemana bağlıdır. Örn: `Stream.iterate(0, n -> n + 1)` - 0, 1, 2, 3...

---

### 6. Geliştirici İpuçları

*   **Debug Zorluğu:** Stream zincirleri (chain) debug etmesi zordur. IntelliJ IDEA'daki "Java Stream Debugger" eklentisini veya `peek()` metodunu kullanın.
*   **For Loop vs Stream:** Basit döngüler için (örn: `for (int i=0; i<10; i++)`) Stream kullanmak "over-engineering" olabilir ve performans kaybı yaratabilir. Stream'i okunabilirliği artırdığı ve karmaşık filtreleme/dönüştürme işlemleri olduğu zaman kullanın.
*   **Side-Effects:** `forEach` içinde veritabanına yazmak veya dış değişkeni değiştirmek yerine, `collect` ile veriyi toplayıp sonra işlem yapmayı tercih edin. Stream'in doğası "fonksiyonel" kalmalıdır.
*   **Primitive Streams:** `Stream<Integer>` yerine `IntStream`, `LongStream` kullanın. Boxing/Unboxing maliyetinden (Integer -> int) kurtarır ve bellek/performans avantajı sağlar. `mapToInt` ile geçiş yapabilirsiniz.

---

