## Konu 23: Gerçek Zamanlı İletişim (SSE, WebSockets, Polling)

Modern uygulamalarda (chat, borsa takibi, bildirimler) verinin anlık olarak istemciye iletilmesi gerekir. Bir geliştirici olarak, **Polling**, **Long Polling**, **Server-Sent Events (SSE)** ve **WebSockets** arasındaki farkları ve kullanım senaryolarını bilmelisiniz.

---

### 1. İletişim Yöntemleri Karşılaştırması

| Yöntem | İletişim Yönü | Protokol | Bağlantı Tipi | Kullanım Senaryosu |
| :--- | :--- | :--- | :--- | :--- |
| **Short Polling** | Client → Server | HTTP | Kısa ömürlü, tekrarlı | Çok nadir güncellenen veriler (örn: dashboard yenileme). |
| **Long Polling** | Client → Server (Bekle) | HTTP | Uzun ömürlü (timeout'a kadar) | Basit bildirimler, eski tarayıcı desteği. |
| **SSE (Server-Sent Events)** | Server → Client | HTTP | Tek yönlü, sürekli açık | Haber akışı, borsa fiyatları, bildirimler. |
| **WebSockets** | Bidirectional (Çift Yönlü) | TCP (ws://) | Full-duplex, sürekli açık | Chat, multiplayer oyunlar, işbirlikçi editörler. |

---

### 2. Polling Stratejileri

#### Short Polling
Client belirli aralıklarla (örn: her 5 saniyede bir) sunucuya "yeni veri var mı?" diye sorar.
*   **Dezavantaj:** Gereksiz trafik, yüksek latency. Veri yoksa bile istek atılır.

#### Long Polling
Client istek atar, sunucu yeni veri oluşana kadar (veya timeout olana kadar) cevabı bekletir.
*   **Avantaj:** Short polling'e göre daha az gereksiz istek.
*   **Dezavantaj:** Her mesajdan sonra yeni bağlantı kurma maliyeti.

---

### 3. Server-Sent Events (SSE)

Standart HTTP protokolü üzerinden sunucudan istemciye tek yönlü veri akışı sağlar. `text/event-stream` content type kullanır.

**Spring Boot Örneği:**

```java
@RestController
public class NotificationController {

    @GetMapping("/stream-notifications")
    public SseEmitter streamNotifications() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Timeout süresi
        
        // Asenkron olarak veri gönder
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    emitter.send(SseEmitter.event().name("message").data("Bildirim " + i));
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }
}
```

**Client (JavaScript):**
```javascript
const eventSource = new EventSource('/stream-notifications');
eventSource.onmessage = function(event) {
    console.log('Yeni Bildirim:', event.data);
};
```

---

### 4. WebSockets

Tek bir TCP bağlantısı üzerinden çift yönlü (full-duplex) iletişim sağlar. HTTP handshake ile başlar, sonra `Upgrade` header ile WebSocket protokolüne geçer.

**Spring Boot (STOMP ile) Örneği:**

1.  **Config:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS(); // Handshake endpoint
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // Client'ın dinleyeceği prefix
        registry.setApplicationDestinationPrefixes("/app"); // Server'a gönderilen prefix
    }
}
```

2.  **Controller:**
```java
@Controller
public class ChatController {

    @MessageMapping("/chat.sendMessage") // /app/chat.sendMessage
    @SendTo("/topic/public") // Abone olan herkese gönder
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }
}
```

---

### 5. Kritik Mülakat Soruları 

#### Soru 1: SSE ile WebSockets arasındaki temel fark nedir? Hangisini ne zaman seçersiniz?
**Cevap:**
*   **SSE:** Sadece Server → Client (tek yönlü). HTTP kullanır, firewall dostudur. Otomatik reconnection vardır. **Borsa, haber akışı** için ideal.
*   **WebSockets:** Çift yönlü. Binary veri destekler. **Chat, oyun** gibi karşılıklı etkileşim gerektiren yerlerde kullanılır.

#### Soru 2: WebSocket bağlantılarını Load Balancer arkasında nasıl yönetirsiniz?
**Cevap:** WebSocket durumlu (stateful) bir bağlantıdır.
1.  **Sticky Sessions:** Load balancer'ın aynı client'ı hep aynı sunucuya yönlendirmesi gerekir.
2.  **Message Broker (Redis/RabbitMQ):** Farklı sunuculara bağlı kullanıcıların birbirine mesaj atabilmesi için araya bir Pub/Sub mekanizması (örn: Redis Pub/Sub) konulmalıdır.

#### Soru 3: Connection Limit (C10K sorunu) nedir?
**Cevap:** Bir sunucunun aynı anda kaç açık bağlantıyı (socket) yönetebileceği sınırlıdır (File Descriptor limiti).
*   **Çözüm:** Non-blocking I/O (Netty/WebFlux), Load Balancing, Kernel tuning (`ulimit`).

#### Soru 4: HTTP/2 bu denklemi nasıl değiştirir?
**Cevap:** HTTP/2 **Server Push** ve **Multiplexing** özelliklerine sahiptir. Tek bir TCP bağlantısı üzerinden çoklu istek/cevap destekler. Ancak, gerçek zamanlı çift yönlü iletişim için WebSockets hala standarttır (veya HTTP/3 QUIC).

#### Soru 5 (Tricky): SSE (Server-Sent Events) kullanmanın tarayıcı limiti nedir?
**Cevap:** HTTP/1.1 ile tarayıcılar aynı domain'e en fazla 6 bağlantı açar. SSE sürekli açık bir bağlantı olduğu için, 6 tab açarsanız 7. tab çalışmaz.
*   **Çözüm:** HTTP/2 kullanın (Multiplexing sayesinde tek bağlantı üzerinden sınırsız stream).

#### Soru 6 (Tricky): WebSocket sunucusu ölçeklenirken (Scaling Out) neye dikkat edilmeli?
**Cevap:** Kullanıcı A Sunucu 1'e, Kullanıcı B Sunucu 2'ye bağlıysa, birbirlerine doğrudan mesaj atamazlar.
*   **Çözüm:** Sunucular arasına bir **Redis Pub/Sub** veya **RabbitMQ** koyarak mesajları tüm sunuculara (veya ilgili sunucuya) dağıtmak gerekir.

---

### 6. Geliştirici İpuçları

*   **Fallback Mekanizması:** Her ağ ortamı WebSocket desteklemeyebilir (kurumsal proxy'ler). **SockJS** gibi kütüphaneler, WebSocket çalışmazsa otomatik olarak Long Polling'e düşer (graceful degradation).
*   **Heartbeat (Ping/Pong):** Bağlantının koptuğunu anlamak için düzenli ping/pong mesajları gönderin. Load balancer'lar boşta kalan (idle) TCP bağlantılarını kesebilir.
*   **State Management:** WebSocket sunucularını **stateless** tutmaya çalışın. Session bilgisini Redis gibi harici bir yerde tutun.
*   **Security:** `ws://` yerine her zaman `wss://` (TLS şifreli) kullanın. Handshake sırasında Auth token (JWT) doğrulayın.


---

