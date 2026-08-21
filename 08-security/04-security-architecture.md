# Security Architecture (Zero Trust, mTLS, Secret Management)

> **Analoji:** Eski güvenlik modeli bir "kale ve hendek" idi — dışarıdan gelen tehdit, içeriden herkes güvenli. Zero Trust ise "havaalanı güvenliği"dir — herkes, her yerde, her seferinde kimlik kontrolünden geçer.

---

## 1. Zero Trust Architecture

### Prensipleri

1. **Never Trust, Always Verify:** İç ağdaki trafik de doğrulanır
2. **Least Privilege:** Minimum gerekli yetki verilir
3. **Assume Breach:** Saldırı olacağını varsay, hasarı minimize et
4. **Micro-segmentation:** Ağı küçük parçalara böl, lateral movement'ı engelle

### Uygulama

| Katman | Gerekenler |
| :--- | :--- |
| **Network** | Service mesh (Istio), mTLS, network policies |
| **Identity** | OAuth2/OIDC, MFA, short-lived tokens |
| **Device** | Device trust, certificate-based auth |
| **Application** | Input validation, WAF, CORS |
| **Data** | Encryption at rest/transit, DLP, masking |

---

## 2. mTLS (Mutual TLS)

Sadece sunucu değil, **istemci de** sertifika ile doğrulanır. Mikroservisler arası iletişimde kullanılır.

```
Normal TLS:
  Client → "Sen kimsin?" → Server (sertifika gösterir)
  
mTLS:
  Client → "Sen kimsin?" → Server (sertifika gösterir)
  Server → "Sen kimsin?" → Client (sertifika gösterir)
```

**Uygulama:** Istio/Linkerd service mesh otomatik mTLS sağlar. Uygulama kodu değişmez.

---

## 3. Secret Management

| Araç | Kullanım |
| :--- | :--- |
| **HashiCorp Vault** | Dynamic secrets, encryption as a service, PKI |
| **AWS Secrets Manager** | AWS servisleri ile entegre, otomatik rotation |
| **K8s Secrets** | Cluster içi basit secret yönetimi (base64, şifrelenmemiş!) |
| **Spring Cloud Config + Vault** | Merkezi config + secret yönetimi |

```java
// Spring Boot + Vault entegrasyonu
@Value("${db.password}")  // Vault'tan otomatik çekilir
private String dbPassword;
```

---

## 4. Kritik Mülakat Soruları

### Soru 1: Zero Trust mimaride servisler arası auth nasıl sağlanır?
**Cevap:** mTLS (service identity) + JWT (user identity). Her servis kendi sertifikasına sahip. Service mesh (Istio) sertifika yönetimini ve rotasyonunu otomatikleştirir.

### Soru 2: Kubernetes Secret'ları neden güvenli değildir?
**Cevap:** Base64 encoded, şifrelenmemiş. `etcd`'ye erişimi olan herkes okuyabilir. **Çözüm:** etcd encryption at rest, External Secrets Operator, Vault.

### Soru 3 (Tricky): Secret rotation nedir?
**Cevap:** Şifrelerin, API key'lerin periyodik olarak değiştirilmesidir. Vault dynamic secrets ile DB şifreleri her lease süresi dolduğunda otomatik yenilenir. Uygulama farkında bile olmaz.

---

## 5. Geliştirici İpuçları

- **.env dosyası commit etmeyin.** `.gitignore`'a ekleyin. Yerine `.env.example` koyun.
- **Encryption at Rest:** DB, S3, disk — her yerde veri şifrelenmiş saklanmalı.
- **Audit Logging:** Tüm auth olaylarını (login, failed login, privilege escalation) loglayın.
- **Dependency Scanning:** `snyk`, `trivy`, `OWASP Dependency-Check` ile kütüphane güvenlik açıklarını tarayın.
