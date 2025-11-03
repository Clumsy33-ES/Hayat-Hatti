# app/db/postgres.py
from urllib.parse import quote

from sqlalchemy import create_engine
from sqlalchemy.engine import make_url
from sqlalchemy.orm import sessionmaker

from app.core.config import settings


def _build_db_url(raw: str) -> str:
    """
    .env'den gelen DATABASE_URL'i parse eder,
    - driver'ı 'postgresql+psycopg' (psycopg3) olarak zorlar,
    - username/password varsa güvenle URL-encode eder.
    """
    url = make_url(raw)

    # 1) Driver'ı psycopg3'e sabitle
    url = url.set(drivername="postgresql+psycopg")

    # 2) Kimlik bilgilerini güvenle encode et
    if url.username:
        url = url.set(username=quote(url.username, safe=""))
    if url.password:
        url = url.set(password=quote(url.password, safe=""))

    return str(url)


DB_URL = _build_db_url(settings.DATABASE_URL)

# Tek ve nihai Engine
engine = create_engine(
    DB_URL,
    pool_pre_ping=True,   # kopan bağlantıları otomatik test et
    pool_recycle=1800,    # 30 dakikada bir bağlantıyı yenile
    future=True,
    # psycopg3 ile genelde gerekmez; psycopg2'de sorun yaşayanlar için:
    # connect_args={"options": "-c client_encoding=utf8"},
)

# Tek Session factory
SessionLocal = sessionmaker(
    bind=engine,
    autocommit=False,
    autoflush=False,
    expire_on_commit=False,
)

# FastAPI dependency
def get_session():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# (İsteğe bağlı) ilk çalıştırmada tabloları oluşturmak için:
# def init_db():
#     from app.db.models import Base
#     Base.metadata.create_all(bind=engine)

# (Geçici debug için açıp kapatabilirsin)
# print("[DB] SQLAlchemy URL:", engine.url)
# print("[DB] dialect:", engine.dialect.name, "driver:", engine.dialect.driver)
