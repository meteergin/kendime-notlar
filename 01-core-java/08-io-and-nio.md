# I/O ve NIO (File Operations, Channels, Buffers)

> **Analoji:** Klasik I/O, bir "tek şeritli köprü" gibidir — bir araç geçene kadar diğerleri bekler (blocking). NIO ise "çok şeritli otoyol" — birden fazla araç aynı anda geçebilir (non-blocking, channel-based).

---

## 1. Klasik I/O (java.io)

### Stream Kavramı

Java I/O, **Stream** (akış) kavramı üzerine kuruludur. Veri bir kaynak ile hedef arasında tek yönlü akar.

| Tür | Byte-Based | Character-Based |
| :--- | :--- | :--- |
| **Input (Okuma)** | `InputStream` | `Reader` |
| **Output (Yazma)** | `OutputStream` | `Writer` |

### Dosya Okuma/Yazma

```java
// Modern yaklaşım (try-with-resources)
try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
} // Otomatik kapanır (AutoCloseable)

// Yazma
try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
    writer.write("Merhaba Dünya");
    writer.newLine();
    writer.write("İkinci satır");
}
```

### Decorator Pattern (I/O'nun DNA'sı)

Java I/O, **Decorator Pattern** kullanır. Stream'leri sarmalayarak katman katman yetenek eklersiniz:

```java
// Katmanlı yapı:
InputStream raw = new FileInputStream("data.bin");        // Ham stream
InputStream buffered = new BufferedInputStream(raw);       // + Buffering
DataInputStream data = new DataInputStream(buffered);      // + Veri tipi okuma

int value = data.readInt();    // 4 byte'ı int olarak oku
double pi = data.readDouble(); // 8 byte'ı double olarak oku
```

### Serialization

```java
// Nesneyi dosyaya yazma
try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("user.ser"))) {
    oos.writeObject(new User("Mete", 30));
}

// Nesneyi dosyadan okuma
try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("user.ser"))) {
    User user = (User) ois.readObject();
}
```

**⚠️ serialVersionUID:** `Serializable` implement eden sınıflarda mutlaka tanımlayın. Yoksa sınıf değiştiğinde deserialize başarısız olur.
```java
private static final long serialVersionUID = 1L;
```

---

## 2. NIO (java.nio) — New I/O

### Temel Farklar

| Özellik | Klasik I/O | NIO |
| :--- | :--- | :--- |
| **Yaklaşım** | Stream-based | Channel + Buffer based |
| **Blocking** | Blocking (varsayılan) | Non-blocking destekler |
| **Yön** | Tek yönlü (InputStream VEYA OutputStream) | Çift yönlü (Channel okur ve yazar) |
| **Performans** | Küçük dosyalar için yeterli | Büyük dosyalar ve yüksek eşzamanlılık için ideal |
| **Selector** | Yok | Var (tek thread ile çoklu channel yönetimi) |

### Channel ve Buffer

```java
// NIO ile dosya okuma
try (FileChannel channel = FileChannel.open(Path.of("data.txt"), StandardOpenOption.READ)) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    
    while (channel.read(buffer) > 0) {
        buffer.flip(); // Yazma modundan okuma moduna geç
        while (buffer.hasRemaining()) {
            System.out.print((char) buffer.get());
        }
        buffer.clear(); // Buffer'ı sıfırla
    }
}
```

### Buffer Lifecycle

```
allocate → [Yazma Modu] → put() → flip() → [Okuma Modu] → get() → clear()/compact()
```

- **`flip()`:** limit = position, position = 0. Yazma → Okuma geçişi.
- **`clear()`:** position = 0, limit = capacity. Buffer'ı sıfırlar.
- **`compact()`:** Okunmamış verileri başa taşır, kalan kısma yazma devam eder.

---

## 3. NIO.2 (java.nio.file) — Path & Files API

Java 7 ile gelen modern dosya işlemleri API'si. `File` sınıfının tüm eksikliklerini giderir.

### Path ve Files

```java
Path path = Path.of("/home/mete/documents/report.txt");

// Dosya bilgileri
boolean exists = Files.exists(path);
long size = Files.size(path);
String mimeType = Files.probeContentType(path);

// Tek satırda dosya okuma
String content = Files.readString(path);
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

// Tek satırda dosya yazma
Files.writeString(path, "Yeni içerik", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

// Dosya kopyalama ve taşıma
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);

// Dizin oluşturma (recursive)
Files.createDirectories(Path.of("/home/mete/a/b/c"));
```

### Stream ile Dosya Tarama (Lazy)

```java
// Dizindeki tüm .java dosyalarını bul (recursive)
try (Stream<Path> stream = Files.walk(Path.of("/project"))) {
    List<Path> javaFiles = stream
        .filter(p -> p.toString().endsWith(".java"))
        .filter(Files::isRegularFile)
        .collect(Collectors.toList());
}

// Dizinin içeriğini listele (non-recursive)
try (Stream<Path> stream = Files.list(Path.of("/home/mete"))) {
    stream.filter(Files::isDirectory)
          .forEach(System.out::println);
}

// Dosya içinde arama (grep benzeri)
try (Stream<String> lines = Files.lines(Path.of("application.log"))) {
    lines.filter(line -> line.contains("ERROR"))
         .forEach(System.out::println);
}
```

### WatchService (Dosya Değişikliği İzleme)

```java
WatchService watcher = FileSystems.getDefault().newWatchService();
Path dir = Path.of("/home/mete/uploads");
dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

while (true) {
    WatchKey key = watcher.take(); // Bloklar, olay bekler
    for (WatchEvent<?> event : key.pollEvents()) {
        Path fileName = (Path) event.context();
        System.out.println("Yeni dosya: " + fileName);
    }
    key.reset();
}
```

---

## 4. Memory-Mapped Files

Çok büyük dosyalar (GB'lar) için idealdir. Dosya doğrudan belleğe map'lenir, kernel/user space kopyalama maliyeti ortadan kalkar.

```java
try (FileChannel channel = FileChannel.open(Path.of("huge-file.dat"), StandardOpenOption.READ)) {
    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    
    // Dosya sanki bir byte array'miş gibi erişim
    while (buffer.hasRemaining()) {
        byte b = buffer.get();
    }
}
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: `File` ile `Path` farkı nedir? Hangisi tercih edilmeli?
**Cevap:**
- **`File`:** Java 1.0'dan beri var. `delete()` başarısız olunca neden söylemez (false döner). Symbolic link desteklemez.
- **`Path`:** Java 7+ NIO.2. Detaylı exception'lar fırlatır. Symbolic link, metadata, WatchService destekler.
- **Tercih:** Her zaman `Path` + `Files` kullanın. `File` legacy API'dir.

### Soru 2: Blocking I/O ile Non-blocking I/O farkı nedir?
**Cevap:**
- **Blocking:** `read()` çağrısı veri gelene kadar thread'i bloklar. Thread başına bir bağlantı gerekir (C10K problemi).
- **Non-blocking:** `read()` veri yoksa hemen `0` döner. Tek thread **Selector** ile binlerce bağlantıyı yönetebilir.

### Soru 3: `try-with-resources` olmadan ne olur?
**Cevap:** Resource leak! Dosya handle'ları kapanmaz, OS limitlerine ulaşılır (`Too many open files`). Her `Closeable`/`AutoCloseable` kaynağı try-with-resources içinde kullanın.

### Soru 4 (Tricky): `BufferedReader` neden performanslıdır?
**Cevap:** Her `read()` çağrısında OS'a system call yapmak yerine, bir seferde büyük bir blok (varsayılan 8KB) okuyup hafızada tutar. Sonraki okumalar bellekten gelir. Disk I/O sayısı dramatik azalır.

### Soru 5 (Tricky): `Files.readAllLines()` ile `Files.lines()` farkı nedir?
**Cevap:**
- **`readAllLines()`:** Tüm dosyayı belleğe yükler (`List<String>` döner). Küçük dosyalar için uygun. Büyük dosyalarda `OutOfMemoryError`.
- **`lines()`:** Lazy `Stream<String>` döner. Satırlar ihtiyaç duyuldukça okunur. Büyük dosyalar için güvenli. **Mutlaka** try-with-resources ile kullanılmalı (stream kapatılmalı).

---

## 6. Geliştirici İpuçları

- **Charset Belirtin:** `new FileReader("x.txt")` platformun varsayılan charset'ini kullanır. `Files.readString(path, StandardCharsets.UTF_8)` ile charset belirtin.
- **Büyük Dosyalar İçin:** `Files.lines()` veya `BufferedReader` kullanın, `readAllLines()` kullanmayın.
- **Binary vs Text:** Binary dosyalar (resim, PDF) için `InputStream/OutputStream`; metin dosyalar için `Reader/Writer` kullanın.
- **Temporary Files:** `Files.createTempFile("prefix", ".tmp")` ile geçici dosya oluşturun. `deleteOnExit()` ile JVM kapanınca silinir.
