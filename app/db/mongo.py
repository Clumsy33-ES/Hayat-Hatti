import os
from motor.motor_asyncio import AsyncIOMotorClient

MONGO_URI = os.getenv("MONGO_URI", "mongodb+srv://hayat_hatti:firdevs64@cluster0.qkwejsr.mongodb.net/?appName=Cluster0")
MONGO_DB_NAME = os.getenv("MONGO_DB_NAME", "hayat_hatti")

client = AsyncIOMotorClient(MONGO_URI)
db = client[MONGO_DB_NAME]

async def init_indexes():
    """Create required indexes for Mongo collections safely."""
    try:
        await db.signals.create_index("device_signal_id", unique=True)
        await db.signals.create_index([("user_id", 1), ("created_at", -1)])
        await db.signals.create_index([("type", 1), ("created_at", -1)])
        # If you store GeoJSON points, also enable:
        # await db.signals.create_index([("location", "2dsphere")])
        return True
    except Exception as e:
        print("[MONGO] index error:", e)
        return False
