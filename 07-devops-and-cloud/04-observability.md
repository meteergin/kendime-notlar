## Konu 38: Observability & DevOps (Metrics, Logs, Tracing, CI/CD)

Dağıtık sistemlerde "bir şeyler ters gittiğinde" sorunu bulmak zordur. Observability (Gözlemlenebilirlik), sistemin iç durumunu dışarıdan gelen verilerle (Logs, Metrics, Traces) anlayabilme yeteneğidir.

---

### 1. The 3 Pillars of Observability

#### 1. Logs (Ne oldu?)
*   Olayların zaman damgalı kaydıdır.
*   **Structured Logging:** Logları metin yerine JSON formatında yazın. (`{"level": "ERROR", "userId": 123, "msg": "Payment failed"}`).
*   **Centralized Logging (ELK/EFK Stack):**
    *   **E**lasticsearch: Arama ve saklama.
    *   **L**ogstash / **F**luentd: Toplama ve işleme.
    *   **K**ibana: Görselleştirme.

#### 2. Metrics (Durum ne?)
*   Sayısal verilerdir (CPU kullanımı, Request/sec, Error rate). Zaman serisi (Time-series) olarak saklanır.
*   **Prometheus:** Pull-based model kullanır. Uygulamanız `/metrics` endpoint'i açar, Prometheus gelip okur.
*   **Grafana:** Metrikleri görselleştirmek için dashboard aracı.
*   **RED Method:**
    *   **R**ate (İstek sayısı)
    *   **E**rrors (Hata sayısı)
    *   **D**uration (Cevap süresi)

#### 3. Tracing (Nerede oldu?)
*   Bir isteğin mikroservisler arasındaki yolculuğunu takip eder.
*   **Distributed Tracing (Zipkin, Jaeger, OpenTelemetry):**
    *   **Trace ID:** İsteğin tamamına verilen ID.
    *   **Span ID:** Her servisteki işlem parçasına verilen ID.
*   *Örnek:* Frontend -> Gateway -> Order Service -> Payment Service. Hangi adımda yavaşlık var? Tracing bunu gösterir.

---

### 2. CI/CD (Continuous Integration / Continuous Deployment)

*   **CI (Sürekli Entegrasyon):** Kodun sık sık ana branch'e merge edilmesi ve otomatik testlerin koşulması. (Jenkins, GitHub Actions, GitLab CI).
*   **CD (Sürekli Dağıtım):** Testten geçen kodun otomatik olarak production'a alınması.

#### Deployment Stratejileri
1.  **Rolling Update (K8s Default):** Pod'ları sırayla yeniler. (3 pod varsa: 1.yi kapat, yenisini aç -> 2.yi kapat...). Downtime yok.
2.  **Blue-Green Deployment:** Yeni versiyon (Green) tamamen ayrı bir ortamda ayağa kalkar. Test edilir. Load Balancer trafiği Blue'dan Green'e çevirir. Anında rollback imkanı vardır ama 2 kat kaynak gerekir.
3.  **Canary Deployment:** Trafiğin küçük bir kısmı (%5) yeni versiyona yönlendirilir. Hata yoksa yavaş yavaş artırılır (%10, %50, %100).

#### Kritik Mülakat Soruları

**Soru 1: High Cardinality nedir? Neden Prometheus'ta sorundur?**
*   **Cevap:** Bir metriğin çok fazla benzersiz etikete (label) sahip olmasıdır.
*   *Örnek:* `http_requests_total{user_id="12345"}`. Eğer 1 milyon kullanıcı varsa, 1 milyon farklı time-series oluşur. Prometheus belleğini şişirir ve çökertir.
*   *Çözüm:* User ID gibi sınırsız değerleri metrik etiketi yapmayın. Loglarda veya Trace'lerde tutun.

**Soru 2: Immutable Infrastructure nedir?**
*   **Cevap:** Sunucuları/Container'ları güncellerken "yama yapmak" (SSH ile girip `apt-get update` demek) yerine, yenisini oluşturup eskisini silmektir.
*   *Avantaj:* Configuration drift (zamanla sunucuların birbirinden farklılaşması) önlenir. Rollback kolaydır.

**Soru 3: Tracing'de "Sampling" neden gereklidir?**
*   **Cevap:** Her isteğin trace datasını saklamak çok maliyetlidir (Storage ve Network).
*   *Çözüm:* İsteklerin sadece %1'ini veya %0.1'ini örnekleyerek (sampling) saklarız. Hata alan istekleri %100 saklamak (Tail-based sampling) daha akıllıcadır.

---

