# app/services/ble_sync.py
import asyncio
import logging
from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import OperationalError

# IMPORT YOLLARI DÜZELDİ (artık sync get_session yok)
from app.db.postgres import AsyncSessionLocal
from app.db import mongo as mdb
from app.models.ble import BleSignal

logger = logging.getLogger(__name__)

async def sync_ble_data_once(limit: int = 200):
    """
    Postgres'te synced=False olan BLE kayıtlarını Mongo'ya aktarır ve synced=True yapar.
    Tek seferlik çalışır. Tamamen ASYNC'tir.
    """
    async with AsyncSessionLocal() as db:  # type: AsyncSession
        try:
            result = await db.execute(
                select(BleSignal).where(BleSignal.synced == False).limit(limit)  # noqa: E712
            )
            unsynced = result.scalars().all()

            if not unsynced:
                logger.info("[SYNC] Senkronize edilecek kayıt yok.")
                return {"success": 0, "failed": 0}

            logger.info(f"[SYNC] {len(unsynced)} adet BLE kaydı aktarılıyor...")

            success, failed = 0, 0
            for row in unsynced:
                try:
                    await mdb.db.ble.insert_one({
                        "device_id": row.device_id,
                        "latitude": row.latitude,
                        "longitude": row.longitude,
                        "emergency": row.emergency,
                        "note": row.note,
                        "timestamp": row.timestamp or datetime.utcnow(),
                        "user_id": getattr(row, "user_id", None),
                    })
                    row.synced = True
                    success += 1
                except Exception as e:
                    failed += 1
                    logger.warning(f"[SYNC] {row.device_id} aktarım hatası: {e}")

            # toplu commit
            await db.commit()
            logger.info(f"[SYNC] Tamamlandı ✅ Başarılı: {success}, Hatalı: {failed}")
            return {"success": success, "failed": failed}

        except (OperationalError,) as e:
            logger.warning(f"[SYNC] DB bağlantı hatası — atlandı: {e}")
            # Not: rollback güvenli tarafta kalır
            try:
                await db.rollback()
            except Exception:
                pass
            return {"success": 0, "failed": 0}

        except Exception as e:
            logger.exception(f"[SYNC] Beklenmeyen hata: {e}")
            try:
                await db.rollback()
            except Exception:
                pass
            raise

async def sync_ble_data_loop(interval_seconds: int = 30):
    """
    Sürekli çalışan döngü (ör. lifespan içinde task olarak başlat).
    Uyarı: Uvicorn --reload ile Windows'ta subprocess yaratır; lifespan içinde başlatmak daha güvenli.
    """
    while True:
        try:
            await sync_ble_data_once()
        except Exception as e:
            logger.error(f"[SYNC] Döngü hatası: {e}")
        await asyncio.sleep(interval_seconds)
