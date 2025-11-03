from datetime import datetime
from sqlalchemy.orm import Session
from fastapi import BackgroundTasks
from pymongo.errors import PyMongoError
from app.db import mongo as mdb
from app.models.ble import BleDataIn, BleSignal

class BleService:
    def __init__(self, db: Session):
        self.db = db

    def save_data(self, payload: BleDataIn, bg: BackgroundTasks, user):
        # Eksik timestamp varsa şimdi ekle
        if not payload.timestamp:
            payload.timestamp = datetime.utcnow()

        doc = {
            "device_id": payload.device_id,
            "latitude": payload.latitude,
            "longitude": payload.longitude,
            "emergency": payload.emergency,
            "note": payload.note,
            "timestamp": payload.timestamp,
            "user_id": int(user["sub"]),
        }

        # 1️⃣ Mongo'ya yazmayı dene (online senaryo)
        try:
            bg.add_task(mdb.db.ble.insert_one, doc)
            print(f"[BLE] Mongo insert queued for device {payload.device_id}")
            return {"id": None, "status": "queued_mongo"}

        # 2️⃣ Mongo başarısızsa Postgres'e kaydet (offline senaryo)
        except PyMongoError as e:
            print(f"[BLE] Mongo bağlantı hatası: {e}")
        except Exception as e:
            print(f"[BLE] Mongo hata: {e}")

        try:
            rec = BleSignal(
                device_id=payload.device_id,
                latitude=payload.latitude,
                longitude=payload.longitude,
                emergency=payload.emergency,
                note=payload.note,
                timestamp=payload.timestamp,
                synced=False,
            )
            self.db.add(rec)
            self.db.commit()
            self.db.refresh(rec)
            print(f"[BLE] Postgres’e kayıt yapıldı: {rec.device_id}")
            return {"id": rec.id, "status": "saved_postgres"}

        except Exception as e:
            self.db.rollback()
            print(f"[BLE] Postgres kayıt hatası: {e}")
            raise
