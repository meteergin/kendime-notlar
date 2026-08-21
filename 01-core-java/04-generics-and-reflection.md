# Generics, Wildcards ve Reflection

> **Analoji:** Generics, bir "şablon kalıp" gibidir. Bir pastane düşünün: aynı kalıpla hem yuvarlak hem de kare pasta yapabilirsiniz. Kalıp şekli (type parameter) neyi üreteceğinizi belirler, ama kalıbın kendisi değişmez.

---

## 1. Generics Temelleri

### Neden Generics?

Java 5 öncesinde Collection'lar `Object` türünde eleman tutardı. Bu iki büyük sorun yaratıyordu:

```java
// Java 5 ÖNCESİ - Tehlikeli!
List list = new ArrayList();
list.add("Mete");
list.add(42);  // Derleme hatası VERMEZ!

String name = (String) list.get(1); // ClassCastException! Runtime'da patlama
```

```java
// Java 5 SONRASI - Güvenli!
List<String> list = new ArrayList<>();
list.add("Mete");
// list.add(42);  // Derleme hatası! IDE kırmızı çizer
String name = list.get(0); // Cast gerekmez
```

**Generics'in 3 Süper Gücü:**
1. **Compile-time Type Safety:** Hatalar derleme anında yakalanır
2. **Cast Eliminasyonu:** Explicit cast gerekmez
3. **Reusable Algoritmalar:** Tip bağımsız algoritmalar yazılabilir

---

### Generic Sınıf

```java
public class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
}

// Kullanım
Pair<String, Integer> score = new Pair<>("Mete", 95);
Pair<Long, LocalDateTime> audit = new Pair<>(1L, LocalDateTime.now());
```

### Generic Metot

```java
public class Utils {
    // Metot seviyesinde tip parametresi
    public static <T> T getFirstElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }

    // Bounded generic metot
    public static <T extends Comparable<T>> T findMax(List<T> list) {
        return list.stream().max(Comparable::compareTo).orElseThrow();
    }
}
```

---

## 2. Type Erasure (Tip Silme)

**Analoji:** Generics, "çevirmen gözlükleri" gibidir. Derleme zamanında her şeyi doğru görürsünüz ama çalışma zamanında (JVM) bu gözlükler çıkarılır. JVM'in generic'lerden haberi yoktur!

### Nasıl Çalışır?

```java
// Yazdığınız kod:
List<String> names = new ArrayList<>();

// JVM'in gördüğü (Type Erasure sonrası):
List names = new ArrayList();  // Object olarak saklanır
```

**Type Erasure'ın Sonuçları:**
1. `List<String>` ve `List<Integer>` runtime'da **aynı** sınıftır
2. `new T()` yapamazsınız (JVM T'nin ne olduğunu bilmez)
3. `instanceof` ile generic tip kontrolü yapılamaz: `obj instanceof List<String>` ❌

```java
// YANLIŞ - Derleme hatası
if (list instanceof List<String>) { } // Compile error

// DOĞRU - Raw type ile kontrol
if (list instanceof List<?>) { }
```

---

## 3. Wildcards (Joker Tipler) ve PECS Prensibi

### Wildcard Türleri

| Wildcard | İsim | Kullanım |
| :--- | :--- | :--- |
| `<?>` | Unbounded | Herhangi bir tip (sadece okuma) |
| `<? extends T>` | Upper Bounded | T veya alt sınıfları (Producer - okuma) |
| `<? super T>` | Lower Bounded | T veya üst sınıfları (Consumer - yazma) |

### PECS: Producer Extends, Consumer Super

> **Analoji:** Bir meyve sepeti düşünün. Sepetten meyve **alıyorsanız** (produce), sepetin "Elma veya alt türleri" olduğunu bilmeniz yeterli (`extends`). Sepete meyve **koyuyorsanız** (consume), sepetin "Elma veya üst türlerini" kabul ettiğini bilmelisiniz (`super`).

```java
// PRODUCER: Veri OKUYOR → extends kullan
public static double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number n : numbers) {
        total += n.doubleValue(); // Okuma yapılıyor
    }
    // numbers.add(42); // COMPILE ERROR! Yazma YASAK
    return total;
}

// CONSUMER: Veri YAZIYOR → super kullan
public static void addIntegers(List<? super Integer> list) {
    list.add(1);   // Yazma yapılıyor
    list.add(2);
    // Integer x = list.get(0); // COMPILE ERROR! Okuma güvenli değil
}
```

**Neden?**
- `List<? extends Number>`: İçinde `Integer`, `Double`, `Long` olabilir. Okuyunca hepsini `Number` olarak alabilirsiniz. Ama ne yazacağınızı bilemezsiniz (Integer mı? Double mı?).
- `List<? super Integer>`: İçine `Integer` yazabilirsiniz (`Number` listesi de Integer kabul eder). Ama okuyunca ne gelir bilemezsiniz.

---

## 4. Bounded Type Parameters

```java
// T, Comparable'ı implement etmek ZORUNDA
public <T extends Comparable<T>> T findMin(List<T> list) {
    return list.stream().min(Comparable::compareTo).orElseThrow();
}

// Birden fazla bound (& ile)
public <T extends Serializable & Comparable<T>> void process(T item) {
    // T hem Serializable hem Comparable olmalı
}
```

---

## 5. Reflection API

**Analoji:** Reflection, bir "röntgen cihazı" gibidir. Bir nesnenin iç yapısını (field'lar, metodlar, anotasyonlar) çalışma zamanında görmenizi ve manipüle etmenizi sağlar. Normal yollarla erişemeyeceğiniz **private** alanları bile görebilirsiniz.

### Temel Kullanım

```java
// Sınıf bilgisi alma
Class<?> clazz = Class.forName("com.example.User");
// veya
Class<?> clazz = User.class;
// veya
Class<?> clazz = user.getClass();

// Constructor ile nesne oluşturma
Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, int.class);
Object user = constructor.newInstance("Mete", 30);

// Private field'a erişim
Field nameField = clazz.getDeclaredField("name");
nameField.setAccessible(true); // Private bypass!
String name = (String) nameField.get(user);

// Private metot çağırma
Method method = clazz.getDeclaredMethod("secretMethod");
method.setAccessible(true);
method.invoke(user);
```

### Anotasyon İşleme

```java
// Custom anotasyon
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {
    String value() default "";
}

// Anotasyonu okuma
for (Method m : clazz.getDeclaredMethods()) {
    if (m.isAnnotationPresent(Timed.class)) {
        Timed timed = m.getAnnotation(Timed.class);
        System.out.println("Timed metot: " + m.getName() + " - " + timed.value());
    }
}
```

### Reflection Performans Uyarısı

| Yöntem | Göreceli Hız |
| :--- | :--- |
| Doğrudan metot çağrısı | 1x (referans) |
| `Method.invoke()` (ilk çağrı) | ~50-100x yavaş |
| `Method.invoke()` (cache'lenmiş) | ~5-10x yavaş |
| `MethodHandle` (Java 7+) | ~2-3x yavaş |

**Best Practice:** Reflection sonuçlarını cache'leyin. `Class`, `Method`, `Field` nesnelerini bir `Map`'te tutun.

---

## 6. Kritik Mülakat Soruları

### Soru 1: Type Erasure nedeniyle runtime'da generic tip bilgisi nasıl elde edilir?
**Cevap:** `TypeToken` veya anonymous subclass pattern kullanılır:
```java
// Guava TypeToken
TypeToken<List<String>> token = new TypeToken<List<String>>() {};
Type type = token.getType(); // java.util.List<java.lang.String>
```
Alt sınıf oluşturulduğunda generic bilgi class metadata'sında saklanır (Type Erasure bridge method'ları korur).

### Soru 2: `List<Object>` ile `List<?>` farkı nedir?
**Cevap:**
- **`List<Object>`:** Her türden eleman eklenebilir (`add(new String())`, `add(new Integer(5))`). Ama `List<String>` atanamaz (covariance yok).
- **`List<?>`:** Sadece `null` eklenebilir. Okuma güvenlidir ama yazma yasaktır. `List<String>`, `List<Integer>` atanabilir.

### Soru 3: Neden `new T()` veya `new T[]` yapılamaz?
**Cevap:** Type Erasure yüzünden. JVM runtime'da `T`'nin ne olduğunu bilmez. Çözüm:
```java
// Factory pattern
public <T> T create(Class<T> clazz) throws Exception {
    return clazz.getDeclaredConstructor().newInstance();
}

// Array için
public <T> T[] createArray(Class<T> clazz, int size) {
    return (T[]) Array.newInstance(clazz, size);
}
```

### Soru 4 (Tricky): `List<Integer>` bir `List<Number>`'a atanabilir mi?
**Cevap:** **Hayır!** Java'da generic'ler **invariant**'tır. `Integer` extends `Number` olmasına rağmen, `List<Integer>` bir `List<Number>` değildir.
```java
List<Integer> intList = new ArrayList<>();
// List<Number> numList = intList; // COMPILE ERROR!
List<? extends Number> numList = intList; // DOĞRU (wildcard ile)
```

### Soru 5 (Tricky): Spring Framework Reflection'ı nerede kullanır?
**Cevap:**
1. **`@Autowired`:** Bağımlılıkları inject ederken field'lara Reflection ile erişir
2. **`@Transactional`:** AOP proxy oluştururken
3. **Component Scanning:** `@Service`, `@Repository` anotasyonlarını tararken
4. **`@Value`:** Property değerlerini inject ederken

---

## 7. Geliştirici İpuçları

- **Recursive Type Bound:** `Comparable` pattern: `<T extends Comparable<T>>`. Kendi kendini karşılaştırabilen tipleri zorunlu kılar.
- **Diamond Problem Çözümü:** Java 7'den itibaren `<>` (diamond operator) ile tip çıkarımı otomatiktir: `new ArrayList<>()`.
- **Raw Type Kullanmayın:** `List` yerine her zaman `List<?>` veya belirli tip kullanın. Raw type, Type Safety'yi ortadan kaldırır.
- **Reflection Güvenliği:** `setAccessible(true)` ile private bypass yapmak, SecurityManager aktifse `SecurityException` fırlatır. Production'da dikkatli kullanın.
- **Mockito ve Generics:** `ArgumentCaptor<List<String>>` gibi nested generic yakalamalar için `@Captor` anotasyonu kullanın, tip güvenliği sağlar.
