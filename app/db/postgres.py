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
    - username/password varsa güvenle URL-encode eder,
    - query kısmında sslmode=require'ı garanti altına alır.
    """
    url = make_url(raw)

    # 1) Driver'ı psycopg3'e sabitle
    url = url.set(drivername="postgresql+psycopg")

    # 2) Kimlik bilgilerini güvenle encode et
    if url.username:
        url = url.set(username=quote(url.username, safe=""))
    if url.password:
        url = url.set(password=quote(url.password, safe=""))

    # 3) sslmode=require zorlaması (Supabase için kritik)
    query = dict(url.query)
    query.setdefault("sslmode", "require")
    url = url.set(query=query)

    return str(url)

DB_URL = _build_db_url(settings.DATABASE_URL)

# ---- Tek ve nihai Engine ----
engine = create_engine(
    DB_URL,
    pool_pre_ping=True,     # kopan bağlantıları otomatik test et
    pool_recycle=1800,      # 30 dak sonra tazele
    pool_size=5,
    max_overflow=10,
    future=True,
)

# ---- Tek Session factory ----
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
