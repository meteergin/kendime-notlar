# Scenario 7: Modern Edge Router (Traefik)

Traefik, modern mikroservis mimarileri için tasarlanmış, "Cloud Native" bir Reverse Proxy ve Load Balancer'dır. Nginx'ten en büyük farkı, konfigürasyon dosyası düzenlemeye gerek kalmadan, Docker etiketleri (labels) üzerinden servisleri otomatik keşfetmesidir.

## Özellikler
1.  **Auto Discovery:** Yeni bir servis eklediğinizde Traefik bunu otomatik algılar.
2.  **Dashboard:** Tüm rotaları ve servisleri görebileceğiniz bir arayüz sunar.
3.  **Labels:** Servislerinizi `docker-compose.yml` içinde etiketleyerek yönlendirme kurallarını belirlersiniz.

## Dosyalar
*   `docker-compose.yml`: Traefik ve örnek bir uygulamanın tanımı.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **Traefik Dashboard:** [http://localhost:8080](http://localhost:8080)
*   **Örnek Uygulama (Whoami):** [http://whoami.localhost](http://whoami.localhost) (Domain'i `/etc/hosts` dosyanıza eklemeniz gerekebilir veya doğrudan localhost üzerinden test edebilirsiniz).
    *   *Not:* Tarayıcınızda `whoami.localhost` adresine gidin. Bu adres 127.0.0.1'e çözümlenir (çoğu modern OS'de).
