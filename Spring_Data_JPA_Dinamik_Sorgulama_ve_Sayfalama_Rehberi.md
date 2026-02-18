# Spring Data JPA: İleri Seviye Dinamik Sorgulama ve Sayfalama Rehberi

---

## 1. Giriş: Neden Standart `@Query` Yetmez?
Geleneksel repository metodları (`findByCategoryAndPrice...`) parametre sayısı arttıkça "metot ismi cehennemine" döner. Specification'lar ise **Criteria API** kullanarak çalışma anında (runtime) SQL oluşturmanızı sağlar.

---

## 2. PageRequestDTO: Sayfalama Standartı
Kurumsal projelerde sayfalama parametrelerini her seferinde Controller'da tek tek karşılamak yerine bir DTO içinde toplarız.

```java
package com.example.dto;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
public class PageRequestDTO {
    private int pageNo = 0;
    private int pageSize = 10;
    private Sort.Direction sortDirection = Sort.Direction.DESC;
    private String sortBy = "id";

    public Pageable getPageable() {
        return PageRequest.of(pageNo, pageSize, sortDirection, sortBy);
    }
} 
```

## 3. Dinamik Filtreleme Mimarisi

Kullanıcıdan gelen filtreleri dinamik bir listeye çevirmek için `SearchCriteria` yapısını kullanırız.

### A. SearchCriteria & Operation


```java
@Data
@AllArgsConstructor
public class SearchCriteria {
    private String key;         // Kolon adı
    private Object value;       // Değer
    private SearchOperation operation; // İşlem (EQUALS, LIKE, vb.)
}

public enum SearchOperation {
    EQUALS, GREATER_THAN, LESS_THAN, LIKE, IN
}
```

### B. Generic Specification

Her entity için ayrı bir sınıf yazmak yerine, projenin her yerinde çalışan bu Generic yapıyı kullanın:

```java
public class GenericSpecification<T> implements Specification<T> {
    private final SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return switch (criteria.getOperation()) {
            case EQUALS -> builder.equal(root.get(criteria.getKey()), criteria.getValue());
            case LIKE -> builder.like(builder.lower(root.get(criteria.getKey())), 
                         "%" + criteria.getValue().toString().toLowerCase() + "%");
            case GREATER_THAN -> builder.greaterThan(root.get(criteria.getKey()), 
                                 criteria.getValue().toString());
            case LESS_THAN -> builder.lessThan(root.get(criteria.getKey()), 
                              criteria.getValue().toString());
            default -> null;
        };
    }
}
```

---

## 4. İleri Seviye: Fetch Join ve N+1 Çözümü

Specification kullanırken ilişkili nesneleri (örn: Ürünün Kategorisi) çekmek istediğinizde Hibernate her satır için yeni bir SELECT atar (N+1 Problemi). Bunu önlemek için `fetch join` şarttır.

```java
public static <T> Specification<T> withFetch(List<String> associations) {
    return (root, query, cb) -> {
        // Count sorgularında (sayfalama için yapılan) fetch yapılmaz!
        if (Long.class != query.getResultType()) {
            for (String relation : associations) {
                root.fetch(relation, JoinType.LEFT);
            }
        }
        return null;
    };
}
```

---

## 5. Gerçek Hayat Senaryosu: Gelişmiş Ürün Arama

E-ticaret sisteminde kullanıcı; kategori seçebilir, fiyat aralığı girebilir ve ürün isminde arama yapabilir.

### Service Katmanı


```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Page<ProductDTO> advancedSearch(String name, Double minPrice, String category, PageRequestDTO pageRequest) {
        Specification<Product> spec = Specification.where(null);

        if (name != null) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name, SearchOperation.LIKE)));
        }
        if (minPrice != null) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("price", minPrice, SearchOperation.GREATER_THAN)));
        }
        // İlişkili tablo (Join) örneği
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("category").get("name"), category));
        }

        // Performans için Fetch Join ekle
        spec = spec.and(withFetch(List.of("category", "seller")));

        return productRepository.findAll(spec, pageRequest.getPageable())
                .map(ProductDTO::new);
    }
}
```

---

## 6. Kritik Profesyonel İpuçları

1. **Count Query Kontrolü:** `root.fetch` yaparken `query.getResultType()` kontrolü yapmazsanız, Spring Data sayfalama yaparken `Long` dönen count sorgusunda hata fırlatır.
    
2. **CriteriaBuilder vs JPQL:** Specification'lar tip güvenlidir. Eğer bir kolon ismini yanlış yazarsanız, IDE'niz sizi (Metamodel kullanıyorsanız) uyarır.
    
3. **Immutability:** `spec.and()` metodu yeni bir instance döner. `spec = spec.and(...)` şeklinde atama yapmayı unutmayın.
    
4. **Security:** Kullanıcıdan gelen parametreleri doğrudan `key` olarak almayın. Sadece izin verdiğiniz (Whitelist) alanların sorgulanmasına izin verin.