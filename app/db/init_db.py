"""
Alembic yoksa tablo oluşturma.
Bu modül yalnızca *eksik* tabloları oluşturur; mevcut olanlara dokunmaz.
"""
from sqlalchemy.ext.asyncio import AsyncEngine
from .postgres import engine, Base

# Modeller import edilmeli ki Base.metadata tabloları görsün
# Projendeki gerçek modelleri buraya ekle:
from app.models import user, signal  # noqa: F401

async def create_tables(db_engine: AsyncEngine | None = None):
    eng = db_engine or engine
    async with eng.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
