# Scenario 9: Object Storage (MinIO)

Uygulamanızda dosya yükleme (file upload) özelliği varsa, production'da genellikle AWS S3, Google Cloud Storage gibi servisler kullanılır. Local development ortamında ise **MinIO**, S3 API'si ile %100 uyumlu çalışan harika bir alternatiftir.

## Özellikler
1.  **S3 Compatible:** AWS SDK'larını kullanarak MinIO'ya bağlanabilirsiniz.
2.  **Console:** Bucket oluşturmak ve dosyaları görmek için web arayüzü.

## Dosyalar
*   `docker-compose.yml`: MinIO Server ve Console tanımı.

## Nasıl Çalıştırılır?

```bash
docker-compose up -d
```

*   **MinIO Console:** [http://localhost:9001](http://localhost:9001)
    *   **User:** `minioadmin`
    *   **Pass:** `minioadmin`
*   **API Endpoint:** `http://localhost:9000` (SDK'da `endpoint` olarak burayı verin).
