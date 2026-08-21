## Konu 19: Hashing Algoritmaları (MD5, SHA, bcrypt, scrypt, Argon2)

Hashing algoritmaları, güvenlik ve veri bütünlüğünün temelidir. Bir geliştirici olarak, hangi algoritmanın ne zaman kullanılacağını, şifre hash'leme için neden bcrypt/Argon2 kullanılması gerektiğini ve Rainbow Table, Salt, Pepper gibi kavramları bilmelisiniz.

---

### 1. Hashing Nedir?

**Analoji:** Hashing, bir "kıyma makinesi" gibidir. Eti (veriyi) makineye koyarsınız, kıyma (hash) çıkar. Kıymadan eti geri çıkaramazsınız (tek yönlü fonksiyon).

#### Hash Fonksiyonunun Özellikleri
1.  **Deterministik:** Aynı girdi her zaman aynı hash'i üretir.
2.  **Hızlı:** Hash hesaplama hızlı olmalıdır.
3.  **Tek Yönlü:** Hash'ten orijinal veriyi çıkarmak imkansız olmalıdır.
4.  **Avalanche Effect:** Girdide küçük bir değişiklik, hash'te büyük değişiklik yaratır.
5.  **Collision Resistance:** İki farklı girdinin aynı hash'i üretmesi çok zor olmalıdır.

---

### 2. Kriptografik Hash Algoritmaları

#### MD5 (Message Digest 5)

**Çıkış:** 128-bit (32 hex karakter)

```java
import java.security.MessageDigest;

public String md5Hash(String input) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hash = md.digest(input.getBytes());
    return bytesToHex(hash);
}
```

**Durum:** **KULLANMAYIN!** Artık güvenli değildir.
*   **Collision saldırıları:** İki farklı girdi aynı hash'i üretebilir.
*   **Kullanım:** Sadece checksum (dosya bütünlüğü kontrolü) için.

#### SHA (Secure Hash Algorithm)

| Algoritma | Çıkış Boyutu | Durum |
| :--- | :--- | :--- |
| **SHA-1** | 160-bit | **Güvensiz** (2017'de kırıldı) |
| **SHA-256** | 256-bit | Güvenli |
| **SHA-512** | 512-bit | Güvenli (daha yavaş) |

```java
public String sha256Hash(String input) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest(input.getBytes());
    return bytesToHex(hash);
}
```

**Kullanım:**
*   Dosya bütünlüğü (checksum).
*   Blockchain (Bitcoin SHA-256 kullanır).
*   **Şifre hash'leme için UYGUN DEĞİL** (çok hızlı, rainbow table saldırısına açık).

---

### 3. Şifre Hash'leme (Password Hashing)

**Sorun:** SHA-256 gibi hızlı algoritmalar, brute-force saldırılarına karşı savunmasızdır. Saniyede milyarlarca hash hesaplanabilir.

**Çözüm:** Yavaş, adaptif algoritmalar kullanın (bcrypt, scrypt, Argon2).

#### bcrypt

**Analoji:** bcrypt, bir "zaman kilidi" gibidir. Şifreyi hash'lerken kasıtlı olarak yavaşlatır. Brute-force saldırısı yapmak yıllar alır.

**Özellikler:**
*   **Adaptif:** Work factor (cost) artırılarak yavaşlatılabilir.
*   **Salt:** Her şifre için rastgele salt otomatik oluşturulur.
*   **Blowfish** algoritmasına dayanır.

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

// Şifre hash'leme
String hashedPassword = encoder.encode("myPassword123");
// $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

// Şifre doğrulama
boolean matches = encoder.matches("myPassword123", hashedPassword); // true
```

**Hash Formatı:**
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 |  |  |                                                          |
 |  |  |                                                          +-- Hash (31 karakter)
 |  |  +-- Salt (22 karakter)
 |  +-- Cost (10 = 2^10 = 1024 round)
 +-- Algoritma versiyonu (2a = bcrypt)
```

**Cost Seçimi:**
*   **10:** Varsayılan, ~100ms (önerilen).
*   **12:** ~400ms (daha güvenli).
*   **14:** ~1.6 saniye (çok yavaş, kullanıcı deneyimi kötü).

#### scrypt

**Özellikler:**
*   **Memory-hard:** Sadece CPU değil, RAM de tüketir. GPU/ASIC saldırılarına karşı daha dayanıklı.
*   **Parametreler:** N (CPU/Memory cost), r (block size), p (parallelization).

```java
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

SCryptPasswordEncoder encoder = new SCryptPasswordEncoder();
String hashedPassword = encoder.encode("myPassword123");
```

**Ne Zaman Kullanılmalı?**
*   Çok yüksek güvenlik gerekiyorsa (örn: kripto para cüzdanları).
*   bcrypt'ten daha yavaş ve kaynak tüketir.

#### Argon2

**2015 Password Hashing Competition kazananı.** En modern ve güvenli algoritma.

**Özellikler:**
*   **Memory-hard:** scrypt gibi RAM tüketir.
*   **3 Varyant:**
    *   **Argon2d:** GPU saldırılarına karşı en güçlü (kripto para madenciliği).
    *   **Argon2i:** Side-channel saldırılarına karşı güçlü (şifre hash'leme).
    *   **Argon2id:** İkisinin karışımı (önerilen).

```java
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

Argon2PasswordEncoder encoder = new Argon2PasswordEncoder();
String hashedPassword = encoder.encode("myPassword123");
```

**Tavsiye:** Yeni projelerde **Argon2id** kullanın.

---

### 4. Salt ve Pepper

#### Salt

**Analoji:** Salt, her yemeğe (şifre) farklı baharat eklemek gibidir. Aynı şifre bile farklı hash üretir.

**Amaç:** Rainbow Table saldırılarını önlemek.

**Rainbow Table:** Önceden hesaplanmış milyarlarca şifre-hash eşleşmesi tablosu. Salt kullanılmazsa, saldırgan tabloda hash'i arar ve şifreyi bulur.

```java
// Salt olmadan (KÖT Ü)
SHA-256("password123") = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"
// Saldırgan rainbow table'da bu hash'i arar, "password123" bulur.

// Salt ile (İYİ)
SHA-256("password123" + "randomSalt123") = "a1b2c3d4..."
// Her kullanıcı farklı salt, rainbow table işe yaramaz.
```

**bcrypt, scrypt, Argon2 otomatik salt ekler.**

#### Pepper

**Analoji:** Pepper, tüm yemeklere (şifreler) eklenen gizli bir baharat gibidir. Veritabanında saklanmaz, sadece uygulama kodunda veya environment variable'da bulunur.

```java
String pepper = System.getenv("PASSWORD_PEPPER"); // Gizli değer
String hashedPassword = encoder.encode(password + pepper);
```

**Amaç:** Veritabanı çalınırsa bile, pepper olmadan şifreler kırılamaz.

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: Neden şifre hash'leme için SHA-256 kullanmamalıyız?
**Cevap:** SHA-256 çok hızlıdır. Modern GPU'lar saniyede milyarlarca SHA-256 hash'i hesaplayabilir. Brute-force saldırısı çok kolaydır.

**Çözüm:** bcrypt, scrypt veya Argon2 gibi yavaş, adaptif algoritmalar kullanın.

#### Soru 2: bcrypt'te "cost" parametresi ne işe yarar?
**Cevap:** Hash hesaplama süresini belirler. Cost = 10 → 2^10 = 1024 round.
*   **Cost arttıkça:** Güvenlik artar ama performans düşer.
*   **Tavsiye:** Her yıl cost'u 1 artırın (Moore Yasası nedeniyle bilgisayarlar hızlanır).

#### Soru 3: Rainbow Table nedir? Nasıl önlenir?
**Cevap:** Önceden hesaplanmış şifre-hash eşleşmeleri tablosudur. Milyarlarca yaygın şifrenin hash'i saklanır.

**Önleme:** **Salt** kullanın. Her şifre için rastgele salt eklenirse, rainbow table işe yaramaz (her şifre farklı hash üretir).

#### Soru 4: Argon2'nin Argon2d, Argon2i, Argon2id varyantları arasındaki fark nedir?
**Cevap:**
*   **Argon2d:** Data-dependent (GPU saldırılarına en güçlü, ama side-channel saldırılarına açık).
*   **Argon2i:** Data-independent (Side-channel saldırılarına güçlü).
*   **Argon2id:** İkisinin karışımı (önerilen, dengeli).

#### Soru 5 (Tricky): Salt yeniden kullanılabilir mi?
**Cevap:** Teknik olarak evet, ama güvenlik açısından **HAYIR**.
*   Eğer iki kullanıcının şifresi aynıysa ve salt aynıysa, hash'leri de aynı olur. Bu, saldırganın "bu iki kullanıcı aynı şifreyi kullanıyor" bilgisini elde etmesini sağlar.
*   Her kullanıcı ve her şifre değişimi için **benzersiz (unique) salt** üretilmelidir.

#### Soru 6 (Tricky): bcrypt ile PBKDF2 arasındaki temel fark nedir?
**Cevap:**
*   **PBKDF2:** Sadece CPU-hard'dır (işlemci gücü ister). GPU/ASIC ile hızlandırılabilir.
*   **bcrypt:** Memory-hard'dır (bellek ister). GPU'ların sınırlı belleği olduğu için paralelleştirmek zordur. Bu yüzden bcrypt daha güvenlidir.

---
#### Soru 5: Mevcut şifreleri MD5'ten bcrypt'e nasıl migrate ederiz?
**Cevap:** Tüm kullanıcıları şifre değiştirmeye zorlayamazsınız. Kademeli geçiş yapın:

```java
public boolean verifyPassword(String password, String storedHash) {
    if (storedHash.startsWith("$2a$")) {
        // bcrypt hash
        return bcryptEncoder.matches(password, storedHash);
    } else {
        // Eski MD5 hash
        String md5Hash = md5(password);
        if (md5Hash.equals(storedHash)) {
            // Doğru şifre, bcrypt'e upgrade et
            String newHash = bcryptEncoder.encode(password);
            userRepository.updatePassword(userId, newHash);
            return true;
        }
        return false;
    }
}
```

---

### 6. Geliştirici İpuçları

*   **Asla Kendi Hash Algoritmanızı Yazmayın:** Kriptografi uzmanları bile hata yapar. Kanıtlanmış algoritmaları kullanın.

*   **Şifreleri Asla Loglamayın:** Hata mesajlarında, log dosyalarında şifreleri yazdırmayın. Hassas bilgileri maskeleyin.

*   **HTTPS Kullanın:** Şifreler network'te plain text olarak gönderilmemeli. HTTPS şart.

*   **Rate Limiting:** Login endpoint'ine rate limiting ekleyin. Brute-force saldırılarını yavaşlatır.

*   **Multi-Factor Authentication (MFA):** Şifre tek başına yeterli değildir. SMS, TOTP (Google Authenticator) gibi ikinci faktör ekleyin.

*   **Şifre Politikası:** Minimum 12 karakter, büyük/küçük harf, rakam, özel karakter zorunlu kılın. Ancak aşırı karmaşık kurallar kullanıcıları zayıf şifreler yazmaya iter (örn: "Password123!").

*   **Breach Detection:** Have I Been Pwned API'sini kullanarak kullanıcının şifresinin daha önce sızdırılmış olup olmadığını kontrol edin.

Bu konu ile **19 kapsamlı teknik mülakat konusu** tamamlandı! 🎉

---

