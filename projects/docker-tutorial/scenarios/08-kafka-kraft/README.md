# Scenario 8: Event Streaming (Kafka KRaft Mode)

Apache Kafka, geleneksel olarak Zookeeper'a bağımlıydı. Ancak yeni **KRaft (Kafka Raft)** modu ile Zookeeper bağımlılığı ortadan kalktı. Bu senaryo, modern ve hafif bir Kafka kurulumunu gösterir.

## Özellikler
1.  **Zookeeper-less:** Sadece Kafka broker çalışır, yönetim daha kolaydır.
2.  **Redpanda Console:** Kafka topic'lerini, mesajlarını ve consumer gruplarını izlemek için modern bir UI.

## Dosyalar
*   `docker-compose.yml`: Kafka ve Redpanda Console tanımı.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **Redpanda Console (UI):** [http://localhost:8080](http://localhost:8080)
*   **Kafka Broker:** `localhost:9092`
