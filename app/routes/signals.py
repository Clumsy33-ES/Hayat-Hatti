from fastapi import APIRouter, Depends, Query, BackgroundTasks
from typing import List, Optional
from datetime import datetime
from sqlalchemy import select, and_, desc
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.signal import SignalIn, SignalOut
from app.core.deps import get_current_user
from app.db.postgres import get_session
from app.db.models import Signal
from app.db import mongo as mdb

router = APIRouter(prefix="/signals", tags=["signals"])

async def archive_to_mongo(sig: SignalIn, user_id: int):
    doc = {
        "device_signal_id": sig.device_signal_id,
        "type": sig.type,
        "timestamp": sig.timestamp,
        "created_at": datetime.utcnow(),
        "lon": sig.lon, "lat": sig.lat,
        "accuracy_m": sig.accuracy_m,
        "notes": sig.notes,
        "attachments": sig.attachments or [],
        "user_id": user_id
    }
    await mdb.db.signals.update_one(
        {"device_signal_id": sig.device_signal_id},
        {"$setOnInsert": doc},
        upsert=True
    )

@router.post("", response_model=SignalOut)
async def create_signal(payload: SignalIn, bg: BackgroundTasks, user=Depends(get_current_user), session: AsyncSession = Depends(get_session)):
    exists = await session.scalar(select(Signal).where(Signal.device_signal_id == payload.device_signal_id))
    if not exists:
        s = Signal(
            device_signal_id=payload.device_signal_id,
            type=payload.type,
            timestamp=payload.timestamp,
            lon=payload.lon, lat=payload.lat,
            accuracy_m=payload.accuracy_m,
            notes=payload.notes,
            attachments=",".join(payload.attachments) if payload.attachments else None,
            user_id=int(user["sub"])
        )
        session.add(s)
        await session.commit()
        await session.refresh(s)
        bg.add_task(archive_to_mongo, payload, int(user["sub"]))
        return {"id": s.id, "status": "ok"}
    bg.add_task(archive_to_mongo, payload, int(user["sub"]))
    return {"id": exists.id, "status": "ok"}

@router.post("/batch", response_model=List[SignalOut])
async def create_signals_batch(items: List[SignalIn], bg: BackgroundTasks, user=Depends(get_current_user), session: AsyncSession = Depends(get_session)):
    outs: List[SignalOut] = []
    for it in items:
        exists = await session.scalar(select(Signal).where(Signal.device_signal_id == it.device_signal_id))
        if not exists:
            s = Signal(
                device_signal_id=it.device_signal_id, type=it.type, timestamp=it.timestamp,
                lon=it.lon, lat=it.lat, accuracy_m=it.accuracy_m, notes=it.notes,
                attachments=",".join(it.attachments) if it.attachments else None,
                user_id=int(user["sub"])
            )
            session.add(s)
            await session.flush()
            outs.append({"id": s.id, "status": "ok"})
        else:
            outs.append({"id": exists.id, "status": "ok"})
        bg.add_task(archive_to_mongo, it, int(user["sub"]))
    await session.commit()
    return outs

@router.get("")
async def query_signals(
    bbox: Optional[str] = Query(None, description="minLon,minLat,maxLon,maxLat"),
    since: Optional[datetime] = Query(None),
    until: Optional[datetime] = Query(None),
    type: Optional[str] = None,
    limit: int = 200,
    user=Depends(get_current_user),
    session: AsyncSession = Depends(get_session)
):
    conds = []
    if type: conds.append(Signal.type == type)
    if since: conds.append(Signal.timestamp >= since)
    if until: conds.append(Signal.timestamp <= until)
    if bbox:
        minLon, minLat, maxLon, maxLat = [float(x) for x in bbox.split(",")]
        conds += [Signal.lon >= minLon, Signal.lon <= maxLon, Signal.lat >= minLat, Signal.lat <= maxLat]

    stmt = (select(Signal).where(and_(*conds)) if conds else select(Signal)).order_by(desc(Signal.timestamp)).limit(limit)
    rows = (await session.execute(stmt)).scalars().all()
    items = [{
        "id": r.id, "device_signal_id": r.device_signal_id, "type": r.type,
        "timestamp": r.timestamp.isoformat(), "lon": r.lon, "lat": r.lat,
        "accuracy_m": r.accuracy_m, "notes": r.notes
    } for r in rows]
    return {"count": len(items), "items": items}
