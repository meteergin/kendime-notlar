# Spring Boot ile A'dan Z'ye gRPC Eğitimi

Bu proje, Spring Boot üzerinde gRPC teknolojisinin profesyonel kullanımını öğretmek amacıyla hazırlanmıştır. İçerisinde tüm iletişim modellerini (Unary, Server Streaming, Client Streaming, Bidirectional Streaming) kapsayan bir bankacılık senaryosu, interceptor kullanımı ve istemci implementasyonu bulunmaktadır.

## İçindekiler
1. [gRPC Nedir?](#1-grpc-nedir)
2. [Protocol Buffers (Protobuf)](#2-protocol-buffers-protobuf)
3. [Proje Kurulumu ve Yapılandırma](#3-proje-kurulumu-ve-yapılandırma)
4. [Service Tanımlama (.proto)](#4-service-tanımlama-proto)
5. [İletişim Modelleri ve Implementasyon](#5-iletişim-modelleri-ve-implementasyon)
    - [Unary RPC](#51-unary-rpc)
    - [Server Streaming RPC](#52-server-streaming-rpc)
    - [Client Streaming RPC](#53-client-streaming-rpc)
    - [Bidirectional Streaming RPC](#54-bidirectional-streaming-rpc)
6. [Interceptorlar (Araya Giren Katmanlar)](#6-interceptorlar-araya-giren-katmanlar)
7. [İstemci (Client) Implementasyonu](#7-istemci-client-implementasyonu)
8. [Nasıl Çalıştırılır?](#8-nasıl-çalıştırılır)

---

## 1. gRPC Nedir?

gRPC (Google Remote Procedure Call), Google tarafından geliştirilen yüksek performanslı, açık kaynaklı bir RPC (Remote Procedure Call) framework'üdür. HTTP/2 protokolünü kullanır ve verileri binary (ikili) formatta taşır.

**REST vs gRPC:**
| Özellik | REST | gRPC |
|---------|------|------|
| **Protokol** | HTTP/1.1 (Genellikle) | HTTP/2 |
| **Veri Formatı** | JSON / XML (Metin tabanlı) | Protocol Buffers (Binary) |
| **Performans** | Daha yavaş, daha büyük veri boyutu | Çok hızlı, küçük veri boyutu |
| **Streaming** | Zor (WebSocket vb. gerekir) | Native destek (Çift yönlü) |
| **Kullanım Alanı** | Public API'lar, Web Tarayıcıları | Mikroservisler arası iletişim |

## 2. Protocol Buffers (Protobuf)

gRPC, veri serileştirme formatı olarak Protocol Buffers kullanır. `.proto` uzantılı dosyalarda servisler ve mesajlar tanımlanır. Bu dosyalar derlenerek hedef dilde (Java, Go, Python vb.) kodlar otomatik üretilir.

**Avantajları:**
- **Tip Güvenli:** Veri tipleri bellidir.
- **Hızlı:** JSON'a göre çok daha hızlı serileştirme/deserileştirme.
- **Dil Bağımsız:** Bir `.proto` dosyasından her dil için kod üretilebilir.

## 3. Proje Kurulumu ve Yapılandırma

Bu projede `spring-grpc-spring-boot-starter` (v0.12.0) kullanılmıştır.

**pom.xml Bağımlılıkları:**
```xml
<dependency>
    <groupId>org.springframework.grpc</groupId>
    <artifactId>spring-grpc-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-services</artifactId>
</dependency>
```

**Protobuf Plugin:**
Maven build sürecinde `.proto` dosyalarından Java sınıflarını üretmek için `protobuf-maven-plugin` kullanılır.

## 4. Service Tanımlama (.proto)

`src/main/proto/BankService.proto` dosyasında servisimiz tanımlanmıştır.

```protobuf
service BankService {
  rpc GetAccountBalance (AccountRequest) returns (AccountResponse);
  rpc GetTransactionHistory (TransactionHistoryRequest) returns (stream TransactionEntry);
  rpc DepositStream (stream DepositRequest) returns (DepositSummary);
  rpc LiveExchangeRate (stream ExchangeRateRequest) returns (stream ExchangeRateResponse);
}
```

## 5. İletişim Modelleri ve Implementasyon

Sunucu tarafı implementasyonu `BankGrpcService.java` sınıfındadır. `@GrpcService` anotasyonu ile Spring context'ine dahil edilir.

### 5.1 Unary RPC
En basit modeldir. İstemci bir istek atar, sunucu bir yanıt döner.
**Kullanım:** Bakiye sorgulama.
```java
public void getAccountBalance(AccountRequest request, StreamObserver<AccountResponse> responseObserver) {
    // İş mantığı...
    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

### 5.2 Server Streaming RPC
İstemci bir istek atar, sunucu bir akış (stream) başlatarak birden fazla mesaj döner.
**Kullanım:** Hesap hareketleri dökümü.
```java
public void getTransactionHistory(TransactionHistoryRequest request, StreamObserver<TransactionEntry> responseObserver) {
    for (TransactionEntry entry : transactions) {
        responseObserver.onNext(entry);
    }
    responseObserver.onCompleted();
}
```

### 5.3 Client Streaming RPC
İstemci bir akış başlatarak sunucuya sürekli veri gönderir. Sunucu akış bitince tek bir yanıt döner.
**Kullanım:** Büyük dosya yükleme veya toplu veri girişi.
```java
public StreamObserver<DepositRequest> depositStream(StreamObserver<DepositSummary> responseObserver) {
    return new StreamObserver<DepositRequest>() {
        public void onNext(DepositRequest request) { /* Veriyi işle */ }
        public void onCompleted() { /* Özeti dön */ responseObserver.onNext(summary); responseObserver.onCompleted(); }
    };
}
```

### 5.4 Bidirectional Streaming RPC
Her iki taraf da bağımsız olarak birbirine mesaj gönderebilir.
**Kullanım:** Chat uygulamaları, canlı veri akışları.
```java
public StreamObserver<ExchangeRateRequest> liveExchangeRate(StreamObserver<ExchangeRateResponse> responseObserver) {
    return new StreamObserver<ExchangeRateRequest>() {
        public void onNext(ExchangeRateRequest request) {
            // İstemciden gelen isteğe göre sürekli yanıt dönülebilir
            responseObserver.onNext(rate1);
            responseObserver.onNext(rate2);
        }
    };
}
```

## 6. Interceptorlar (Araya Giren Katmanlar)

gRPC çağrılarında araya girerek loglama, kimlik doğrulama gibi işlemler yapmak için Interceptor kullanılır.
`LogInterceptor.java` sınıfı `@GlobalServerInterceptor` ile işaretlenerek tüm servisler için aktif edilmiştir.

```java
@Override
public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(...) {
    System.out.println("Method: " + call.getMethodDescriptor().getFullMethodName());
    return next.startCall(call, headers);
}
```

## 7. İstemci (Client) Implementasyonu

`BankClient.java` sınıfı, `CommandLineRunner` implemente ederek uygulama açılışında örnek senaryoları çalıştırır.

- **BlockingStub:** Senkron çağrılar için (Unary, Server Streaming).
- **Stub (Async):** Asenkron çağrılar için (Client Streaming, Bidirectional).

## 8. Nasıl Çalıştırılır?

1. Projeyi derleyin:
   ```bash
   ./mvnw clean install
   ```
2. Uygulamayı başlatın:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Konsol çıktısını izleyin. İstemci otomatik olarak çalışacak ve tüm senaryoları test edecektir.

---
**Not:** Bu proje eğitim amaçlıdır. Prodüksiyon ortamında hata yönetimi, güvenlik (TLS/SSL) ve retry mekanizmaları eklenmelidir.
