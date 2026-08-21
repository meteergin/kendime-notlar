# MockServer Kullanım ve Senaryo Rehberi

Bu rehber, Docker üzerinde çalışan MockServer (Port: 1080) için temel kurulumdan karmaşık POST senaryolarına kadar tüm adımları içerir.

---

## 1. Başlangıç ve Doğrulama

Mockserver'ı dockerda ayağa kaldırmak için aşağıdaki **docker-compose.yml** dosyasını `docker compose up` komutu ile çalıştırırız.

```yml

services:
	mockServer:
		image: mockserver/mockserver:latest
		ports: 
			-1080:1080
		environment:
			MOCKSERVER_PROPERTY_FILE: /config/mockserver.properties
			MOCKSERVER_INITIALIZATION_JSON_PATH: /config/initializerJson.json
		volumes:
			-type: bind
			source: .
			target: /config

```

MockServer ayağa kalktıktan sonra aşağıdaki adresten görsel arayüze erişebilirsiniz:
* **Dashboard:** `http://localhost:1080/mockserver/dashboard`

---

## 2. İlk Adım: Hello World
En temel GET isteği beklentisini oluşturmak için:

```bash
curl -v -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "httpRequest": {
    "method": "GET",
    "path": "/hello"
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "Hello World! MockServer calisiyor."
  }
}'
```
**Test:** `http://localhost:1080/hello`

---

## 3. Temel API Senaryoları

### 3.1. JSON Yanıtı ve Header Ekleme
```bash
curl -v -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "httpRequest": {
    "method": "GET",
    "path": "/api/users"
  },
  "httpResponse": {
    "statusCode": 200,
    "headers": { "Content-Type": ["application/json; charset=utf-8"] },
    "body": [{ "id": 1, "name": "Ahmet" }, { "id": 2, "name": "Ayşe" }]
  }
}'
```

### 3.2. Hata Senaryosu (401 Unauthorized)
```bash
curl -v -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "httpRequest": { "method": "POST", "path": "/api/login" },
  "httpResponse": {
    "statusCode": 401,
    "body": { "hata": "Giriş başarısız!", "kod": "AUTH_ERROR" }
  }
}'
```

### 3.3. Gecikmeli Yanıt (Yavaşlık/Timeout Testi)
```bash
curl -v -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "httpRequest": { "path": "/api/slow-data" },
  "httpResponse": {
    "statusCode": 200,
    "body": "Bu veri 5 saniye gecikmeli geldi.",
    "delay": { "timeUnit": "SECONDS", "value": 5 }
  }
}'
```

---

## 4. POST ve Payload (Body) Eşleştirme

### 4.1. Kısmi JSON Eşleşmesi (Partial Match)
Sadece belirli alanları kontrol etmek için `ONLY_MATCHING_FIELDS` kullanılır.
```bash
curl -v -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "httpRequest": {
    "method": "POST",
    "path": "/api/update-user",
    "body": {
      "type": "JSON",
      "json": "{\"id\": 500}",
      "matchType": "ONLY_MATCHING_FIELDS"
    }
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "Kullanici 500 güncellendi."
  }
}'
```

### 4.2. Regex (Düzenli İfade) ile İçerik Kontrolü
```bash
curl -v -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "httpRequest": {
    "method": "POST",
    "path": "/api/subscribe",
    "body": { "type": "REGEX", "regex": ".*@.*\\.com" }
  },
  "httpResponse": { "statusCode": 202, "body": "Email onaylandi." }
} '
```

---

## 5. Yönetim ve Silme İşlemleri

### 5.1. Tüm Mockları Sıfırlama (Reset)
Hem beklentileri hem de logları tamamen temizler.
```bash
curl -X PUT "http://localhost:1080/mockserver/reset"
```

### 5.2. Kriter Belirterek Spesifik Silme (Clear)
Belirli bir path'e ait tüm mockları silmek için:
```bash
curl -X PUT "http://localhost:1080/mockserver/clear" -d '{ "path": "/api/users" }'
```

### 5.3. ID ile Nokta Atışı Silme
Mock oluştururken verdiğiniz benzersiz bir ID üzerinden silme işlemi:

**Oluşturma:**
```bash
curl -X PUT "http://localhost:1080/mockserver/expectation" -d '{
  "id": "ozel-id-123",
  "httpRequest": { "path": "/spesifik-api" },
  "httpResponse": { "body": "Veri" }
}'
```

**Silme:**
```bash
curl -X PUT "http://localhost:1080/mockserver/clear" -d '{ "id": "ozel-id-123" }'
```

---

## Özet Tablo

| İşlem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| **Oluştur/Güncelle** | `/expectation` | Yeni kural ekler veya var olanı günceller. |
| **Hepsini Sil** | `/reset` | Her şeyi fabrika ayarlarına döndürür. |
| **Kriterle Sil** | `/clear` | Path, Method veya ID ile eşleşenleri siler. |
| **Logları İzle** | `/dashboard` | Tarayıcı üzerinden görsel takip sağlar. |

