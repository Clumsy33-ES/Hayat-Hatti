from datetime import datetime, timedelta

from fastapi import HTTPException, status
from passlib.context import CryptContext
from jose import jwt, JWTError

from app.core.config import settings

pwd_ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(pw: str) -> str:
    """
    Kullanıcının düz şifresini bcrypt ile hashler.
    Bcrypt'in 72 byte limiti olduğu için önce bunu kontrol ediyoruz.
    """
    # Bcrypt gerçek limit: 72 BYTE
    if len(pw.encode("utf-8")) > 72:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Şifre en fazla 72 karakter (72 byte) olabilir.",
        )

    return pwd_ctx.hash(pw)


def verify_password(pw: str, hashed: str) -> bool:
    """
    Düz şifreyi, veritabanındaki hash ile karşılaştırır.
    """
    return pwd_ctx.verify(pw, hashed)


def create_access_token(payload: dict, expires_minutes: int | None = None) -> str:
    """
    Kullanıcıya JWT üretir.
    payload -> içine 'sub', 'email' gibi alanlar koyuyoruz.
    """
    # Süre: parametre geldiyse onu kullan, gelmediyse config'teki default'u kullan
    minutes = expires_minutes or settings.ACCESS_TOKEN_EXPIRE_MINUTES

    expire_at = datetime.utcnow() + timedelta(minutes=minutes)

    to_encode = {
        **payload,
        "exp": expire_at,
    }

    token = jwt.encode(
        to_encode,
        settings.JWT_SECRET_KEY,   # config'teki unified secret
        algorithm=settings.jwt_algorithm,  # küçük harf attribute
    )

    return token


def decode_token(token: str) -> dict | None:
    """
    Token'ı çözüp geri payload döndürür.
    Geçersizse None döndürür.
    """
    try:
        data = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.jwt_algorithm],
        )
        return data
    except JWTError:
        return None