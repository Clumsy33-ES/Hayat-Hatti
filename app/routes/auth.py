from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import select

from app.db.postgres import get_session
from app.db.models import User
from app.models.user import UserIn, UserPublic, LoginResponse
from app.core.security import hash_password, verify_password, create_access_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=LoginResponse, status_code=201)
def register_user(payload: UserIn, db: Session = Depends(get_session)):
    # aynı email var mı kontrol et
    existing_user = db.execute(
        select(User).where(User.email == payload.email)
    ).scalar_one_or_none()

    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )

    # şifreyi hashle
    hashed_pw = hash_password(payload.password)

    # yeni user objesi oluştur
    new_user = User(
        first_name=payload.first_name or "",
        last_name=payload.last_name or "",
        email=payload.email,
        password=hashed_pw,
        phone=payload.phone,
    )

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    # jwt üret
    token = create_access_token({"sub": str(new_user.id), "email": new_user.email})

    # LoginResponse → { token: "...", user: { ... } }
    return LoginResponse(
        token=token,
        user=UserPublic.model_validate(new_user)
    )


@router.post("/login", response_model=LoginResponse)
def login_user(payload: UserIn, db: Session = Depends(get_session)):
    # kullanıcıyı email ile bul
    user = db.execute(
        select(User).where(User.email == payload.email)
    ).scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password"
        )

    # şifre doğru mu
    if not verify_password(payload.password, user.password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password"
        )

    # jwt üret
    token = create_access_token({"sub": str(user.id), "email": user.email})

    return LoginResponse(
        token=token,
        user=UserPublic.model_validate(user)
    )
