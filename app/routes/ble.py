# app/routes/ble.py  (veya sende app/ble.py nerede duruyorsa)

from datetime import datetime
import logging
from typing import Any, Dict

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.postgres import get_async_session
from app.db.mongo import get_mongo_db
from app.services.ble_service import BleService
from app.models.ble import BleSosCreate  # sende zaten var

logger = logging.getLogger("uvicorn.error")

# -------------------------
# 1) BLE SOS (Mongo)
# -------------------------
ble_router = APIRouter(prefix="/ble", tags=["BLE"])

@ble_router.post("/sos")
async def receive_ble_sos(data: BleSosCreate, db=Depends(get_mongo_db)):
    sos_doc = {
        "device_id": data.device_id,
        "message": data.message,
        "latitude": data.latitude,
        "longitude": data.longitude,
        "rssi": data.rssi,
        "created_at": datetime.utcnow(),
    }

    await db.ble_sos.insert_one(sos_doc)
    return {"status": "ok", "message": "BLE SOS alındı"}


# -------------------------
# 2) BLE DATA (Service -> Postgres)
# -------------------------
api_ble_router = APIRouter(prefix="/api/ble-data", tags=["ble"])

@api_ble_router.post("", status_code=status.HTTP_201_CREATED)
async def receive_ble_data(
    payload: Dict[str, Any],          # Şeman yoksa geçici olarak dict
    bg: BackgroundTasks,
    db: AsyncSession = Depends(get_async_session),
):
    service = BleService(db)
    try:
        # Not: user=None gönderdiğin için BleService içinde user bekleniyorsa patlar.
        # O yüzden payload içinde user_id varsa user objesi gibi geçiyoruz.
        user_obj = None
        if isinstance(payload, dict) and "user_id" in payload and payload["user_id"] is not None:
            user_obj = {"id": payload["user_id"]}

        result = await service.save_data(payload, bg, user=user_obj)
        return result

    except HTTPException:
        # Service zaten HTTPException atıyorsa aynen yükselt
        raise
    except Exception:
        # Railway loglarında stack trace görmek için:
        logger.exception("[BLE] API hata: BLE data işlenemedi")
        raise HTTPException(status_code=500, detail="BLE data işlenemedi")
