from datetime import datetime
from typing import Optional

from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import String, DateTime, ForeignKey, Float, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy import String, Integer, Double, Text, TIMESTAMP, func
from app.db.postgres import Base

class User(Base):
    __tablename__ = "users"  # mevcut tablo adı

    id: Mapped[int] = mapped_column(primary_key=True)
    first_name: Mapped[str] = mapped_column(String(50))
    last_name: Mapped[str] = mapped_column(String(50))
    email: Mapped[str] = mapped_column(String(100))
    password: Mapped[str] = mapped_column(String(200))
    phone: Mapped[str | None] = mapped_column(String(20), nullable=True)
    created_at: Mapped[str] = mapped_column(TIMESTAMP(timezone=True), server_default=func.now())

class Signal(Base):
    __tablename__ = "signals"  # mevcut tablo adı

    id: Mapped[int] = mapped_column(primary_key=True)
    device_signal_id: Mapped[str] = mapped_column(String, unique=True)
    user_id: Mapped[int] = mapped_column(Integer)  # FK var ama ilişki kurmak zorunda değiliz
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    attachments: Mapped[str | None] = mapped_column(Text, nullable=True)
    lon: Mapped[float] = mapped_column(Double)
    lat: Mapped[float] = mapped_column(Double)
    accuracy_m: Mapped[float | None] = mapped_column(Double, nullable=True)
    type: Mapped[str] = mapped_column(String(32))  # "deprem" | "sel" | "yangin"
    timestamp: Mapped[str] = mapped_column(TIMESTAMP(timezone=True))
    created_at: Mapped[str] = mapped_column(TIMESTAMP(timezone=True))


    lon: Mapped[float] = mapped_column(Float)
    lat: Mapped[float] = mapped_column(Float)

    accuracy_m: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # attachments bizde string olarak tutuluyor, virgülle joinlenmiş
    attachments: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # hangi kullanıcıya ait
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
