# Scenario 2: Database Persistence & Migration

Bu senaryo, stateful (durum tutan) bir veritabanı servisini Docker Compose ile nasıl yöneteceğinizi gösterir.

## Özellikler
1.  **Data Persistence:** `volumes` kullanarak verilerin konteyner silinse bile kaybolmamasını sağlar.
2.  **Initialization:** `init.sql` ile veritabanı ilk açıldığında tablo ve veri oluşturur.
3.  **Healthcheck:** Uygulamanın, veritabanı tamamen hazır olmadan başlamasını engeller.
4.  **Environment Variables:** Şifre ve kullanıcı adlarını dışarıdan yönetir.

## Dosyalar
*   `docker-compose.yml`: Servis tanımları.
*   `init.sql`: Başlangıç SQL scripti.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

Veritabanına bağlanmak için:
```bash
docker-compose exec db psql -U user -d mydb
```
