from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.db.postgres import get_async_session
from app.models import User

router = APIRouter(prefix="/api/users", tags=["users"])

@router.get("")
async def list_users(db: AsyncSession = Depends(get_async_session)):
    res = await db.execute(select(User).order_by(User.id.asc()))
    rows = res.scalars().all()
    return [
        {
            "id": u.id,
            "first_name": u.first_name,
            "last_name": u.last_name,
            "email": u.email,
            "phone": u.phone,
            "created_at": u.created_at.isoformat() if u.created_at else None,
        }
        for u in rows
    ]
