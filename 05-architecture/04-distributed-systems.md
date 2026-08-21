# Dağıtık Sistemler (CAP Teoremi, Consensus, Consistency Modelleri)

> **Analoji:** Dağıtık sistem, bir "zincirleme restoran" gibidir. Her şubede (node) aynı menü ve aynı kalite olmalı. Ama bir şube kapanırsa (partition), diğerleri çalışmaya devam etmeli. Merkez ofisten (coordinator) tüm şubelere eş zamanlı haber vermek bazen imkansızdır — işte burada trade-off'lar başlar.

---

## 1. CAP Teoremi

> Eric Brewer, 2000: Bir dağıtık sistem aynı anda **üçünü birden** garanti edemez. İkisini seçmek zorundasınız.

### 3 Özellik

| Özellik | Açıklama | Analoji |
| :--- | :--- | :--- |
| **C - Consistency** | Her okuma en son yazılan veriyi görür | Tüm şubelerde aynı menü |
| **A - Availability** | Her istek cevap alır (hata olsa bile) | Her şube her zaman açık |
| **P - Partition Tolerance** | Ağ bölünmesinde sistem çalışmaya devam eder | İki şube arasında iletişim kopsa bile çalışır |

### Neden 3'ü Birden Olamaz?

Ağ bölünmesi (P) **kaçınılmazdır** (kablolar kopar, switch'ler çöker). Bu yüzden gerçek dünyada seçim **CP** veya **AP** arasındadır:

| Seçim | Davranış | Örnek Sistemler |
| :--- | :--- | :--- |
| **CP** | Tutarlılık garantiler, bölünme sırasında bazı istekleri reddeder | ZooKeeper, etcd, HBase, MongoDB (default) |
| **AP** | Her zaman cevap verir, tutarlılık geçici olarak bozulabilir | Cassandra, DynamoDB, CouchDB |
| **CA** | Pratikte imkansız (network partition olmayan sistem yok) | Tek node RDBMS (teorik) |

### PACELC Teoremi (CAP'in Genişletilmişi)

CAP sadece partition durumunu ele alır. Normal durumda da trade-off vardır:

```
if (Partition) {
    choose: Availability vs Consistency  // PA veya PC
} else {
    choose: Latency vs Consistency       // EL veya EC
}
```

| Sistem | Partition durumunda | Normal durumda |
| :--- | :--- | :--- |
| **DynamoDB** | PA (Availability) | EL (Latency) |
| **MongoDB** | PC (Consistency) | EC (Consistency) |
| **Cassandra** | PA (Availability) | EL (Latency) |

---

## 2. Consistency Modelleri

### Strong Consistency (Güçlü Tutarlılık)

Yazılan veri anında tüm node'larda görünür. ACID veritabanları bunu sağlar.

```
Client A → write(x=5) → Node 1
Client B → read(x) → Node 2 → x=5 ✅ (Hemen görür)
```

**Maliyet:** Yüksek latency. Her write tüm replica'ları bekler.

### Eventual Consistency (Nihayetinde Tutarlılık)

Veri bir süre sonra tüm node'larda tutarlı olur. Ne kadar sürede? Garanti yok ama genellikle milisaniyeler.

```
Client A → write(x=5) → Node 1
Client B → read(x) → Node 2 → x=3 ❌ (Henüz güncellenmedi)
... 50ms sonra ...
Client B → read(x) → Node 2 → x=5 ✅ (Sonunda tutarlı)
```

**Kullanım:** Social media feed'leri, like sayıları, sepet güncellemeleri.

### Causal Consistency

Neden-sonuç ilişkisi olan olaylar sıralı görünür, ilişkisiz olaylar herhangi bir sırada olabilir.

```
Client A: "Ankara'ya taşındım" → beğen: 5
Client B: "Tebrikler!" (A'nın mesajından SONRA görünmeli)
Client C: Bağımsız bir yorum (herhangi bir sırada olabilir)
```

### Read-Your-Writes Consistency

Yazdığınız veriyi hemen kendiniz okuyabilirsiniz. Başkaları bir süre sonra görür.

```
Client A → write(x=5) → read(x) → x=5 ✅ (Kendi yazdığını görür)
Client B → read(x) → x=3 ❓ (Henüz görmeyebilir)
```

---

## 3. Consensus Algoritmaları

### Raft

> **Analoji:** Bir sınıfta öğretmen seçimi. Adaylar oy ister, çoğunluk kazanır. Seçilen "lider" tüm kararları alır ve öğrencilere (follower'lara) bildirir.

**Roller:**
- **Leader:** Tüm write'ları kabul eder, follower'lara dağıtır
- **Follower:** Leader'dan gelen komutları uygular
- **Candidate:** Leader çökünce seçim başlatır

**Çalışma Adımları:**
1. Leader seçilir (election timeout, rastgele)
2. Client write isteği leader'a gelir
3. Leader, follower'lara "log entry" gönderir
4. Çoğunluk (majority) onaylarsa commit edilir
5. Leader çökerse, follower'lar yeni seçim başlatır

### Paxos

Daha eski ve daha karmaşık bir consensus algoritması. Google'ın Chubby ve Spanner sistemlerinde kullanılır. Raft, Paxos'un anlaşılır bir versiyonudur.

---

## 4. Dağıtık Sistemlerde Yaygın Problemler

### Split Brain

İki node kendini leader sanır:

```
   ┌── Node A (Leader olduğunu sanıyor) ──┐
───┤         NETWORK PARTITION             ├───
   └── Node B (Leader olduğunu sanıyor) ──┘
```

**Çözüm:** Quorum-based karar. Çoğunluk (N/2 + 1) olmadan leader olunamaz.

### Clock Skew

Farklı makinelerin saatleri farklı olabilir. Olayların sırası karışır.

**Çözümler:**
- **NTP:** Saatleri senkronize eder (ms hassasiyetinde)
- **Logical Clocks (Lamport):** Fiziksel saat yerine mantıksal sıralama
- **Vector Clocks:** Her node kendi sayacını tutar, nedensellik (causality) izlenir
- **Hybrid Logical Clock (HLC):** Fiziksel + mantıksal saat birleşimi (CockroachDB)

### Distributed Transactions

Birden fazla serviste atomik işlem yapma:

| Pattern | Açıklama | Avantaj | Dezavantaj |
| :--- | :--- | :--- | :--- |
| **2PC (Two-Phase Commit)** | Coordinator tüm katılımcılardan onay alır | Strong consistency | Blocking, coordinator SPOF |
| **Saga Pattern** | Her adım bağımsız, hata durumunda compensating transaction | Non-blocking, resilient | Eventual consistency |
| **TCC (Try-Confirm-Cancel)** | Kaynağı rezerve et, onayla veya iptal et | Esnek | Uygulama karmaşıklığı |

---

## 5. Kritik Mülakat Soruları

### Soru 1: CAP teoreminde neden Partition Tolerance vazgeçilmezdir?
**Cevap:** Dağıtık sistemlerde ağ hatası **kaçınılmazdır**. Kablolar kesilir, switch'ler çöker, datacenter'lar arası bağlantı kopar. P'yi bırakmak = tek makineye düşmek = dağıtık sistem olmamak demektir.

### Soru 2: Bir e-ticaret sepeti hangi consistency modeli kullanmalı?
**Cevap:** **Eventual Consistency** yeterlidir. Sepete eklenen ürün 100ms sonra görünse sorun olmaz. Ama **sipariş onayı** ve **ödeme** için **Strong Consistency** gerekir. Hibrit yaklaşım en doğrusudur.

### Soru 3: Leader-Follower replication'da leader çökerse ne olur?
**Cevap:**
1. Follower'lar heartbeat alamadığını fark eder
2. Election timeout sonrası yeni bir leader seçimi başlar (Raft)
3. En güncel log'a sahip follower leader olur
4. **Sorun:** Commit edilmemiş write'lar kaybolabilir → bu yüzden majority acknowledgment gerekir

### Soru 4 (Tricky): Eventual consistency'de "stale read" nasıl azaltılır?
**Cevap:**
1. **Read-your-writes:** Kullanıcıyı aynı replica'ya yönlendirin (sticky session)
2. **Quorum read:** R + W > N formülü (Read replica + Write replica > Toplam replica)
3. **Monotonic reads:** Bir client hep aynı veya daha güncel veriyi görsün

### Soru 5 (Tricky): 2PC neden mikroservislerde önerilmez?
**Cevap:**
1. **Blocking:** Coordinator kilitlenirse tüm katılımcılar askıda kalır
2. **Latency:** Tüm katılımcıların cevap vermesi beklenir
3. **Heterogeneous:** Farklı veritabanları/servisler 2PC desteklemeyebilir
4. **Çözüm:** Saga pattern (choreography veya orchestration)

---

## 6. Geliştirici İpuçları

- **Idempotency Key:** Distributed sistemlerde her isteğe unique key ekleyin. Retry durumunda aynı işlem tekrarlanmaz.
- **Circuit Breaker:** Bir downstream servis çökünce, cascade failure önlemek için circuit breaker kullanın (Resilience4j).
- **Observability:** Distributed tracing (Jaeger, Zipkin) olmadan dağıtık sistemlerde debugging imkansızdır. Correlation ID her yerde taşınmalı.
- **Graceful Degradation:** Bir servis çökünce tüm sistem çökmemeli. Cache'ten eski veri sunun, default değerler döndürün.
- **Bulkhead Pattern:** Farklı servislere farklı thread pool'lar atayın. Bir servisin yavaşlaması diğerlerini etkilemesin.
