import os
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase

# Environment variable yoksa fallback olarak Supabase connection string
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql+psycopg://neondb_owner:npg_4GihsT6jDIw@ep-round-shadow-adbf4weh-pooler.c-2.us-east-1.aws.neon.tech:5432/neondb?sslmode=require")

engine = create_async_engine(
    DATABASE_URL,
    future=True,
    echo=False,
    pool_pre_ping=True,   # bağlantıyı koparsa otomatik düzeltir
    pool_size=10,         # Railway için ideal
    max_overflow=20,      # yoğunluk olursa ek bağlantı açar
)

AsyncSessionLocal = async_sessionmaker(
    engine,
    expire_on_commit=False,
    autoflush=False,
    class_=AsyncSession,
)

async def get_async_session() -> AsyncSession:
    async with AsyncSessionLocal() as session:
        yield session

class Base(DeclarativeBase):
    """Tüm SQLAlchemy modellerinin miras alacağı Base sınıfı"""
    pass