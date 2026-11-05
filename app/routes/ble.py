# app/routes/ble.py  (veya sende app/ble.py nerede duruyorsa)

from datetime import datetime
import logging
from typing import Any, Dict

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.deps import get_current_user
from app.db.postgres import get_async_session
from app.db.mongo import get_mongo_db
from app.services.ble_service import BleService
from app.models.ble import BleSosCreate, BleDataIn  # sende zaten var

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

@api_ble_router.get("", dependencies=[Depends(get_current_user)])
async def list_ble_data(
    limit: int = Query(100, ge=1, le=500),
    offset: int = Query(0, ge=0),
    db=Depends(get_mongo_db),
):
    cursor = db.ble.find({}).sort("timestamp", -1).skip(offset).limit(limit)
    docs = await cursor.to_list(length=limit)
    return [
        {
            "id": str(d.get("_id")),
            "device_id": d.get("device_id"),
            "user_id": d.get("user_id"),
            "latitude": d.get("latitude"),
            "longitude": d.get("longitude"),
            "emergency": d.get("emergency"),
            "note": d.get("note"),
            "timestamp": d.get("timestamp").isoformat() if d.get("timestamp") else None,
            "source": "mongo",
        }
        for d in docs
    ]


@api_ble_router.post("", status_code=status.HTTP_201_CREATED)
async def receive_ble_data(
    payload: BleDataIn,   # ✅ dict değil, zaten sende var
    bg: BackgroundTasks,
    db: AsyncSession = Depends(get_async_session),
):
    service = BleService(db)
    try:
        # user istemiyorsa bile None kalabilir, ama servis user bekliyorsa patlar.
        # O yüzden en güvenlisi: payload içinden bir user objesi türetmek:
        user_obj = None
        # sende user_id yok, o yüzden şimdilik None bırakıyoruz.
        # Eğer servis user istiyorsa, user_id eklemek gerekecek (aşağıda anlattım).

        result = await service.save_data(payload, bg, user=user_obj)
        return result

    except Exception:
        logger.exception("[BLE] API hata")
        raise HTTPException(status_code=500, detail="BLE data işlenemedi")