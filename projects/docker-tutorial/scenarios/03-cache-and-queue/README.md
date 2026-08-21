# Scenario 3: Caching & Messaging (Redis + RabbitMQ)

Bu senaryo, modern mikroservis mimarilerinde sıkça kullanılan asenkron iletişim ve önbellekleme katmanlarını içerir.

## Özellikler
1.  **Redis:** Cache ve Session yönetimi için.
2.  **RabbitMQ:** Asenkron mesajlaşma için. Management plugin açık gelir.
3.  **Restart Policy:** Servisler çökerse otomatik yeniden başlar.

## Dosyalar
*   `docker-compose.yml`: Redis ve RabbitMQ tanımları.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **RabbitMQ Management UI:** [http://localhost:15672](http://localhost:15672) (User: `guest`, Pass: `guest`)
*   **Redis:** Port 6379 üzerinden erişilebilir.
