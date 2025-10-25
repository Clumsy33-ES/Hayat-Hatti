from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi.middleware import SlowAPIMiddleware
from slowapi.errors import RateLimitExceeded
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.core.limits import limiter
from app.routes import health, auth, signals

app = FastAPI(title="Afet Backend", version="1.0.0")

origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins or ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.state.limiter = limiter
app.add_middleware(SlowAPIMiddleware)

@app.exception_handler(RateLimitExceeded)
def ratelimit_handler(request, exc):
    return JSONResponse({"detail": "Rate limit exceeded"}, status_code=429)

app.include_router(health.router, prefix=settings.API_PREFIX)
app.include_router(auth.router,   prefix=settings.API_PREFIX)
app.include_router(signals.router, prefix=settings.API_PREFIX)
