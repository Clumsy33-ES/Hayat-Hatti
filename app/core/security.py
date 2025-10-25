from datetime import datetime, timedelta
import jwt
from passlib.context import CryptContext
from app.core.config import settings

pwd_ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(pw: str) -> str:
    return pwd_ctx.hash(pw)

def verify_password(pw: str, hashed: str) -> bool:
    return pwd_ctx.verify(pw, hashed)

def create_jwt(payload: dict) -> str:
    exp = datetime.utcnow() + timedelta(minutes=settings.JWT_EXPIRES_MIN)
    return jwt.encode({**payload, "exp": exp}, settings.JWT_SECRET, algorithm="HS256")
