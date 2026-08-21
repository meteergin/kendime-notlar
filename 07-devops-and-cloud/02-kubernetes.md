# Kubernetes (Container Orchestration)

> **Analoji:** Docker bir "konteyner (yük konteyneri)" ise, Kubernetes bir "liman" gibidir. Liman, yüzlerce konteyneri nereye yerleştireceğini, hangisinin hasarlı olduğunu, yeni gelen yükü nasıl dağıtacağını yönetir. Siz sadece "100 konteyner istiyorum" dersiniz, liman gerisini halleder.

---

## 1. Temel Kavramlar

### Mimari

```
┌──────────── Kubernetes Cluster ─────────────┐
│                                              │
│  ┌── Control Plane (Master) ──────────────┐  │
│  │  API Server    ← kubectl/dashboard      │  │
│  │  etcd          ← Cluster state store    │  │
│  │  Scheduler     ← Pod placement          │  │
│  │  Controller Mg ← Desired state manager  │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌── Worker Node 1 ──┐  ┌── Worker Node 2 ──┐ │
│  │  kubelet           │  │  kubelet           │ │
│  │  kube-proxy        │  │  kube-proxy        │ │
│  │  ┌─Pod──┐ ┌─Pod──┐│  │  ┌─Pod──┐ ┌─Pod──┐│ │
│  │  │ 🐳   │ │ 🐳   ││  │  │ 🐳   │ │ 🐳   ││ │
│  │  └──────┘ └──────┘│  │  └──────┘ └──────┘│ │
│  └───────────────────┘  └───────────────────┘ │
└──────────────────────────────────────────────┘
```

### Temel Nesneler

| Nesne | Açıklama | Analoji |
| :--- | :--- | :--- |
| **Pod** | En küçük deploy birimi (1+ container) | Bir oda |
| **Deployment** | Pod'ların yaşam döngüsünü yönetir | Apartman yönetimi |
| **Service** | Pod'lara sabit erişim noktası sağlar | Apartman kapı zili |
| **ConfigMap** | Konfigürasyon verisi (non-secret) | Pano |
| **Secret** | Hassas veri (base64 encoded) | Kasa |
| **Ingress** | Dışarıdan gelen HTTP trafiğini yönlendirir | Kapıcı |
| **HPA** | Otomatik yatay ölçekleme | Asistan müdür |
| **PersistentVolume** | Kalıcı depolama | Depo |

---

## 2. YAML Manifestoları

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Aynı anda fazladan 1 pod
      maxUnavailable: 0   # Hiçbir pod kapanmasın
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
        - name: user-service
          image: myregistry/user-service:1.2.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: password
          resources:
            requests:
              cpu: "250m"      # 0.25 CPU core
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1Gi"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
```

### Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: ClusterIP  # Cluster içi erişim
  selector:
    app: user-service
  ports:
    - port: 80         # Service portu
      targetPort: 8080  # Container portu
```

### Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /users
            pathType: Prefix
            backend:
              service:
                name: user-service
                port:
                  number: 80
          - path: /orders
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 80
```

---

## 3. Health Checks (Probes)

| Probe | Amaç | Başarısız olursa |
| :--- | :--- | :--- |
| **Liveness** | Pod canlı mı? | Pod yeniden başlatılır |
| **Readiness** | Pod trafik almaya hazır mı? | Service'den çıkarılır |
| **Startup** | Uygulama başladı mı? | Diğer probe'lar bekler |

### Spring Boot Actuator Entegrasyonu

```yaml
# application.yml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

---

## 4. Deployment Stratejileri

| Strateji | Açıklama | Risk |
| :--- | :--- | :--- |
| **RollingUpdate** | Kademeli güncelleme (default) | Düşük |
| **Recreate** | Tümünü durdur, yenisini başlat | Downtime var |
| **Blue-Green** | İki ortam, traffic geçişi | Kaynak maliyeti |
| **Canary** | Yeni versiyon trafiğin %5'ini alır | Düşük, gözlemleme gerekir |

---

## 5. Kritik Mülakat Soruları

### Soru 1: Pod restart edilince veri kaybolur mu?
**Cevap:** Evet! Pod'lar **ephemeral**'dir. Kalıcı veri için `PersistentVolume` (PV) ve `PersistentVolumeClaim` (PVC) kullanın.

### Soru 2: Liveness ve Readiness probe farkı nedir?
**Cevap:** Liveness: "Canlı mı?" → Hayır ise pod öldürülüp yeniden başlatılır. Readiness: "Hazır mı?" → Hayır ise trafik gelmez ama pod öldürülmez.

### Soru 3: `kubectl` ile en çok kullanılan komutlar?
```bash
kubectl get pods                     # Pod listesi
kubectl describe pod <name>          # Pod detayı
kubectl logs <pod-name> -f           # Canlı log
kubectl exec -it <pod-name> -- bash  # Pod içine gir
kubectl apply -f deployment.yaml     # Manifest uygula
kubectl rollout status deployment/x  # Deploy durumu
kubectl rollout undo deployment/x    # Önceki versiyona dön
kubectl scale deployment/x --replicas=5  # Manual ölçekleme
```

### Soru 4 (Tricky): ConfigMap değişince pod'lar otomatik güncellenir mi?
**Cevap:** **Hayır!** Volume olarak mount edilmişse güncellenebilir ama env variable olarak kullanılıyorsa pod restart gerekir. **Çözüm:** `kubectl rollout restart deployment/x` veya Reloader gibi araçlar kullanın.

---

## 6. Geliştirici İpuçları

- **Resource Limits:** Her container'a `requests` ve `limits` koyun. Yoksa bir pod tüm node kaynaklarını tüketebilir.
- **Namespace:** Ortamları (dev, staging, prod) namespace ile ayırın.
- **Helm:** Karmaşık YAML'ları Helm chart'ları ile template'leyin ve versiyon yönetin.
- **Service Mesh (Istio/Linkerd):** Servisler arası mTLS, traffic management, observability için kullanın.
- **Pod Disruption Budget:** Aynı anda kaç pod'un kapatılabileceğini sınırlayın (maintenance sırasında downtime önleme).
