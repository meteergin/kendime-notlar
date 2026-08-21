## Konu 1: Java Hafıza Yapıları (Java Memory Structures)

Java Sanal Makinesi (JVM), işletim sisteminden aldığı belleği kendi içinde farklı alanlara bölerek yönetir. Bu alanların her birinin farklı bir amacı, yaşam döngüsü ve yönetim şekli vardır. Bir geliştirici olarak, sadece "Heap nesneleri tutar, Stack metodları tutar" demek yeterli değildir; bu yapıların nasıl etkileştiğini, performans etkilerini ve GC (Garbage Collector) ile ilişkisini derinlemesine bilmek gerekir.

### 1. Genel Bakış (JVM Runtime Data Areas)

JVM hafızası temel olarak 5 ana bölüme ayrılır:
1.  **Heap Area** (Yığın Alanı)
2.  **Stack Area** (Yığın/İstif Alanı) - Her Thread için ayrı
3.  **Method Area / Metaspace** (Metot Alanı)
4.  **PC Register** (Program Sayacı) - Her Thread için ayrı
5.  **Native Method Stack** - Her Thread için ayrı

---

### 2. Stack Memory (Yığın Bellek)

**Analoji:** Stack'i bir "iş takip defteri" veya üst üste dizilmiş "tabaklar" gibi düşünebilirsiniz. Her yeni iş (metot çağrısı) geldiğinde en üste yeni bir sayfa (Frame) açılır, iş bitince o sayfa yırtılıp atılır. Erişim her zaman en üstteki elemanadır (LIFO - Last In First Out).

*   **Yapısı:** Thread-safe'dir. Her Thread oluşturulduğunda kendine ait bir Stack alanı tahsis edilir. Diğer thread'ler bu alana erişemez.
*   **İçeriği (Stack Frames):** Her metot çağrısı yapıldığında, Stack içinde yeni bir blok oluşturulur. Buna **Stack Frame** denir.
    *   **Local Variables Array:** Metot içindeki ilkel tipler (int, double, boolean vb.) ve nesne referansları (heap'teki nesnenin adresi) burada tutulur.
    *   **Operand Stack:** Ara işlem sonuçlarının tutulduğu yerdir (örneğin `a + b` işlemi yapılırken değerler buraya atılır ve çekilir).
    *   **Frame Data:** Constant pool referansları, return adresleri vb.
*   **Yaşam Döngüsü:** Metot çalışmayı bitirdiğinde (return olduğunda veya exception fırlattığında), ilgili Stack Frame yok edilir. Bellek otomatik olarak temizlenir, GC buraya uğramaz.
*   **Hata Durumu:** Eğer stack alanı dolarsa (örneğin sonsuz recursive çağrı), `java.lang.StackOverflowError` fırlatılır.

### 3. Heap Memory (Öbek Bellek)

**Analoji:** Heap, büyük bir "serbest çalışma masası" veya "depo" gibidir. Herkes (tüm thread'ler) buraya erişebilir, buraya bir şeyler koyabilir. Ancak burası çok dağınık olabilir ve düzenli aralıklarla bir temizlikçinin (Garbage Collector) gelip kullanılmayan eşyaları atması gerekir.

*   **Yapısı:** Tüm thread'ler tarafından paylaşılan ortak alandır. JVM başladığında oluşturulur.
*   **İçeriği:** `new` anahtar kelimesi ile oluşturulan **TÜM NESNELER** (Objects) ve **DİZİLER** (Arrays) burada saklanır. İlkel tipler (primitive types) asla burada tek başına durmaz, ancak bir nesnenin üye değişkeni (field) iseler nesne ile birlikte Heap'te dururlar.
*   **Nesil Yapısı (Generational Heap):** GC performansını artırmak için Heap genellikle bölgelere ayrılır (Java versiyonuna ve GC algoritmasına göre değişebilir ama klasik yapı şöyledir):
    *   **Young Generation (Genç Nesil):** Yeni oluşturulan nesneler buraya (Eden Space) gelir. Hayatta kalanlar Survivor alanlarına (S0, S1) taşınır.
    *   **Old Generation (Yaşlı Nesil):** Young generation'da uzun süre hayatta kalan (birçok GC döngüsünden sağ çıkan) nesneler buraya terfi eder.
*   **Hata Durumu:** Heap dolarsa ve GC yer açamazsa `java.lang.OutOfMemoryError: Java heap space` hatası alınır.

### 4. Method Area / Metaspace

*   **Java 7 ve öncesi (PermGen):** Sınıf yapıları, statik değişkenler, string pool (Java 6 ve öncesi) burada tutulurdu. Heap'in bir parçası gibi yönetilirdi ve boyutu sabitti, bu da sık sık `OutOfMemoryError: PermGen space` hatasına yol açardı.
*   **Java 8 ve sonrası (Metaspace):** PermGen kaldırıldı. Artık sınıf metadata'ları **Native Memory** (İşletim sistemi belleği) üzerinde tutulur.
*   **Özellikleri:** Class tanımları, metot kodları, statik değişkenler burada bulunur. Boyutu dinamik olarak büyüyebilir (OS limiti kadar).

---

### 5. Karşılaştırma: Stack vs Heap

| Özellik | Stack Memory | Heap Memory |
| :--- | :--- | :--- |
| **Kullanım Amacı** | Metot çalıştırma, lokal değişkenler, referanslar | Nesneler ve sınıf instance'ları |
| **Erişim Hızı** | Çok hızlıdır (LIFO erişim) | Stack'e göre daha yavaştır (Kompleks bellek yönetimi) |
| **Boyut** | Küçüktür, işletim sistemine göre limitlidir | Büyüktür, JVM parametreleri ile (-Xmx) ayarlanabilir |
| **Yaşam Döngüsü** | Metot bitince biter | Uygulama çalıştığı sürece veya GC temizleyene kadar |
| **Görünürlük** | Sadece sahibi olan Thread görür (Thread-Safe) | Tüm Thread'ler görebilir (Thread-Safe Değil) |
| **Yönetim** | Otomatik (OS/JVM tarafından) | Garbage Collector tarafından |

---

### 6. Kod Üzerinden İnceleme

```java
class Person {
    int id;
    String name;

    public Person(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

public class MemoryTest {
    public static void main(String[] args) { // 1. main() Stack Frame oluşur
        int x = 10; // 2. Stack'te 'x' değişkeni oluşturulur, değeri 10
        Person p = new Person(1, "Ali"); // 3. Heap'te Person nesnesi oluşur. Stack'te 'p' referansı oluşur.
        doSomething(p); // 4. doSomething() Stack Frame oluşur
    } // 5. main() biter, Stack Frame silinir, program sonlanır.

    public static void doSomething(Person person) {
        String message = "Merhaba"; // 6. Heap'te (String Pool) "Merhaba" oluşur (veya varsa referans alınır). Stack'te 'message' referansı oluşur.
    } // 7. doSomething() biter, 'message' ve 'person' (kopya referans) Stack'ten silinir.
}
```

**Adım Adım Analiz:**
1.  `main` metodu çağrıldığında Stack'te bir blok açılır.
2.  `int x = 10;` -> `x` ilkel tip olduğu için değeriyle birlikte Stack'te saklanır.
3.  `Person p = new Person(...)` ->
    *   `new Person(...)`: Heap alanında bir Person nesnesi oluşturulur. Bu nesnenin içinde `id` (int) ve `name` (String referansı) için yer ayrılır.
    *   `p`: Stack alanında bir referans (adres) değişkeni oluşturulur. Bu değişken Heap'teki Person nesnesini işaret eder.
4.  `doSomething(p)` çağrıldığında Stack'in en tepesine yeni bir Frame eklenir.
    *   `p` referansı kopyalanarak metoda geçilir (Java pass-by-value çalışır, ama nesneler için referansın kopyası/değeri geçilir).

---

### 7. Kritik Mülakat Soruları ve Cevapları 

#### Soru 1: Java'da parametre geçişi nasıldır? Pass-by-value mu, pass-by-reference mı?
**Cevap:** Java **kesinlikle ve her zaman Pass-by-Value (Değer ile geçiş)** kullanır.
*   **İlkel tiplerde:** Değerin kendisi kopyalanır. Metot içinde yapılan değişiklik orijinali etkilemez.
*   **Nesnelerde:** Nesnenin **referansının değeri (bellek adresi)** kopyalanır.
    *   *Senaryo:* Metot içinde `nesne.setName("Ece")` derseniz, orijinal nesne değişir çünkü kopyalanan adres aynı evi gösteriyordur.
    *   *Senaryo:* Metot içinde `nesne = new Person(...)` derseniz, sadece o metot içindeki yerel referans yeni bir adresi gösterir. Orijinal `p` değişkeni hala eski nesneyi göstermeye devam eder. Bu, pass-by-reference olmadığının kanıtıdır.

#### Soru 2: `StackOverflowError` ile `OutOfMemoryError` arasındaki fark nedir? Hangi durumlarda oluşur?
**Cevap:**
*   **StackOverflowError:** Stack belleğinin dolmasıdır. Genellikle **hatalı recursive (özyinelemeli)** metot çağrılarında oluşur. Metot sürekli kendini çağırır ve return etmezse, stack frame'ler üst üste biner ve limit aşılır.
*   **OutOfMemoryError (Heap Space):** Heap belleğinin dolmasıdır.
    *   Çok fazla nesne üretip referanslarını tutuyorsanız (Memory Leak).
    *   Çok büyük bir dosya içeriğini tek seferde belleğe almaya çalışıyorsanız.
    *   GC, nesneleri temizlemeye yetişemiyorsa veya temizlenecek nesne yoksa oluşur.

#### Soru 3: String Pool nedir ve hafızanın neresinde bulunur?
**Cevap:** String Pool, String literallerini (`"Merhaba"`) saklamak için kullanılan özel bir alandır. Amacı bellek tasarrufudur.
*   **Java 6 ve öncesi:** PermGen içindeydi.
*   **Java 7 ve sonrası:** **Heap** alanına taşındı. Bu önemlidir çünkü PermGen sınırlıydı, Heap ise çok daha geniştir.
*   `String s = "Test";` derseniz JVM önce Pool'a bakar. Varsa referansını döner, yoksa oluşturur.
*   `String s = new String("Test");` derseniz Pool kontrol edilmez, Heap'te zorla yeni bir nesne oluşturulur.

#### Soru 4: Bir nesnenin Heap'te mi yoksa Stack'te mi oluşturulacağına biz karar verebilir miyiz? (C++'daki gibi)
**Cevap:** Hayır, Java'da nesneler (Objects) her zaman Heap'te oluşturulur. Stack'te sadece bu nesnelerin referansları tutulur. Ancak, **JIT Compiler** (Just-In-Time) ve **Escape Analysis** (Kaçış Analizi) denilen bir optimizasyon tekniği vardır. Eğer JVM, bir nesnenin metot dışına çıkmadığını (escape etmediğini) ve sadece o metot içinde kullanıldığını analiz ederse, optimizasyon olarak o nesneyi (veya parçalarını) Stack üzerinde tutabilir (Scalar Replacement). Bu tamamen JVM'in inisiyatifindedir, geliştirici doğrudan kontrol edemez.

#### Soru 5: Statik metotlar ve değişkenler hafızada nerede tutulur?
**Cevap:** Statik değişkenler (class variables) sınıfın bir parçasıdır. Java 8 öncesinde PermGen'de, Java 8 ve sonrasında ise Heap içinde, ilgili Class nesnesinin (java.lang.Class instance) bir parçası olarak tutulurlar (Metaspace sadece sınıf metadata'sını tutar, statik field'ların değerleri genellikle Heap'teki Class objesindedir, ancak implementasyona göre Metaspace'de de referanslanabilirler. Modern HotSpot JVM'lerde statik değişkenler Heap'te saklanan Class nesnesinin içindedir).

#### Soru 6: Metaspace neden PermGen'in yerini aldı? Avantajı nedir?
**Cevap:**
*   **Esneklik:** PermGen sabit boyutluydu ve JVM başlatılırken belirlenirdi. Metaspace ise Native Memory kullanır ve işletim sistemi izin verdiği sürece büyüyebilir.
*   **GC Performansı:** PermGen'in temizlenmesi (GC) karmaşıktı ve genellikle Full GC gerektirirdi. Metaspace yönetimi daha moderndir ve sınıf yükleme/boşaltma işlemleri daha verimlidir.
*   **OOM Hataları:** `java.lang.OutOfMemoryError: PermGen space` hatası eski Java uygulamalarının kabusuydu. Metaspace ile bu risk azaltıldı (yine de native memory sızıntısı olursa makineyi kilitleyebilir).

#### Soru 7 (Tricky): `String s1 = "Java"; String s2 = new String("Java"); s2.intern();` ifadesinden sonra `s1 == s2` sonucu ne olur?
**Cevap:** **False** olur!
*   `s1`, String Pool'daki "Java" literalinin referansını tutar.
*   `s2 = new String("Java")` Heap'te yeni bir String nesnesi oluşturur (Pool'da değil).
*   `s2.intern()` metodu Pool'a bakar. "Java" zaten Pool'da olduğu için Pool'daki referansı döner ama **s2 değişkenine atama yapmadık!**
*   `s2` hala Heap'teki nesneyi gösterdiği için `s1 == s2` false döner.
*   **Doğru kullanım:** `s2 = s2.intern();` olmalıydı, o zaman true dönerdi.

#### Soru 8 (Tricky): `Integer a = 127; Integer b = 127; a == b` sonucu ne olur? Peki `Integer c = 128; Integer d = 128; c == d`?
**Cevap:** İlki **true**, ikincisi **false**!
*   Java, `-128` ile `127` arasındaki Integer nesneleri **Integer Cache** (flyweight pattern) kullanarak cache'ler.
*   `Integer a = 127;` (autoboxing) aslında `Integer.valueOf(127)` çağrılır ve cache'ten döner.
*   Her iki `a` ve `b` aynı cache'lenmiş nesneyi gösterir, bu yüzden `a == b` true.
*   Ancak `128`, cache aralığının dışında olduğu için her seferinde yeni nesne oluşturulur. `c` ve `d` farklı nesneler, bu yüzden `c == d` false.
*   **Trap:** Mülakatta sizi "Integer karşılaştırması hep false olur" dedirtmeye çalışabilir. Aralığa dikkat!

#### Soru 9 (Tricky): Bir metodun içinde oluşturulan yerel değişkenler (local variables) Garbage Collection ile silinir mi?
**Cevap:** **Hayır!** Yerel değişkenler Stack'te saklanır ve metot bittiğinde **Stack Frame kaldırılır**, yani otomatik silinir. GC **sadece Heap'teki nesneleri** temizler.
*   **Trap:** GC her zaman devreye girer sanılabilir ama Stack'teki veriler GC'nin işi değildir.

#### Soru 10 (Tricky): `finalize()` metodu ne zaman çalışır? Güvenilir bir kaynak temizleme yöntemi midir?
**Cevap:** `finalize()` metodu, bir nesne GC tarafından silinmeden **hemen önce** çağrılır (teoride). Ancak:
*   **Garantisiz:** GC ne zaman çalışacağı belirsizdir, dolayısıyla `finalize()` hiç çalışmayabilir veya çok geç çalışabilir.
*   **Deprecated:** Java 9'dan itibaren deprecated edildi. **Try-with-resources** veya `Cleaner` API kullanılmalı.
*   **Performans Sorunu:** Finalize içeren nesneler GC tarafından daha yavaş temizlenir (ek bir cycle gerektirir).
*   **Trap:** Mülakat sorunda "Dosya kapatma için finalize kullanırım" derseniz kötü puan alırsınız.

### 8. Geliştirici İpuçları (Pros/Cons & Best Practices)

*   **Pro:** Java'nın otomatik bellek yönetimi (Stack/Heap ayrımı ve GC), geliştiriciyi `malloc`/`free` gibi manuel işlemlerden kurtarır, memory leak riskini azaltır (yok etmez!).
*   **Con:** GC çalıştığında "Stop-the-world" (uygulamanın donması) olayları yaşanabilir. Heap çok büyükse GC süresi uzayabilir.
*   **Tavsiye:** Mülakatta "Memory Leak Java'da olmaz" demeyin. Olur! Eğer statik bir `List`'e sürekli nesne ekler ve silmezseniz, bu nesneler Heap'te kalır, GC bunları silemez çünkü hala referansları vardır. Buna **Java Memory Leak** denir.
*   **Tavsiye:** `String` birleştirmelerinde döngü içinde `+` operatörü kullanmak yerine `StringBuilder` kullanın. Her `+` işlemi Heap'te gereksiz geçici String nesneleri oluşturur ve belleği kirletir.

---

