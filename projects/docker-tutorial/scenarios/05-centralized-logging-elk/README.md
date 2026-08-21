# Scenario 5: Centralized Logging (ELK Stack + Filebeat)

Bu senaryo, uygulama loglarını merkezi bir yerde toplamak ve analiz etmek için ELK (Elasticsearch, Logstash, Kibana) stack'ini kullanır. Burada Logstash yerine daha hafif olan **Filebeat** kullanılmıştır.

## Özellikler
1.  **Filebeat:** Log dosyalarını okur ve Elasticsearch'e gönderir.
2.  **Elasticsearch:** Logları saklar ve indeksler.
3.  **Kibana:** Logları görselleştirir ve arama yapmanızı sağlar.
4.  **Volume Sharing:** Uygulama loglarını bir volume'e yazar, Filebeat aynı volume'den okur.

## Dosyalar
*   `docker-compose.yml`: Servis tanımları.
*   `filebeat.yml`: Filebeat konfigürasyonu.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **Kibana:** [http://localhost:5601](http://localhost:5601)
*   **Log Kontrolü:** Uygulama `/var/log/app/application.log` dosyasına log yazmalıdır (Logback ayarı gerektirir).
