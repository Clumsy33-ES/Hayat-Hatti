from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi.middleware import SlowAPIMiddleware
from slowapi.errors import RateLimitExceeded
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.core.limits import get_limiter
from app.routes import health, auth, signals
from app.db.mongo import init_indexes  # eğer bu dosya sende yoksa bu satırı silebilirsin

app = FastAPI(title="Afet Backend", version="1.0.0")

# CORS ayarları
origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins or ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Limiter'i burada oluşturuyoruz (import anında değil)
app.state.limiter = get_limiter()
app.add_middleware(SlowAPIMiddleware)

# Rate limit aşımı cevabı
@app.exception_handler(RateLimitExceeded)
def ratelimit_handler(request, exc):
    return JSONResponse({"detail": "Rate limit exceeded"}, status_code=429)

# Mongo indekslerini startup'ta oluştur (mongo yoksa bu bloğu silebilirsin)
@app.on_event("startup")
async def startup():
    try:
        await init_indexes()
    except Exception as e:
        print(f"[mongo] init skipped: {e}")

# Router'ları ekle
app.include_router(health.router,  prefix=settings.API_PREFIX)
app.include_router(auth.router,    prefix=settings.API_PREFIX)
app.include_router(signals.router, prefix=settings.API_PREFIX)
@app.get("/")
def root():
    return {
        "message": "Afet Backend çalışıyor 🚀",
        "prefix": settings.API_PREFIX,
        "docs_url": "/docs",
        "status": "ok"
    }

