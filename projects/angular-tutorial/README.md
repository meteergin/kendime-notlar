# Modern Angular Enterprise Guide for Java Developers

**Hedef Kitle:** Java Backend Geliştiricileri  
**Angular Versiyonu:** 18+ (Modern, Standalone, Signals)

Bu rehber, bir Java geliştiricisinin zihinsel modellerini (Class, Bean, Annotation) kullanarak, modern Angular mimarisini en hızlı ve derinlemesine öğrenmesi için hazırlanmıştır. Eski Angular (NgModule, Zone.js tabanlı reaktivite) yerine, sektörün gittiği yön olan **Modern Angular** (Standalone, Signals, Hydration) odaklıdır.

---

## 1. Giriş: Java Geliştiricisi Gözüyle Angular

Angular, Frontend dünyasının "Spring Boot"udur. React gibi sadece bir "View" kütüphanesi değil, "Batteries-included" (her şey dahil) bir framework'tür.

| Java / Spring Kavramı | Angular Karşılığı | Açıklama |
| :--- | :--- | :--- |
| **Class** | **Component / Service** | Her şey bir TypeScript sınıfıdır. |
| **Annotation (`@Component`, `@Service`)** | **Decorator (`@Component`, `@Injectable`)** | Metadata eklemek için kullanılır. |
| **Dependency Injection (IoC Container)** | **Angular DI System** | Constructor injection ile bağımlılıklar yönetilir. |
| **Maven / Gradle** | **NPM / Angular CLI** | Paket yönetimi ve build işlemleri. |
| **POJO / DTO** | **Interface / Type** | TypeScript interface'leri ile tip güvenliği sağlanır. |
| **JSP / Thymeleaf** | **Template (HTML)** | Görünüm katmanı. |

---

## 2. Modern Kurulum ve Tooling

Modern Angular'da `NgModule` kavramı artık opsiyoneldir ve yeni projelerde **Standalone Components** standarttır.

### Proje Oluşturma (CLI)

```bash
# Angular CLI yükle
npm install -g @angular/cli

# Yeni proje oluştur (Standalone defaulttur)
# --ssr: Server Side Rendering (SEO için önemli)
# --style=scss: SCSS kullanımı
ng new enterprise-app --ssr --style=scss
```

### Proje Yapısı (LIFT Prensibi)
Java paket yapısına benzer şekilde, Angular'da da **LIFT** (Locate, Identify, Flat, Try-Dry) prensibi uygulanır.

```text
src/
  app/
    core/           # Singleton servisler, Interceptorlar (Java: @Configuration, Utils)
    shared/         # Ortak UI bileşenleri, Pipe'lar (Java: Commons)
    features/       # İş mantığı modülleri (Java: Domain Packages)
      auth/
      dashboard/
      users/
    app.config.ts   # Uygulama konfigürasyonu (Eski app.module.ts yerine)
    app.routes.ts   # Rotalar
```

---

## 3. Core Mimari: "The New Angular"

### 3.1. Standalone Components
Artık modül (`NgModule`) tanımlamaya gerek yok. Her component kendi bağımlılıklarını `imports` array'inde belirtir.

**Java Benzerliği:** Bir sınıfın `import` ifadeleri gibi.

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserCardComponent } from './user-card.component';

@Component({
  selector: 'app-user-list',
  standalone: true, // ARTIK VARSAYILAN
  imports: [CommonModule, UserCardComponent], // Bağımlılıklar burada
  template: `
    <h1>Kullanıcılar</h1>
    @for (user of users; track user.id) {
      <app-user-card [user]="user" />
    }
  `
})
export class UserListComponent {
  users = [{id: 1, name: 'Ahmet'}, {id: 2, name: 'Ayşe'}];
}
```

### 3.2. Signals (Reaktivite)
Angular'ın yeni reaktivite modeli. RxJS'in karmaşıklığını azaltır ve performansı artırır (Zone.js'siz değişim algılama).

**Java Benzerliği:** `Observable` pattern veya JavaFX Properties, ama daha basiti. Değer değiştiğinde, onu dinleyen template otomatik güncellenir.

```typescript
import { Component, signal, computed, effect } from '@angular/core';

@Component({ ... })
export class CounterComponent {
  // 1. State tanımlama (WritableSignal)
  count = signal(0);

  // 2. Türetilmiş değer (Computed Signal) - Cachelenir
  doubleCount = computed(() => this.count() * 2);

  constructor() {
    // 3. Side Effect (Değişim izleme)
    effect(() => {
      console.log(`Sayaç değişti: ${this.count()}`);
    });
  }

  increment() {
    // Değer güncelleme
    this.count.update(value => value + 1);
  }
}
```

### 3.3. Control Flow (Yeni Syntax)
Eski `*ngIf`, `*ngFor` yerine, daha performanslı ve okunabilir bloklar geldi.

```html
<!-- ESKİ -->
<div *ngIf="isLoggedIn; else loginTpl">Hoşgeldin</div>
<ng-template #loginTpl>Giriş Yap</ng-template>

<!-- YENİ (Modern) -->
@if (isLoggedIn) {
  <div>Hoşgeldin</div>
} @else {
  <button>Giriş Yap</button>
}

<!-- DÖNGÜ -->
@for (item of items; track item.id) {
  <div>{{ item.name }}</div>
} @empty {
  <div>Liste boş</div>
}
```

---

## 4. Enterprise Patterns & Advanced Features

### 4.1. Dependency Injection (DI) Deep Dive
Angular'ın DI sistemi Spring'in IoC container'ına çok benzer.

*   **providedIn: 'root'**: Servisin tüm uygulama genelinde Singleton olmasını sağlar (Spring `@Service` default davranışı).
*   **InjectionToken**: Interface veya primitive değerleri inject etmek için kullanılır (Spring `@Qualifier` veya `@Value` benzeri).

```typescript
// config.token.ts
import { InjectionToken } from '@angular/core';

export const API_URL = new InjectionToken<string>('API_URL');

// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    { provide: API_URL, useValue: 'https://api.enterprise.com' }
  ]
};

// user.service.ts
@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(@Inject(API_URL) private apiUrl: string, private http: HttpClient) {}
}
```

### 4.2. Routing & Lazy Loading
Enterprise uygulamalarda modüllerin gerektiğinde yüklenmesi (Lazy Loading) kritiktir.

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: 'admin',
    // Bu dosya sadece kullanıcı /admin'e gittiğinde indirilir
    loadComponent: () => import('./admin/admin.component').then(m => m.AdminComponent),
    // Fonksiyonel Guard (Spring Security Filter benzeri)
    canActivate: [authGuard] 
  }
];

// auth.guard.ts
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  return authService.isLoggedIn() ? true : router.createUrlTree(['/login']);
};
```

### 4.3. HTTP & Interceptors
Global hata yönetimi, token ekleme gibi işlemler için Interceptor kullanılır (Spring `HandlerInterceptor` veya `Filter`).

```typescript
// auth.interceptor.ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  
  const clonedReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  return next(clonedReq).pipe(
    catchError((err) => {
      if (err.status === 401) {
        inject(AuthService).logout();
      }
      return throwError(() => err);
    })
  );
};

// app.config.ts
providers: [
  provideHttpClient(withInterceptors([authInterceptor]))
]
```

---

## 5. State Management & Performans

### 5.1. State Management: Signals vs NGRX
Eskiden Redux (NGRX) standarttı ama çok boilerplate gerektiriyordu. Modern Angular'da **Signal Store** veya basit **Service with Signals** tercih edilir.

**Örnek: Service-based State (Basit & Etkili)**

```typescript
@Injectable({ providedIn: 'root' })
export class CartStore {
  // Read-only state dışarıya açılır
  private _items = signal<CartItem[]>([]);
  readonly items = this._items.asReadonly();

  // Computed values
  readonly totalAmount = computed(() => 
    this.items().reduce((acc, item) => acc + item.price, 0)
  );

  addToCart(item: CartItem) {
    this._items.update(current => [...current, item]);
  }
}
```

### 5.2. Performans Optimizasyonu

*   **ChangeDetectionStrategy.OnPush**: Component sadece `@Input` değiştiğinde veya bir event tetiklendiğinde render edilir. Default strateji her şeyi kontrol eder (yavaştır). Enterprise uygulamalarda **her zaman OnPush** kullanın.

```typescript
@Component({
  selector: 'app-heavy-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush, // ÖNEMLİ
  template: ...
})
```

*   **@defer (Lazy Loading Views)**: Component'in bir parçasını sonradan yüklemek için.

```html
@defer (on viewport) {
  <app-heavy-chart />
} @placeholder {
  <div>Grafik yükleniyor...</div>
}
```

---

## 6. Testing & Best Practices

### 6.1. Unit Testing
Angular default olarak Jasmine/Karma ile gelir ama modern projelerde **Jest** veya **Vitest** tercih edilir.

**Service Testi (Java JUnit benzeri):**
```typescript
it('should return user data', () => {
  const service = TestBed.inject(UserService);
  const httpMock = TestBed.inject(HttpTestingController);
  
  service.getUser(1).subscribe(user => {
    expect(user.name).toBe('Ali');
  });

  const req = httpMock.expectOne('/api/users/1');
  req.flush({ name: 'Ali' });
});
```

### 6.2. Best Practices Checklist
1.  **Strict Mode**: `tsconfig.json` içinde `strict: true` olmalı.
2.  **Async Pipe**: Template içinde `.subscribe()` kullanmayın. Her zaman `AsyncPipe` veya `Signals` kullanın. Memory leak'i önler.
3.  **Smart vs Dumb Components**:
    *   **Smart (Container)**: Servislere erişir, state yönetir.
    *   **Dumb (Presentational)**: Sadece `@Input` alır ve `@Output` event fırlatır.
4.  **TrackBy**: Döngülerde (`@for`) her zaman `track` kullanın.

---

## 7. Mülakat Soruları ve Cevapları (30 Adet)

### Başlangıç Seviyesi

1.  **Soru:** Angular ile React arasındaki temel fark nedir?
    *   **Cevap:** Angular "Opinionated" (fikir sahibi) bir framework'tür, router, HTTP client, form yönetimi dahili gelir. React ise sadece bir kütüphanedir, diğer parçalar için 3. parti kütüphaneler gerekir.

2.  **Soru:** `ngOnInit` ile `constructor` farkı nedir?
    *   **Cevap:** `constructor` TypeScript sınıfının başlatılması içindir (Dependency Injection burada yapılır). `ngOnInit` ise Angular component'i başlattığında ve inputları set ettiğinde çalışır. İş mantığı `ngOnInit`'te olmalıdır.

3.  **Soru:** Data Binding türleri nelerdir?
    *   **Cevap:** Interpolation `{{}}`, Property Binding `[]`, Event Binding `()`, Two-way Binding `[()]`.

4.  **Soru:** Directive nedir? Türleri nelerdir?
    *   **Cevap:** DOM'u manipüle eden sınıflardır. 3 türü vardır: Component (template'i olan), Structural (`*ngIf`, `@if`), Attribute (`ngClass`, `ngStyle`).

5.  **Soru:** Pipe nedir? Pure ve Impure Pipe farkı nedir?
    *   **Cevap:** Veriyi transform etmek için kullanılır (örn: tarih formatlama). Pure pipe sadece input değeri değiştiğinde çalışır (performanslı). Impure pipe her change detection döngüsünde çalışır (performanssız).

6.  **Soru:** Lifecycle Hook'ları sıralayınız.
    *   **Cevap:** OnChanges, OnInit, DoCheck, AfterContentInit, AfterContentChecked, AfterViewInit, AfterViewChecked, OnDestroy.

7.  **Soru:** `Observable` ve `Promise` farkı nedir?
    *   **Cevap:** Promise tek bir değer döner ve iptal edilemez. Observable zaman içinde çoklu değer dönebilir (stream), iptal edilebilir ve operatörlerle (map, filter) işlenebilir.

8.  **Soru:** `Dependency Injection` nedir?
    *   **Cevap:** Bir sınıfın bağımlılıklarının dışarıdan verilmesi prensibidir. Angular'da bu işi DI Container yapar.

9.  **Soru:** `Standalone Component` nedir?
    *   **Cevap:** `NgModule`'e ihtiyaç duymayan, kendi bağımlılıklarını yöneten component türüdür. Angular 14+ ile gelmiştir.

10. **Soru:** Angular CLI ne işe yarar?
    *   **Cevap:** Proje oluşturma, component/service generate etme (`ng g c`), build alma ve test koşma işlemlerini otomatize eder.

### Orta Seviye

11. **Soru:** `Subject`, `BehaviorSubject` ve `ReplaySubject` farkları nelerdir?
    *   **Cevap:** `Subject` başlangıç değeri almaz, sadece yeni abonelere sonraki değerleri verir. `BehaviorSubject` başlangıç değeri alır ve yeni aboneye *son* değeri hemen verir. `ReplaySubject` geçmişteki X kadar değeri yeni aboneye verir.

12. **Soru:** `ChangeDetectionStrategy.OnPush` ne işe yarar?
    *   **Cevap:** Performans optimizasyonu sağlar. Component sadece `@Input` referansı değiştiğinde veya içeriden bir event tetiklendiğinde render edilir.

13. **Soru:** `ViewChild` ve `ContentChild` farkı nedir?
    *   **Cevap:** `ViewChild`, component'in kendi template'indeki elemanlara erişir. `ContentChild`, component'in içine dışarıdan projected edilen (`<ng-content>`) elemanlara erişir.

14. **Soru:** `Lazy Loading` nasıl uygulanır?
    *   **Cevap:** Router konfigürasyonunda `loadChildren` veya `loadComponent` kullanılarak yapılır. Modül/Component sadece o rotaya gidildiğinde indirilir.

15. **Soru:** `Interceptor` nedir ve nerelerde kullanılır?
    *   **Cevap:** HTTP isteklerini ve cevaplarını global olarak yakalayıp değiştirmek için kullanılır. Auth token ekleme, hata yakalama, loading spinner gösterme gibi işlerde kullanılır.

16. **Soru:** `Guard` türleri nelerdir?
    *   **Cevap:** `CanActivate` (giriş izni), `CanDeactivate` (çıkış izni - örn: kaydedilmemiş form), `CanMatch` (route eşleşme izni), `Resolve` (veri yükleme).

17. **Soru:** `Reactive Forms` ile `Template Driven Forms` farkı nedir?
    *   **Cevap:** Reactive Forms (kod odaklı) daha esnek, test edilebilir ve immutable'dır. Template Driven (HTML odaklı) daha basittir ama karmaşık senaryolarda yönetimi zordur.

18. **Soru:** `ng-content` (Content Projection) nedir?
    *   **Cevap:** Bir component'in içine dışarıdan HTML içeriği göndermek için kullanılır (React'teki `children` prop'u gibi).

19. **Soru:** `trackBy` (veya `@for track`) neden önemlidir?
    *   **Cevap:** Liste render ederken performans için kritiktir. Angular'ın DOM elemanlarını silip baştan yaratmak yerine, sadece değişenleri güncellemesini sağlar.

20. **Soru:** `Resolver` ne işe yarar?
    *   **Cevap:** Rota açılmadan önce gerekli verinin yüklenmesini sağlar. Veri hazır olmadan sayfa gösterilmez.

### İleri Seviye & Tricky (Zor)

21. **Soru:** **(Tricky)** `constructor` içinde `subscribe` olmak neden kötü bir pratiktir?
    *   **Cevap:** Memory leak riski vardır ve test etmesi zordur. Ayrıca `constructor` çalıştığında component henüz tam olarak initialize olmamış olabilir. `ngOnInit` tercih edilmelidir.

22. **Soru:** **(Tricky)** `OnPush` stratejisi kullanan bir component'te, input değişmediği halde view'ı nasıl güncellersiniz?
    *   **Cevap:** `ChangeDetectorRef.markForCheck()` metodunu manuel çağırarak veya `AsyncPipe` kullanarak (AsyncPipe otomatik `markForCheck` yapar).

23. **Soru:** **(Tricky)** `forkJoin`, `combineLatest` ve `merge` farkları nelerdir?
    *   **Cevap:** `forkJoin` tüm observable'lar tamamlandığında (complete) son değerleri verir (Promise.all gibi). `combineLatest` herhangi biri yeni değer yaydığında hepsinin son değerlerini verir. `merge` sırayla hepsinden gelen değerleri tek bir akışta birleştirir.

24. **Soru:** **(Tricky)** Angular'da `Zone.js` ne işe yarar? `Zone.js` olmadan Angular çalışır mı?
    *   **Cevap:** Zone.js asenkron işlemleri (setTimeout, HTTP) monkey-patch ederek Angular'ın ne zaman change detection yapacağını bilmesini sağlar. Angular 18+ ile `Zone.js` olmadan (Zoneless) `Signals` kullanarak çalışabilir, bu da performansı artırır.

25. **Soru:** **(Tricky)** Bir servisin `providedIn: 'root'` olması ile `providers: [Service]` array'ine eklenmesi arasındaki fark nedir?
    *   **Cevap:** `providedIn: 'root'` servisi Singleton yapar ve Tree-shakable (kullanılmıyorsa bundle'a dahil edilmez) olur. `providers` array'ine eklenirse, o modül/component her yüklendiğinde yeni bir instance oluşabilir (Lazy loading durumunda) ve Tree-shakable değildir.

26. **Soru:** **(Tricky)** `ExpressionChangedAfterItHasBeenCheckedError` hatası neden olur ve nasıl çözülür?
    *   **Cevap:** Angular change detection döngüsünü tamamladıktan sonra, bir değerin hemen değişmesi durumunda (development modda) fırlatılır. Çözüm: Değişikliği `setTimeout` içine almak (tavsiye edilmez) veya akışı düzelterek değişikliğin doğru lifecycle'da olmasını sağlamaktır.

27. **Soru:** **(Tricky)** `switchMap`, `mergeMap`, `concatMap` ve `exhaustMap` farkları nedir? (Flattening Operators)
    *   **Cevap:**
        *   `switchMap`: Yeni istek gelince öncekini iptal eder (Arama kutusu).
        *   `mergeMap`: Hepsini paralel işler (Sıra önemsiz).
        *   `concatMap`: Sırayla işler, biri bitmeden diğerine geçmez (Sıralı işlemler).
        *   `exhaustMap`: Mevcut işlem bitene kadar yeni gelenleri yoksayar (Login butonu).

28. **Soru:** **(Tricky)** Angular Universal (SSR) ile Hydration nedir?
    *   **Cevap:** SSR, HTML'in sunucuda oluşturulup gönderilmesidir. Hydration, tarayıcıya gelen statik HTML'in üzerine Angular'ın JS event listener'larını bağlayarak sayfayı "canlandırmasıdır". Angular 16+ ile "Non-destructive Hydration" gelmiştir, yani DOM silinip tekrar yapılmaz, mevcut DOM kullanılır (Flicker olmaz).

29. **Soru:** **(Tricky)** Circular Dependency (Döngüsel Bağımlılık) hatası alırsanız nasıl çözersiniz?
    *   **Cevap:** Genelde mimari hatasıdır. Ortak kodları `SharedModule` veya ayrı bir servise taşıyarak çözülür. Mecbur kalınırsa `forwardRef(() => Component)` kullanılabilir.

30. **Soru:** **(Tricky)** `HostListener` ve `HostBinding` ne işe yarar?
    *   **Cevap:** `HostListener` component'in host elementindeki eventleri (click, scroll) dinler. `HostBinding` host elementin özelliklerini (class, style) değiştirmek için kullanılır.
