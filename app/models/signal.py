from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class SignalIn(BaseModel):
    device_signal_id: str
    type: str
    timestamp: datetime
    lon: float
    lat: float
    accuracy_m: Optional[float] = None
    notes: Optional[str] = None
    attachments: Optional[List[str]] = None

class SignalOut(BaseModel):
    id: int
    status: str = "ok"
