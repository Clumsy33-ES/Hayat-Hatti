from slowapi import Limiter
from slowapi.util import get_remote_address  # yardımcı bir fonksiyondur; istek yapanın IP adresini otomatik olarak döndürür.
# sistemin kime limit uygulayacağını belirler.

from app.core.config import settings  # config içindeki settings'i çekeriz.
# Buradan özellikle RATE_LIMIT değerini alıyoruz. RATE_LIMIT:str = "60/minute" olarak ayarlı.
# İstersen test ortamında "1000/hour" gibi yapabilirsin.

# ÖNEMLİ DEĞİŞİKLİK:
# Daha önce burada limiter = Limiter(...) diyorduk.
# Bu import anında SlowAPI'nin kendi içinde .env okumaya çalışmasına ve Windows encoding hatasına sebep oluyordu.
# Artık limiter'i doğrudan oluşturmuyoruz.
# Bunun yerine bir fonksiyon yazıyoruz ve limiter'i main.py içinde, app yaratılırken oluşturacağız.

def get_limiter():
    """
    Bu fonksiyon çağrıldığında yeni bir Limiter nesnesi döner.
    - key_func=get_remote_address  -> Hangi IP'nin limitleneceğini belirler.
    - default_limits=[settings.RATE_LIMIT] -> .env içindeki RATE_LIMIT değerini kullanır.
      Örn: "60/minute" = her IP dakikada en fazla 60 istek atabilir.
    """
    return Limiter(
        key_func=get_remote_address,
        default_limits=[settings.RATE_LIMIT]
    )
