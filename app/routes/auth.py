from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.user import UserIn, UserOut
from app.core.security import hash_password, verify_password, create_jwt
from app.db.postgres import get_session
from app.db.models import User

router = APIRouter(prefix="/auth", tags=["auth"])

@router.post("/register", response_model=UserOut)
async def register(user: UserIn, session: AsyncSession = Depends(get_session)):
    exists = await session.scalar(select(User).where(User.email == user.email))
    if exists:
        raise HTTPException(409, "Email already exists")
    u = User(email=user.email, password=hash_password(user.password))
    session.add(u)
    await session.commit()
    await session.refresh(u)
    token = create_jwt({"sub": str(u.id), "email": u.email})
    return {"id": str(u.id), "email": u.email, "token": token}

@router.post("/login", response_model=UserOut)
async def login(user: UserIn, session: AsyncSession = Depends(get_session)):
    u = await session.scalar(select(User).where(User.email == user.email))
    if not u or not verify_password(user.password, u.password):
        raise HTTPException(401, "Invalid credentials")
    token = create_jwt({"sub": str(u.id), "email": u.email})
    return {"id": str(u.id), "email": u.email, "token": token}
