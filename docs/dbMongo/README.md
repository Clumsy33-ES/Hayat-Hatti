# Hayat Hattı – MongoDB Atlas Kullanımı

Bu branch, Hayat Hattı projesinde kullanılan *MongoDB Atlas* veritabanı
yapısını ve NoSQL tarafındaki veri modelini göstermek amacıyla oluşturulmuştur.

MongoDB, özellikle *BLE, sensör, sinyal ve log* gibi
şeması esnek ve yüksek hacimli veriler için tercih edilmiştir.

---

## Kullanım Amacı

MongoDB Atlas aşağıdaki veri türleri için kullanılmaktadır:

- BLE cihazlarından gelen sinyal verileri
- SOS / afet kayıtları
- Zaman bazlı ve konum bazlı olaylar
- Log ve izleme verileri

Bu yapı sayesinde ilişkisel olmayan ve hızlı büyüyen veriler
performanslı şekilde saklanabilmektedir.

---

## Koleksiyon Yapısı

### signals
Afet ve SOS bildirimleri için kullanılan ana koleksiyon.

Örnek alanlar:
- device_signal_id
- type (deprem, yangın, sel vb.)
- timestamp
- created_at
- lat, lon, accuracy_m
- notes
- attachments
- user_id (opsiyonel)

---

## İndeksleme (Performance)

Sık kullanılan alanlar için *MongoDB index* yapıları oluşturulmuştur:

- _id → otomatik primary key
- device_signal_id → *unique*
- timestamp → zaman bazlı sorgular için
- user_id + created_at → kullanıcıya göre son kayıtlar
- type + created_at → afet türüne göre filtreleme

Bu sayede yüksek veri hacminde bile hızlı sorgulama hedeflenmiştir.

---

## Notlar

- MongoDB koleksiyonları, backend ilk veri eklediğinde otomatik olarak oluşabilir.
- Hassas kullanıcı bilgileri MongoDB tarafında tutulmamaktadır.
- MongoDB Atlas, PostgreSQL (Neon) ile birlikte hibrit veritabanı mimarisi içinde kullanılmaktadır.
