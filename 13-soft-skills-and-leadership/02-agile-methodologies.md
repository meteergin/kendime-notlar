# Agile Metodolojileri ve Yazılım Mimarının Rolü

---

## 1. Agile vs Waterfall

| Kriter | Waterfall | Agile |
| :--- | :--- | :--- |
| **Değişiklik** | Maliyetli | Hoş karşılanır |
| **Teslimat** | Proje sonunda | Her sprint sonunda |
| **Dokümantasyon** | Kapsamlı | Yeterli düzeyde |
| **Feedback** | Geç | Sürekli |

---

## 2. Scrum Çerçevesi

### Roller

| Rol | Sorumluluk |
| :--- | :--- |
| **Product Owner** | İş öncelikleri, backlog yönetimi |
| **Scrum Master** | Süreç kolaylaştırıcı, engel kaldırıcı |
| **Development Team** | Cross-functional, self-organizing |

### Seremoniler

| Seremoni | Süre | Amaç |
| :--- | :--- | :--- |
| **Sprint Planning** | 2-4 saat | Sprint hedefi ve görev seçimi |
| **Daily Standup** | 15 dk | Günlük senkronizasyon |
| **Sprint Review** | 1-2 saat | Demo ve feedback |
| **Retrospective** | 1-2 saat | Süreç iyileştirme |

### Sprint Metrikleri

- **Velocity:** Sprint başına tamamlanan story point ortalaması
- **Burndown Chart:** Sprint içinde kalan iş miktarının zamanla azalması
- **Cycle Time:** Bir görevin "In Progress" → "Done" süresi
- **Lead Time:** Bir görevin "Backlog" → "Done" süresi

---

## 3. Kanban

| Özellik | Scrum | Kanban |
| :--- | :--- | :--- |
| **İterasyon** | Sprint (1-4 hafta) | Sürekli akış |
| **WIP Limiti** | Sprint kapasitesi | Kolon bazlı limit |
| **Roller** | PO, SM, Dev Team | Zorunlu rol yok |
| **Planlama** | Sprint bazlı | İhtiyaç bazlı |

---

## 4. Yazılım Mimarının Agile'daki Rolü

### Geleneksel vs Agile Mimar

| Geleneksel (Ivory Tower) | Agile Mimar |
| :--- | :--- |
| Başlangıçta her şeyi tasarlar | Emergent design, iteratif kararlar |
| Dokümana yazar, uygulama ekibe ait | Kod yazar, ekiple birlikte çalışır |
| Değişikliğe dirençli | Değişimi kucaklar |
| Tek seferde büyük karar | İnkremental mimari evrim |

### Mimar Ne Yapar?

1. **Enabling:** Ekibin doğru teknoloji seçimi yapmasını sağlar
2. **Guardrails:** Mimari standartlar ve kurallar belirler (ADR'lar)
3. **Technical Debt Yönetimi:** Teknik borç birikimini izler ve planlı geri ödeme yapar
4. **Cross-cutting Concerns:** Logging, security, monitoring gibi ortak altyapıları tasarlar
5. **Mentoring:** Ekip üyelerini teknik olarak geliştirir

---

## 5. Estimation Teknikleri

| Teknik | Açıklama | Ne Zaman |
| :--- | :--- | :--- |
| **Planning Poker** | Ekip üyeleri bağımsız tahmin, sonra tartışma | Sprint Planning |
| **T-Shirt Sizing** | XS, S, M, L, XL ile kaba tahmin | Backlog refinement |
| **Three-Point** | Optimist + Pessimist + Realist / 3 | Risk analizi |
| **Story Mapping** | Kullanıcı yolculuğu üzerinden kapsam belirleme | Release planlama |

---

## 6. Mülakat Soruları

### Soru 1: Sprint ortasında scope değişirse ne yaparsınız?
**Cevap:** Product Owner ile görüşüp önceliklendirme yapılır. Yeni iş eklenecekse, eşdeğer başka iş sprint'ten çıkarılır. Sprint hedefi korunur.

### Soru 2: Teknik borç nasıl yönetilir?
**Cevap:**
1. Her sprint'te %15-20 kapasite teknik borç ödemeye ayrılır
2. Teknik borç backlog'da "Tech Debt" etiketi ile izlenir
3. Büyük refactoring'ler ayrı sprint'lerde planlanır
4. ADR'larla mimari kararlar dokümante edilir

### Soru 3: Mimar olarak ekiple nasıl çalışırsınız?
**Cevap:** "Ivory Tower Architect" olmam. Kod yazarım, code review yaparım, pair programming yaparım. Kararları tek başıma almam, ekiple tartışıp ADR olarak dokümante ederim. Enabling ve mentoring odaklı çalışırım.
