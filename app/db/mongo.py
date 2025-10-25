from motor.motor_asyncio import AsyncIOMotorClient
from app.core.config import settings

client = AsyncIOMotorClient(settings.MONGODB_URI)
db = client[settings.MONGODB_DB]

async def init_indexes():
    await db.signals.create_index("device_signal_id", unique=True)
    await db.signals.create_index("timestamp")
