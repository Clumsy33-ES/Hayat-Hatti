# app/services/ble_service.py
from datetime import datetime
from typing import Optional

from fastapi import BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession

# IMPORT YOLLARI DÜZELDİ
from app.db import mongo as mdb                    # motor (async) client: app/mongo.py
from app.models.ble import BleDataIn, BleSignal # mevcut modellerin
                                                # (BleSignal: SQLAlchemy modeli, BleDataIn: pydantic şema)

class BleService:
    def __init__(self, db: AsyncSession):
        self.db = db  # ASYNC session

    async def save_data(self, payload: BleDataIn, bg: Optional[BackgroundTasks] = None, user=None):
        """
        Öncelik: Mongo'ya yaz. Mongo hata verirse Postgres'e (async) kaydet.
        Bu method tamamen ASYNC'tir.
        """
        # Eksik timestamp varsa ekle
        if not payload.timestamp:
            payload.timestamp = datetime.utcnow()

        # Kullanıcı id bilgisini (JWT vs.) eklemek istersen:
        user_id = None
        if isinstance(user, dict) and "sub" in user:
            try:
                user_id = int(user["sub"])
            except Exception:
                user_id = None

        doc = {
            "device_id": payload.device_id,
            "latitude": payload.latitude,
            "longitude": payload.longitude,
            "emergency": payload.emergency,
            "note": payload.note,
            "timestamp": payload.timestamp,
            "user_id": user_id,
        }

        # 1) Mongo'ya yazmayı dene (Motor async)
        try:
            await mdb.db.ble.insert_one(doc)
            # BackgroundTasks kullanmak istersen alternatif:
            # if bg is not None:
            #     bg.add_task(mdb.db.ble.insert_one, doc)  # FastAPI coroutine'leri de bekler
            #     return {"id": None, "status": "queued_mongo"}
            return {"id": None, "status": "saved_mongo"}

        # 2) Mongo başarısızsa Postgres'e yaz (ASYNC SQLAlchemy)
        except Exception as e:
            # Fallback: Postgres
            try:
                rec = BleSignal(
                    device_id=payload.device_id,
                    latitude=payload.latitude,
                    longitude=payload.longitude,
                    emergency=payload.emergency,
                    note=payload.note,
                    timestamp=payload.timestamp,
                    synced=False,
                    user_id=user_id,
                )
                self.db.add(rec)
                await self.db.commit()
                await self.db.refresh(rec)
                return {"id": rec.id, "status": "saved_postgres"}
            except Exception as e_pg:
                await self.db.rollback()
                # Hatanın dışarı çıkması daha faydalı: üst katmanda 500 döner ve log’da görünür
                raise e_pg
