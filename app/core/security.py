from datetime import datetime, timedelta
from passlib.context import CryptContext
from jose import jwt, JWTError
from app.core.config import settings

pwd_ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(pw: str) -> str:
    return pwd_ctx.hash(pw)


def verify_password(pw: str, hashed: str) -> bool:
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
