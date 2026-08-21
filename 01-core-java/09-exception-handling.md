# Exception Handling ve Best Practices

> **Analoji:** Exception handling, bir binanın "yangın kaçış planı" gibidir. Normal şartlarda asla kullanılmaz, ama olağanüstü bir durum oluştuğunda insanları güvenli bir şekilde çıkarmak için mutlaka olması gerekir. İyi bir plan yoksa panik olur, bina çöker.

---

## 1. Exception Hiyerarşisi

```
java.lang.Object
  └── java.lang.Throwable
        ├── java.lang.Error          (Kurtarılamaz - JVM seviyesi)
        │     ├── OutOfMemoryError
        │     ├── StackOverflowError
        │     └── VirtualMachineError
        └── java.lang.Exception      (Kurtarılabilir)
              ├── IOException                  ← Checked
              ├── SQLException                 ← Checked
              ├── ClassNotFoundException       ← Checked
              └── java.lang.RuntimeException   ← Unchecked
                    ├── NullPointerException
                    ├── IllegalArgumentException
                    ├── IllegalStateException
                    ├── IndexOutOfBoundsException
                    └── UnsupportedOperationException
```

### Checked vs Unchecked

| Özellik | Checked Exception | Unchecked Exception |
| :--- | :--- | :--- |
| **Derleme** | `throws` bildirimi veya `try-catch` zorunlu | Zorunlu değil |
| **Üst Sınıf** | `Exception` (RuntimeException hariç) | `RuntimeException` |
| **Ne Zaman** | Dış sistemlerden kaynaklanan hatalar (I/O, DB, Network) | Programlama hataları (null, index, argument) |
| **Örnek** | `IOException`, `SQLException` | `NullPointerException`, `IllegalArgumentException` |

---

## 2. Exception Handling Mekanizmaları

### try-catch-finally

```java
try {
    Connection conn = DriverManager.getConnection(url);
    // Tehlikeli işlem
} catch (SQLException e) {
    log.error("Veritabanı bağlantısı başarısız: {}", e.getMessage(), e);
    throw new DataAccessException("DB connection failed", e);  // Sarmalama (wrap)
} finally {
    // Mutlaka çalışır (başarılı veya başarısız)
    // Resource temizliği yapılır
}
```

### try-with-resources (Java 7+)

```java
// AutoCloseable/Closeable implement eden kaynaklar otomatik kapanır
try (Connection conn = dataSource.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
    
    while (rs.next()) {
        // İşlem
    }
} // conn, stmt, rs otomatik ve TERS SIRADA kapanır
```

**⚠️ Dikkat:** `finally` bloğu bir exception fırlatırsa, `try` bloğundaki orijinal exception **kaybolur** (suppressed). try-with-resources bunu düzeltir: orijinal exception korunur, kapanış hatası `getSuppressed()` ile erişilir.

### Multi-catch (Java 7+)

```java
try {
    // İşlem
} catch (IOException | SQLException e) {
    // İkisi de aynı şekilde işlenir
    log.error("I/O veya DB hatası: {}", e.getMessage(), e);
}
```

---

## 3. Custom Exception Tasarımı

### Doğru Custom Exception

```java
// Business exception - kendi domain dilinizde
public class InsufficientBalanceException extends RuntimeException {
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Yetersiz bakiye. Mevcut: %s, İstenen: %s",
            currentBalance, requestedAmount));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
}
```

### Exception Hierarchy Tasarımı

```java
// Temel business exception
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;
    
    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() { return errorCode; }
}

// Alt sınıflar
public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String entityName, Object id) {
        super("NOT_FOUND", entityName + " bulunamadı. ID: " + id);
    }
}

public class DuplicateEntityException extends BusinessException {
    public DuplicateEntityException(String entityName, String field, String value) {
        super("DUPLICATE", entityName + " zaten mevcut. " + field + ": " + value);
    }
}
```

---

## 4. Spring Boot Global Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
        
        ErrorResponse error = new ErrorResponse(400, "VALIDATION_ERROR",
            String.join(", ", errors), LocalDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Beklenmeyen hata: ", ex);
        ErrorResponse error = new ErrorResponse(500, "INTERNAL_ERROR",
            "Beklenmeyen bir hata oluştu", LocalDateTime.now());
        return ResponseEntity.status(500).body(error);
    }
}

public record ErrorResponse(int status, String errorCode, String message, LocalDateTime timestamp) {}
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: Checked Exception mı Unchecked Exception mı kullanmalı?
**Cevap:** Modern Java'da genel eğilim **Unchecked** (RuntimeException) kullanmaktır.
- **Checked:** Yalnızca çağıranın makul bir şekilde kurtarabileceği durumlarda (retry, fallback).
- **Unchecked:** Programlama hataları ve iş kuralı ihlalleri için. Spring Framework tamamen unchecked kullanır.
- **Trap:** "Her şey checked olsun" yaklaşımı `catch-and-swallow` anti-pattern'ine yol açar.

### Soru 2: `throw` ile `throws` farkı nedir?
**Cevap:**
- **`throw`:** Exception **fırlatır** → `throw new IllegalArgumentException("Invalid");`
- **`throws`:** Metot imzasında checked exception **bildirir** → `void read() throws IOException`

### Soru 3: Exception'ları neden asla yutmamalısınız (swallow)?
**Cevap:**
```java
// ANTİ-PATTERN: Exception swallowing
try {
    riskyOperation();
} catch (Exception e) {
    // BOŞ! Hata sessizce yok sayılır.
    // Bug gizlenir, debug imkansızlaşır
}

// DOĞRU
try {
    riskyOperation();
} catch (Exception e) {
    log.error("İşlem başarısız: {}", e.getMessage(), e); // Logla
    throw new ServiceException("Operation failed", e);    // Veya yeniden fırlat
}
```

### Soru 4: `finally` bloğu her zaman çalışır mı?
**Cevap:** Neredeyse her zaman, **şu durumlar hariç:**
1. `System.exit()` çağrılırsa
2. JVM çökerse (crash)
3. Thread `kill` edilirse (`Thread.stop()` - deprecated)
4. Sonsuz döngüde takılırsa

### Soru 5 (Tricky): `return` ve `finally` birlikte kullanılırsa ne olur?
**Cevap:**
```java
public int test() {
    try {
        return 1;
    } finally {
        return 2; // ⚠️ Bu değer döner! try'daki return geçersiz olur
    }
}
// Sonuç: 2 döner. ASLA finally içinde return yazmayın!
```

### Soru 6 (Tricky): Spring'de `@Transactional` hangi exception'larda rollback yapar?
**Cevap:** Varsayılan olarak **sadece unchecked exception** (RuntimeException) ve **Error** için rollback yapar. Checked exception'lar için **rollback yapmaz**!
```java
@Transactional(rollbackFor = IOException.class) // Checked için de rollback
public void transfer() throws IOException { ... }
```

---

## 6. Geliştirici İpuçları

- **Exception Mesajı:** Hata mesajında **ne oldu**, **neden oldu** ve **nasıl düzeltilir** bilgisi olsun. `"User not found"` yerine `"User not found with id=42. Check if the user has been created."`.
- **Exception Chaining:** Orijinal exception'ı her zaman `cause` olarak ekleyin: `new ServiceException("msg", originalException)`. Stack trace kaybolmasın.
- **Fail-Fast:** Metot başında parametreleri validate edin. `Objects.requireNonNull(param, "param must not be null")`.
- **Exception Katmanlaması:** Her katman kendi exception'ını fırlatsın. Repository → `DataAccessException`, Service → `BusinessException`, Controller → `ErrorResponse`.
- **Suppressed Exceptions:** try-with-resources kullanarak `getSuppressed()` ile close() sırasındaki hataları da yakalayın.
