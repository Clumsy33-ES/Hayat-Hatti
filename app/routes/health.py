from fastapi import APIRouter
from datetime import datetime

router = APIRouter(prefix="/health", tags=["health"])

@router.get("")
def health():
    return {"status": "ok", "time": datetime.utcnow().isoformat()}
