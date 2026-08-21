# 🏗️ Yazılım Mimarı Bilgi Tabanı (Master Architect's Knowledge Base)

> **20 yıllık Java deneyimi** ile hazırlanmış, yazılım mimarı mülakat hazırlığı ve sürekli öğrenme kaynağı.
>
> Her konu Türkçe anlatılmıştır. Zor kavramlar analojilerle açıklanır, kritik mülakat soruları ve tricky sorular dahil edilmiştir.

---

## 📚 İçindekiler

### 1. Core Java
| # | Konu | Dosya |
|---|------|-------|
| 1 | JVM Memory Yapıları (Stack, Heap, Metaspace) | [01-memory-model.md](01-core-java/01-memory-model.md) |
| 2 | OOP İlkeleri (Encapsulation, Inheritance, Polymorphism, Abstraction) | [02-oop-principles.md](01-core-java/02-oop-principles.md) |
| 3 | Collections Framework (List, Set, Map, Queue, Deque) | [03-collections-framework.md](01-core-java/03-collections-framework.md) |
| 4 | Generics, Wildcards, PECS, Type Erasure, Reflection | [04-generics-and-reflection.md](01-core-java/04-generics-and-reflection.md) |
| 5 | Modern Java (Java 8-21: Records, Sealed, Pattern Matching) | [05-modern-java-features.md](01-core-java/05-modern-java-features.md) |
| 6 | Functional Programming (Lambda, Stream API, Optional) | [06-functional-programming.md](01-core-java/06-functional-programming.md) |
| 7 | Concurrency (Thread, Executor, CompletableFuture, Virtual Threads) | [07-concurrency.md](01-core-java/07-concurrency.md) |
| 8 | I/O ve NIO (File, Channel, Buffer, Path, WatchService) | [08-io-and-nio.md](01-core-java/08-io-and-nio.md) |
| 9 | Exception Handling ve Best Practices | [09-exception-handling.md](01-core-java/09-exception-handling.md) |

---

### 2. Spring Ecosystem
| # | Konu | Dosya |
|---|------|-------|
| 1 | Spring Core (IoC, DI, Bean Lifecycle, AOP) | [01-spring-core-ioc-di.md](02-spring-ecosystem/01-spring-core-ioc-di.md) |
| 2 | Spring Boot (Auto-config, Actuator, Profiles) | [02-spring-boot.md](02-spring-ecosystem/02-spring-boot.md) |
| 3 | Spring Security (Auth, JWT, OAuth2, CORS) | [03-spring-security.md](02-spring-ecosystem/03-spring-security.md) |
| 4 | Spring Data JPA (Repository, Query Methods, Specifications) | [04-spring-data-jpa.md](02-spring-ecosystem/04-spring-data-jpa.md) |
| 5 | Spring Cloud (Config, Gateway, Resilience4j, Service Discovery) | [05-spring-cloud.md](02-spring-ecosystem/05-spring-cloud.md) |
| 6 | Spring WebFlux (Reactive Programming, Mono/Flux, R2DBC) | [06-spring-webflux.md](02-spring-ecosystem/06-spring-webflux.md) |

---

### 3. Data Persistence
| # | Konu | Dosya |
|---|------|-------|
| 1 | Hibernate/JPA Deep Dive (Caching, Fetching, N+1) | [01-hibernate-jpa-deep-dive.md](03-data-persistence/01-hibernate-jpa-deep-dive.md) |
| 2 | Transaction Yönetimi ve Kilitleme Stratejileri | [02-transactions-and-locking.md](03-data-persistence/02-transactions-and-locking.md) |
| 3 | Sorgu Optimizasyonu (Dinamik Sorgulama, Sayfalama) | [03-query-optimization.md](03-data-persistence/03-query-optimization.md) |
| 4 | Veritabanı Tasarımı (Normalizasyon, Indexing) | [04-database-design.md](03-data-persistence/04-database-design.md) |
| 5 | Database Migration (Liquibase, Flyway) | [05-liquibase-flyway.md](03-data-persistence/05-liquibase-flyway.md) |
| 6 | NoSQL (MongoDB, Redis, Cassandra, Elasticsearch) | [06-nosql-overview.md](03-data-persistence/06-nosql-overview.md) |

---

### 4. Software Design
| # | Konu | Dosya |
|---|------|-------|
| 1 | SOLID Prensipleri | [01-solid-principles.md](04-software-design/01-solid-principles.md) |
| 2 | Clean Code (KISS, DRY, YAGNI, Boy Scout Rule) | [02-clean-code.md](04-software-design/02-clean-code.md) |
| 3 | Design Patterns (GoF - 23 Pattern) | [03-design-patterns-gof/](04-software-design/03-design-patterns-gof/README.md) |
| 4 | Enterprise Patterns (Repository, Specification, Unit of Work) | [04-enterprise-patterns.md](04-software-design/04-enterprise-patterns.md) |
| 5 | Domain-Driven Design (Aggregate, Entity, Value Object, Bounded Context) | [05-domain-driven-design.md](04-software-design/05-domain-driven-design.md) |

---

### 5. Architecture
| # | Konu | Dosya |
|---|------|-------|
| 1 | Mimari Stiller (Monolith, SOA, Microservice, Serverless) | [01-architectural-styles.md](05-architecture/01-architectural-styles.md) |
| 2 | Microservices (Decomposition, Communication, Patterns) | [02-microservices.md](05-architecture/02-microservices.md) |
| 3 | Event-Driven Architecture (Event Sourcing, CQRS) | [03-event-driven-architecture.md](05-architecture/03-event-driven-architecture.md) |
| 4 | Dağıtık Sistemler (CAP, Consensus, Consistency) | [04-distributed-systems.md](05-architecture/04-distributed-systems.md) |
| 5 | API Tasarımı (REST, GraphQL, gRPC, Versioning) | [05-api-design.md](05-architecture/05-api-design.md) |
| 6 | Saga Patterns (Choreography vs Orchestration) | [06-saga-patterns.md](05-architecture/06-saga-patterns.md) |
| 7 | 12-Factor App | [07-12-factor-app.md](05-architecture/07-12-factor-app.md) |
| 8 | Architecture Decision Records (ADR) | [08-architecture-decision-records.md](05-architecture/08-architecture-decision-records.md) |

---

### 6. System Design
| # | Konu | Dosya |
|---|------|-------|
| 1 | System Design Temelleri | [01-fundamentals.md](06-system-design/01-fundamentals.md) |
| 2 | Ölçeklenebilirlik (Horizontal/Vertical, Sharding) | [02-scalability.md](06-system-design/02-scalability.md) |
| 3 | Caching Stratejileri (Write-Through, Write-Behind, Cache-Aside) | [03-caching-strategies.md](06-system-design/03-caching-strategies.md) |
| 4 | Message Queues Temel Karşılaştırması | [04-message-queues.md](06-system-design/04-message-queues.md) |
| 5 | Gerçek Dünya Tasarımları | [05-real-world-designs/](06-system-design/05-real-world-designs/) |
| 6 | Real-Time İletişim (WebSocket, SSE, Long Polling) | [06-real-time-communication.md](06-system-design/06-real-time-communication.md) |
| 7 | **Apache Kafka Derinlemesine Rehber (KStreams, Connect, KRaft, Clustering)** | [07-kafka-deep-dive.md](06-system-design/07-kafka-deep-dive.md) |
| 8 | **RabbitMQ Derinlemesine Rehber (Exchanges, Quorum Queues, DLX, Ack/Nack)** | [08-rabbitmq-deep-dive.md](06-system-design/08-rabbitmq-deep-dive.md) |

---

### 7. DevOps & Cloud
| # | Konu | Dosya |
|---|------|-------|
| 1 | Docker (Containerization, Dockerfile, Multi-stage) | [01-docker.md](07-devops-and-cloud/01-docker.md) |
| 2 | Kubernetes (Pod, Deployment, Service, Ingress, HPA) | [02-kubernetes.md](07-devops-and-cloud/02-kubernetes.md) |
| 3 | CI/CD Pipelines (GitHub Actions, Jenkins) | [03-cicd-pipelines.md](07-devops-and-cloud/03-cicd-pipelines.md) |
| 4 | Observability (Prometheus, Grafana, ELK, Distributed Tracing) | [04-observability.md](07-devops-and-cloud/04-observability.md) |

---

### 8. Security
| # | Konu | Dosya |
|---|------|-------|
| 1 | Web & API Güvenliği (OWASP Top 10, XSS, CSRF, SQLi) | [01-web-api-security.md](08-security/01-web-api-security.md) |
| 2 | Hashing & Encryption (bcrypt, AES, RSA, Digital Signatures) | [03-hashing-encryption.md](08-security/03-hashing-encryption.md) |
| 3 | Security Architecture (Zero Trust, mTLS, Secret Management) | [04-security-architecture.md](08-security/04-security-architecture.md) |

---

### 9. Testing
| # | Konu | Dosya |
|---|------|-------|
| 1 | Unit Testing (JUnit 5, Mockito, AssertJ) | [01-unit-testing.md](09-testing/01-unit-testing.md) |
| 2 | Integration Testing (Testcontainers, MockMvc) | [02-integration-testing.md](09-testing/02-integration-testing.md) |
| 3 | BDD & Cucumber | [03-bdd-and-cucumber.md](09-testing/03-bdd-and-cucumber.md) |
| 4 | Test Stratejileri (Piramit, TDD, Contract Testing, Mutation Testing) | [05-testing-strategies.md](09-testing/05-testing-strategies.md) |

---

### 10. Algorithms & Data Structures
| # | Konu | Dosya |
|---|------|-------|
| 1 | Whiteboard Problemleri ve Çözüm Stratejileri | [09-whiteboard-problems.md](10-algorithms-and-data-structures/09-whiteboard-problems.md) |

---

### 11. Build Tools & Ecosystem
| # | Konu | Dosya |
|---|------|-------|
| 1 | Maven & Gradle | [01-maven-gradle.md](11-build-tools-and-ecosystem/01-maven-gradle.md) |
| 2 | Logging (SLF4J, Logback, Log4j2) | [02-logging.md](11-build-tools-and-ecosystem/02-logging.md) |
| 3 | gRPC Tutorial | [03-grpc.md](11-build-tools-and-ecosystem/03-grpc.md) |
| 4 | MockServer Kullanım Rehberi | [04-mockserver.md](11-build-tools-and-ecosystem/04-mockserver.md) |

---

### 12. Frontend for Architects
| # | Konu | Dosya |
|---|------|-------|
| 1 | Angular Temelleri | [01-angular.md](12-frontend-for-architects/01-angular.md) |
| 2 | React Temelleri | [02-react.md](12-frontend-for-architects/02-react.md) |

---

### 13. Soft Skills & Leadership
| # | Konu | Dosya |
|---|------|-------|
| 1 | Davranışsal Mülakat (STAR Metodu) | [01-behavioral-interview.md](13-soft-skills-and-leadership/01-behavioral-interview.md) |
| 2 | Agile Metodolojileri (Scrum, Kanban) | [02-agile-methodologies.md](13-soft-skills-and-leadership/02-agile-methodologies.md) |
| 3 | Teknik Liderlik ve Mentorluk | [03-technical-leadership.md](13-soft-skills-and-leadership/03-technical-leadership.md) |

---

### 14. JVM Internals & Performance
| # | Konu | Dosya |
|---|------|-------|
| 1 | JVM Mimarisi (ClassLoader, JIT, Bytecode) | [01-jvm-architecture.md](14-jvm-internals-and-performance/01-jvm-architecture.md) |
| 2 | Garbage Collection (G1, ZGC, GC Tuning) | [02-garbage-collection.md](14-jvm-internals-and-performance/02-garbage-collection.md) |
| 3 | Performans Optimizasyonu (JMH, Profiling, Bottleneck) | [03-performance-optimization.md](14-jvm-internals-and-performance/03-performance-optimization.md) |

---

## 🔬 Çalışan Projeler

| Proje | Açıklama | Konum |
|-------|----------|-------|
| Design Patterns | GoF tasarım desenleri Java implementasyonları | [projects/design-patterns/](projects/design-patterns/) |
| Best Practices | Java best practices örnekleri | [projects/best-practices/](projects/best-practices/) |
| Hibernate Tutorial | Hibernate/JPA pratik örnekleri | [projects/hibernate-tutorial/](projects/hibernate-tutorial/) |
| JPA Demo | JPA ileri seviye demo projesi | [projects/jpa-demo/](projects/jpa-demo/) |
| gRPC Tutorial | gRPC server/client implementasyonu | [projects/grpc-tutorial/](projects/grpc-tutorial/) |
| Saga Choreography | Choreography-based saga örneği | [projects/saga-choreography/](projects/saga-choreography/) |
| Saga Orchestration | Orchestration-based saga örneği | [projects/saga-orchestration/](projects/saga-orchestration/) |
| Microservice K8s Demo | Kubernetes üzerinde microservice demo | [projects/microservice-k8s-demo/](projects/microservice-k8s-demo/) |
| Docker Tutorial | Docker/Docker Compose örnekleri | [projects/docker-tutorial/](projects/docker-tutorial/) |
| Algorithm Questions | Mülakat algoritma soruları çözümleri | [projects/algorithm-questions/](projects/algorithm-questions/) |
| Angular Tutorial | Angular SPA tutorial projesi | [projects/angular-tutorial/](projects/angular-tutorial/) |
| React Tutorial | React tutorial projesi | [projects/react-tutorial/](projects/react-tutorial/) |

---

## 📖 Kullanım Rehberi

### Mülakat Hazırlığı İçin
1. **Kısa hazırlık (1-2 gün):** Core Java (Bölüm 1) + SOLID/Design Patterns (Bölüm 4) + System Design (Bölüm 6)
2. **Orta hazırlık (1 hafta):** Yukarıdakilere ek olarak Spring (Bölüm 2) + Architecture (Bölüm 5) + Testing (Bölüm 9)
3. **Tam hazırlık (2-3 hafta):** Tüm bölümler + Projelerle pratik

### Günlük Referans İçin
- Her dosyanın sonundaki **"Geliştirici İpuçları"** bölümü hızlı hatırlatıcıdır
- **"Kritik Mülakat Soruları"** bölümleri tricky soruları ve cevaplarını içerir

---

> *"The only way to go fast, is to go well."* — Robert C. Martin
