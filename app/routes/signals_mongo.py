from fastapi import APIRouter, Query
from app.db.mongo import db

router = APIRouter(prefix="/api/signals", tags=["signals"])

@router.get("/mongo")
async def list_signals_mongo(
    limit: int = Query(100, ge=1, le=500),
    offset: int = Query(0, ge=0),
):
    cursor = (
        db.signals.find(
            {},
            {
                "_id": 1, "device_signal_id": 1, "user_id": 1,
                "type": 1, "notes": 1, "attachments": 1,
                "lat": 1, "lon": 1, "accuracy_m": 1,
                "timestamp": 1, "created_at": 1
            }
        )
        .sort("created_at", -1)
        .skip(offset)
        .limit(limit)
    )
    docs = await cursor.to_list(length=limit)
    return [
        {
            "id": str(d.get("_id")),
            "device_signal_id": d.get("device_signal_id"),
            "user_id": d.get("user_id"),
            "type": d.get("type"),
            "notes": d.get("notes"),
            "attachments": d.get("attachments"),
            "location": {"lat": d.get("lat"), "lon": d.get("lon"), "accuracy_m": d.get("accuracy_m")},
            "timestamp": (d.get("timestamp").isoformat() if d.get("timestamp") else None),
            "created_at": (d.get("created_at").isoformat() if d.get("created_at") else None),
            "source": "mongo",
        }
        for d in docs
    ]
