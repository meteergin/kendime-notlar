## Konu 40: Soft Skills, Leadership & Behavioral Interview (STAR Method)

Teknik bilgi sizi kapıdan içeri sokar, ancak soft skilleriniz içeride kalmanızı ve yükselmenizi sağlar. Senior/Lead roller için mülakatın %50'si davranışsaldır.

---

### 1. STAR Metodu

Davranışsal sorulara (Behavioral Questions) cevap verirken bu formülü kullanın:

*   **S - Situation (Durum):** Bağlamı anlatın. (Hangi proje? Sorun neydi?).
*   **T - Task (Görev):** Sizin sorumluluğunuz neydi?
*   **A - Action (Eylem):** Ne yaptınız? (En önemli kısım. "Biz" değil "Ben" dili kullanın).
*   **R - Result (Sonuç):** Ne elde ettiniz? (Sayısal veri verin: "%30 hızlandı", "Maliyet %10 düştü").

---

### 2. Kritik Senaryolar ve Örnek Cevaplar

#### Soru 1: "Takım arkadaşınla teknik bir konuda anlaşamadığın bir anı anlat." (Conflict Resolution)
*   **Situation:** Mikroservis projesinde arkadaşım REST kullanmak istedi, ben gRPC önerdim.
*   **Task:** En performanslı ve geleceğe dönük yapıyı seçmemiz gerekiyordu.
*   **Action:** Onun argümanlarını dinledim (REST daha kolay, herkes biliyor). Kendi argümanlarımı (gRPC type-safe, daha hızlı) POC (Proof of Concept) yaparak kanıtladım. İki protokolün benchmark sonuçlarını takıma sundum.
*   **Result:** gRPC'nin %40 daha hızlı olduğunu gördük ve gRPC'de karar kıldık. Arkadaşım da ikna oldu ve ona gRPC öğrenmesi için kaynak sağladım.

#### Soru 2: "Başarısız olduğun bir projeyi anlat." (Handling Failure)
*   **Situation:** E-ticaret projesinde Black Friday için cache stratejisini yanlış kurguladım.
*   **Task:** Sistemin çökmesini engellemek.
*   **Action:** Yük altında Redis cluster yetersiz kaldı. Hızlıca "Circuit Breaker" açarak cache'i devre dışı bıraktım ve DB'ye yükü kontrollü verdim. Sorunu çözdükten sonra Post-Mortem analizi yaptım.
*   **Result:** 10 dakika kesinti oldu ama kök nedeni bulup düzelttik. Bir daha aynı hatayı yapmamak için yük testlerini (Load Testing) sürecimize ekledim.

#### Soru 3: "Junior bir geliştiriciye nasıl mentorluk yaparsın?" (Leadership)
*   **Cevap:** Ona balık vermem, balık tutmayı öğretirim. Sorunla geldiğinde "Cevap bu" demek yerine, "Nasıl düşündün? Neleri denedin?" diye sorarak doğru yola yönlendiririm. Hata yapmasına (kontrollü ortamda) izin veririm çünkü en iyi öğrenme hatadan gelir. Kod incelemelerinde (Code Review) sadece hataları değil, iyi yaptığı şeyleri de söylerim.

---

### 3. System Design Mülakatlarında İletişim

*   **Varsayım Yapma, Soru Sor:** "Twitter tasarla" dendiğinde hemen çizmeye başlama. "Kaç kullanıcı?", "Okuma/Yazma oranı ne?", "Video olacak mı?" diye sor.
*   **Trade-off Konuş:** "Burada SQL kullandım ama NoSQL kullansaydık şu avantajı olurdu, bu dezavantajı olurdu" diyerek bildiğini göster.
*   **Dinle:** Mülakatçı seni yönlendiriyorsa (ipucu veriyorsa), direnme. İşbirliğine açık ol.

---

