from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr


class UserIn(BaseModel):
    """
    İstemciden (frontend/mobil) KAYIT olurken (register) veya giriş yaparken (login)
    aldığımız body.
    Register'da hepsi kullanılır.
    Login'de email + password kısmı kullanılır.
    """
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: EmailStr
    password: str
    phone: Optional[str] = None


class UserPublic(BaseModel):
    """
    Dışarı döndüğümüz güvenli kullanıcı bilgisi.
    ŞİFRE YOK.
    """
    id: int
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: EmailStr
    phone: Optional[str] = None
    created_at: Optional[datetime] = None

    class Config:
        # SQLAlchemy objesini direkt return edebilelim diye
        from_attributes = True  # (pydantic v2) - pydantic v1'de orm_mode = True


class LoginResponse(BaseModel):
    """
    Login/Register cevabımız.
    Ekip 'id, email de dön, token da dön' dedi ya,
    işte o tam olarak burası.
    """
    token: str
    user: UserPublic
