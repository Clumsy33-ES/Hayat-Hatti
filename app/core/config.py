# app/core/config.py
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # Pydantic v2 settings config
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        extra="ignore",          # .env'deki fazlalıklar patlatmasın
    )

    # ---- App meta
    project_name: str = Field(default="Afet Yardım Backend", alias="PROJECT_NAME")
    version: str = Field(default="1.0.0", alias="VERSION")
    api_prefix: str = Field(default="/api", alias="API_PREFIX")

    # ---- Database
    database_url: str = Field(alias="DATABASE_URL")

    # ---- Mongo (env: MONGO_URI / MONGO_DB_NAME)
    mongo_uri: str = Field(alias="MONGO_URI")
    mongo_db_name: str = Field(alias="MONGO_DB_NAME")

    # ---- JWT
    # İki isimden birini verebilirsin; aşağıda resolve edeceğiz.
    jwt_secret_key: str | None = Field(default=None, alias="JWT_SECRET_KEY")
    jwt_secret: str | None = Field(default=None, alias="JWT_SECRET")
    jwt_algorithm: str = Field(default="HS256", alias="JWT_ALGORITHM")
    access_token_expire_minutes: int | None = Field(default=None, alias="ACCESS_TOKEN_EXPIRE_MINUTES")
    jwt_expires_min: int | None = Field(default=None, alias="JWT_EXPIRES_MIN")

    # ---- CORS & Rate limit & Feature flags
    cors_origins: str = Field(default="*", alias="CORS_ORIGINS")
    rate_limit: str = Field(default="60/minute", alias="RATE_LIMIT")
    enable_ble_sync: bool = Field(default=False, alias="ENABLE_BLE_SYNC")

    # ---------- Backward-compatible properties (UPPER_CASE erişimleri koru) ----------
    @property
    def PROJECT_NAME(self) -> str: return self.project_name
    @property
    def VERSION(self) -> str: return self.version
    @property
    def API_PREFIX(self) -> str: return self.api_prefix

    @property
    def DATABASE_URL(self) -> str: return self.database_url

    @property
    def MONGO_URI(self) -> str: return self.mongo_uri
    @property
    def MONGO_DB_NAME(self) -> str: return self.mongo_db_name

    @property
    def CORS_ORIGINS(self) -> str: return self.cors_origins
    @property
    def RATE_LIMIT(self) -> str: return self.rate_limit
    @property
    def ENABLE_BLE_SYNC(self) -> bool: return self.enable_ble_sync

    # JWT alanlarını tek isimde çöz
    @property
    def JWT_SECRET_KEY(self) -> str:
        # Öncelik JWT_SECRET_KEY; yoksa JWT_SECRET
        return self.jwt_secret_key or (self.jwt_secret or "")

    @property
    def ACCESS_TOKEN_EXPIRE_MINUTES(self) -> int:
        # Öncelik ACCESS_TOKEN_EXPIRE_MINUTES; yoksa JWT_EXPIRES_MIN; ikisi de yoksa 7 gün
        return (
            self.access_token_expire_minutes
            if self.access_token_expire_minutes is not None
            else (self.jwt_expires_min if self.jwt_expires_min is not None else 60 * 24 * 7)
        )

settings = Settings()
