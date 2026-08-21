# Teknik Liderlik ve Mentorluk

---

## 1. Teknik Liderlik Nedir?

Teknik lider, "en iyi kodu yazan kişi" değildir. Ekibin **toplu çıktısını** maksimize eden kişidir.

### Sorumluluklar

| Alan | Açıklama |
| :--- | :--- |
| **Mimari Vizyon** | Büyük resmi çizer, teknoloji yol haritası oluşturur |
| **Code Review** | Kalite standartlarını korur, ekibe öğretir |
| **Mentorluk** | Junior/Mid-level geliştiricilere rehberlik eder |
| **Problem Çözme** | Zor teknik problemlerde çözüm öncülük eder |
| **İletişim** | Teknik kararları iş birimine açıklar |
| **Risk Yönetimi** | Teknik riskleri erken tespit eder |

---

## 2. Yazılım Mimarı Mülakat Davranışsal Soruları

### STAR Metodu

Her davranışsal soruyu **STAR** formatında yanıtlayın:

| Harf | Açıklama | Örnek |
| :--- | :--- | :--- |
| **S** - Situation | Durum neydi? | "E-ticaret platformumuz Black Friday'de çöktü" |
| **T** - Task | Göreviniz neydi? | "Performance sorunlarını tespit etmem istendi" |
| **A** - Action | Ne yaptınız? | "JFR ile profiling yaptım, N+1 query tespit ettim" |
| **R** - Result | Sonuç ne oldu? | "Response time %80 azaldı, 10K TPS'e çıktık" |

### Sık Sorulan Sorular ve İpuçları

**"Ekibinizle teknik anlaşmazlık yaşadığınız bir durumu anlatın."**
- Saygılı bir şekilde farklı bakış açılarını dinlediğinizi gösterin
- Veri ve kanıtlarla karar verdiğinizi belirtin (benchmark, POC)
- ADR ile kararı dokümante ettiğinizi söyleyin

**"Büyük bir refactoring projesini nasıl yönettiniz?"**
- Strangler Fig Pattern ile kademeli geçiş
- Feature flag ile risk minimizasyonu
- Metrics ile iyileşmeyi ölçme

**"Bir deadline baskısı altında kalite ile hız arasında nasıl denge kurdunuz?"**
- MVP scope belirleme
- Teknik borcu bilinçli kabul edip backlog'a ekleme
- Non-negotiable kalite standartları (test coverage, security)

---

## 3. Conflict Resolution (Çatışma Çözme)

| Strateji | Ne Zaman | Örnek |
| :--- | :--- | :--- |
| **POC/Spike** | Teknik tartışmalarda | "İkisini de deneyelim, veriyle karar verelim" |
| **ADR** | Mimari kararlarda | Kararı ve gerekçesini yazılı kayıt altına alın |
| **Timeboxed Debate** | Uzayan tartışmalarda | "30 dakikamız var, sonra oylayalım" |
| **Escalation** | Çıkmazda | Engineering Manager'a taşıyın (son çare) |

---

## 4. Geliştirici İpuçları

- **1:1 Toplantılar:** Ekip üyeleriyle düzenli 1:1 yapın. Kariyer hedeflerini öğrenin, teknik gelişimlerini destekleyin.
- **Ego Bırakın:** "Ben haklıyım" yerine "Veri ne diyor?" yaklaşımını benimseyin.
- **Delegation:** Her şeyi kendiniz yapmayın. Zorlu görevleri ekibe vererek onları büyütün.
- **Yazılı İletişim:** Önemli teknik kararları Slack'te değil, ADR/Wiki'de dokümante edin.
- **Fail Safely:** Başarısızlığı öğrenme fırsatı olarak kullanın. Blameless postmortem yapın.
