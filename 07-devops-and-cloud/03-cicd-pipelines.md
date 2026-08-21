# CI/CD Pipelines (Jenkins, GitLab CI, GitHub Actions)

> **Analoji:** CI/CD, bir "otomatik araba yıkama makinesi" gibidir. Araba (kod) girişte teslim edilir, otomatik olarak yıkanır (build), kurulanır (test), cilalı çıkar (deploy). Manuel işlem yok, insan hatası yok.

---

## 1. CI/CD Kavramları

| Kavram | Açıklama | Analoji |
| :--- | :--- | :--- |
| **CI (Continuous Integration)** | Her commit'te otomatik build + test | Her malzeme geldiğinde kalite kontrol |
| **CD (Continuous Delivery)** | Production'a deploy hazır artifact | Paket hazır, onay bekleniyor |
| **CD (Continuous Deployment)** | Otomatik production deploy | Paket onaysız gönderiliyor |

### Tipik Pipeline Aşamaları

```
Commit → Build → Unit Test → Code Analysis → Integration Test → Package → Deploy (Staging) → E2E Test → Deploy (Prod)
```

---

## 2. GitHub Actions Örneği

```yaml
name: Java CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Build & Test
        run: mvn clean verify -B
      
      - name: SonarQube Analysis
        run: mvn sonar:sonar -Dsonar.host.url=${{ secrets.SONAR_URL }}
      
      - name: Build Docker Image
        run: docker build -t myapp:${{ github.sha }} .
      
      - name: Push to Registry
        run: |
          docker tag myapp:${{ github.sha }} registry.example.com/myapp:${{ github.sha }}
          docker push registry.example.com/myapp:${{ github.sha }}

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    steps:
      - name: Deploy to Staging
        run: kubectl set image deployment/myapp myapp=registry.example.com/myapp:${{ github.sha }} -n staging

  deploy-production:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production  # Manuel onay gerektirir
    steps:
      - name: Deploy to Production
        run: kubectl set image deployment/myapp myapp=registry.example.com/myapp:${{ github.sha }} -n production
```

---

## 3. Kritik Mülakat Soruları

### Soru 1: Blue-Green deployment nedir?
**Cevap:** İki özdeş ortam (Blue=mevcut, Green=yeni). Yeni versiyon Green'e deploy edilir, test edilir, traffic Blue'dan Green'e yönlendirilir. Sorun olursa anında Blue'ya geri dönülür.

### Soru 2: Feature Flag nedir?
**Cevap:** Yeni özelliği kodla birlikte deploy eder ama açıp kapatabilirsiniz. Belirli kullanıcılara (canary) veya yüzdelere göre özelliği aktif edersiniz. Trunk-based development'ı mümkün kılar.

### Soru 3: Pipeline'da hangi testler çalıştırılmalı?
**Cevap:** Unit Test → Integration Test → Contract Test → E2E Test (piramit sırası). Her aşama bir öncekinden yavaştır, az sayıda çalıştırılır.

---

## 4. Geliştirici İpuçları

- **Fail Fast:** Pipeline'ın ilk aşamalarında hataları yakalayın (lint, compile, unit test). Yavaş testleri (E2E) sona bırakın.
- **Cache:** Maven/Gradle dependency cache'i kullanın. Build süresi %50 azalır.
- **Parallel Jobs:** Bağımsız test suite'lerini paralel çalıştırın.
- **Secrets Management:** Şifreleri asla YAML'a yazmayın. GitHub Secrets, Vault kullanın.
- **Rollback Stratejisi:** Her deploy'dan önce rollback planı olsun. `kubectl rollout undo` veya Blue-Green switch.
