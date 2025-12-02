# app/auth.py
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.db.postgres import get_async_session
from app.models import User
# Bu üçlü sende mevcutsa kullanıyoruz; yoksa sonradan ekleriz.
from app.core.security import hash_password, verify_password, create_access_token
from app.models.user import UserIn, UserPublic, LoginResponse

router = APIRouter(prefix="/api/auth", tags=["auth"])

@router.post("/register", response_model=LoginResponse, status_code=201)
async def register_user(payload: UserIn, db: AsyncSession = Depends(get_async_session)):
    # aynı email var mı?
    res = await db.execute(select(User).where(User.email == payload.email))
    existing_user = res.scalar_one_or_none()
    if existing_user:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Email already registered")

    # şifreyi hashle
    hashed_pw = hash_password(payload.password)

    # kullanıcı oluştur
    new_user = User(
        first_name=payload.first_name or "",
        last_name=payload.last_name or "",
        email=payload.email,
        password=hashed_pw,
        phone=payload.phone,
    )
    db.add(new_user)
    await db.commit()
    await db.refresh(new_user)

    token = create_access_token({"sub": str(new_user.id), "email": new_user.email})
    return LoginResponse(token=token, user=UserPublic.model_validate(new_user))

@router.post("/login", response_model=LoginResponse)
async def login_user(payload: UserIn, db: AsyncSession = Depends(get_async_session)):
    res = await db.execute(select(User).where(User.email == payload.email))
    user = res.scalar_one_or_none()
    if not user or not verify_password(payload.password, user.password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password")

    token = create_access_token({"sub": str(user.id), "email": user.email})
    return LoginResponse(token=token, user=UserPublic.model_validate(user))
