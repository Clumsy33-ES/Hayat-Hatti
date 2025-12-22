# Hayat Hattı – PostgreSQL Veritabanı Tasarımı

Bu branch, Hayat Hattı projesinde kullanılan PostgreSQL veritabanı şemasını
ve ilişkisel veri modelini göstermek amacıyla oluşturulmuştur.

## Kullanılan Yapı
- PostgreSQL (Neon uyumlu)
- users ve signals tabloları
- Foreign Key ile kullanıcı–sinyal ilişkisi

## Tablolar

### users
- Kullanıcı bilgileri tutulur
- email alanı unique
- Şifreler hashlenmiş şekilde saklanır

### signals
- Kullanıcıya bağlı SOS/sinyal kayıtları
- user_id → users.id foreign key ilişkisi
