## Konu 42: Beyaz Tahta Mülakat Soruları (30 Klasik Algoritma)

Bu bölüm, mülakatlarda **beyaz tahta (whiteboard)** veya **kağıt üzerinde** kod yazmanız istendiğinde karşılaşabileceğiniz en klasik 30 algoritma sorusunu içerir. Bu sorular genellikle "ezber" değil, "problem çözme yeteneği" ve "temiz kod yazma" becerisini ölçer.

**Önemli İpucu:** Kodu yazarken sesli düşünün. "Burada HashMap kullanıyorum çünkü O(1) erişim istiyorum" gibi açıklamalar yapın.

---

### A. String Manipulation

**Soru 1: Longest Palindromic Substring**
*   **Problem:** Verilen bir string içindeki en uzun palindromu bulun. (Örn: "babad" -> "bab" veya "aba", "mete" -> "ete").
*   **Çözüm (Expand Around Center):** Her karakteri (ve iki karakter arasını) merkez kabul edip sağa ve sola doğru genişletin.
    ```java
    public String longestPalindrome(String s) {
        if (s == null || s.length() < 1) return "";
        int start = 0, end = 0;
        for (int i = 0; i < s.length(); i++) {
            int len1 = expandAroundCenter(s, i, i);       // Tekli merkez (racecar)
            int len2 = expandAroundCenter(s, i, i + 1);   // Çiftli merkez (abba)
            int len = Math.max(len1, len2);
            if (len > end - start) {
                start = i - (len - 1) / 2;
                end = i + len / 2;
            }
        }
        return s.substring(start, end + 1);
    }
    private int expandAroundCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--; right++;
        }
        return right - left - 1;
    }
    ```
*   **Complexity:** Time O(N^2), Space O(1).

**Soru 2: Valid Palindrome**
*   **Problem:** Verilen string palindrom mu? (Sadece harf/rakam, case-insensitive). "A man, a plan, a canal: Panama" -> True.
*   **Çözüm (Two Pointers):** Biri baştan, biri sondan gelir. Alphanumeric olmayanları atlar.
*   **Complexity:** Time O(N), Space O(1).

**Soru 3: Valid Anagram**
*   **Problem:** İki string anagram mı? (Harfler aynı, sıra farklı). "listen", "silent" -> True.
*   **Çözüm:** 26 elemanlı bir `int[] count` dizisi tutun. Birinci string için artırın, ikinci için azaltın. Sonunda hepsi 0 olmalı.
*   **Complexity:** Time O(N), Space O(1) (Alfabe boyutu sabit).

**Soru 4: Reverse String**
*   **Problem:** String'i ters çevirin (In-place, ekstra hafıza yok). `["h","e","l","l","o"]` -> `["o","l","l","e","h"]`.
*   **Çözüm:** Two Pointers. `left=0`, `right=len-1`. `swap(s[left], s[right])` yapıp pointerları yaklaştırın.
*   **Complexity:** Time O(N), Space O(1).

**Soru 5: Longest Substring Without Repeating Characters**
*   **Problem:** Tekrar eden karakter içermeyen en uzun alt string. "abcabcbb" -> "abc" (3).
*   **Çözüm (Sliding Window):** `HashSet` veya `HashMap` ile penceredeki karakterleri tutun. Tekrar edene rastlayınca sol pointer'ı (window start) ilerletin.
*   **Complexity:** Time O(N), Space O(min(N, M)) (M: Alfabe boyutu).

---

### B. Arrays & Hashing

**Soru 6: Two Sum**
*   **Problem:** Dizide toplamı `target` olan iki sayının indekslerini dönün.
*   **Çözüm:** `HashMap<Sayı, İndeks>` kullanın. `target - num` map'te var mı diye bakın.
*   **Complexity:** Time O(N), Space O(N).

**Soru 7: Maximum Subarray**
*   **Problem:** Toplamı en büyük olan bitişik alt diziyi (subarray) bulun. `[-2,1,-3,4,-1,2,1,-5,4]` -> `[4,-1,2,1]` (6).
*   **Çözüm (Kadane's Algorithm):** `currentSum = max(num, currentSum + num)`. Eğer `currentSum` negatifse sıfırla (veya o sayıdan başla).
*   **Complexity:** Time O(N), Space O(1).

**Soru 8: Move Zeroes**
*   **Problem:** Tüm 0'ları sona atın, diğerlerinin sırasını bozmayın. `[0,1,0,3,12]` -> `[1,3,12,0,0]`.
*   **Çözüm:** `insertPos` pointer'ı tutun. 0 olmayan her sayıyı `insertPos`'a yazıp artırın. Kalan yerleri 0 ile doldurun.
*   **Complexity:** Time O(N), Space O(1).

**Soru 9: Contains Duplicate**
*   **Problem:** Dizide tekrar eden sayı var mı?
*   **Çözüm:** `HashSet`'e ekleyin. `add()` false dönerse duplicate vardır.
*   **Complexity:** Time O(N), Space O(N).

**Soru 10: Product of Array Except Self**
*   **Problem:** Kendisi hariç diğer elemanların çarpımını içeren dizi oluşturun. (Bölme işlemi yasak!).
*   **Çözüm:** Önce soldan sağa çarpımları (Prefix), sonra sağdan sola çarpımları (Suffix) hesaplayın.
*   **Complexity:** Time O(N), Space O(1) (Sonuç dizisi hariç).

**Soru 11: Intersection of Two Arrays**
*   **Problem:** İki dizinin kesişimini bulun.
*   **Çözüm:** Birini `HashSet`'e atın. Diğerini gezerken set'te var mı diye bakın.
*   **Complexity:** Time O(N+M), Space O(min(N,M)).

**Soru 12: Missing Number**
*   **Problem:** `[0, n]` aralığındaki sayılardan biri eksik. Hangisi?
*   **Çözüm (Gauss Sum):** `n * (n+1) / 2` toplamından dizi toplamını çıkarın. Veya XOR kullanın.
*   **Complexity:** Time O(N), Space O(1).

---

### C. Linked Lists

**Soru 13: Reverse Linked List**
*   **Problem:** Bağlı listeyi ters çevirin.
*   **Çözüm:** `prev`, `curr`, `next` pointerları kullanın. `curr.next = prev` yapıp ilerleyin.
*   **Complexity:** Time O(N), Space O(1).

**Soru 14: Detect Cycle in Linked List**
*   **Problem:** Listede döngü var mı?
*   **Çözüm (Floyd's Tortoise and Hare):** `slow` 1 adım, `fast` 2 adım gider. Çakışırlarsa döngü vardır.
*   **Complexity:** Time O(N), Space O(1).

**Soru 15: Merge Two Sorted Lists**
*   **Problem:** İki sıralı listeyi birleştirin.
*   **Çözüm:** `dummy` node oluşturun. İki listenin başını karşılaştırıp küçüğü ekleyin.
*   **Complexity:** Time O(N+M), Space O(1).

**Soru 16: Remove Nth Node From End**
*   **Problem:** Sondan n. düğümü silin.
*   **Çözüm:** İki pointer. `fast` pointer'ı n adım önden başlatın. `fast` sona gelince `slow` silinecek düğümün öncesindedir.
*   **Complexity:** Time O(N), Space O(1).

---

### D. Trees & Graphs

**Soru 17: Invert Binary Tree**
*   **Problem:** Ağacı aynalayın (Sağ ve sol çocukları yer değiştirin).
*   **Çözüm:** Recursive. `temp = left; left = invert(right); right = invert(temp);`.
*   **Complexity:** Time O(N), Space O(N) (Recursion stack).

**Soru 18: Maximum Depth of Binary Tree**
*   **Problem:** Ağacın derinliğini bulun.
*   **Çözüm:** `max(maxDepth(left), maxDepth(right)) + 1`.
*   **Complexity:** Time O(N), Space O(N).

**Soru 19: Validate Binary Search Tree (BST)**
*   **Problem:** Ağaç geçerli bir BST mi? (Sol < Kök < Sağ).
*   **Çözüm:** Recursive olarak `min` ve `max` sınırları geçirin. `isValid(node, min, max)`.
*   **Complexity:** Time O(N), Space O(N).

**Soru 20: Symmetric Tree**
*   **Problem:** Ağaç simetrik mi (Ayna görüntüsü mü)?
*   **Çözüm:** `isMirror(left, right)` fonksiyonu. `left.val == right.val` ve `isMirror(left.left, right.right)` ve `isMirror(left.right, right.left)`.
*   **Complexity:** Time O(N), Space O(N).

**Soru 21: Number of Islands**
*   **Problem:** 1'ler kara, 0'lar su. Kaç ada var?
*   **Çözüm:** Grid'i gezin. 1 bulunca sayacı artırın ve o adayı yok etmek (visited yapmak) için DFS/BFS başlatın.
*   **Complexity:** Time O(M*N), Space O(M*N).

---

### E. Dynamic Programming & Math

**Soru 22: Climbing Stairs**
*   **Problem:** n basamaklı merdiveni 1 veya 2 adım atarak kaç farklı şekilde çıkarsın?
*   **Çözüm:** Fibonacci dizisidir. `dp[i] = dp[i-1] + dp[i-2]`.
*   **Complexity:** Time O(N), Space O(1).

**Soru 23: Best Time to Buy and Sell Stock**
*   **Problem:** Bir kez alıp bir kez satarak maksimum karı bulun.
*   **Çözüm:** Tek geçiş. `minPrice`'ı güncelleyin ve her adımda `price - minPrice` ile karı hesaplayın.
*   **Complexity:** Time O(N), Space O(1).

**Soru 24: House Robber**
*   **Problem:** Yan yana iki evi soyamazsın. Maksimum kazanç nedir?
*   **Çözüm:** `dp[i] = max(dp[i-1], dp[i-2] + nums[i])`. (Soyma veya soy).
*   **Complexity:** Time O(N), Space O(1).

**Soru 25: Coin Change**
*   **Problem:** Verilen bozuk paralarla `amount` tutarını en az kaç parayla oluşturursun?
*   **Çözüm:** `dp[amount]`. `dp[i] = min(dp[i], dp[i - coin] + 1)`.
*   **Complexity:** Time O(S*N), Space O(N).

**Soru 26: Fizz Buzz**
*   **Problem:** 3'ün katı Fizz, 5'in katı Buzz, 15'in katı FizzBuzz yazdır.
*   **Çözüm:** Modulo operatörü (`%`). Önce 15'i kontrol edin (veya string birleştirme yapın).
*   **Complexity:** Time O(N).

**Soru 27: Single Number**
*   **Problem:** Dizide her sayı iki kere var, biri hariç. O(N) time ve O(1) space ile bulun.
*   **Çözüm:** XOR işlemi. `a ^ a = 0`, `a ^ 0 = a`. Tüm sayıları XOR'larsanız geriye tek kalan kalır.

---

### F. Stacks & Queues

**Soru 28: Valid Parentheses**
*   **Problem:** `()[]{}` parantez dizilimi geçerli mi?
*   **Çözüm:** Stack kullanın. Açılanı atın, kapanan gelince stack tepesindekiyle eşleşiyor mu bakın.
*   **Complexity:** Time O(N), Space O(N).

**Soru 29: Implement Queue using Stacks**
*   **Problem:** İki Stack kullanarak Queue (FIFO) yapın.
*   **Çözüm:** `push` için `stack1`'e atın. `pop` için `stack2` boşsa `stack1`'i `stack2`'ye boşaltın.
*   **Complexity:** Amortized O(1).

---

### G. System Design (Whiteboard Friendly)

**Soru 30: LRU Cache (Least Recently Used)**
*   **Problem:** Kapasitesi sınırlı bir cache tasarlayın. Dolunca en az kullanılanı silsin. `get` ve `put` O(1) olsun.
*   **Çözüm:** `HashMap` + `Doubly Linked List`.
    *   **HashMap:** Key -> Node (Hızlı erişim için).
    *   **Doubly Linked List:** Elemanların kullanım sırasını tutar. En son erişilen başa (head), en eski sona (tail) gider.
    *   `get(key)`: Map'ten bul, listeden çıkar, başa ekle.
    *   `put(key, value)`: Map'e ekle, başa ekle. Kapasite dolduysa sondakini sil.
*   **Complexity:** Time O(1), Space O(N).

