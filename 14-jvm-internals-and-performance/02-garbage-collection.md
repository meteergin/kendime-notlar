# Garbage Collection Tuning ve GC Algoritmaları

> **Analoji:** GC, bir "apartman çöp toplama servisi" gibidir. Daireler (nesneler) çöplerini biriktirir. Çöpçü (GC) gelip kullanılmayan çöpleri toplar. Ama çöp toplarken merdivenleri ve asansörleri geçici olarak kapatır (Stop-the-World pause). İyi bir çöp servisi, bina sakinlerini minimum rahatsız ederek temizlik yapar.

---

## 1. JVM Heap Yapısı

```
┌────────────────── JVM Heap ──────────────────────┐
│                                                    │
│  ┌─── Young Generation ────┐  ┌─── Old Gen ────┐  │
│  │  ┌─Eden─┐ ┌S0┐ ┌S1┐    │  │                 │  │
│  │  │  New │ │  │ │  │    │  │  Long-lived      │  │
│  │  │ obj  │ │  │ │  │    │  │  objects          │  │
│  │  └──────┘ └──┘ └──┘    │  │                 │  │
│  └─────────────────────────┘  └─────────────────┘  │
│                                                    │
│  ┌─── Metaspace (Non-Heap) ───┐                   │
│  │  Class metadata, method    │                   │
│  │  data, constant pool       │                   │
│  └────────────────────────────┘                   │
└────────────────────────────────────────────────────┘
```

### Nesne Yaşam Döngüsü

1. Nesne **Eden**'de oluşturulur
2. Eden dolunca **Minor GC** çalışır → hayatta kalanlar **Survivor** (S0→S1) alanına taşınır
3. Survivor'da belirli sayıda GC'den sağ çıkan nesneler **Old Generation**'a taşınır (tenuring threshold)
4. Old Gen dolunca **Major/Full GC** çalışır (pahalı, uzun pause)

---

## 2. GC Algoritmaları

### Serial GC (`-XX:+UseSerialGC`)
- Tek thread ile GC yapar
- Küçük uygulamalar, client JVM'ler için
- **Hiç production'da kullanmayın**

### Parallel GC (`-XX:+UseParallelGC`)
- Birden fazla thread ile GC (throughput odaklı)
- Java 8'de varsayılan
- Yüksek throughput, ama uzun pause süreleri

### G1 GC (`-XX:+UseG1GC`) ⭐
- Java 9+'da varsayılan. **Çoğu production sistemi için ideal**
- Heap'i eşit büyüklükte bölgelere (region) ayırır
- Pause süresi hedefi belirlenebilir (`-XX:MaxGCPauseMillis=200`)
- Mixed GC: Young + seçilmiş Old region'ları birlikte temizler

### ZGC (`-XX:+UseZGC`) 🚀
- Java 15+ (production-ready: Java 17+)
- **Sub-millisecond** pause süreleri (< 1ms)
- Terabayt'larca heap destekler
- Colored pointers + load barriers kullanır
- **Büyük heap, düşük latency gerektiren sistemler için ideal**

### Shenandoah GC (`-XX:+UseShenandoahGC`)
- Red Hat tarafından geliştirilmiş
- ZGC'ye benzer düşük pause süreleri
- Concurrent compaction

### Karşılaştırma

| GC | Pause Süresi | Throughput | Heap Boyutu | Java Versiyonu |
| :--- | :--- | :--- | :--- | :--- |
| **Serial** | Yüksek | Düşük | Küçük | Tümü |
| **Parallel** | Orta-Yüksek | Çok Yüksek | Orta | 8 default |
| **G1** | Düşük-Orta | Yüksek | Büyük | 9+ default |
| **ZGC** | Çok Düşük (<1ms) | Yüksek | Çok Büyük (TB) | 17+ |
| **Shenandoah** | Çok Düşük | Yüksek | Büyük | 12+ |

---

## 3. GC Tuning Parametreleri

### Temel JVM Ayarları

```bash
java -Xms2g -Xmx2g \                # Min/Max heap (aynı tutun!)
     -XX:+UseG1GC \                  # GC algoritması
     -XX:MaxGCPauseMillis=200 \      # Hedef pause süresi (ms)
     -XX:+HeapDumpOnOutOfMemoryError \  # OOM'da heap dump
     -XX:HeapDumpPath=/tmp/heapdump.hprof \
     -Xlog:gc*:file=gc.log:time \    # GC logları
     -jar app.jar
```

### Spring Boot İçin Önerilen Ayarlar

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-Xms512m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+HeapDumpOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 4. Memory Leak Tespiti

### Yaygın Sebepler

1. **Static Collections:** Static `Map` veya `List`'e sürekli ekleme yapıp hiç silmeme
2. **Listener/Observer Pattern:** Kayıt olup (register) hiç çıkmama (unregister)
3. **ThreadLocal:** Thread pool'da temizlenmemiş ThreadLocal değerleri
4. **Unclosed Resources:** Stream, Connection, FileHandle kapatılmaması
5. **Inner Class:** Non-static inner class, dış sınıfın referansını tutar

### Tespit Araçları

```bash
# Heap dump alma
jmap -dump:live,format=b,file=heap.hprof <PID>

# GC loglarını analiz
# GCViewer veya GCEasy.io kullanın

# JVisualVM ile canlı izleme
jvisualvm

# Flight Recorder (JFR) - Production'da güvenli
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr -jar app.jar
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: `-Xms` ve `-Xmx`'i neden aynı değere ayarlamalıyız?
**Cevap:** Heap'in runtime'da büyümesi (resize) GC pause'a neden olur. Aynı tutmak predictable performans sağlar ve resize overhead'ini ortadan kaldırır.

### Soru 2: G1 GC'de "Mixed GC" nedir?
**Cevap:** Young region'larla birlikte, en çok çöp içeren Old region'ları da temizler. Full GC'den kaçınmak için preventif çalışır. G1'in gücü burada.

### Soru 3: ZGC neden sub-millisecond pause sağlar?
**Cevap:** Tüm GC işlemlerini (marking, relocation) uygulama thread'leri ile **concurrent** (eşzamanlı) yapar. STW (Stop-the-World) sadece root scanning için birkaç mikrosaniye sürer.

### Soru 4 (Tricky): `finalize()` metodu neden kullanılmamalı?
**Cevap:** Java 9'da deprecated, Java 18'de kaldırıldı.
1. Ne zaman çağrılacağı garanti değil
2. GC'yi yavaşlatır (Finalizer thread)
3. Nesne resurrection riski (finalize'da `this` referansı kayda alınırsa GC toplayamaz)
4. **Alternatif:** `try-with-resources` + `Cleaner` (Java 9+)

### Soru 5 (Tricky): "GC thrashing" nedir?
**Cevap:** GC sürekli çalışıp neredeyse hiç uygulama kodu çalıştırılamadığı durum. Genellikle heap çok küçükken veya memory leak olduğunda oluşur. Uygulama çalışıyor gibi görünür ama aslında CPU'nun %99'unu GC harcıyordur.

---

## 6. Geliştirici İpuçları

- **Java 17+ kullanın:** G1 çok olgunlaştı, ZGC production-ready. Java 8'de kalmayın.
- **GC Log Analizi:** Her production deploy'dan sonra GC loglarını analiz edin. Anomali varsa hemen müdahale edin.
- **Object Pool Kullanmayın:** Modern GC'ler çok hızlıdır. Object pooling genellikle ters etki yapar (GC'nin nesne yaşam döngüsünü takip etmesini zorlaştırır).
- **Primitive Streams:** `Stream<Integer>` yerine `IntStream` kullanın. Boxing/Unboxing GC yükü oluşturur.
- **-XX:+UseStringDeduplication:** G1 GC ile String duplicate'larını otomatik temizler. Bellek tasarrufu sağlar.
