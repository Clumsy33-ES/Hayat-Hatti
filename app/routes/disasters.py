# app/disasters.py
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.db.postgres import get_async_session
from app.db.mongo import db

router = APIRouter(prefix="/api/disasters", tags=["disasters"])

NAME_MAP = {"deprem": "Deprem", "sel": "Sel", "yangin": "Orman Yangını"}

@router.get("")
async def list_disaster_types(
    source: str = Query("pg", pattern="^(pg|mongo)$"),
    db_pg: AsyncSession = Depends(get_async_session),
):
    out = []
    if source == "pg":
        q = text("""
            SELECT type AS code, COUNT(*) AS count
            FROM signals
            WHERE type IS NOT NULL
            GROUP BY type
            ORDER BY count DESC
        """)
        res = await db_pg.execute(q)
        for r in res.mappings().all():
            code = r["code"]
            out.append({"code": code, "name": NAME_MAP.get(code, code), "count": int(r["count"])})
    else:
        pipeline = [
            {"$match": {"type": {"$ne": None}}},
            {"$group": {"_id": "$type", "count": {"$sum": 1}}},
            {"$sort": {"count": -1}},
        ]
        items = await db.signals.aggregate(pipeline).to_list(100)
        for x in items:
            code = x["_id"]
            out.append({"code": code, "name": NAME_MAP.get(code, code), "count": x["count"]})
    return out
