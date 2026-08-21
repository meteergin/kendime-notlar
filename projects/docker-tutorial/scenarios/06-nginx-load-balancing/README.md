# Scenario 6: High Availability (Nginx Load Balancer)

Bu senaryo, uygulamanızın birden fazla kopyasını (replica) çalıştırıp, trafiği Nginx üzerinden dağıtarak (Load Balancing) yüksek erişilebilirlik (High Availability) ve ölçeklenebilirlik sağlar.

## Özellikler
1.  **Scaling:** `docker-compose up -d --scale app=3` komutu ile uygulamanın 3 kopyası çalışır.
2.  **Nginx:** Reverse Proxy ve Load Balancer olarak çalışır.
3.  **Round Robin:** İstekleri sırayla (1-2-3-1-2-3) dağıtır.

## Dosyalar
*   `docker-compose.yml`: Servis tanımları.
*   `nginx.conf`: Nginx load balancing ayarı.

## Nasıl Çalıştırılır?

```bash
# 3 kopya ile başlat
docker-compose up -d --scale app=3
```

*   **Erişim:** [http://localhost:80](http://localhost:80) (Nginx üzerinden)
*   Her yenilemede farklı bir container ID'si (loglardan) görebilirsiniz.
