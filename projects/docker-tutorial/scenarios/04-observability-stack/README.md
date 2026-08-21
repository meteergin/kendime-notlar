# Scenario 4: Observability Stack (Prometheus + Grafana + Zipkin)

Bu senaryo, uygulamanızın sağlığını, metriklerini ve trace'lerini izlemek için tam bir gözlemleme (observability) stack'i kurar.

## Özellikler
1.  **Prometheus:** Metrik toplama (Scraping).
2.  **Grafana:** Görselleştirme Dashboard'u.
3.  **Zipkin:** Distributed Tracing.
4.  **Otomatik Konfigürasyon:** Grafana datasource'ları otomatik tanımlanır, elle eklemenize gerek kalmaz.

## Dosyalar
*   `docker-compose.yml`: Stack tanımları.
*   `prometheus.yml`: Prometheus'un nereden veri toplayacağının ayarı.
*   `grafana/provisioning/datasources/datasource.yml`: Grafana'ya Prometheus ve Zipkin'i tanıtan ayar.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **Grafana:** [http://localhost:3000](http://localhost:3000) (User: `admin`, Pass: `admin`)
*   **Prometheus:** [http://localhost:9090](http://localhost:9090)
*   **Zipkin:** [http://localhost:9411](http://localhost:9411)
