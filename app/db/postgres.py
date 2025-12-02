import os
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase




DATABASE_URL = os.getenv("DATABASE_URL", "postgresql+asyncpg://postgres:password@localhost:5432/afet")

engine = create_async_engine(
    DATABASE_URL,
    future=True,
    echo=False,
    pool_pre_ping=True,
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
