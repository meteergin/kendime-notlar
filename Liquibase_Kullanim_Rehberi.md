# Liquibase Kapsamlı Eğitim Rehberi ve Kullanım Senaryoları

## 1. Klasör Yapısı ve Dosya Görevleri

Hiyerarşi şu şekilde çalışır:

* **`db.changelog-master.xml`:** Orkestra şefidir. Hiçbir zaman doğrudan tablo oluşturma kodu içermez. Sadece diğer changelog dosyalarını hangi sırayla çalıştıracağını söyler (`<include>` etiketleri ile).
* **`changes/` Klasörü:** **DDL (Data Definition Language)** işlemleri içindir. Tablo oluşturma, kolon ekleme, index tanımlama gibi yapısal değişiklikler burada tutulur.
    * `db.changelog-0.0.1.xml`: Bu dosya, 0.0.1 versiyonuna ait tüm yapısal değişiklikleri bir araya toplar.
    * `001-create-hello-world-table.xml`: Gerçek atomik değişiklik setidir (ChangeSet).
* **`data/` Klasörü:** **DML (Data Manipulation Language)** işlemleri içindir. Tabloya başlangıç verilerini (seed data) eklemek, config değerlerini girmek veya veri temizliği yapmak için kullanılır.

* **`changes/`**: Yapısal değişiklikler (Tablo, Kolon, Index, Constraint).
* **`data/`**: Konfigürasyon ve başlangıç verileri (Seed Data).
* **`functions/`** (Opsiyonel): PostgreSQL Stored Procedure ve Trigger tanımları.

**Master XML İpucu:**
`<includeAll path="db/changelog/changes/" />` kullanarak klasördeki tüm dosyaları otomatik yükleyebilirsiniz. Ancak sıralama önemliyse, manuel `<include>` kullanmaya devam edin.

---

## 2. Kullanım Senaryoları

Aşağıdaki örnekler, Liquibase XML formatında en çok ihtiyaç duyacağın operasyonları kapsar.

### A. Tablo ve Kolon İşlemleri (DDL)

#### 1. Yeni Tablo Oluşturma

```xml
<changeSet id="20260217-1" author="mete">
    <createTable tableName="employees">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="email" type="varchar(255)">
            <constraints unique="true" nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

#### 2. Mevcut Tabloya Yeni Kolon Ekleme

```xml
<changeSet id="20260217-2" author="mete">
    <addColumn tableName="hello_world">
        <column name="phone_number" type="varchar(20)">
            <constraints nullable="true"/>
        </column>
    </addColumn>
</changeSet>
```

#### 3. Kolon Tipini Değiştirme
Örneğin bir `int` alanı `bigint` yapmak veya `varchar` uzunluğunu artırmak için:

```xml
<changeSet id="20260217-3" author="mete">
    <modifyDataType tableName="hello_world" 
                    columnName="message" 
                    newDataType="varchar(1000)"/>
</changeSet>
```

#### 4. Kolon Silme
```xml
<changeSet id="20260217-4" author="mete">
    <dropColumn tableName="hello_world" columnName="greeting"/>
</changeSet>
```

#### 5. Tablo Silme
```xml
<changeSet id="20260217-5" author="mete">
    <dropTable tableName="old_unused_table"/>
</changeSet>
```

---

### B. Veri İşlemleri (DML)

Veri işlemleri `resources/db.changelog/data` altındaki dosyalarda yapılır.

#### 6. Tabloya Veri Ekleme (Insert)
```xml
<changeSet id="20260217-data-1" author="mete">
    <insert tableName="hello_world">
        <column name="name" value="Merhaba Dunya"/>
        <column name="message" value="Liquibase ile ilk veri girişi"/>
        <column name="version" valueNumeric="1"/>
        <column name="deleted" valueBoolean="false"/>
    </insert>
</changeSet>
```

#### 7. Veri Güncelleme (Update)
Belirli bir kritere uyan kayıtları güncellemek için:

```xml
<changeSet id="20260217-data-2" author="mete">
    <update tableName="hello_world">
        <column name="message" value="Mesaj güncellendi"/>
        <where>id = 1</where>
    </update>
</changeSet>
```

#### 8. Veri Silme (Delete)
```xml
<changeSet id="20260217-data-3" author="mete">
    <delete tableName="hello_world">
        <where>id = 1</where>
    </delete>
</changeSet>
```

---

### C. İlişkiler ve Kısıtlamalar

#### 9. Foreign Key Ekleme
İki tabloyu birbirine bağlamak için:

```xml
<changeSet id="20260217-fk-1" author="mete">
    <addForeignKeyConstraint baseTableName="hello_world" 
                             baseColumnNames="created_by_id" 
                             constraintName="fk_hello_world_user" 
                             referencedTableName="users" 
                             referencedColumnNames="id"/>
</changeSet>
```

---

## 3. PostgreSQL'e Özel Senaryolar

### A. UUID Kullanımı (Primary Key olarak)
Performans ve güvenlik için UUID kullanımı yaygındır.
```xml
<changeSet id="pg-uuid-extension" author="mete">
    <sql>CREATE EXTENSION IF NOT EXISTS "uuid-ossp";</sql>
</changeSet>

<changeSet id="create-user-with-uuid" author="mete">
    <createTable tableName="users">
        <column name="id" type="uuid" defaultValueComputed="uuid_generate_v4()">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="username" type="varchar(50)"/>
    </createTable>
</changeSet>
```

### B. JSONB Kolon Yönetimi
Postgres'in en güçlü özelliklerinden biri JSONB'dir.
```xml
<changeSet id="add-jsonb-column" author="mete">
    <addColumn tableName="hello_world">
        <column name="metadata" type="jsonb">
            <constraints nullable="true"/>
        </column>
    </addColumn>
</changeSet>
```

### C. PostgreSQL ENUM Tipleri
```xml
<changeSet id="create-status-enum" author="mete">
    <sql>CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING');</sql>
    <addColumn tableName="users">
        <column name="status" type="user_status">
            <constraints nullable="false"/>
        </column>
    </addColumn>
</changeSet>
```

---

## 4. İleri Seviye DDL Senaryoları

### Senaryo: Veri Dolu Tabloya NOT NULL Kolon Ekleme (Zero-Downtime)
Eğer tablo doluysa, doğrudan `NOT NULL` kolon eklemek hata verir. Güvenli yolu şudur:
1. Kolonu `NULLable` ekle.
2. Veriyi güncelle (update).
3. Kolonu `NOT NULL` yap.

```xml
<changeSet id="add-safe-not-null-step-1" author="mete">
    <addColumn tableName="hello_world">
        <column name="category" type="varchar(50)"/>
    </addColumn>
</changeSet>

<changeSet id="add-safe-not-null-step-2" author="mete">
    <update tableName="hello_world">
        <column name="category" value="DEFAULT"/>
        <where>category IS NULL</where>
    </update>
</changeSet>

<changeSet id="add-safe-not-null-step-3" author="mete">
    <addNotNullConstraint tableName="hello_world" columnName="category" columnDataType="varchar(50)"/>
</changeSet>
```

### Senaryo: GIN Index Oluşturma (JSONB için)
```xml
<changeSet id="create-gin-index" author="mete">
    <createIndex tableName="hello_world" indexName="idx_hw_metadata_gin">
        <column name="metadata" type="jsonb"/>
    </createIndex>
    <modifySql dbms="postgresql">
        <replace replace="USING btree" with="USING gin"/>
    </modifySql>
</changeSet>
```

---

## 5. İleri Seviye DML Senaryoları

### CSV Dosyasından Veri Yükleme
Büyük verileri XML içinde tek tek yazmak yerine CSV kullanın.
```xml
<changeSet id="load-initial-cities" author="mete">
    <loadData tableName="cities"
              file="db/changelog/data/cities.csv"
              separator=";">
        <column name="id" type="numeric"/>
        <column name="name" type="string"/>
    </loadData>
</changeSet>
```

---

## 6. BEST PRACTICES
1.  **Rollback Tanımlayın:** Özellikle manuel `sql` tagı kullandığınızda mutlaka bir `<rollback>` etiketi ekleyin.
    ```xml
    <changeSet id="manual-sql-example" author="mete">
        <sql>ALTER TABLE hello_world ADD COLUMN secret_code INT;</sql>
        <rollback>
            <sql>ALTER TABLE hello_world DROP COLUMN secret_code;</sql>
        </rollback>
    </changeSet>
    ```
2.  **Context Kullanımı:** `context="prod"` veya `context="test"` kullanarak bazı verilerin sadece test ortamında yüklenmesini sağlayın.
3.  **Schema Kullanımı:** PostgreSQL'de `public` şeması yerine uygulamanıza özel bir şema (örn: `app_schema`) kullanmayı düşünün.
4.  **İsimlendirme Standartları:**
    * Tablo isimleri: `snake_case` ve çoğul (`users`, `orders`).
    * Index isimleri: `idx_tableName_columnName`.
    * FK isimleri: `fk_baseTable_referencedTable`.
5.  **DATABASECHANGELOG'a Müdahale Etme:** Liquibase'in kendi yönetim tablosuna asla manuel SQL ile veri ekleme/silme.
6.  **ChangeSet ID Mantığı:** `id` alanı sadece bir sayı olmak zorunda değildir. Çakışmaları önlemek için `20260217-create-user-table` gibi tarih bazlı isimlendirmeler kullanmak takibi kolaylaştırır.
7.  **Geri Alınamazlık:** Bir ChangeSet bir kez veritabanına uygulandıktan sonra (DATABASECHANGELOG tablosuna kaydedilir), XML üzerinde değişiklik yapmamalısın. Eğer bir hata yaptıysan, yeni bir ChangeSet açarak düzeltme yapmalısın.
8.  **Master XML Dosya Yolları:** Master dosyasında `<include>` yaparken dosya yollarına dikkat et. Spring Boot genellikle `classpath:db/changelog/db.changelog-master.xml` üzerinden okur. Alt dosyaların yolları master dosyasına göre göreceli (relative) olmalıdır.
