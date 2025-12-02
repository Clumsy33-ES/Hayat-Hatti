# app/ble.py
from fastapi import APIRouter, Depends, BackgroundTasks, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.postgres import get_async_session
from app.services.ble_service import BleService

# İsteğe bağlı: Pydantic şemaların varsa importla
# from app.models.ble import BleDataIn, BleDataOut
# Şema yoksa payload'ı dict kabul eden basit bir versiyon da yazabiliriz.

router = APIRouter(prefix="/api/ble-data", tags=["ble"])

@router.post("", status_code=status.HTTP_201_CREATED)
async def receive_ble_data(
    payload: dict,                  # Şeman yoksa geçici olarak dict
    bg: BackgroundTasks,
    db: AsyncSession = Depends(get_async_session),
):
    service = BleService(db)
    try:
        result = await service.save_data(payload, bg, user=None)  # BleService async
        return result
    except Exception as e:
        print(f"[BLE] API hata: {e}")
        raise HTTPException(status_code=500, detail="BLE data işlenemedi")
