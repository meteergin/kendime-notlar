# Scenario 10: Identity Management (Keycloak)

Mikroservis mimarilerinde kimlik doğrulama (Authentication) ve yetkilendirme (Authorization) karmaşık bir iştir. **Keycloak**, açık kaynaklı bir IAM (Identity and Access Management) çözümüdür.

## Özellikler
1.  **OAuth2 & OIDC:** Standart protokolleri destekler.
2.  **PostgreSQL Backend:** Kullanıcı verilerini kalıcı olarak PostgreSQL'de saklar.
3.  **Admin Console:** Kullanıcıları, rolleri ve client'ları yönetmek için arayüz.

## Dosyalar
*   `docker-compose.yml`: Keycloak ve PostgreSQL tanımı.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **Keycloak Admin Console:** [http://localhost:8080](http://localhost:8080)
    *   **User:** `admin`
    *   **Pass:** `admin`
