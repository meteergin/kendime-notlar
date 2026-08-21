# Scenario 1: Professional Java Docker Builder

Bu senaryo, bir Java uygulamasını (Spring Boot) production ortamına hazırlamak için gereken **en ideal Dockerfile** yapısını içerir.

## Özellikler
1.  **Multi-Stage Build:** Build araçları (Maven) ile Runtime ortamını (JRE) ayırır. Image boyutu küçülür.
2.  **Layered JAR:** Spring Boot'un katmanlı yapısını kullanarak build cache optimizasyonu sağlar.
3.  **Security:** `root` yerine `spring` kullanıcısı ile çalışır.
4.  **JVM Tuning:** Konteyner limitlerine saygılı JVM parametreleri.

## Dosyalar
*   `Dockerfile`: Production-ready Dockerfile.
*   `.dockerignore`: Gereksiz dosyaların build context'e girmesini engeller.

## Nasıl Çalıştırılır?

```bash
# Image'ı build et
docker build -t myapp:prod .

# Çalıştır
docker run -p 8080:8080 --memory="512m" --cpus="1.0" myapp:prod
```
