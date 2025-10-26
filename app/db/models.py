from datetime import datetime
from typing import Optional

from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import String, DateTime, ForeignKey, Float, Text
from sqlalchemy.sql import func


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "users"

    # Kimlik
    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)

    # Kullanıcı bilgileri
    first_name: Mapped[str] = mapped_column(String(50))
    last_name: Mapped[str] = mapped_column(String(50))

    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    password: Mapped[str] = mapped_column(String(255))  # HASHLENMİŞ şifre saklanacak

    phone: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)

    # Kullanıcı ne zaman oluşturuldu
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),   # DB seviyesinde now()
        index=True
    )


class Signal(Base):
    __tablename__ = "signals"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)

    device_signal_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    type: Mapped[str] = mapped_column(String(32), index=True)

    timestamp: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        index=True,
    )

    lon: Mapped[float] = mapped_column(Float)
    lat: Mapped[float] = mapped_column(Float)

    accuracy_m: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # attachments bizde string olarak tutuluyor, virgülle joinlenmiş
    attachments: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # hangi kullanıcıya ait
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
