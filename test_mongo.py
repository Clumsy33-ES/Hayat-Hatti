# test_mongo.py
import os
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()  # .env içindeki MONGO_URI'ı yükler

uri = os.getenv("MONGO_URI") or os.getenv("MONGODB_URL")
if not uri:
    raise SystemExit("MONGODB_URL .env içinde yok. Ayarla ve tekrar dene.")

client = MongoClient(uri, serverSelectionTimeoutMS=5000)
try:
    # bağlantı test
    client.server_info()   # Sunucuya bağlanmazsa exception fırlatır
    db = client["hayat_hatti"]
    print("Koleksiyonlar:", db.list_collection_names())
    sample = db["signals"].find_one()
    print("Örnek kayıt:", sample)
finally:
    client.close()

