# app/services/ble_sync.py
import logging
import socket
from datetime import datetime
from sqlalchemy.exc import OperationalError
from app.db.postgres import get_session
from app.db import mongo as mdb
from app.models.ble import BleSignal

logger = logging.getLogger(__name__)

def sync_ble_data():
    """Postgres'teki senkronize edilmemiş BLE kayıtlarını Mongo'ya aktarır."""
    try:
        db = next(get_session())
        unsynced = db.query(BleSignal).filter_by(synced=False).limit(200).all()

        if not unsynced:
            logger.info("[SYNC] Senkronize edilecek kayıt yok.")
            return

        logger.info(f"[SYNC] {len(unsynced)} adet BLE kaydı aktarılıyor...")

        success, failed = 0, 0
        for row in unsynced:
            try:
                mdb.db.ble.insert_one({
                    "device_id": row.device_id,
                    "latitude": row.latitude,
                    "longitude": row.longitude,
                    "emergency": row.emergency,
                    "note": row.note,
                    "timestamp": row.timestamp or datetime.utcnow(),
                })
                row.synced = True
                success += 1
            except Exception as e:
                failed += 1
                db.rollback()
                logger.warning(f"[SYNC] {row.device_id} aktarım hatası: {e}")

        db.commit()
        logger.info(f"[SYNC] Tamamlandı ✅ Başarılı: {success}, Hatalı: {failed}")
    except (OperationalError, socket.gaierror) as e:
        logger.warning(f"[SYNC] DB bağlantı hatası — atlandı: {e}")
    except Exception as e:
        logger.exception(f"[SYNC] Beklenmeyen hata: {e}")
    finally:
        try:
            db.close()
        except Exception:
            pass
