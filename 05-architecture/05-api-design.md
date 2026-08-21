# API Tasarımı (REST, GraphQL, gRPC, API Versioning)

> **Analoji:** API, bir "restoran menüsü" gibidir. Müşteri (client) menüde ne varsa onu görebilir ve sipariş verebilir. İyi bir menü sade, anlaşılır ve tutarlıdır. Kötü bir menü karmakarışıktır ve müşteriyi şaşırtır.

---

## 1. REST API Tasarım İlkeleri

### Richardson Maturity Model

REST API'lerin olgunluk seviyesini ölçen model:

| Seviye | Açıklama | Örnek |
| :--- | :--- | :--- |
| **Level 0** | Tek endpoint, tek HTTP metodu | `POST /api` (her şey burada) |
| **Level 1** | Kaynak bazlı URI'lar | `/users`, `/orders` |
| **Level 2** | HTTP Metodları doğru kullanılır | `GET /users`, `POST /users`, `DELETE /users/1` |
| **Level 3** | HATEOAS (Hypermedia) | Response'ta link'ler içerir |

### URL Tasarım Kuralları

```
✅ DOĞRU:
GET    /api/v1/users              → Tüm kullanıcıları listele
GET    /api/v1/users/42           → ID=42 kullanıcıyı getir
POST   /api/v1/users              → Yeni kullanıcı oluştur
PUT    /api/v1/users/42           → Kullanıcıyı tamamen güncelle
PATCH  /api/v1/users/42           → Kısmi güncelleme
DELETE /api/v1/users/42           → Kullanıcıyı sil
GET    /api/v1/users/42/orders    → Kullanıcının siparişleri (nested resource)

❌ YANLIŞ:
GET    /api/getUser               → Fiil kullanmayın
POST   /api/deleteUser            → HTTP metodu zaten sil diyor
GET    /api/v1/Users              → Küçük harf kullanın
GET    /api/v1/user_list          → Çoğul isim kullanın (users)
```

### HTTP Status Code Stratejisi

| Kod | Anlamı | Ne Zaman |
| :--- | :--- | :--- |
| `200 OK` | Başarılı | GET, PUT, PATCH |
| `201 Created` | Oluşturuldu | POST (Location header ekleyin) |
| `204 No Content` | İçerik yok | DELETE |
| `400 Bad Request` | İstek hatalı | Validation hatası |
| `401 Unauthorized` | Kimlik doğrulanmadı | Token yok/geçersiz |
| `403 Forbidden` | Yetkisiz | Rolü yeterli değil |
| `404 Not Found` | Bulunamadı | Kaynak yok |
| `409 Conflict` | Çakışma | Duplicate, concurrent update |
| `422 Unprocessable Entity` | İşlenemez | İş kuralı ihlali |
| `429 Too Many Requests` | Rate limit aşıldı | Throttling |
| `500 Internal Server Error` | Sunucu hatası | Beklenmeyen hata |

### Pagination, Filtering, Sorting

```
GET /api/v1/users?page=0&size=20&sort=name,asc&status=ACTIVE&search=mete
```

**Response Format (Spring Page):**
```json
{
  "content": [ ... ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 142,
    "totalPages": 8
  },
  "_links": {
    "self": "/api/v1/users?page=0",
    "next": "/api/v1/users?page=1",
    "last": "/api/v1/users?page=7"
  }
}
```

---

## 2. GraphQL

> **Analoji:** REST "fix menü" ise, GraphQL "açık büfe"dir. REST'te sunucu ne verirse onu alırsınız (over-fetching/under-fetching). GraphQL'de ihtiyacınız olan alanları siz seçersiniz.

### Over-fetching ve Under-fetching Problemi

```
REST:
  GET /users/42           → { id, name, email, address, phone, ... } (Çok fazla veri)
  GET /users/42/orders    → İkinci istek gerekli (Az veri, N+1)

GraphQL:
  query {
    user(id: 42) {
      name
      email
      orders {
        id
        total
      }
    }
  }
  → Tek istekte, sadece istenen alanlar döner
```

### Schema Definition

```graphql
type User {
  id: ID!
  name: String!
  email: String!
  orders: [Order!]!
}

type Order {
  id: ID!
  total: Float!
  status: OrderStatus!
  createdAt: DateTime!
}

enum OrderStatus {
  PENDING
  CONFIRMED
  SHIPPED
  DELIVERED
}

type Query {
  user(id: ID!): User
  users(page: Int, size: Int): [User!]!
}

type Mutation {
  createUser(input: CreateUserInput!): User!
  updateUser(id: ID!, input: UpdateUserInput!): User!
}
```

### Ne Zaman REST, Ne Zaman GraphQL?

| Kriter | REST | GraphQL |
| :--- | :--- | :--- |
| **Basit CRUD** | ✅ İdeal | ❌ Overkill |
| **Farklı client'lar** (Mobile/Web/TV) | ❌ Her client farklı veri ister | ✅ Client kendi sorgusunu yazar |
| **Real-time** | WebSocket/SSE ayrı | ✅ Subscriptions built-in |
| **Caching** | ✅ HTTP caching kolay | ❌ POST-based, caching zor |
| **File upload** | ✅ Multipart kolay | ❌ Ek konfigürasyon gerekir |
| **Microservices arası** | ✅ Basit, hızlı | ❌ Gereksiz overhead |

---

## 3. gRPC (Google Remote Procedure Call)

> **Analoji:** REST "mektup yazışması" ise (text-based, HTTP), gRPC "telefon görüşmesi"dir (binary, HTTP/2). Daha hızlı, daha verimli ama her iki tarafın da "aynı dili" (Protobuf) konuşması gerekir.

### Protobuf ile Tip Güvenli İletişim

```protobuf
syntax = "proto3";

package com.example;

service UserService {
  rpc GetUser (GetUserRequest) returns (UserResponse);
  rpc ListUsers (ListUsersRequest) returns (stream UserResponse); // Server streaming
  rpc CreateUser (CreateUserRequest) returns (UserResponse);
}

message GetUserRequest {
  string id = 1;
}

message UserResponse {
  string id = 1;
  string name = 2;
  string email = 3;
}
```

### gRPC İletişim Türleri

| Tür | Açıklama | Kullanım |
| :--- | :--- | :--- |
| **Unary** | 1 request → 1 response | Normal API çağrısı |
| **Server Streaming** | 1 request → N response | Gerçek zamanlı veri akışı |
| **Client Streaming** | N request → 1 response | Dosya upload, log toplama |
| **Bidirectional Streaming** | N request ↔ N response | Chat, gaming |

### REST vs gRPC vs GraphQL

| Özellik | REST | gRPC | GraphQL |
| :--- | :--- | :--- | :--- |
| **Protokol** | HTTP/1.1 (text) | HTTP/2 (binary) | HTTP (text) |
| **Performans** | Orta | Çok yüksek (~10x REST) | Orta |
| **Browser desteği** | ✅ Native | ❌ Proxy gerekir | ✅ Native |
| **Schema** | OpenAPI (opsiyonel) | Protobuf (zorunlu) | SDL (zorunlu) |
| **Best for** | Public API, CRUD | Mikroservisler arası | Mobile/Frontend |

---

## 4. API Versioning Stratejileri

| Strateji | Örnek | Avantaj | Dezavantaj |
| :--- | :--- | :--- | :--- |
| **URI Path** | `/api/v1/users` | Açık, anlaşılır | URL kirlenmesi |
| **Header** | `X-API-Version: 2` | Temiz URL | Keşfedilebilirlik zayıf |
| **Query Parameter** | `/api/users?version=2` | Kolay geçiş | Caching zorluğu |
| **Content Negotiation** | `Accept: application/vnd.app.v2+json` | RESTful, standart | Karmaşık |

**Tavsiye:** URI Path (`/v1/`) en yaygın ve en anlaşılır yöntemdir. Büyük ekiplerde tercih edin.

### Deprecation Stratejisi

```java
@Deprecated(since = "2024-01-01", forRemoval = true)
@GetMapping("/api/v1/users")
public List<UserV1DTO> getUsersV1() {
    // Sunset header ekle
    return ResponseEntity.ok()
        .header("Sunset", "Sat, 01 Jun 2025 00:00:00 GMT")
        .header("Deprecation", "true")
        .body(userService.getUsersV1());
}
```

---

## 5. Kritik Mülakat Soruları

### Soru 1: PUT vs PATCH farkı nedir?
**Cevap:**
- **PUT:** Kaynağı tamamen değiştirir (full replacement). Gönderilmeyen alanlar null olur.
- **PATCH:** Sadece gönderilen alanları günceller (partial update).
- **Trap:** PUT ile sadece bir alan güncellemek, diğer alanları silmek anlamına gelir!

### Soru 2: HATEOAS nedir?
**Cevap:** Response içinde client'ın yapabileceği sonraki adımların linklerini eklemektir:
```json
{
  "id": 42,
  "name": "Mete",
  "_links": {
    "self": { "href": "/users/42" },
    "orders": { "href": "/users/42/orders" },
    "delete": { "href": "/users/42", "method": "DELETE" }
  }
}
```
Client hardcoded URL bilmek zorunda kalmaz. API keşfedilebilir olur.

### Soru 3: GraphQL'de N+1 problemi nasıl çözülür?
**Cevap:** **DataLoader** pattern kullanılır. Her resolver ayrı DB sorgusu yapmak yerine, DataLoader istekleri toplar (batch) ve tek bir sorgu ile çözer.

### Soru 4 (Tricky): Idempotent HTTP metodları hangileridir?
**Cevap:** `GET`, `PUT`, `DELETE`, `HEAD`, `OPTIONS` idempotent'tir. Aynı isteği 10 kez gönderin, sonuç değişmez. `POST` ve `PATCH` **idempotent değildir**.
- **Trap:** `DELETE /users/42` ilk seferinde siler, ikincisinde 404 döner ama yan etki yoktur → idempotent sayılır.

### Soru 5 (Tricky): gRPC neden mikroservisler arası iletişimde REST'ten daha iyi?
**Cevap:**
1. **Binary format (Protobuf):** JSON'dan ~10x küçük payload
2. **HTTP/2:** Multiplexing, header compression, server push
3. **Type-safe:** Compile-time contract doğrulama
4. **Streaming:** Bidirectional stream desteği
5. **Code generation:** Client/Server kodu otomatik üretilir

---

## 6. Geliştirici İpuçları

- **OpenAPI/Swagger:** REST API'lerinizi `springdoc-openapi` ile belgeleyin. `/swagger-ui.html` otomatik oluşur.
- **API Gateway:** Tüm API'leri tek bir giriş noktasından sunun (Spring Cloud Gateway, Kong). Rate limiting, auth, logging merkezi yapılır.
- **Backward Compatibility:** Yeni alan eklemek güvenlidir (additive change). Alan silmek veya tipini değiştirmek breaking change'dir.
- **Error Response Standardı:** RFC 7807 (Problem Details) formatını kullanın:
  ```json
  {
    "type": "https://api.example.com/errors/insufficient-balance",
    "title": "Yetersiz Bakiye",
    "status": 422,
    "detail": "Hesap bakiyesi 500 TL, istenen tutar 1000 TL"
  }
  ```
- **Contract-First Design:** Önce API spec (OpenAPI YAML) yazın, sonra kodu generate edin. Backend-Frontend paralel geliştirme sağlar.
