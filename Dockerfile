FROM python:3.11-slim

# Çalışma dizini
WORKDIR /app

# Sistem bağımlılıkları (psycopg için derleyici vs.)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Python bağımlılıkları
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Uygulama kodu
COPY . .

# Railway PORT env değişkenini kullanarak uvicorn'u başlat
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port $PORT"]
