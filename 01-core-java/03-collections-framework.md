## Konu 3: Java Collections Framework

Java Collections Framework, nesne gruplarını depolamak, yönetmek ve işlemek için standart bir mimari sunar. Bir geliştirici olarak, sadece "List sıralıdır, Set unique'dir" demek yetmez; veri yapılarının **Big O karmaşıklığını**, **hafıza ayak izini (memory footprint)** ve **thread-safety** durumlarını bilmeniz gerekir.

### 1. Genel Hiyerarşi

Collections Framework iki ana kökten türetilir:
1.  **`Collection` Interface:** (Iterable'dan türetilir)
    *   `List` (Sıralı, tekrarlı eleman olabilir)
    *   `Set` (Benzersiz elemanlar)
    *   `Queue` / `Deque` (İşlem sırası)
2.  **`Map` Interface:** (Collection'dan türetilmez!)
    *   Key-Value çiftleri tutar.

---

### 2. Array vs ArrayList

**Analoji:**
*   **Array:** Bir yumurta kolisidir. Boyutu sabittir (10'lu, 30'lu). Dolarsa yenisini almanız gerekir, genişletemezsiniz.
*   **ArrayList:** Akordeon dosya gibidir. İçine kağıt koydukça genişler.

| Özellik | Array (`[]`) | ArrayList (`List<T>`) |
| :--- | :--- | :--- |
| **Boyut** | Sabittir (Fixed). Oluşturulurken belirlenir. | Dinamiktir. Gerektiğinde otomatik büyür. |
| **Performans** | En hızlısıdır. Primitive tutabilir. | Biraz daha yavaştır. Sadece Obje tutar (Autoboxing). |
| **Tip** | Hem primitive (`int`) hem Object tutar. | Sadece Object (`Integer`) tutar. |
| **Generic** | Generic desteklemez (Covariant'tır). | Generic destekler. |

**ArrayList Büyüme Mantığı:**
ArrayList dolduğunda, kapasitesi genellikle **eski kapasitenin %50'si kadar** artırılır (`oldCapacity + (oldCapacity >> 1)`). Yeni bir dizi oluşturulur ve eski elemanlar `System.arraycopy` ile kopyalanır. Bu maliyetli bir işlemdir.
*   **Tavsiye:** Eleman sayısı tahmin edilebiliyorsa `new ArrayList<>(1000)` gibi başlangıç kapasitesi verin.

---

### 3. List Interface (ArrayList vs LinkedList)

*   **ArrayList:** Arkada dinamik array kullanır.
    *   **Erişim (`get`):** O(1) - Çok hızlı (Random Access).
    *   **Ekleme/Silme (Ortadan):** O(n) - Kaydırma (shift) işlemi gerekir.
    *   **Kullanım:** Okuma ağırlıklı işlerde.
*   **LinkedList:** Arkada Doubly Linked List (Çift Yönlü Bağlı Liste) kullanır.
    *   **Erişim (`get`):** O(n) - Baştan/sondan tek tek gider.
    *   **Ekleme/Silme:** O(1) - Sadece pointer değişir (Eğer iteratör ile yerindeyse).
    *   **Kullanım:** Sürekli ekleme/çıkarma yapılan kuyruk yapılarında.

---

### 4. Set Interface (Benzersizlik)

*   **HashSet:** En hızlısıdır. Sıra garantisi yoktur. Arkada `HashMap` kullanır (Value kısmına dummy obje koyar).
*   **LinkedHashSet:** Ekleme sırasını (Insertion Order) korur. Arkada Doubly Linked List + Hash Table kullanır.
*   **TreeSet:** Elemanları sıralı tutar (Sorted). `Comparable` veya `Comparator` kullanır. Arkada `TreeMap` (Red-Black Tree) kullanır. Ekleme/Arama O(log n)'dir.

---

### 5. Map Interface ve HashMap Çalışma Mantığı (DERİNLEMESİNE)

**Analoji:** HashMap devasa bir kütüphane dolabıdır. Her kitabın (Value) bir barkodu (Key) vardır. Barkodun hash'ine göre hangi rafa (Bucket) konulacağı hesaplanır.

#### HashMap Nasıl Çalışır? (Internal Working)
HashMap, **Hashing** prensibine dayanır. `Node<K,V>` dizisi (bucket array) tutar.

1.  **`put(K key, V value)` Çağrıldığında:**
    *   `key.hashCode()` hesaplanır.
    *   Hash kodu, dizi boyutuna göre bir index'e dönüştürülür (`hash & (n-1)`).
    *   O index boşsa, yeni Node oraya konur.
    *   **Collision (Çakışma):** Eğer o index doluysa (farklı key ama aynı bucket index), `equals()` metodu ile key'ler karşılaştırılır.
        *   Key'ler eşitse: Value güncellenir.
        *   Key'ler farklıysa: Linked List olarak ucuna eklenir (Java 8 öncesi).

2.  **Java 8 İyileştirmesi (Treeify):**
    *   Bir bucket'taki eleman sayısı **TREEIFY_THRESHOLD (8)** değerini geçerse, Linked List yapısı **Red-Black Tree** yapısına dönüştürülür.
    *   Bu, en kötü durumda (worst-case) arama performansını O(n)'den **O(log n)**'e düşürür.

3.  **`get(K key)` Çağrıldığında:**
    *   Hash hesaplanır, index bulunur.
    *   Bucket'taki listede/ağaçta `key.equals()` ile arama yapılır.

**Önemli Sözleşme (Contract):**
Eğer iki nesne `equals()` ile eşitse, `hashCode()` değerleri de **KESİNLİKLE** eşit olmalıdır. Aksi takdirde HashMap'e koyduğunuz nesneyi geri bulamazsınız.

---

### 6. Queue, Deque ve Stack

*   **Queue (Kuyruk):** FIFO (First In First Out).
    *   `PriorityQueue`: Öncelik sırasına göre (Heap yapısı) tutar. Sıralı çıkış sağlar.
*   **Deque (Double Ended Queue):** Hem baştan hem sondan ekleme/çıkarma.
    *   `ArrayDeque`: Stack ve Queue implementasyonu için en iyi performansı verir.
*   **Stack:** LIFO (Last In First Out).
    *   **Legacy (Eski):** `java.util.Stack` sınıfı `Vector`'den türetilmiştir ve synchronized'dır (yavaştır).
    *   **Modern:** Stack yapısı için `Deque` interface'i ve `ArrayDeque` implementasyonu kullanılmalıdır (`push`, `pop`).

---

### 7. Iterator ve Fail-Fast vs Fail-Safe

*   **Iterator:** Koleksiyon üzerinde gezinmek için standart arayüz.
*   **Fail-Fast:** İterasyon sırasında koleksiyonun yapısı değiştirilirse (add/remove), anında `ConcurrentModificationException` fırlatır.
    *   Örnek: `ArrayList`, `HashMap`, `HashSet` iteratörleri.
*   **Fail-Safe (Weakly Consistent):** İterasyon sırasında değişiklik yapılsa bile hata vermez. Genellikle koleksiyonun bir kopyası veya anlık görüntüsü üzerinde çalışır.
    *   Örnek: `ConcurrentHashMap`, `CopyOnWriteArrayList`.

---

### 8. Generic Collections

Java 5 ile geldi. **Compile-time type safety** sağlar.
*   **Type Erasure:** Generics sadece derleme zamanında vardır. Runtime'da JVM generic tipleri bilmez, her şeyi `Object` (veya bound type) olarak görür. Bu geriye uyumluluk içindir.
*   `List<Object>` ile `List<String>` arasında kalıtım ilişkisi yoktur.
*   **Wildcards:**
    *   `<?>`: Bilinmeyen tip.
    *   `<? extends Number>`: Number veya alt sınıfları (Upper Bounded).
    *   `<? super Integer>`: Integer veya üst sınıfları (Lower Bounded).

---

### 9. Karşılaştırma Tablosu (Big O)

| Veri Yapısı | Get (Erişim) | Add (Ekleme) | Contains (Arama) | Remove (Silme) |
| :--- | :--- | :--- | :--- | :--- |
| **ArrayList** | O(1) | O(1) (Amortized) | O(n) | O(n) |
| **LinkedList** | O(n) | O(1) | O(n) | O(1) |
| **HashSet** | - | O(1) | O(1) | O(1) |
| **TreeSet** | - | O(log n) | O(log n) | O(log n) |
| **HashMap** | O(1) | O(1) | O(1) | O(1) |
| **TreeMap** | O(log n) | O(log n) | O(log n) | O(log n) |

*(Not: Hash tabanlı yapılarda O(1) ortalama durumdur, kötü hash fonksiyonu ile O(n) olabilir, Java 8 ile O(log n) garantilenir.)*

---

### 10. Kritik Mülakat Soruları 

#### Soru 1: HashMap'te `equals()` ve `hashCode()` metodlarını override etmezsek ne olur?
**Cevap:**
*   `hashCode()` override edilmezse: Aynı veriye sahip iki farklı nesne farklı hash kodları üretir. Map'e koyduğunuzu `get` ile alamazsınız (null döner).
*   `equals()` override edilmezse: Hash collision olsa bile, doğru nesneyi bulamaz (referans eşitliğine bakar).
*   **Kural:** Map'te Key olarak kullanılacak nesneler **Immutable** olmalıdır veya hash kodu değişmemelidir.

#### Soru 2: `ArrayList` thread-safe midir? Nasıl thread-safe yapılır?
**Cevap:** Hayır, değildir.
1.  `Collections.synchronizedList(new ArrayList<>())` (Eski yöntem, tüm metodlar kilitlenir).
2.  `CopyOnWriteArrayList` (Okuma çok, yazma az ise).
3.  Manuel senkronizasyon (`synchronized` bloğu).

#### Soru 3: `ConcurrentHashMap` ile `Hashtable` farkı nedir?
**Cevap:**
*   **Hashtable:** Tüm metodları `synchronized`'dır. Tek bir kilit (lock) tüm haritayı kilitler. Çok yavaştır.
*   **ConcurrentHashMap:** **Lock Striping** (Parçalı Kilitleme) kullanır. Haritayı segmentlere böler (varsayılan 16). Bir thread bir segmente yazarken, diğerleri başka segmentlere yazabilir veya okuyabilir. Okuma işlemi (get) genellikle kilitsizdir (lock-free). `null` key veya value kabul etmez.

#### Soru 4: `List<Integer>` bir `List<Object>` midir?
**Cevap:** Hayır. Java'da Generics **invariant**'tır. Eğer öyle olsaydı, `List<Object>` referansına `List<Integer>` atayıp, içine `String` ekleyebilirdik, bu da runtime'da patlardı.

#### Soru 5: Bir döngü içinde Listeden eleman silerken `for(int i=0...)` mı `foreach` mi kullanılmalı?
**Cevap:**
*   `foreach` döngüsünde `list.remove(item)` yaparsanız `ConcurrentModificationException` alırsınız.
*   `Iterator` kullanıp `iterator.remove()` demek en güvenli yoldur.
*   Java 8+: `list.removeIf(filter)` en temiz yöntemdir.

#### Soru 6 (Tricky): `HashMap`'te `null` key ve `null` value kullanabilir miyim? Peki `Hashtable` veya `ConcurrentHashMap`?
**Cevap:**
*   **HashMap:** Bir adet `null` key ve sınırsız `null` value kabul eder.
*   **Hashtable:** **Hayır**, ne `null` key ne de `null` value kabul eder. `NullPointerException` fırlatır.
*   **ConcurrentHashMap:** **Hayır**, hiçbirini kabul etmez. `NullPointerException` fırlatır.
*   **Trap:** HashMap ile diğerleri karıştırılabilir. Concurrent yapılar `null` kabul etmez çünkü "get null dönerse key yok mu yoksa value null mu?" belirsizliği yaratır.

#### Soru 7 (Tricky): `HashSet` içeride hangi veri yapısını kullanır?
**Cevap:** **HashMap** kullanır!
*   HashSet'e eklenen her eleman, HashMap'te **key** olarak saklanır. Value kısmına dummy bir `Object` konulur (`PRESENT` sabiti).
*   Bu yüzden HashSet'in tüm özellikleri (hız, tekrarsızlık) HashMap'ten gelir.
*   **Trap:** "HashSet başlı başına bir implementasyondur" sanılabilir ama aslında HashMap wrapper'ıdır.

#### Soru 8 (Tricky): `TreeMap` ve `HashMap` arasındaki fark nedir? Hangisi daha hızlıdır?
**Cevap:**
*   **HashMap:** Hash table kullanır. O(1) ortalama erişim. **Sıralamaz**.
*   **TreeMap:** Red-Black Tree (self-balancing BST) kullanır. O(log n) erişim. Key'lere göre **sıralı** tutar (natural order veya custom Comparator).
*   **Hız:** HashMap daha hızlıdır (O(1) vs O(log n)). TreeMap sıralama gerektiğinde kullanılır.
*   **Trap:** "TreeMap her zaman daha iyidir" denmemeli. Sıralama gerekmiyorsa HashMap tercih edilir.

#### Soru 9 (Tricky): `Arrays.asList()` ile dönen liste değiştirilebilir mi? `add()` veya `remove()` çalışır mı?
**Cevap:** **Hayır**, dönen liste **fixed-size**'dır.
*   `set(index, element)` çalışır (mevcut eleman değiştirilir).
*   `add()` veya `remove()` çalıştırılırsa `UnsupportedOperationException` fırlatır.
*   **Çözüm:** `new ArrayList<>(Arrays.asList(...))` ile yeni bir liste oluşturun.
*   **Trap:** "ArrayList döner" sanılabilir ama aslında `Arrays.ArrayList` (iç sınıf) döner.

#### Soru 10 (Tricky): `fail-fast` iterator nedir? Hangi durumda `ConcurrentModificationException` alırız?
**Cevap:** İterasyon sırasında koleksiyonun **structural modification** (yapısal değişiklik: add/remove) yapılırsa exception fırlatır.
*   **Çalışır:** `iterator.remove()` (iteratorün kendi metodu).
*   **Patlar:** `list.remove()` (koleksiyonun metodu).
*   **Örnek:**
```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
for (String s : list) {
    list.remove(s); // ConcurrentModificationException!
}

// Doğru yol:
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (condition) it.remove(); // Güvenli
}
```
*   **Trap:** "Multithreading sorunu" sanılabilir ama single-thread'de de olur.

### 11. Geliştirici İpuçları

*   **Initial Capacity:** Koleksiyon oluştururken tahmini boyutu biliyorsanız mutlaka verin (`new HashMap<>(10000)`). Bu, sürekli yeniden boyutlandırma (rehashing/resizing) maliyetini önler.
*   **Key Seçimi:** HashMap key'i olarak `String` veya `Integer` gibi immutable sınıfları tercih edin. Kendi sınıfınızı kullanacaksanız `equals` ve `hashCode`'u IDE'ye generate ettirin ve field'ları `final` yapın.
*   **Set Kullanımı:** Bir listedeki tekrarlı elemanları silmenin en hızlı yolu `new ArrayList<>(new HashSet<>(list))`'tir (Sıra önemli değilse).
*   **Arrays.asList:** `Arrays.asList()` ile dönen liste **fixed-size**'dır. `add/remove` yaparsanız `UnsupportedOperationException` alırsınız. Değiştirilebilir yapmak için `new ArrayList<>(Arrays.asList(...))` kullanın.

---

