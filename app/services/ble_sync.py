from datetime import datetime
from app.db import mongo as mdb
from app.db.postgres import get_session
from app.models.ble import BleSignal

def sync_ble_data():
    db = next(get_session())
    unsynced = db.query(BleSignal).filter_by(synced=False).limit(200).all()

    if not unsynced:
        return

    print(f"[SYNC] {len(unsynced)} adet BLE kaydı senkronize ediliyor...")

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
            print(f"[SYNC] {row.device_id} senkronizasyon hatası: {e}")

    db.commit()
    db.close()
    print(f"[SYNC] Başarılı: {success}, Hatalı: {failed}")
