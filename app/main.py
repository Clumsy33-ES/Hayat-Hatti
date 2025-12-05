# app/main.py
import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes.auth import router as auth_router


# Async başlangıç işleri (Mongo index vb.)
from app.db.mongo import init_indexes

# Routerlar
from app.routes.health import router as health_router           # /api/health
from app.routes.signals import router as signals_pg_router          # /api/signals
from app.routes.signals_mongo import router as signals_mongo_router    # /api/signals/mongo
from app.routes.disasters import router as disasters_router            # /api/disasters
from app.routes.users import router as users_router                    # /api/users


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Uygulama başlarken Mongo indexlerini hazırla (opsiyonel)
    try:
        await init_indexes()
        print("[MONGO] Index hazır.")
    except Exception as e:
        print(f"[MONGO] Index atlandı/bağlantı yok: {e}")
    yield
    # Kapanışta özel bir şey yok


app = FastAPI(title="Elif API", lifespan=lifespan)

# --- CORS ---
origins_env = os.getenv("CORS_ORIGINS", "")
origins = [o.strip() for o in origins_env.split(",") if o.strip()] or ["*"]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Routers ---
app.include_router(health_router)            # GET /api/health
app.include_router(signals_pg_router)        # GET /api/signals
app.include_router(signals_mongo_router)     # GET /api/signals/mongo
app.include_router(disasters_router)         # GET /api/disasters
app.include_router(users_router)             # GET /api/users
app.include_router(auth_router)

# --- Root (bilgi amaçlı) ---
@app.get("/")
def root():
    return {
        "message": "Afet Backend çalışıyor",
        "docs_url": "/docs",
        "status": "ok",
    }
