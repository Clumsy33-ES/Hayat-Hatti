from fastapi import APIRouter, Depends, BackgroundTasks, HTTPException, status
from sqlalchemy.orm import Session
from app.core.deps import get_current_user
from app.db.postgres import get_session
from app.models.ble import BleDataIn, BleDataOut
from app.services.ble_service import BleService

router = APIRouter(prefix="/ble-data", tags=["ble"])

@router.post("", response_model=BleDataOut, status_code=status.HTTP_201_CREATED)
def receive_ble_data(
    payload: BleDataIn,
    bg: BackgroundTasks,
    user=Depends(get_current_user),
    db: Session = Depends(get_session),
):
    service = BleService(db)
    try:
        result = service.save_data(payload, bg, user)
        return result
    except Exception as e:
        print(f"[BLE] API hata: {e}")
        raise HTTPException(status_code=500, detail="BLE data işlenemedi")
