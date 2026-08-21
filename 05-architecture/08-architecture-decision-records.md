# Architecture Decision Records (ADR)

> **Analoji:** ADR, bir "yönetim kurulu toplantı tutanağı" gibidir. Neden bu kararı aldık, hangi alternatifleri değerlendirdik, ne olursa kararı değiştiririz — hepsi kayıt altında. 6 ay sonra "neden Kafka seçtik?" sorusuna yanıt verebilirsiniz.

---

## 1. ADR Nedir?

Mimari kararları dokümante eden, kısa ve öz kayıtlardır. Her ADR tek bir kararı kapsar.

### Neden Gerekli?

- Yeni katılan ekip üyesi "neden X yerine Y seçildi?" sorusuna cevap bulabilir
- Kararlar tekrar tartışılmaz (context kaybolmaz)
- Hangi kararların revizyona ihtiyacı olduğu görülür

---

## 2. ADR Şablonu (Michael Nygard Formatı)

```markdown
# ADR-001: Mesaj Broker Olarak Kafka Seçimi

## Durum
Kabul Edildi (2024-03-15)

## Bağlam (Context)
Mikroservisler arası asenkron iletişim için bir mesaj broker'a ihtiyacımız var.
Günlük ~5 milyon event işlenmesi bekleniyor. Event ordering kritik.

## Karar (Decision)
Apache Kafka'yı mesaj broker olarak kullanacağız.

## Değerlendirilen Alternatifler
| Kriter | Kafka | RabbitMQ | AWS SQS |
|--------|-------|----------|---------|
| Throughput | Çok Yüksek | Orta | Yüksek |
| Ordering | Partition bazlı garanti | Queue bazlı | FIFO Queue ile |
| Retention | Günlerce saklama | Tüketilince silinir | 14 gün max |
| Operasyonel Yük | Yüksek | Orta | Düşük (managed) |

## Sonuçlar (Consequences)
- ✅ Yüksek throughput ve event replay yeteneği
- ✅ Partition bazlı ordering garantisi
- ❌ Operasyonel karmaşıklık (ZooKeeper/KRaft yönetimi)
- ❌ Takımın Kafka deneyimi sınırlı, eğitim gerekecek

## Notlar
Bu karar, yıllık >100M event hacmine ulaşılırsa veya managed service
(Confluent Cloud) maliyeti kabul edilemez hale gelirse tekrar değerlendirilecektir.
```

---

## 3. ADR Dosya Yapısı

```
docs/
  adr/
    0001-use-kafka-for-messaging.md
    0002-choose-postgresql-over-mysql.md
    0003-adopt-hexagonal-architecture.md
    0004-switch-to-grpc-for-internal-apis.md
    template.md
```

---

## 4. ADR Durumları

| Durum | Anlamı |
| :--- | :--- |
| **Proposed** | Tartışılıyor |
| **Accepted** | Kabul edildi, uygulanıyor |
| **Deprecated** | Artık geçerli değil, yerine yenisi var |
| **Superseded by ADR-XXX** | Başka bir ADR ile değiştirildi |

---

## 5. Mülakat Sorusu

### Soru: Yeni bir projeye başlarken hangi mimari kararları ADR olarak dokümante edersiniz?
**Cevap:**
1. Programlama dili ve framework seçimi
2. Veritabanı seçimi (SQL vs NoSQL)
3. Mimari stil (Monolith vs Microservices)
4. Authentication stratejisi (JWT vs Session)
5. Deployment platformu (K8s vs Serverless)
6. Mesaj broker seçimi
7. API stili (REST vs gRPC vs GraphQL)

---

## 6. Geliştirici İpuçları

- **Kısa Tutun:** Bir ADR 1-2 sayfa olmalı. Roman yazmayın.
- **Immutable:** Kabul edilen ADR'ları değiştirmeyin. Yeni karar gerekiyorsa yeni ADR yazıp eskisini "Superseded" yapın.
- **adr-tools:** CLI aracı ile ADR oluşturmayı otomatize edin: `adr new "Use Kafka for messaging"`
- **Code Review'da:** Mimari değişiklik içeren PR'larda ADR referansı zorunlu kılın.
