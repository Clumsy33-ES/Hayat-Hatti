from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi.middleware import SlowAPIMiddleware
from slowapi.errors import RateLimitExceeded
from fastapi.responses import JSONResponse
from apscheduler.schedulers.background import BackgroundScheduler

from app.core.config import settings
from app.core.limits import get_limiter
from app.routes import health, auth, signals, ble
from app.db.mongo import init_indexes  # mongo yoksa silebilirsin
from app.services.ble_sync import sync_ble_data

# ------------------------------------------------------
# FastAPI uygulaması
# ------------------------------------------------------
app = FastAPI(title="Afet Backend", version="1.0.0")

# ------------------------------------------------------
# CORS ayarları
# ------------------------------------------------------
origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins or ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ------------------------------------------------------
# Rate limiter
# ------------------------------------------------------
app.state.limiter = get_limiter()
app.add_middleware(SlowAPIMiddleware)

@app.exception_handler(RateLimitExceeded)
def ratelimit_handler(request, exc):
    return JSONResponse({"detail": "Rate limit exceeded"}, status_code=429)

# ------------------------------------------------------
# Startup event
# ------------------------------------------------------
@app.on_event("startup")
async def startup_event():
    # MongoDB indeksleri oluştur
    try:
        await init_indexes()
        print("[MONGO] Index oluşturuldu.")
    except Exception as e:
        print(f"[MONGO] Index oluşturulamadı veya bağlantı yok: {e}")

    # BLE Sync Scheduler başlat
    try:
        scheduler = BackgroundScheduler()
        scheduler.add_job(sync_ble_data, "interval", seconds=60)
        scheduler.start()
        print("[SYNC] BLE senkronizasyon scheduler başlatıldı.")
    except Exception as e:
        print(f"[SYNC] Scheduler başlatılamadı: {e}")

# ------------------------------------------------------
# Router'lar
# ------------------------------------------------------
app.include_router(health.router,  prefix=settings.API_PREFIX)
app.include_router(auth.router,    prefix=settings.API_PREFIX)
app.include_router(signals.router, prefix=settings.API_PREFIX)
app.include_router(ble.router,     prefix=settings.API_PREFIX)

# ------------------------------------------------------
# Root endpoint
# ------------------------------------------------------
@app.get("/")
def root():
    return {
        "message": "Afet Backend çalışıyor ",
        "prefix": settings.API_PREFIX,
        "docs_url": "/docs",
        "status": "ok"
    }
print("[DB] RAW DSN repr:", repr(settings.DATABASE_URL))
non_ascii = [ (i, ch, hex(ord(ch))) for i, ch in enumerate(settings.DATABASE_URL) if ord(ch) > 127 ]
print("[DB] non-ascii:", non_ascii)

