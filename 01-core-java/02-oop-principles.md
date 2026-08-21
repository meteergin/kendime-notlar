## Konu 2: OOP Prensipleri - Inheritance, Abstraction, Encapsulation, Interfaces

Object-Oriented Programming (Nesne Yönelimli Programlama), yazılım geliştirmede kod yeniden kullanılabilirliğini, bakımını ve ölçeklenebilirliğini artıran bir paradigmadır. Java, OOP dillerinin en klasik ve saf örneklerinden biridir. Bir geliştirici olarak, sadece "Inheritance miras demektir" gibi tanımlar yerine, **ne zaman, neden ve nasıl** kullanılacağını, **trade-off'ları** ve **gerçek dünya problemlerindeki uygulama şekillerini** bilmeniz beklenir.

---

### 1. Inheritance (Kalıtım/Miras)

**Analoji:** Kalıtım, biyolojik mirastır. Çocuklar ebeveynlerinden gen alır, bazı özellikleri doğal olarak taşırlar (göz rengi, boy gibi). Ancak kendi benzersiz özelliklerini de geliştirebilirler (meslek, hobiler).

#### Tanım ve Kullanım
Inheritance, bir sınıfın (child/subclass) başka bir sınıftan (parent/superclass) özelliklerini (field) ve davranışlarını (method) devralmasıdır. Java'da `extends` anahtar kelimesi ile gerçekleştirilir.

```java
class Animal {
    protected String name;
    
    public void eat() {
        System.out.println(name + " yemek yiyor...");
    }
    
    public void sleep() {
        System.out.println(name + " uyuyor...");
    }
}

class Dog extends Animal {
    private String breed;
    
    public Dog(String name, String breed) {
        this.name = name; // Parent'tan gelen protected field
        this.breed = breed;
    }
    
    // Override: Parent metodunun davranışını değiştirme
    @Override
    public void eat() {
        System.out.println(name + " köpek mamasını yiyor...");
    }
    
    // Yeni metod: Child sınıfının kendine özgü davranışı
    public void bark() {
        System.out.println(name + " havlıyor: Hav! Hav!");
    }
}
```

#### Java'da Inheritance Kuralları
1. **Single Inheritance:** Java'da bir sınıf yalnızca BİR sınıftan extend edebilir. Çoklu kalıtım (Multiple Inheritance) sınıflar için yasaktır. (C++'ta vardır)
2. **Multilevel Inheritance:** A → B → C şeklinde zincir halinde miras mümkündür.
3. **Hierarchical Inheritance:** Birden fazla sınıf aynı parent'tan miras alabilir.
4. **Constructor Zinciri:** Child nesne oluşturulurken **ÖNCE** parent constructor çağrılır. (`super()` çağrısı yoksa bile otomatik olarak default constructor çağrılır)

```java
class Parent {
    public Parent() {
        System.out.println("Parent constructor çalıştı");
    }
}

class Child extends Parent {
    public Child() {
        // super(); // Bu satır yazılmasa bile otomatik eklenir
        System.out.println("Child constructor çalıştı");
    }
}

// new Child() çağrısı yazdırır:
// Parent constructor çalıştı
// Child constructor çalıştı
```

#### Pros & Cons

**✅ Avantajları:**
- **Kod Tekrarını Azaltır:** Ortak davranışlar parent'ta tanımlanır.
- **Mantıksal Hiyerarşi:** Gerçek dünyayı modellemek kolaylaşır (Hayvan → Köpek → Golden Retriever).
- **Polymorphism Temeli:** Method overriding ve dynamic binding için gereklidir.

**❌ Dezavantajları:**
- **Tight Coupling (Sıkı Bağlılık):** Child sınıf parent'a bağımlıdır. Parent değişirse child etkilenir.
- **Fragile Base Class Problem:** Parent sınıftaki bir değişiklik tüm child'ları bozabilir.
- **Yanlış Kullanım:** "Is-A" ilişkisi yerine "Has-A" ilişkisi varsa Composition kullanılmalıdır.

---

### 2. Encapsulation (Kapsülleme)

**Analoji:** Kapsülleme, bir ilaç kapsülü gibidir. İçinde ne olduğu dışarıdan görünmez, kullanıcı sadece doğru şekilde yutup faydalarını görür. Nasıl çalıştığını bilmesine gerek yoktur. Veya bir araba: Kapağın altındaki motor detaylarını görmezsiniz, sadece direksiyonu ve pedalları kullanırsınız.

#### Tanım ve Kullanım
Encapsulation, bir nesnenin **iç durumunu (internal state)** gizleyerek dışarıdan doğrudan erişimi engellemek ve sadece **kontrollü erişim** sağlamaktır. Bu, **access modifier** (private, protected) ve **getter/setter** metodları ile yapılır.

```java
public class BankAccount {
    private String accountNumber; // Dışarıdan doğrudan erişilemez
    private double balance;       // Dışarıdan doğrudan erişilemez
    
    public BankAccount(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }
    
    // Getter: Sadece okuma izni
    public double getBalance() {
        return balance;
    }
    
    // Setter: Validasyon ile yazma izni
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Para yatırma miktarı pozitif olmalı");
        }
        this.balance += amount;
    }
    
    public void withdraw(double amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("Yetersiz bakiye");
        }
        this.balance -= amount;
    }
    
    // accountNumber için setter yok, değiştirilemesin
}
```

#### Neden Gerekli?
- **Data Integrity (Veri Bütünlüğü):** Geçersiz değer atamasını önler. (Örn: Negatif yaş)
- **Esneklik:** İç yapı değiştirildiğinde (field adı, tip vb.) dış kod etkilenmez.
- **Kontrol:** Okuma-yazma yetkilerini ayrı ayrı belirleyebilirsiniz.

#### Best Practice
- **Tüm field'ları `private` yapın** (bazı durumlarda `protected` kabul edilebilir ama nadiren).
- Getter/Setter kullanın ama **körü körüne tüm field'lar için yazmayın**. Sadece gerekenlere açın.
- **Immutable Objeler** için (örn: `String`) setter olmaz, field'lar `final` olur.

---

### 3. Abstraction (Soyutlama)

**Analoji:** Bir kahve makinesini kullanırken, sadece "Espresso" butonuna basarsınız. Makinenin içindeki pompa mekanizması, sıcaklık kontrolü, basınç ayarı gibi detayları bilmezsiniz. Abstraction, "ne yapıldığını" gösterir ama "nasıl yapıldığını" gizler.

#### Tanım ve Kullanım
Abstraction, **implementasyon detaylarını gizleyerek sadece gerekli bilgiyi göstermektir**. Java'da iki şekilde yapılır:
1. **Abstract Class** (Soyut Sınıf)
2. **Interface** (Arayüz)

#### Abstract Class

```java
abstract class Shape {
    protected String color;
    
    public Shape(String color) {
        this.color = color;
    }
    
    // Abstract metod: Gövdesi yok, alt sınıflar MUTLAKA implement etmeli
    public abstract double calculateArea();
    
    // Concrete metod: Normal metod, tüm alt sınıflar kullanabilir
    public void displayColor() {
        System.out.println("Renk: " + color);
    }
}

class Circle extends Shape {
    private double radius;
    
    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }
    
    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

class Rectangle extends Shape {
    private double width, height;
    
    public Rectangle(String color, double width, double height) {
        super(color);
        this.width = width;
        this.height = height;
    }
    
    @Override
    public double calculateArea() {
        return width * height;
    }
}
```

**Kurallar:**
- Abstract class'tan **doğrudan nesne oluşturamazsınız** (`new Shape()` hata verir).
- Abstract metod içerebilir, concrete metod da içerebilir.
- Constructor'ı olabilir (child constructorlardan `super()` ile çağrılır).
- **0 veya daha fazla abstract metod** olabilir (Yani hiç abstract metod olmasa bile sınıfı abstract tanımlayabilirsiniz).

---

### 4. Interface (Arayüz)

**Analoji:** Interface, bir "sözleşme" veya "garanti belgesi" gibidir. Bir USB cihazı alırken, "Bu cihaz USB standardını destekler" garantisi alırsınız. Cihazın içi nasıl çalışıyor bilmezsiniz ama USB portuna takıp çalıştıracağınızı bilirsiniz.

#### Tanım ve Kullanım
Interface, **sadece ne yapılacağını tanımlar** (what), nasıl yapılacağını tanımlamaz (how). Java 8 öncesinde sadece abstract metodlar içerebilirdi. Java 8 ile `default` ve `static` metodlar, Java 9 ile `private` metodlar eklendi.

```java
interface Flyable {
    // Abstract metod (public abstract otomatik eklenir)
    void fly();
    
    // Java 8: Default metod (implementasyon var, override isteğe bağlı)
    default void takeOff() {
        System.out.println("Kalkış yapılıyor...");
    }
    
    // Java 8: Static metod
    static void checkWeather() {
        System.out.println("Hava durumu kontrol ediliyor...");
    }
}

interface Swimmable {
    void swim();
}

// Bir sınıf BIRDEN FAZLA interface implement edebilir (Çoklu Kalıtım!)
class Duck implements Flyable, Swimmable {
    @Override
    public void fly() {
        System.out.println("Ördek uçuyor...");
    }
    
    @Override
    public void swim() {
        System.out.println("Ördek yüzüyor...");
    }
}

// Functional Interface (Java 8): Sadece 1 abstract metodu olan interface
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);
    
    // default ve static olabilir, abstract tek olmalı
    default void info() {
        System.out.println("Bu bir hesap makinesidir");
    }
}

// Lambda ile kullanılır
Calculator add = (a, b) -> a + b;
System.out.println(add.calculate(5, 3)); // 8
```

#### Interface vs Abstract Class (EN ÇOK SORULAN!)

| Özellik | Interface | Abstract Class |
| :--- | :--- | :--- |
| **Çoklu Kalıtım** | ✅ Bir sınıf birden fazla interface implement edebilir | ❌ Sadece bir class extend edilebilir |
| **Metodlar (Java 7 öncesi)** | Sadece abstract | Abstract ve concrete olabilir |
| **Metodlar (Java 8+)** | Abstract, default, static | Abstract ve concrete olabilir |
| **Field'lar** | Sadece `public static final` (constant) | Her türlü access modifier ile instance variable olabilir |
| **Constructor** | ❌ Olamaz | ✅ Olabilir |
| **Kullanım Amacı** | "Can-Do" ilişkisi (Yapabilir): Flyable (uçabilir), Serializable | "Is-A" ilişkisi (Türüdür): Vehicle (araç), Animal |
| **Ne Zaman Kullanılır?** | Contract tanımlamak, behavior eklemek | Ortak kod paylaşmak, partial implementation |

**Tavsiye:** "Composition over Inheritance" prensibine uyarak, **mümkünse Interface tercih edin**. Bu daha esnek bir yapı sağlar. Eğer ortak kod (state + behavior) paylaşılacaksa Abstract Class kullanın.

---

### 5. Static vs Dynamic Binding (Method Binding)

**Analoji:** Static binding, bir kitabın içindekiler sayfası gibidir. Hangi sayfada ne olduğu baştan yazılıdır (compile-time). Dynamic binding ise, bir menüde "Günün Özel Yemeği" var, ne olduğu o gün belli olur (runtime).

#### Method Binding Nedir?
Bir metot çağrısının hangi kodla eşleştirileceğine (bind) karar verme sürecidir.

#### Static Binding (Early Binding)
**Compile-time** (Derleme zamanı) sırasında karar verilir.

**Hangi Durumlarda Olur?**
- `private` metodlar
- `static` metodlar
- `final` metodlar
- Constructor'lar

```java
class Parent {
    public static void display() {
        System.out.println("Parent static metod");
    }
    
    public final void show() {
        System.out.println("Parent final metod");
    }
}

class Child extends Parent {
    // Static metod override edilmez, hiding (gizleme) olur!
    public static void display() {
        System.out.println("Child static metod");
    }
    
    // final metod override edilemez (compile error)
    // public void show() { } // HATA!
}

public class Test {
    public static void main(String[] args) {
        Parent p = new Child(); // Referans Parent, Nesne Child
        p.display(); // "Parent static metod" (referans tipi önemli, static binding)
        p.show();    // "Parent final metod" (final, static binding)
    }
}
```

#### Dynamic Binding (Late Binding / Runtime Polymorphism)
**Runtime** (Çalışma zamanı) sırasında nesnenin gerçek tipi baz alınarak karar verilir.

**Hangi Durumlarda Olur?**
- **Overridden instance metodlar** (Polymorphism!)

```java
class Animal {
    public void sound() {
        System.out.println("Hayvan ses çıkarıyor");
    }
}

class Dog extends Animal {
    @Override
    public void sound() {
        System.out.println("Köpek havlıyor: Hav!");
    }
}

class Cat extends Animal {
    @Override
    public void sound() {
        System.out.println("Kedi miyavlıyor: Miyav!");
    }
}

public class Test {
    public static void main(String[] args) {
        Animal a1 = new Dog(); // Compile-time type: Animal, Runtime type: Dog
        Animal a2 = new Cat(); // Compile-time type: Animal, Runtime type: Cat
        
        a1.sound(); // "Köpek havlıyor: Hav!" (Dynamic Binding, runtime'da Dog.sound() çağrılır)
        a2.sound(); // "Kedi miyavlıyor: Miyav!" (Dynamic Binding, runtime'da Cat.sound() çağrılır)
    }
}
```

**Runtime'da Karar Verme (JVM Perspektifi):**
1. JVM, `a1` referansının işaret ettiği nesnenin gerçek tipine bakar (Dog).
2. O tipin method tablosunda (vtable) ilgili metodu arar.
3. Bulduğu implementasyonu çalıştırır.

---

### 6. Object Lifecycle (Nesne Yaşam Döngüsü)

**Analoji:** Bir insanın yaşam döngüsü gibi: Doğum (Creation), Yaşam (Usage), Ölüm (Destruction). Java'da da nesneler doğar, kullanılır ve GC tarafından temizlenir.

#### 1. Object Creation (Nesne Oluşturma)

**Adımlar:**
1. **Memory Allocation (Bellek Tahsisi):** `new` anahtar kelimesi ile Heap'te yer ayrılır.
2. **Initialization (İlkleme):**
    - **Instance variable'lar** default değerlerini alır (int → 0, boolean → false, object → null).
    - **Instance Initializer Block** çalışır (varsa).
    - **Constructor** çalıştırılır.

```java
class Person {
    String name;
    int age;
    
    // Instance Initializer Block (Constructor'dan önce çalışır)
    {
        System.out.println("Instance block çalıştı");
        age = 18; // Default değer
    }
    
    public Person(String name) {
        System.out.println("Constructor çalıştı");
        this.name = name;
    }
}

// Çıktı:
// Instance block çalıştı
// Constructor çalıştı
```

**Sıralama (Inheritance Var ise):**
1. Parent class'ın static block'u
2. Child class'ın static block'u
3. Parent class'ın instance block'u
4. Parent class'ın constructor'u
5. Child class'ın instance block'u
6. Child class'ın constructor'u

#### 2. Object Usage (Nesne Kullanımı)
Nesne oluşturulduktan sonra field'larına erişilir, metodları çağrılır.

#### 3. Object Destruction (Nesne Yok Edilmesi)

Java'da **açık bir destructor yoktur** (C++'taki gibi). Bunun yerine:
- **Garbage Collector (GC):** Kullanılmayan nesneleri otomatik olarak temizler.
- **finalize() metodu (Deprecated!):** GC nesneyi silmeden önce çağrılır. **Java 9'dan beri kullanımı önerilmez**, yerine `try-with-resources` ve `Cleaner` API kullanılır.

```java
class Resource {
    @Override
    protected void finalize() throws Throwable {
        System.out.println("finalize() çağrıldı, kaynak temizleniyor");
        super.finalize();
    }
}

// Modern Yaklaşım (Java 7+): try-with-resources
class FileHandler implements AutoCloseable {
    public void readFile() {
        System.out.println("Dosya okunuyor...");
    }
    
    @Override
    public void close() {
        System.out.println("Dosya kapatıldı.");
    }
}

try (FileHandler fh = new FileHandler()) {
    fh.readFile();
} // Otomatik olarak close() çağrılır, exception olsa bile
```

#### Garbage Collection (GC) Tetikleme
Nesne GC için uygun hale gelir:
1. **Referansı null yapılırsa:** `person = null;`
2. **Referans kapsam dışına çıkarsa:** Metot bittiğinde lokal değişkenler yok olur.
3. **Yeni nesne atanırsa:** `person = new Person("Ali");` (eski nesne referanssız kalır).

**GC'yi Manuel Çalıştırma (Tavsiye Edilmez!):**
```java
System.gc(); // veya Runtime.getRuntime().gc();
```
Bu sadece JVM'e "rica" edersiniz, garanti edilmez. GC kendi optimizasyonuna göre çalışır.

---

### 7. Kritik Mülakat Soruları ve Cevapları 

#### Soru 1: `Composition` ve `Inheritance` arasında ne zaman hangisini seçersiniz?
**Cevap:**
- **Inheritance:** "Is-A" (türüdür) ilişkisi varsa. Örn: `Dog is-a Animal`. Ortak davranış ve hiyerarşi gerekiyorsa.
- **Composition:** "Has-A" (içerir) ilişkisi varsa. Örn: `Car has-a Engine`. Daha esnek yapı istiyorsanız, çünkü runtime'da davranış değiştirilebilir.

**Örnek (Composition):**
```java
class Engine {
    public void start() { System.out.println("Motor çalıştı"); }
}

class Car {
    private Engine engine; // Composition (Has-A)
    
    public Car() {
        this.engine = new Engine();
    }
    
    public void drive() {
        engine.start();
        System.out.println("Araba gidiyor");
    }
}
```
**Neden Composition?** Engine değiştirilebilir (ElectricEngine, DieselEngine). Inheritance'ta bu esneklik yoktur.

#### Soru 2: `@Override` annotation'ı zorunlu mu? Kullanmazsanız ne olur?
**Cevap:** Zorunlu değil ama **ŞIDDETLE TAVSİYE EDİLİR**. Kullanmazsanız:
- Method signature yanlış olursa (typo, parametre farkı), compiler hata vermez, yeni bir metod oluşturmuş olursunuz.
- `@Override` ile compiler kontrol eder, gerçekten override edip etmediğinizi doğrular.

```java
class Parent {
    public void display(int x) { }
}

class Child extends Parent {
    // Yanlışlıkla parametre tipi String yazdık, override olmadı!
    public void display(String x) { } // Yeni metod, override değil!
    
    // Doğru kullanım:
    @Override
    public void display(int x) { } // Compile-time kontrol
}
```

#### Soru 3: Abstract class'ın constructor'ı neden vardır? Nasıl çağrılır?
**Cevap:** Abstract class'tan doğrudan nesne üretemezsiniz ama **child class nesnesi oluşturulduğunda parent constructor çağrılır**. Parent'taki ortak field'ları initialize etmek için kullanılır.

#### Soru 4: Interface'lerdeki `default` metod ne işe yarar? (Java 8)
**Cevap:** **Backward compatibility** (geriye uyumluluk) sağlar. Mevcut bir interface'e yeni metod eklendiğinde, onu implement eden TÜM sınıfları bozmaz.

**Örnek:** Java 8'de `Collection` interface'ine `stream()` metodu eklenirken, milyonlarca mevcut implementasyon bozulmasın diye `default` olarak eklendi.

```java
interface MyInterface {
    void oldMethod();
    
    // Yeni metod, eski implementasyonlar etkilenmez
    default void newMethod() {
        System.out.println("Default implementasyon");
    }
}
```

#### Soru 5: Diamond Problem nedir? Java nasıl çözer?
**Cevap:** Çoklu kalıtımda iki parent'tan aynı isimli metodu miras alırsanız hangi implementasyon kullanılacak belirsizdir.

**Java'nın Çözümü:**
- **Sınıflarda:** Çoklu kalıtım yasaktır, problem yok.
- **Interface'lerde:** İki interface'te aynı `default` metod varsa, child **MUTLAKA override etmeli**.

```java
interface A {
    default void show() { System.out.println("A"); }
}

interface B {
    default void show() { System.out.println("B"); }
}

class C implements A, B {
    @Override
    public void show() {
        A.super.show(); // A'nın implementasyonunu seçebilirsiniz
        // veya kendi implementasyonunuzu yazın
    }
}
```

#### Soru 6 (Tricky): `@Override` anotasyonu olmadan bir metodu override edebilir miyiz? Neden kullanmalıyız?
**Cevap:** **Evet**, `@Override` olmadan da override edilir ama **kullanmak zorunludur** çünkü:
*   **Compile-time güvenlik:** Eğer yanlışlıkla metod adını veya parametre tipini yanlış yazarsanız, `@Override` olmadan Java yeni bir metod olarak kabul eder (overload). `@Override` varsa compiler hata verir.
*   **Trap:** Mülakatta "Opsiyoneldir" demeyin, **best practice olarak zorunlu** deyin.

#### Soru 7 (Tricky): `private` metodlar override edilebilir mi? Peki `static` metodlar?
**Cevap:**
*   **Private metodlar:** **Hayır**. Private metodlar child class'ta görünmez, dolayısıyla override edilemez. Child'da aynı isimli bir private metod yazarsanız, bu tamamen yeni bir metoddur.
*   **Static metodlar:** **Hayır**, override edilemez ama **method hiding** (metod gizleme) olur. Child class'ta aynı imzayla static metod yazarsanız, parent'ın metodu gizlenir ama override değildir (polymorphism çalışmaz).
*   **Trap:** "Static metodlar override edilir" tuzağı yaygındır.

#### Soru 8 (Tricky): `final` sınıftan inheritance yapabilir miyiz? `final` metodu override edebilir miyiz?
**Cevap:**
*   **Final sınıf:** **Hayır**, extend edilemez. (Örnek: `String`, `Integer` final'dır).
*   **Final metod:** **Hayır**, override edilemez.
*   **Trap:** "Final değişken değiştirilemez" bilgisi ile karıştırılır. Final sınıf ve metod inheritance ile ilgilidir.

#### Soru 9 (Tricky): Bir interface, başka bir interface'i `implement` edebilir mi yoksa `extend` mi eder?
**Cevap:** Interface başka bir interface'i **extend** eder, `implements` değil!
```java
interface A { void methodA(); }
interface B extends A { void methodB(); } // Doğru
// interface B implements A { } // YANLIŞ, compile error
```
*   Sınıflar interface'i `implements`, sınıfları `extends` eder.
*   Interface'ler birbirini `extends` eder.
*   **Trap:** Mülakatta kelime karışıklığı yapılabilir.

#### Soru 6: `final` keyword'ü nerede kullanılır ve etkileri nedir?
**Cevap:**
- **final class:** Extend edilemez (örn: `String`, `Integer`). Güvenlik ve tasarım kararı.
- **final method:** Override edilemez. Alt sınıfların davranışı değiştirmesini engeller.
- **final variable:** Bir kez atandıktan sonra değiştirilemez (constant). Instance variable ise constructor'da atanmalı.

```java
final class ImmutableClass { } // Extend edilemez

class Parent {
    public final void criticalMethod() { } // Override edilemez
}

class Example {
    private final int id; // Constructor'da atanmalı
    
    public Example(int id) {
        this.id = id;
    }
}
```

#### Soru 7: `instanceof` operatörü ne zaman kullanılır? Alternatifi var mı?
**Cevap:** Runtime'da bir nesnenin belirli bir tipte olup olmadığını kontrol eder. Polymorphic kod yazarken tip kontrolü için kullanılır.

```java
Animal a = new Dog();
if (a instanceof Dog) {
    Dog d = (Dog) a; // Safe casting
    d.bark();
}
```

**Alternatif (Java 16+): Pattern Matching**
```java
if (a instanceof Dog d) { // Otomatik cast
    d.bark();
}
```

**❌ Anti-Pattern:** Çok fazla `instanceof` kullanımı kötü tasarımın göstergesidir. Polymorphism ile çözülmelidir.

---

### 8. Geliştirici İpuçları (Pros/Cons & Best Practices)

**✅ İyi Kullanım:**
- **Favor Composition over Inheritance:** Esneklik için öncelikle composition düşünün.
- **Program to Interface, not Implementation:** Somut sınıflar yerine interface'lere bağımlılık oluşturun.
  ```java
  List<String> list = new ArrayList<>(); // ✅ İyi
  ArrayList<String> list = new ArrayList<>(); // ❌ Kötü (ArrayList'e bağımlısınız)
  ```
- **SOLID Prensipleri:** Özellikle **Liskov Substitution Principle** (Child, parent'ın yerine geçebilmeli) ve **Dependency Inversion Principle** (Abstraction'a bağımlı ol).

**❌ Kaçınılması Gerekenler:**
- **God Class:** Çok fazla sorumluluk yüklenmiş devasa sınıflar yapmayın.
- **Deep Inheritance:** 3-4 seviyeden fazla kalıtım hiyerarşisi karmaşıklığa yol açar.
- **Getter/Setter Abuse:** Her field için körü körüne getter/setter yazmak encapsulation'ı bozar. Nesnenin davranışlarını (behavior) ortaya çıkarın, iç yapısını değil.

**Gerçek Dünya Örneği (Banking Sistemi):**
```java
// İyi Tasarım: Interface + Composition
interface PaymentProcessor {
    void processPayment(double amount);
}

class CreditCardProcessor implements PaymentProcessor {
    public void processPayment(double amount) {
        // Kredi kartı işlemi
    }
}

class BankAccount {
    private PaymentProcessor paymentProcessor; // Composition
    
    public BankAccount(PaymentProcessor processor) {
        this.paymentProcessor = processor;
    }
    
    public void pay(double amount) {
        paymentProcessor.processPayment(amount);
    }
}

// Runtime'da farklı ödeme yöntemleri kullanılabilir
BankAccount account = new BankAccount(new CreditCardProcessor());
account.pay(100.0);
```

---

### Özet (OOP Prensipleri)

| Prensip | Anahtar Kelime | Amaç | Gerçek Dünya Benzetmesi |
| :--- | :--- | :--- | :--- |
| **Inheritance** | `extends` | Kod yeniden kullanımı, hiyerarşi | Ebeveyn-çocuk ilişkisi |
| **Encapsulation** | `private`, getter/setter | Veri gizleme, kontrollü erişim | Kapsül ilaç, araba motoru |
| **Abstraction** | `abstract`, `interface` | Detay gizleme, contract tanımlama | Kahve makinesi butonu |
| **Polymorphism** | `@Override`, `interface` | Çok biçimlilik, esneklik | Aynı tuş farklı cihazlarda farklı işler |

Bu prensipler birbirini tamamlar. Mülakatta sadece tanımları değil, **ne zaman, neden ve nasıl** kullanıldığını örneklerle açıklayabilmek beklenir.

---

