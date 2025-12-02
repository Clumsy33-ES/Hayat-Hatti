from pydantic import BaseModel, Field
from datetime import datetime
from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, Text
from app.db.postgres import Base  # <-- BURASI DÜZELDİ (eskiden app.db.models idi)

# ---- Pydantic modeller ----
class BleDataIn(BaseModel):
    device_id: str = Field(..., example="123e4567-e89b-12d3-a456-426614174000")
    latitude: float
    longitude: float
    emergency: bool
    note: str | None = None
    timestamp: datetime | None = None

class BleDataOut(BaseModel):
    id: int | None = None
    status: str
    class Config:
        from_attributes = True

# ---- SQLAlchemy modeli ----
class BleSignal(Base):
    __tablename__ = "ble_signals"

    id = Column(Integer, primary_key=True, index=True)
    device_id = Column(String(100), nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    emergency = Column(Boolean, default=False)
    note = Column(Text, nullable=True)
    timestamp = Column(DateTime(timezone=True), default=datetime.utcnow)
    synced = Column(Boolean, default=False)
