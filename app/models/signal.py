from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

# --- Pydantic şemalar ---
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

# --- SQLAlchemy modeli ---
from sqlalchemy import Column, Integer, String, Float, Text, DateTime
from app.db.postgres import Base

class Signal(Base):
    __tablename__ = "signals"

    id = Column(Integer, primary_key=True)
    device_signal_id = Column(String(64), unique=True)
    user_id = Column(Integer)
    notes = Column(Text, nullable=True)
    attachments = Column(Text, nullable=True)
    lon = Column(Float, nullable=False)
    lat = Column(Float, nullable=False)
    accuracy_m = Column(Float, nullable=True)
    type = Column(String(32), nullable=False)   # "deprem" | "sel" | "yangin"
    timestamp = Column(DateTime(timezone=True))
    created_at = Column(DateTime(timezone=True))
