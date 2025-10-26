from pydantic_settings import BaseSettings

# Uygulamanın merkezi konfigürasyonu
class Settings(BaseSettings):
    # Ortam bilgisi
    ENV: str = "dev"  # "dev", "prod", "test"

    # API prefix
    API_PREFIX: str = "/api"

    # JWT ayarları
    JWT_SECRET: str                 # zorunlu (.env'den gelmeli)
    JWT_ALGORITHM: str = "HS256"    # default HS256
    JWT_EXPIRES_MIN: int = 60       # dakikayla token ömrü

    # Veritabanları
    DATABASE_URL: str              # PostgreSQL async URL (postgresql+asyncpg://...)
    MONGODB_URI: str               # Mongo bağlantı URI
    MONGODB_DB: str                # MongoDB database adı

    # CORS
    CORS_ORIGINS: str = "*"

    # Rate limiting
    RATE_LIMIT: str = "60/minute"

    class Config:
        env_file = ".env"          # tüm bu değerleri .env dosyasından oku


# ÖNEMLİ:
# Bu satır olmazsa settings import edilemez ve main.py patlar.
settings = Settings()
