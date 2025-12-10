import os
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase

# **BU URL'İN KESİNLİKLE DOĞRU VE TAM OLDUĞUNDAN EMİNİZ.**
# (Driver: +psycopg, Güvenlik: ?sslmode=require, Pooler: aws-1...)
DATABASE_URL = "postgresql+psycopg://postgres.osptrzeugafjehxuvqrf:hayathatti2025@aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres?sslmode=require"

engine = create_async_engine(
    DATABASE_URL,
    future=True,
    echo=False,
    pool_pre_ping=True, 
    pool_size=10,        
    max_overflow=20,     
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
    pass