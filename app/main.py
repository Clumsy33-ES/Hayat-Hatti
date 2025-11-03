from fastapi import FastAPI, APIRouter, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from apscheduler.schedulers.background import BackgroundScheduler
from app.core.config import settings
from app.core.limits import get_limiter
from app.db.mongo import init_indexes
from app.services.ble_sync import sync_ble_data
from app.routes import auth, signals, ble, health
import logging

# ---------------- Logging ----------------
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ---------------- FastAPI ----------------
app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    openapi_url=f"{settings.API_PREFIX}/openapi.json",
)

# ---------------- Router ----------------
router = APIRouter()

# ---------------- CORS ----------------
origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins or ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------- Rate limit ----------------
app.state.limiter = get_limiter()
app.add_middleware(SlowAPIMiddleware)

@app.exception_handler(RateLimitExceeded)
def ratelimit_handler(request, exc):
    return JSONResponse({"detail": "Rate limit exceeded"}, status_code=429)

# ---------------- Lifecycle ----------------
@app.on_event("startup")
async def startup_event():
    try:
        await init_indexes()
        logger.info("[MONGO] Index hazır.")
    except Exception as e:
        logger.warning("[MONGO] Index atlandı/bağlantı yok: %s", e)

    if getattr(app.state, "scheduler", None) is None:
        try:
            sched = BackgroundScheduler()
            if settings.ENABLE_BLE_SYNC:
                sched.add_job(
                    sync_ble_data,
                    "interval",
                    seconds=60,
                    id="ble_sync",
                    max_instances=1,
                    coalesce=True,
                )
                logger.info("[SYNC] BLE senk başlatıldı.")
            else:
                logger.warning("BLE sync devre dışı (ENABLE_BLE_SYNC=false).")

            sched.start()
            app.state.scheduler = sched
        except Exception as e:
            logger.exception("[SYNC] Scheduler başlatılamadı: %s", e)

@app.on_event("shutdown")
def shutdown_event():
    sched = getattr(app.state, "scheduler", None)
    if sched and sched.running:
        sched.shutdown(wait=False)
        logger.info("[SYNC] Scheduler durduruldu.")

# ---------------- Routers ----------------
app.include_router(health.router,  prefix=settings.API_PREFIX)
app.include_router(auth.router,    prefix=settings.API_PREFIX)
app.include_router(signals.router, prefix=settings.API_PREFIX)
app.include_router(ble.router,     prefix=settings.API_PREFIX)

# ---------------- Root ----------------
@app.get("/")
def root():
    return {
        "message": "Afet Backend çalışıyor",
        "prefix": settings.API_PREFIX,
        "docs_url": "/docs",
        "status": "ok",
    }


