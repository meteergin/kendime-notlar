# Java Geliştiricileri İçin Modern React: Enterprise Rehberi

Bu rehber, bir Java Backend geliştiricisi için özel olarak hazırlanmıştır. Amaç, Java dünyasındaki kurumsal mimari ve desen bilgilerini, modern React ekosistemine en doğru şekilde transfer etmektir.

---

## İçindekiler
1. [Zihinsel Model Değişimi: Java vs React](#1-zihinsel-model-değişimi-java-vs-react)
2. [Modern Kurulum ve Tooling](#2-modern-kurulum-ve-tooling)
3. [Çekirdek Kavramlar](#3-çekirdek-kavramlar)
4. [Hooks: React'ın Motoru](#4-hooks-reactın-motoru)
5. [Enterprise State Management](#5-enterprise-state-management)
6. [Data Fetching & Caching](#6-data-fetching--caching)
7. [Routing & Navigation](#7-routing--navigation)
8. [Form Yönetimi ve Validasyon](#8-form-yönetimi-ve-validasyon)
9. [Test ve Kalite](#9-test-ve-kalite)

---

## 1. Zihinsel Model Değişimi: Java vs React

Java dünyasında alıştığımız **Imperative (Emirsel)** ve **Nesne Yönelimli (OOP)** yaklaşımdan, **Declarative (Bildirimsel)** ve **Fonksiyonel** bir dünyaya geçiş yapıyoruz.

### 1.1. Imperative vs Declarative
*   **Java (Swing/JSF - Imperative):** "Butona tıklandığında, label'ın text'ini 'Merhaba' yap." (DOM'u/UI'ı doğrudan manipüle edersiniz).
*   **React (Declarative):** "UI, State'in bir fonksiyonudur (`UI = f(state)`). State 'Merhaba' olduğunda UI böyle görünmelidir." (Siz state'i değiştirirsiniz, React DOM'u günceller).

### 1.2. Class vs Function
Eskiden React'ta Class componentler vardı (Java class'larına benzer). Artık **sadece Fonksiyonel Componentler** kullanıyoruz.
*   **Java:** `public class UserService { ... }` (State ve behavior bir arada).
*   **React:** `const UserProfile = (props) => { ... }` (Sadece render mantığı ve hook'lar).

### 1.3. Virtual DOM
Java'daki Garbage Collector gibi düşünebilirsiniz. Siz bellekte (Virtual DOM) değişiklik yaparsınız, React bunu en optimize şekilde gerçek DOM'a yansıtır (Reconciliation). Manuel DOM manipülasyonu (jQuery tarzı) **yasaktır**.

---

## 2. Modern Kurulum ve Tooling

`create-react-app` (CRA) artık **deprecated** (kullanımdan kalktı). Modern standart **Vite**'dır.

### 2.1. Neden Vite?
Java dünyasındaki Maven/Gradle build sürelerini düşünün. Webpack (CRA'nın kullandığı) çok yavaştır. Vite, Go ile yazılmış `esbuild` kullanır ve inanılmaz hızlıdır (Hot Module Replacement - HMR anlıktır).

### 2.2. TypeScript (Strict Mode)
Java'daki tip güvenliğini (Type Safety) frontend'e getirir. Enterprise projelerde **zorunludur**.
*   `interface` ve `type` kullanımı Java'daki `interface` ve `POJO`'lara benzer.

**Kurulum Komutu:**
```bash
npm create vite@latest my-enterprise-app -- --template react-ts
cd my-enterprise-app
npm install
npm run dev
```

---

## 3. Çekirdek Kavramlar

### 3.1. JSX (JavaScript XML)
Java'daki JSP veya JSF Facelets'e benzer ama daha güçlüdür. JavaScript'in içine HTML yazarız.

```tsx
// Java'daki String concatenation yerine Template Literals
const title = "Dashboard";
return <h1>{title}</h1>; // Süslü parantez içine her türlü JS ifadesi girer.
```

### 3.2. Components (Bileşenler)
Uygulamanın yapı taşlarıdır. Java'daki "Class"lar gibi düşünebilirsiniz ama fonksiyon olarak tanımlanır.

```tsx
// UserCard.tsx
interface UserCardProps {
  name: string;
  role: "ADMIN" | "USER"; // Enum benzeri Union Type
}

// Props: Java metod argümanları gibidir. Immutable'dır (değiştirilemez).
export const UserCard = ({ name, role }: UserCardProps) => {
  return (
    <div className="card">
      <h2>{name}</h2>
      <p>Role: {role}</p>
    </div>
  );
};
```

### 3.3. Props vs State
*   **Props:** Dışarıdan gelen parametreler. (Java: Metod parametreleri). Component içinde değiştirilemez (`final` gibidir).
*   **State:** Component'in iç hafızası. (Java: Class instance variables / fields). Değiştiğinde component **yeniden render** olur.

---

## 4. Hooks: React'ın Motoru

React 16.8 ile gelen Hooks, class componentlerin yerini aldı. "Hook", React özelliklerine (state, lifecycle) "kanca atmak" demektir.

### 4.1. `useState` (Local State)
Java'daki bir field'a değer atamak ve getter/setter kullanmak gibidir.

```tsx
import { useState } from 'react';

const Counter = () => {
  // [değer, setterMetodu] = useState(başlangıçDeğeri)
  const [count, setCount] = useState<number>(0);

  const increment = () => {
    // setCount(count + 1); // ASLA count = count + 1 yapmayın!
    setCount((prev) => prev + 1); // Best practice: callback kullanımı (Thread-safe gibi düşünün)
  };

  return <button onClick={increment}>Count: {count}</button>;
};
```

### 4.2. `useEffect` (Side Effects & Lifecycle)
Java'daki `@PostConstruct`, `@PreDestroy` veya bir event listener gibi çalışır.
*   API çağrıları, subscriptionlar, manuel DOM işlemleri burada yapılır.

```tsx
import { useEffect } from 'react';

const UserProfile = ({ userId }: { userId: string }) => {
  
  useEffect(() => {
    // 1. Component mount olduğunda veya userId değiştiğinde çalışır.
    console.log("Fetching data for", userId);
    
    const connection = connectToSocket(userId);

    // 2. Cleanup function (@PreDestroy)
    // Component unmount olmadan hemen önce veya effect tekrar çalışmadan önce çalışır.
    return () => {
      console.log("Disconnecting...");
      connection.disconnect();
    };
  }, [userId]); // Dependency Array: Bu array içindeki değerler değişirse effect tekrar çalışır.

  return <div>User Profile</div>;
};
```

### 4.3. `useMemo` & `useCallback` (Performance)
Java'daki Caching veya Memoization gibidir. Gereksiz hesaplamaları önler.

*   **`useMemo`:** Bir **değeri** cache'ler. (Hesaplama sonucu).
*   **`useCallback`:** Bir **fonksiyonu** cache'ler. (Referans eşitliği için).

```tsx
const expensiveCalculation = useMemo(() => {
  return heavyMathOperation(data);
}, [data]); // Sadece data değişirse tekrar hesapla.
```

### 4.4. Custom Hooks (Service Layer)
Java'daki "Service" veya "Utility" class mantığını React'a taşımanın yoludur. Logic'i UI'dan ayırır.

```tsx
// useUser.ts (Custom Hook)
export const useUser = (userId: string) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUser(userId).then(u => {
      setUser(u);
      setLoading(false);
    });
  }, [userId]);

  return { user, loading };
};

// Component içinde kullanımı
const Profile = () => {
  const { user, loading } = useUser("123"); // Logic tamamen soyutlandı.
  if (loading) return <div>Loading...</div>;
  return <div>{user.name}</div>;
};
```

---

## 5. Enterprise State Management

Java'da Spring Bean'leri (Singleton) veya Database kullanarak global state yönetiriz. React'ta ise durum biraz farklıdır.

### 5.1. Prop Drilling Problemi
Veriyi en tepeden en aşağıya parametre olarak geçmek (Parent -> Child -> Grandchild) yönetilemez hale gelir.

### 5.2. Context API (Dependency Injection)
React'ın yerleşik DI mekanizmasıdır. Veriyi "yayınlar" (Provider) ve ihtiyacı olan component "tüketir" (useContext).

```tsx
// UserContext.tsx
import { createContext, useContext, useState } from 'react';

// 1. Context oluştur
const UserContext = createContext<User | null>(null);

// 2. Provider oluştur (Spring @Configuration gibi)
export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  return <UserContext.Provider value={{ user, setUser }}>{children}</UserContext.Provider>;
};

// 3. Hook ile tüket (@Autowired gibi)
export const useUserContext = () => useContext(UserContext);
```

### 5.3. Zustand (Modern & Basit)
Redux (Java dünyasında çok popülerdi ama çok boilerplate kodu var) yerine artık **Zustand** öneriliyor. Çok daha basit ve performanslıdır.

```tsx
import { create } from 'zustand';

interface AuthState {
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
}

// Global Store (Singleton Service gibi)
export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  login: (token) => set({ token }),
  logout: () => set({ token: null }),
}));

// Kullanımı
const LoginButton = () => {
  const login = useAuthStore((state) => state.login); // Sadece login fonksiyonunu seçer (Selector)
  return <button onClick={() => login("xyz")}>Login</button>;
};
```

---

## 6. Data Fetching & Caching

**ÖNEMLİ:** `useEffect` içinde `fetch` yapmak amatörce kabul edilir (Race condition, caching yok, loading state manuel).

### 6.1. TanStack Query (React Query)
Backend'den gelen veriyi (Server State) yönetmek için endüstri standardıdır. Java'daki Hibernate L2 Cache + Spring Data gibi düşünebilirsiniz.

*   **Otomatik Caching:** Aynı veriyi tekrar tekrar çekmez.
*   **Background Updates:** Kullanıcı başka tab'e gidip gelince veriyi tazeler.
*   **Deduping:** Aynı anda 5 component aynı veriyi isterse sadece 1 request atar.

```tsx
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// Service call
const fetchTodos = async () => {
  const res = await fetch('/api/todos');
  return res.json();
};

const TodoList = () => {
  // useQuery(uniqueKey, fetchFunction)
  const { data, isLoading, error } = useQuery({
    queryKey: ['todos'],
    queryFn: fetchTodos,
    staleTime: 1000 * 60 * 5, // 5 dakika boyunca veri taze kabul edilir (Cache)
  });

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error!</div>;

  return (
    <ul>
      {data.map(todo => <li key={todo.id}>{todo.title}</li>)}
    </ul>
  );
};
```

---

## 7. Routing & Navigation

Single Page Application (SPA) mantığında sayfa yenilenmez, sadece URL ve içerik değişir.

### 7.1. React Router v6
Standart routing kütüphanesidir.

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        
        {/* Protected Route (Interceptor/Filter mantığı) */}
        <Route element={<AuthGuard />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/settings" element={<Settings />} />
        </Route>

        {/* 404 */}
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
};
```

---

## 8. Form Yönetimi ve Validasyon

Java'da JSF veya Spring MVC form binding kullanırdık. React'ta "Controlled Components" (state ile input bağlama) performans sorunlarına yol açabilir (her tuş basımında render).

### 8.1. React Hook Form (Uncontrolled)
Performans odaklıdır. Input değerlerini React state'inde değil, DOM'da tutar (ref ile). Sadece submit anında veriyi alır.

### 8.2. Zod (Schema Validation)
Java'daki Bean Validation (Hibernate Validator) gibidir. Şema tabanlı validasyon sağlar.

```tsx
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

// 1. Şema Tanımı (DTO gibi)
const schema = z.object({
  email: z.string().email("Geçersiz email formatı"),
  age: z.number().min(18, "18 yaşından büyük olmalısınız"),
});

// TypeScript tipi çıkarımı
type FormData = z.infer<typeof schema>;

const RegistrationForm = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema), // Zod ile entegrasyon
  });

  const onSubmit = (data: FormData) => {
    console.log("Valid Data:", data);
    // API call here
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <input {...register("email")} placeholder="Email" />
        {errors.email && <span className="error">{errors.email.message}</span>}
      </div>
      
      <div>
        <input type="number" {...register("age", { valueAsNumber: true })} placeholder="Age" />
        {errors.age && <span className="error">{errors.age.message}</span>}
      </div>

      <button type="submit">Kaydet</button>
    </form>
  );
};
```

---

## 9. Test ve Kalite

Java'da JUnit ve Mockito neyse, React'ta Vitest ve React Testing Library odur.

### 9.1. Unit Testing (Vitest)
Jest'in daha hızlı ve modern alternatifidir (Vite ile native çalışır).

### 9.2. Component Testing (React Testing Library)
Implementation detaylarını değil, kullanıcı davranışını test eder.
*   **Yanlış:** `component.state.count === 1` kontrolü yapmak.
*   **Doğru:** "Ekranda 'Count: 1' yazısını görüyor muyum?" kontrolü yapmak.

```tsx
// Counter.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import Counter from './Counter';

test('increments counter on click', () => {
  render(<Counter />);
  
  const button = screen.getByText(/Count: 0/i);
  fireEvent.click(button);
  
  expect(screen.getByText(/Count: 1/i)).toBeInTheDocument();
});
```

---

## Özet: Java Geliştiricisi İçin Yol Haritası

1.  **Unutun:** `class`, `this`, `inheritance`, `imperative DOM manipulation`.
2.  **Öğrenin:** `const`, `arrow functions`, `hooks`, `declarative UI`.
3.  **Tooling:** Vite, TypeScript, ESLint.
4.  **State:** Zustand (Global), React Query (Server), React Hook Form (Form).
5.  **Mimari:** Logic'i Custom Hook'lara taşıyın, UI'ı aptal (dumb) tutun.

# React Mülakat Soruları ve Cevapları (Java Geliştiricileri İçin)

Bu doküman, 15+ yıl deneyimli bir Java geliştiricisinin React mülakatlarına hazırlanması için özel olarak derlenmiştir. Sorular basitten zora ve "tricky" (şaşırtmalı) senaryolara doğru ilerler.

---

## Bölüm 1: Temel Kavramlar (Basic Concepts)

### 1. React nedir ve neden "Declarative"dir?
**Cevap:**
React, kullanıcı arayüzleri oluşturmak için kullanılan bir JavaScript kütüphanesidir.
*   **Imperative (Java Swing/jQuery):** *Nasıl* yapılacağını söylersiniz. (Örn: "Butonu bul, rengini kırmızı yap, text'i sil").
*   **Declarative (React):** *Ne* olması gerektiğini söylersiniz. (Örn: "Butonun rengi `isError` true ise kırmızıdır"). React, DOM güncellemelerini kendisi halleder.

### 2. Virtual DOM nedir ve nasıl çalışır?
**Cevap:**
Virtual DOM, gerçek DOM'un bellekteki hafif bir kopyasıdır.
1.  State değiştiğinde React yeni bir Virtual DOM ağacı oluşturur.
2.  Eski ağaç ile yeni ağacı karşılaştırır (Diffing).
3.  Sadece değişen kısımları gerçek DOM'a yansıtır (Reconciliation).
Bu, her değişiklikte tüm sayfayı yeniden çizmekten (Java JSP/JSF postback) çok daha performanslıdır.

### 3. JSX tarayıcıda nasıl çalışır?
**Cevap:**
Tarayıcılar JSX'i (JavaScript XML) doğrudan anlayamaz. Build aşamasında (Vite/Babel), JSX kodları `React.createElement()` fonksiyon çağrılarına dönüştürülür. Bu fonksiyonlar JavaScript objeleri (Virtual DOM node'ları) döner.

### 4. Component Lifecycle (Yaşam Döngüsü) fonksiyonel componentlerde nasıldır?
**Cevap:**
Class componentlerdeki `componentDidMount`, `componentDidUpdate`, `componentWillUnmount` yerine `useEffect` hook'u kullanılır.
*   **Mount:** `useEffect(() => { ... }, [])` (Boş array)
*   **Update:** `useEffect(() => { ... }, [prop])` (Dependency array dolu)
*   **Unmount:** `useEffect(() => { return () => { ... } }, [])` (Cleanup function)

### 5. State ve Props arasındaki fark nedir?
**Cevap:**
*   **Props (Properties):** Dışarıdan (Parent'tan) gelir. Immutable'dır (değiştirilemez). Java'daki metod argümanlarına benzer.
*   **State:** Component'in kendi iç durumudur. Mutable'dır (değiştirilebilir, ama setter ile). Java'daki private class field'larına benzer.

### 6. "Key" prop'u neden önemlidir?
**Cevap:**
Listeleri render ederken React'ın hangi elemanın değiştiğini, eklendiğini veya silindiğini anlaması için benzersiz bir `key`'e ihtiyacı vardır.
*   **Yanlış:** Array index'i kullanmak (`key={index}`). (Sıralama değişirse bug oluşur).
*   **Doğru:** Database ID'si kullanmak (`key={user.id}`).

### 7. Controlled vs Uncontrolled Component farkı nedir?
**Cevap:**
*   **Controlled:** Form elemanının değeri (`value`) React state'i tarafından yönetilir. Her değişimde (`onChange`) state güncellenir. (Daha yetenekli ama daha çok render).
*   **Uncontrolled:** Değer DOM'un kendisinde tutulur. `useRef` ile erişilir. (Daha performanslı, React Hook Form bunu kullanır).

### 8. Prop Drilling nedir ve nasıl çözülür?
**Cevap:**
Verinin parent'tan çok alt seviyedeki child'a taşınması için aradaki kullanmayan componentlerden geçirilmesidir.
**Çözüm:** Context API, Redux/Zustand (Global State) veya Component Composition (Slot pattern).

### 9. React.Fragment (veya `<>`) neden kullanılır?
**Cevap:**
React componentleri tek bir parent element dönmek zorundadır. Gereksiz `<div>` cehenneminden (div soup) kaçınmak için DOM'a ekstra node eklemeyen Fragment (`<>...</>`) kullanılır.

### 10. `super(props)` (Class component) ne işe yarardı?
**Cevap:**
(Tarihçe sorusu) Java'daki `super()` ile aynıdır. Parent class'ın (`React.Component`) constructor'ını çağırır. Bunu yapmazsanız `this.props` undefined olurdu. Fonksiyonel componentlerde buna gerek yoktur.

---

## Bölüm 2: Hooks & Modern React

### 11. `useEffect` dependency array boş `[]` ise ne olur, yoksa ne olur?
**Cevap:**
*   `[]` (Boş): Sadece **mount** anında 1 kere çalışır.
*   (Yok): **Her render**'da çalışır. (Tehlikelidir, sonsuz döngüye girebilir).
*   `[data]`: Sadece `data` değiştiğinde çalışır.

### 12. `useLayoutEffect` vs `useEffect` farkı nedir?
**Cevap:**
*   `useEffect`: Render ekrana yansıdıktan **sonra** çalışır (Asenkron). UI bloklanmaz.
*   `useLayoutEffect`: Render ekrana yansımadan **önce** çalışır (Senkron). DOM ölçümleri (width/height) alıp UI titremesini (flicker) önlemek için kullanılır.

### 13. `useMemo` ve `useCallback` farkı nedir?
**Cevap:**
İkisi de memoization (caching) yapar.
*   `useMemo`: Bir **değeri** (hesaplama sonucunu) cache'ler. `const value = useMemo(() => compute(a), [a])`
*   `useCallback`: Bir **fonksiyon referansını** cache'ler. `const fn = useCallback(() => { ... }, [])`. Child component'e prop olarak fonksiyon geçerken gereksiz re-render'ı önlemek için şarttır.

### 14. `useRef` hangi durumlarda kullanılır?
**Cevap:**
1.  DOM elemanlarına doğrudan erişmek için (`inputRef.current.focus()`).
2.  Re-render tetiklemeden değer saklamak için (Mutable variable). `let` değişkenleri her render'da sıfırlanır, `useRef` değerini korur ama değişince render tetiklemez.

### 15. Custom Hook nedir? Neden yazarız?
**Cevap:**
Logic'i UI'dan ayırmak ve tekrar kullanmak (DRY) için. Java'daki "Service" veya "Utility" class'lar gibidir. İsimleri `use` ile başlamalıdır.

### 16. `useReducer` ne zaman `useState` yerine tercih edilmeli?
**Cevap:**
State mantığı karmaşıksa (bir sonraki state bir öncekine bağlıysa) veya birden fazla alt-değer içeriyorsa. Redux pattern'ini component içinde uygulamayı sağlar.

### 17. Context API performans sorunları nelerdir?
**Cevap:**
Context Provider'daki value değiştiğinde, o context'i tüketen **tüm** componentler re-render olur.
**Çözüm:** State'i parçalamak, `memo` kullanmak veya Zustand gibi selector destekleyen kütüphaneler kullanmak.

### 18. React'ta "Lifting State Up" ne demektir?
**Cevap:**
İki kardeş (sibling) component aynı veriye ihtiyaç duyuyorsa, state'i ortak parent'larına taşıyıp props ile aşağı indirmektir.

### 19. Strict Mode ne işe yarar?
**Cevap:**
Development ortamında olası hataları yakalamak için componentleri **iki kez** render eder (ve effectleri iki kez çalıştırır). Bu sayede side-effect temizliği (cleanup) yapılmamışsa hemen fark edilir. Production'da çalışmaz.

### 20. Portal nedir?
**Cevap:**
Bir componenti DOM hiyerarşisinde bulunduğu yerin dışına (örneğin `body`'nin sonuna) render etmektir. Modal, Tooltip ve Dropdown yapımında `z-index` sorunlarını aşmak için kullanılır.

---

## Bölüm 3: Advanced & Tricky (Zor Sorular)

### 21. `setState` (veya `setCount`) asenkron mudur?
**Cevap:**
Evet, React state güncellemelerini performans için **batch** (toplu) yapar. Yani `setCount(c + 1)` dediğinizde `count` değeri bir sonraki satırda hemen güncellenmez.
*   **Tricky:** Eğer hemen güncellenmiş değere ihtiyacınız varsa `useEffect` kullanmalısınız veya setter fonksiyonuna callback geçmelisiniz (`setCount(prev => prev + 1)`).

### 22. `useEffect` içinde tanımlanan bir fonksiyonu neden dependency array'e eklemeliyiz? (Closure Trap)
**Cevap:**
JavaScript'teki **Closure** mantığı yüzünden. Eğer effect içinde dışarıdaki bir değişkeni kullanırsanız ve onu dependency'e eklemezseniz, effect o değişkenin **ilk render edildiği andaki** (eski/stale) değerini hatırlar.
```javascript
useEffect(() => {
  console.log(count); // Dependency'e [count] eklemezsen hep 0 yazar.
}, []); 
```

### 23. React Fiber mimarisi nedir?
**Cevap:**
React 16 ile gelen yeni render motorudur. Ana amacı **Incremental Rendering**'dir. Render işlemini küçük parçalara (unit of work) böler ve tarayıcının ana thread'ini bloklamadan (kullanıcı arayüzünü dondurmadan) araya girip yüksek öncelikli işleri (animasyon, input) yapabilir.

### 24. Bir componentin re-render olmasını nasıl engellersiniz?
**Cevap:**
1.  **`React.memo`:** Componenti sarmalar. Props değişmediyse render etmez.
2.  **`useMemo`:** Pahalı hesaplamaları cache'ler.
3.  **`useCallback`:** Fonksiyon referanslarını sabit tutarak child componentlerin gereksiz render olmasını engeller.
4.  **State'i aşağı itmek:** State'i sadece ihtiyaç duyan en alt componente taşımak.

### 25. Higher Order Component (HOC) vs Render Props vs Hooks
**Cevap:**
*   **HOC:** Bir componenti alıp yeni bir component dönen fonksiyon (`withAuth(Component)`). (Eski yöntem, "Wrapper Hell" yaratır).
*   **Render Props:** Prop olarak fonksiyon alan component (`<Mouse render={pos => ...} />`).
*   **Hooks:** Logic'i paylaşmanın en modern ve temiz yolu. HOC ve Render Props'un yerini almıştır.

### 26. `capture` value (Closure) sorunu nedir?
**Cevap:**
Özellikle `setTimeout` veya asenkron işlemlerde görülür. Fonksiyonel componentlerdeki değişkenler, o render'a ait sabit değerlerdir (Snapshot).
```javascript
const [count, setCount] = useState(0);
const handleClick = () => {
  setTimeout(() => {
    alert(count); // 3 saniye sonra count değişmiş olsa bile, tıklandığı andaki değeri (0) gösterir.
  }, 3000);
};
// Çözüm: useRef kullanmak (Mutable ref her zaman güncel değeri tutar).
```

### 27. Error Boundary nedir? Fonksiyonel componentlerde nasıl kullanılır?
**Cevap:**
Child componentlerdeki JavaScript hatalarını yakalayıp tüm uygulamanın çökmesini (beyaz ekran) engelleyen yapıdır.
**Tricky:** Sadece **Class Component** olarak yazılabilir (`componentDidCatch`). Fonksiyonel componentlerde kullanmak için `react-error-boundary` gibi kütüphaneler veya bir tane class wrapper gerekir.

### 28. React'ta "Synthetic Event" nedir?
**Cevap:**
React, tarayıcılar arasındaki uyumsuzlukları gidermek için native DOM eventlerini (örn: `onclick`) sarmalar ve `SyntheticEvent` (örn: `onClick`) sunar. Bu sayede eventler tüm tarayıcılarda tutarlı çalışır.

### 29. Code Splitting ve `React.lazy` nasıl çalışır?
**Cevap:**
Büyük bir bundle dosyasını (tek bir JS dosyası) küçük parçalara (chunks) bölmektir. `React.lazy` ve `Suspense` kullanarak, kullanıcı sadece o sayfaya/componente ihtiyaç duyduğunda ilgili JS dosyası indirilir.
```javascript
const OtherComponent = React.lazy(() => import('./OtherComponent'));
```

### 30. Senaryo: Sayfada çok yavaş bir hesaplama var ve UI donuyor. Nasıl çözersin?
**Cevap:**
1.  **`useMemo`:** Hesaplamayı cache'lemek.
2.  **Web Workers:** Hesaplamayı ana thread'den (UI thread) ayırıp arka planda yapmak.
3.  **`useTransition` / `useDeferredValue` (React 18):** React'a bu işlemin "düşük öncelikli" olduğunu söylemek. React, UI güncellemelerine öncelik verip hesaplamayı arkaplanda yapar.

