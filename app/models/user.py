from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr,constr

PasswordStr = constr(min_length=8, max_length=72)
# --- Pydantic şemalar ---
class UserIn(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: EmailStr
    password: PasswordStr
    phone: Optional[str] = None

class UserPublic(BaseModel):
    id: int
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: EmailStr
    phone: Optional[str] = None
    created_at: Optional[datetime] = None
    class Config:
        from_attributes = True

class LoginResponse(BaseModel):
    token: str
    user: UserPublic

# --- SQLAlchemy modeli ---
from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.sql import func
from app.db.postgres import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True)
    first_name = Column(String(50))
    last_name  = Column(String(50))
    email      = Column(String(100))
    password   = Column(String(200))
    phone      = Column(String(20), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
