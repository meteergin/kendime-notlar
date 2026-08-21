# Scenario 11: The Ultimate Microservices Stack

Bu senaryo, profesyonel bir Spring Boot Mikroservis projesinde görebileceğiniz **tüm bileşenlerin** tek bir `docker-compose.yml` dosyasında nasıl orkestre edildiğini gösteren bir **Mimari Şablondur**.

## İçerdiği Bileşenler

### Infrastructure (Altyapı)
1.  **PostgreSQL:** Ana veritabanı.
2.  **Redis:** Cache.
3.  **RabbitMQ:** Message Broker.
4.  **MinIO:** Object Storage.

### Platform Services (Platform)
5.  **Config Server:** Merkezi konfigürasyon yönetimi.
6.  **Discovery Server (Eureka):** Servis keşfi.
7.  **API Gateway (Spring Cloud Gateway):** Tek giriş noktası.

### Observability (Gözlemleme)
8.  **Zipkin:** Distributed Tracing.
9.  **Prometheus:** Metrik toplama.
10. **Grafana:** Dashboard.
11. **ELK Stack (Elasticsearch, Logstash, Kibana):** Log yönetimi.

### Business Services (İş Mantığı)
12. **Product Service:** (Template)
13. **Order Service:** (Template)

## Nasıl Kullanılır?

Bu dosya bir şablondur. Kendi projelerinizde kullanmak için `image: my-product-service` kısımlarını kendi image isimlerinizle değiştirmeniz gerekir.

```bash
# Tüm sistemi başlat (Dikkat: 8GB+ RAM gerektirir)
docker-compose up -d
```

## Başlangıç Sırası (Startup Order)
Docker Compose'un `depends_on` ve `healthcheck` özellikleri sayesinde sistem şu sırayla açılır:
1.  Infrastructure (Db, Broker, vb.)
2.  Config Server & Discovery Server
3.  Business Services
4.  API Gateway
