## Konu 39: JVM Internals, GC Tuning & Performance Optimization

Senior bir Java geliştiricisi, yazdığı kodun makine seviyesinde nasıl çalıştığını bilmelidir. "Memory Leak" ile "Stack Overflow" arasındaki farkı bilmek yetmez; Garbage Collector'ın nasıl çalıştığını, JIT derleyicisinin optimizasyonlarını ve JVM parametrelerini (flags) bilmek gerekir.

---

### 1. Garbage Collection (GC) Algoritmaları

GC, Heap belleğindeki kullanılmayan nesneleri temizler. Ancak "nasıl" temizlediği performansı doğrudan etkiler.

#### G1GC (Garbage First Garbage Collector)
*   **Default (Java 9+):** Sunucu uygulamaları için varsayılan GC'dir.
*   **Region-Based:** Heap'i eşit boyutlu küçük bölgelere (Region) böler.
*   **Nasıl Çalışır?** En çok çöp (garbage) içeren bölgeyi öncelikli temizler (Garbage First).
*   **Avantaj:** Pause time (duraksama süresi) tahmin edilebilir. `-XX:MaxGCPauseMillis=200` diyerek hedeflenen duraksama süresini verebilirsiniz.

#### ZGC (Z Garbage Collector)
*   **Low Latency:** Java 11'de deneysel, Java 15'te production-ready oldu.
*   **Hedef:** Terabyte'larca heap olsa bile duraksama süresini **<1ms** (Java 21 ile) tutmak.
*   **Teknik:** "Colored Pointers" ve "Load Barriers" kullanır. Uygulama çalışırken (concurrent) temizlik yapar.

#### Shenandoah GC
*   Red Hat tarafından geliştirilen, ZGC benzeri ultra-low latency GC.

---

### 2. JIT (Just-In-Time) Compilation

Java "Write Once, Run Anywhere" der ama performans için makine koduna dönüşmesi gerekir.

*   **Interpreter:** Bytecode'u satır satır okur ve çalıştırır. (Yavaş).
*   **C1 Compiler (Client):** Hızlı başlar, basit optimizasyonlar yapar.
*   **C2 Compiler (Server):** Uygulama çalıştıkça "Hot Spot"ları (çok çalışan metodları) analiz eder ve çok agresif optimizasyonlar yapar (Inlining, Loop Unrolling).
*   **Tiered Compilation:** Önce C1 ile hızlı başlatır, sonra C2 ile optimize eder (Varsayılan).

---

### 3. Classloading Mekanizması

JVM, sınıfları ihtiyaç duyduğunda yükler (Lazy Loading).

1.  **Bootstrap ClassLoader:** `rt.jar` (java.lang.*, java.util.*) gibi çekirdek sınıfları yükler. (Java ile değil, C++ ile yazılmıştır).
2.  **Platform (Extension) ClassLoader:** JDK uzantılarını yükler.
3.  **Application (System) ClassLoader:** Classpath'teki (`-cp`) sizin sınıflarınızı yükler.

**Delegation Model:** Bir ClassLoader sınıf yüklemeden önce üstüne (parent) sorar. "Sen yükledin mi?". Eğer en tepeye kadar bulunamazsa kendisi yüklemeye çalışır.

---

### 4. Kritik Mülakat Soruları

**Soru 1: "Stop-the-World" (STW) nedir?**
*   **Cevap:** GC çalışırken uygulamanın tamamen durduğu andır. Tüm thread'ler donar. G1GC ve ZGC bunu minimize etmeye çalışır ama tamamen sıfırlayamaz (ZGC neredeyse sıfırlar).

**Soru 2: Memory Leak (Bellek Sızıntısı) Java'da nasıl olur? GC yok mu?**
*   **Cevap:** GC sadece **referansı olmayan** nesneleri siler. Eğer kullanılmayan bir nesneye hala referans varsa (örn: static bir List'e ekleyip silmeyi unutmak, kapatılmayan DB bağlantısı), GC onu silemez. Heap dolar ve `OutOfMemoryError` alırsınız.

**Soru 3: `System.gc()` çağırmak iyi midir?**
*   **Cevap:** Asla. Bu sadece bir "istek"tir, garantisi yoktur. Ayrıca GC'nin kendi algoritmasını bozabilir ve performansı düşürebilir.

---

