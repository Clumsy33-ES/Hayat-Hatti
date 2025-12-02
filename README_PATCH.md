# Afet Backend Patch (FastAPI)

Bu paket, zip'inde tespit ettiğim kritik noktaları düzeltmek için hazırlanmıştır.

## Neler var?
- `app/db/postgres.py`: asyncpg DSN ve sağlam session factory
- `app/db/mongo.py`: Motor client, güvenli DSN ve index initializer
- `app/models/__init__.py`: User/Signal export
- `app/routes/users.py`: Doğru importlar ve sade liste endpoint
- `app/routes/signals.py`: Idempotent create + list (PostgreSQL)
- `app/routes/signals_mongo.py`: Mongo liste endpoint
- `.env.example`: Güvenli örnek ortam değişkenleri

## Uygulama
1. Zip'i çıkarın ve içerikleri proje kökünüzdeki aynı yollara kopyalayın (dosyaların yedeğini alın).
2. `.env` dosyanızı `.env.example`'a göre düzenleyin (repo dışında tutun).
3. Mongo indekslerini uygulama `lifespan`'ınızda `await init_indexes()` ile kurabilirsiniz.
4. Uygulamayı çalıştırın: `uvicorn app.main:app --reload`

Oluşturulma zamanı: 2025-11-06T15:01:22.874619Z
