## Konu 5: Concurrency (Eşzamanlılık), Threads ve Virtual Threads

Concurrency, modern yazılım geliştirmenin en karmaşık ama en kritik konularından biridir. Bir Java geliştiricisi olarak sadece thread başlatmayı değil, **Thread Safety**, **Memory Visibility**, **Race Conditions** ve **Deadlock** gibi kavramları derinlemesine bilmeli ve Java 21 ile gelen devrimsel **Virtual Threads** özelliğine hakim olmalısınız.

---

### 1. Thread Temelleri ve Yaşam Döngüsü

**Analoji:** İşlemci (CPU) bir mutfak, Thread ise bir aşçıdır. Tek çekirdekli bir işlemcide aynı anda sadece bir aşçı çalışabilir (Time slicing ile sırayla). Çok çekirdekli işlemcide ise birden fazla aşçı aynı anda yemek yapabilir (Parallelism).

#### Thread Oluşturma Yöntemleri
1.  **`Thread` sınıfını extend etmek:** (Önerilmez, çünkü Java'da multiple inheritance yoktur).
2.  **`Runnable` interface'ini implement etmek:** (Önerilir, iş mantığını thread yapısından ayırır).
3.  **`Callable` ve `Future`:** Sonuç döndüren işler için.

```java
// Runnable (En yaygın)
Thread t1 = new Thread(() -> System.out.println("Thread çalışıyor"));
t1.start();
```

#### Thread Yaşam Döngüsü (States)
*   **New:** Oluşturuldu ama `start()` çağrılmadı.
*   **Runnable:** Çalışmaya hazır veya çalışıyor (CPU bekliyor olabilir).
*   **Blocked:** Bir kilidi (monitor lock) bekliyor.
*   **Waiting:** Başka bir thread'in sinyalini bekliyor (`wait()`, `join()`).
*   **Timed Waiting:** Süreli bekleme (`sleep(1000)`).
*   **Terminated:** İşini bitirdi.

---

### 2. Synchronization ve Race Condition

**Race Condition:** İki veya daha fazla thread'in paylaşılan bir veriye aynı anda erişip değiştirmeye çalışması ve sonucun, erişim sırasına bağlı olarak bozulması durumudur.

**Çözüm: Synchronization (Senkronizasyon)**
`synchronized` anahtar kelimesi, bir kod bloğuna veya metoda aynı anda sadece bir thread'in girmesini garanti eder (Mutual Exclusion).

```java
class Counter {
    private int count = 0;
    
    // Aynı anda sadece 1 thread girebilir
    public synchronized void increment() {
        count++;
    }
    
    // Veya blok seviyesinde (Daha performanslı olabilir)
    public void incrementBlock() {
        synchronized(this) {
            count++;
        }
    }
}
```

---

### 3. Volatile Keyword (Hafıza Görünürlüğü)

**Analoji:** `volatile`, "önbelleği (cache) atla, direkt ana hafızadan (RAM) oku/yaz" demektir.

#### Sorun: Caching
Modern CPU'lar performans için değişkenleri kendi L1/L2 önbelleklerinde (cache) tutarlar.
*   Thread A, `flag` değişkenini değiştirir (kendi cache'inde).
*   Thread B, `flag` değişkenini okur (kendi cache'inden).
*   Thread A'nın yaptığı değişiklik ana belleğe (RAM) henüz yazılmadığı için, Thread B değişikliği göremez.

#### Çözüm: `volatile`
Bir değişken `volatile` olarak işaretlenirse:
1.  **Visibility Guarantee:** Değere yapılan her yazma işlemi anında ana belleğe yansıtılır. Her okuma işlemi ana bellekten yapılır.
2.  **Instruction Reordering Engelleme:** Derleyici, optimizasyon için kod satırlarının yerini değiştiremez (Happens-before ilişkisi).

```java
private volatile boolean running = true;

public void stop() {
    running = false; // Diğer thread'ler bunu ANINDA görür
}
```

**Önemli:** `volatile` atomiklik (atomicity) SAĞLAMAZ. Yani `count++` işl**Cevap:** Hayır, `volatile` sadece tek bir değişkenin okunma/yazma işleminin atomic olmasını garanti eder. Ama `count++` aslında 3 işlemdir (oku + artır + yaz). `AtomicInteger` kullanılmalı veya `synchronized` blok gereklidir.

#### Soru 6 (Tricky): `synchronized` metod ile `synchronized(this)` arasında fark var mı?
**Cevap:** **Hayır**, aynıdır. Her ikisi de instance'ın monitor lock'ını kullanır.
```java
public synchronized void method1() { } // this'in lock'u
public void method2() { synchronized(this) { } } // Aynı
```
*   **Trap:** "Farklı lock kullanır" sanılabilir ama aynıdır.
*   **Static metod:** `synchronized(ClassName.class)` kullanır (class lock).

#### Soru 7 (Tricky): Deadlock nasıl oluşur? Nasıl önlenir?
**Cevap:** İki thread birbirinin tuttuğu kilidi bekliyorsa deadlock oluşur.
*   **Örnek:** Thread1: Lock A tuttu, Lock B bekliyor. Thread2: Lock B tuttu, Lock A bekliyor.
*   **Önleme:**
    1.  Lock'ları her zaman **aynı sırada** alın (lock ordering).
    2.  `tryLock()` ile timeout kullanın, kilit alamazsanız geri çekin.
    3.  Deadlock detection tool'ları kullanın (jstack, jconsole).

#### Soru 8 (Tricky): Virtual Thread'ler ne zaman kullanılır? Her zaman daha mı iyidir?
**Cevap:** **Hayır**, CPU-intensive işler için değil, **I/O-bound** işler için mükemmeldir.
*   **Uygun:** Database query, REST API call, file I/O (çok sayıda concurrent bağlantı).
*   **Uygunsuz:** CPU-intensive hesaplamalar (örn: matematik, kriptografi). Burada platform thread havuzu daha iyi.
*   **Trap:** "Virtual thread her durumda daha hızlıdır" yanlış bir algıdır.

---

### 4. Virtual Threads (Sanal Threadler - Java 21+)

Java tarihinin en büyük concurrency devrimidir (Project Loom).

**Problem:**
Geleneksel Java thread'leri (Platform Threads), işletim sistemi (OS) thread'lerine 1:1 bağlıdır.
*   Oluşturması maliyetlidir (bellek ve zaman).
*   Sayısı sınırlıdır (birkaç bin taneden sonra OS zorlanır).
*   Bloklandığında (I/O, DB sorgusu) OS thread'i de bloklanır ve kaynak israf edilir.

**Çözüm: Virtual Threads**
*   **Hafiftir:** JVM tarafından yönetilir (OS thread'i değildir).
*   **Milyonlarca** oluşturulabilir.
*   **Blocking ucuzdur:** Bir sanal thread bloklandığında (örn: DB beklerken), bağlı olduğu OS thread'i boşa çıkar ve başka bir sanal thread'i çalıştırır.
*   **Kullanım:** "Thread-per-request" modelini tekrar popüler hale getirmiştir. Reactive programlamanın (WebFlux) karmaşıklığına girmeden yüksek ölçeklenebilirlik sağlar.

```java
// Platform Thread (Eski - OS Thread)
Thread.ofPlatform().start(() -> System.out.println("Platform Thread"));

// Virtual Thread (Yeni - Java 21)
Thread.ofVirtual().start(() -> System.out.println("Virtual Thread"));

// Executor ile kullanımı
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 100_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(1000); // 100 bin thread uyur ama OS zorlanmaz!
            return i;
        });
    });
}
```

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: `volatile` ile `synchronized` farkı nedir?
**Cevap:**
*   **`volatile`:** Sadece değişkenler için kullanılır. Hafıza görünürlüğünü (visibility) garanti eder ama atomikliği (atomicity) garanti etmez. Thread'i bloklamaz.
*   **`synchronized`:** Metot veya bloklar için kullanılır. Hem görünürlüğü hem de atomikliği garanti eder. Thread'i bloklar (lock mekanizması).

#### Soru 2: Deadlock nedir? Nasıl önlenir?
**Cevap:** İki veya daha fazla thread'in, birbirlerinin elindeki kilidi beklemesi ve sonsuza kadar kilitlenmesi durumudur.
*   **Önleme:**
    1.  Kilitleri her zaman **aynı sırada** almak.
    2.  `tryLock()` gibi zaman aşımı olan kilit mekanizmaları kullanmak.
    3.  Kilit tutma süresini minimumda tutmak.

#### Soru 3: `wait()` ve `sleep()` farkı nedir?
**Cevap:**
*   **`sleep()`:** `Thread` sınıfının metodudur. Thread'i belirtilen süre kadar uyutur ama **elindeki kilitleri bırakmaz**.
*   **`wait()`:** `Object` sınıfının metodudur. Sadece `synchronized` blok içinde çağrılabilir. Thread'i uyutur ve **elindeki kilidi bırakır** (diğer thread'ler girebilsin diye). `notify()` ile uyandırılır.

#### Soru 4: Virtual Thread'ler her durumda Platform Thread'lerden hızlı mıdır?
**Cevap:** Hayır.
*   **I/O Bound (Giriş/Çıkış yoğun) işlerde:** Evet, çok daha verimlidir (DB sorguları, HTTP istekleri). Çünkü bekleme süresinde CPU boşa çıkmaz.
*   **CPU Bound (İşlemci yoğun) işlerde:** Hayır, fark yaratmaz hatta hafif bir yönetim maliyeti olabilir. Video işleme, karmaşık matematiksel hesaplamalar için hala Platform Thread'ler (veya ForkJoinPool) uygundur.

#### Soru 5: `AtomicInteger` nasıl çalışır? `synchronized`'dan farkı nedir?
**Cevap:** `AtomicInteger`, **CAS (Compare-And-Swap)** algoritmasını kullanır. Bu, donanım seviyesinde desteklenen iyimser (optimistic) bir kilitleme tekniğidir. `synchronized` gibi thread'i bloklamaz (lock-free), bu yüzden yüksek rekabetin olmadığı durumlarda çok daha hızlıdır.

---

### 6. Geliştirici İpuçları

*   **Thread Safety:** Bir sınıfın thread-safe olması için en iyi yol, onu **Immutable** (değişmez) yapmaktır. Durum (state) yoksa, race condition da yoktur.
*   **ExecutorService:** Asla ve asla üretim ortamında `new Thread().start()` ile manuel thread yönetmeyin. Her zaman `ExecutorService` (Thread Pool) kullanın. Bu, kaynak tüketimini kontrol altında tutar.
*   **Virtual Threads:** Eğer Spring Boot 3.2+ veya Java 21+ kullanıyorsanız, I/O yoğun mikroservislerinizde Virtual Threads'i etkinleştirin. Performans artışı ve kaynak tasarrufu muazzamdır.
*   **Double-Checked Locking:** Singleton pattern'de lazy loading yaparken `volatile` kullanmayı unutmayın, aksi takdirde yarım oluşmuş (partially constructed) nesne referansı dönebilir.

---

