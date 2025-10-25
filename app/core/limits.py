from slowapi import Limiter
from slowapi.util import get_remote_address #yardımcı bir fonksiyondur get_remote_adress i otomatik olarak döndürür ve istek yapanın IP adresini alır.
#sistemin kime limit yapacağını belirler. 
from app.core.config import settings # bu fonksiyon env deki settingsleri  çeker özellikle RATE_LIMIT i çekeriz.ki RATE_LİMİT:str=60/minute olarak ayarladık.test ortamnda 1000/ hour yapabiliriz.

limiter = Limiter(key_func=get_remote_address, default_limits=[settings.RATE_LIMIT]) # bu satırda herşeyi birleştiren yeni mir limiter nesnesi oluşturuyoruz.
#slowapı kutuphanesinden limiter sınıfını getirir. bu sayede istenilen istekleri sınırlayabiliriz.
#limitlememizin sebebi backendin çökmesini önlemek ve spamı önlemektir.
#yani her ip adresi dakikada 60 istek atabiliyor.