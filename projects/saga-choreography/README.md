# Microservices Choreography Projesi

Bu proje, Spring Boot, Kafka ve PostgreSQL kullanılarak oluşturulmuş, **Choreography (Koreografi)** tabanlı Saga tasarım desenini uygulayan bir mikroservis mimarisi örneğidir.

## 🏗 Mimari Genel Bakış

Sistemde merkezi bir orkestratör yoktur. Servisler birbirleriyle **Kafka Eventleri** üzerinden haberleşir.

**Akış:**
1.  **Sipariş Oluşturma:** Kullanıcı `Order Service` üzerinden sipariş verir -> `OrderCreatedEvent` fırlatılır.
2.  **Ödeme İşleme:** `Payment Service` bu eventi dinler, ödemeyi alır -> `PaymentProcessedEvent` fırlatır.
3.  **Kargo Hazırlama:** `Shipping Service` ödeme eventini dinler, kargoyu hazırlar -> `ShippingArrangedEvent` fırlatır.
4.  **Sipariş Tamamlama:** `Order Service` kargo eventini dinler ve sipariş durumunu `COMPLETED` olarak günceller.

Eğer ödeme başarısız olursa, `PaymentFailedEvent` fırlatılır ve `Order Service` siparişi iptal eder (`CANCELLED`).

---

## 📦 Modüller ve Detaylı Açıklamaları

Proje çok modüllü (multi-module) bir Maven projesidir.

### 1. Altyapı Modülleri (Infrastructure)

*   **`infra-service-registry` (Eureka Server)**
    *   **Port:** `8761`
    *   **Görevi:** Tüm mikroservislerin kendilerini kaydettiği ve birbirlerini bulduğu telefon rehberi gibidir. Servisler ayağa kalkarken buraya kayıt olur.

*   **`infra-config-server` (Config Server)**
    *   **Port:** `8888`
    *   **Görevi:** Tüm servislerin konfigürasyonlarını (veritabanı ayarları, kafka ayarları vb.) merkezi bir yerden dağıtır. Şu an `native` profil ile çalışmaktadır (dosya tabanlı).

*   **`infra-api-gateway` (API Gateway)**
    *   **Port:** `8080`
    *   **Görevi:** Dış dünyadan gelen istekleri karşılayan tek giriş kapısıdır. İstekleri ilgili servislere yönlendirir (Routing).
    *   **Örnek:** `/orders` isteğini `order-service`'e yönlendirir.

### 2. İş Modülleri (Microservices)

*   **`order-service`**
    *   **Port:** `8081`
    *   **Veritabanı:** `order_db` (PostgreSQL)
    *   **Görevi:** Siparişleri yönetir.
    *   **Kafka:** `OrderCreatedEvent` üretir. `PaymentFailedEvent` ve `ShippingArrangedEvent` dinler.

*   **`payment-service`**
    *   **Port:** `8082`
    *   **Veritabanı:** `payment_db` (PostgreSQL)
    *   **Görevi:** Ödeme işlemlerini simüle eder.
    *   **Kafka:** `OrderCreatedEvent` dinler. `PaymentProcessedEvent` veya `PaymentFailedEvent` üretir.

*   **`shipping-service`**
    *   **Port:** `8083`
    *   **Veritabanı:** `shipping_db` (PostgreSQL)
    *   **Görevi:** Kargo işlemlerini simüle eder.
    *   **Kafka:** `PaymentProcessedEvent` dinler. `ShippingArrangedEvent` üretir.

### 3. Ortak Kütüphane

*   **`common-dtos`**
    *   Tüm servislerin ortak kullandığı Java sınıflarını içerir (Eventler, DTO'lar). Kod tekrarını önlemek için kullanılır.

---

## 🚀 Kurulum ve Çalıştırma

### Gereksinimler
*   **Java 21** (Proje Java 21 ile derlenmektedir)
*   **Docker & Docker Compose** (Veritabanı ve Kafka için)

### Adım 1: Altyapıyı Başlatma
Veritabanı (Postgres), Mesaj Kuyruğu (Kafka), Zookeeper ve Zipkin'i başlatmak için Docker Compose kullanılır.

```bash
docker-compose up -d
```

### Adım 2: Projeyi Derleme
Tüm modülleri derlemek için ana dizinde şu komutu çalıştırın:

```bash
mvn clean install -DskipTests
```

### Adım 3: Servisleri Başlatma
Kolaylık olması açısından hazırlanan `run.sh` scriptini kullanabilirsiniz. Bu script tüm servisleri sırasıyla ve arka planda başlatır.

```bash
./run.sh
```

**Manuel Başlatma Sırası (Eğer script kullanmazsanız):**
1.  `infra-service-registry`
2.  `infra-config-server`
3.  `infra-api-gateway`
4.  `order-service`
5.  `payment-service`
6.  `shipping-service`

### Adım 4: Test Etme
Sistemin çalıştığını doğrulamak için `test_flow.sh` scriptini kullanabilirsiniz. Bu script API Gateway üzerinden bir sipariş oluşturur.

```bash
./test_flow.sh
```

Veya manuel olarak `curl` isteği atabilirsiniz:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "c4a8d570-...", "productId": "a1b2c3d4-...", "price": 100.0}'
```

### Logları İzleme
Servislerin loglarını anlık takip etmek için:

```bash
tail -f order.log payment.log shipping.log
```

## 🛠 Sorun Giderme (Troubleshooting)

*   **Build Hatası (Java Version):** Eğer `mvn clean install` sırasında hata alırsanız, Java 21 kullandığınızdan emin olun (`java -version`).
*   **503 Service Unavailable:** Gateway üzerinden istek attığınızda bu hatayı alırsanız, servisler henüz Eureka'ya tam olarak kaydolmamış olabilir. 1-2 dakika bekleyip tekrar deneyin.
*   **Port Çakışması:** Eğer 8080, 8081 vb. portlar doluysa `application.yml` dosyalarından portları değiştirebilirsiniz.
