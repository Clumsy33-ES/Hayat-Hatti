from fastapi import APIRouter, Depends, Query, Body, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.db.postgres import get_async_session
from app.models import Signal
from datetime import datetime, timezone

router = APIRouter(prefix="/api/signals", tags=["signals"])

@router.get("")
async def list_signals(
    db: AsyncSession = Depends(get_async_session),
    limit: int = Query(100, ge=1, le=500),
    offset: int = Query(0, ge=0),
    type: str | None = Query(None),
):
    stmt = select(Signal)
    if type:
        stmt = stmt.where(Signal.type == type)
    stmt = stmt.order_by(Signal.created_at.desc()).limit(limit).offset(offset)

    res = await db.execute(stmt)
    rows = res.scalars().all()
    return [
        {
            "id": r.id,
            "device_signal_id": r.device_signal_id,
            "user_id": r.user_id,
            "type": r.type,
            "notes": r.notes,
            "attachments": r.attachments,
            "location": {"lat": r.lat, "lon": r.lon, "accuracy_m": r.accuracy_m},
            "timestamp": r.timestamp.isoformat() if r.timestamp else None,
            "created_at": r.created_at.isoformat() if r.created_at else None,
            "source": "pg",
        }
        for r in rows
    ]

@router.post("", status_code=201)
async def create_signal(
    payload: dict = Body(...),
    db: AsyncSession = Depends(get_async_session),
):
    # Quick validation
    for f in ["device_signal_id", "user_id", "type", "timestamp", "lon", "lat"]:
        if f not in payload:
            raise HTTPException(400, detail={"error": "VALIDATION_ERROR", "field": f, "message": "required"})

    # Idempotent check
    existing = await db.execute(select(Signal).where(Signal.device_signal_id == payload["device_signal_id"]))
    s = existing.scalar_one_or_none()
    if s:
        return {
            "id": s.id,
            "device_signal_id": s.device_signal_id,
            "user_id": s.user_id,
            "type": s.type,
            "notes": s.notes,
            "attachments": s.attachments,
            "location": {"lat": s.lat, "lon": s.lon, "accuracy_m": s.accuracy_m},
            "timestamp": s.timestamp.isoformat() if s.timestamp else None,
            "created_at": s.created_at.isoformat() if s.created_at else None,
            "source": "pg",
        }

    # Insert
    try:
        ts = payload["timestamp"]
        if isinstance(ts, str):
            ts = datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except Exception:
        raise HTTPException(400, detail={"error": "VALIDATION_ERROR", "field": "timestamp", "message": "invalid ISO datetime"})

    s = Signal(
        device_signal_id=payload["device_signal_id"],
        user_id=int(payload["user_id"]),
        type=str(payload["type"]),
        notes=payload.get("notes"),
        attachments=payload.get("attachments"),
        lon=float(payload["lon"]),
        lat=float(payload["lat"]),
        accuracy_m=payload.get("accuracy_m"),
        timestamp=ts,
        created_at=datetime.now(timezone.utc),
    )
    db.add(s)
    await db.commit()
    await db.refresh(s)

    return {
        "id": s.id,
        "device_signal_id": s.device_signal_id,
        "user_id": s.user_id,
        "type": s.type,
        "notes": s.notes,
        "attachments": s.attachments,
        "location": {"lat": s.lat, "lon": s.lon, "accuracy_m": s.accuracy_m},
        "timestamp": s.timestamp.isoformat() if s.timestamp else None,
        "created_at": s.created_at.isoformat() if s.created_at else None,
        "source": "pg",
    }
