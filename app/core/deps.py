#deps dependency anlamına geliyor ve türkçe karşılığı bağımlılıklar demek . amacı ise farklı modüllerde ortak kullanılan fonksiyonları burada toplamak ve ihtiyaç duyulan yerlerde bu fonksiyonları import ederek kullanmak. sanki bir kütüphane gibi düşünülebilir.

from fastapi import Header, HTTPException # HTTPException, Header modüllerini fastapi kütüphanesinden import ediyoruz. HTTPException, HTTP hatalarını yönetmek için kullanılırken, Header ise HTTP başlıklarını almak için kullanılır.
#FastapıninHeader: FastAPI’nin “dependency” sisteminde HTTP header değerlerini fonksiyon parametresi olarak enjekte etmek için kullanılır.
#FastAPI’ye “isteğin içindeki HTTP başlıklarından (headers) veri alacağım” dememizi sağlar. ve burdada authorization başlığını alıyoruz.
import jwt #tokenı (açmak) decode etmek ve süresi dolmuş mu diye kontrol etmek için kullanıyoruz.
from app.core.config import settings #settings modülünü app.core.config paketinden import ediyoruz. settings, uygulamanın yapılandırma ayarlarını içerir. bizim envdeki ayarlarımızı tutuyor. örneğin JWT_SECRET gibi.

async def get_current_user(authorization: str = Header(None)):#bu bizim kimlik doğrulama fonksiyonumuz. authorization başlığını alıyor. korumalı endpointlere girmek istiyorsak bu fonksiyonu kullanacağız.örneğin api/signals endpointine girmek istiyorsak önce bu fonksiyon
    #authorization: str= Header(None) ifadesi gelen isteğin HTTP başlıklarından "Authorization" başlığını alır. Eğer bu başlık yoksa None değeri atanır. token isteği olmadığı için none olur. Token anlamı nedir? Token, kimlik doğrulama ve yetkilendirme için kullanılan dijital bir anahtardır. Genellikle kullanıcıların kimliklerini doğrulamak ve belirli kaynaklara erişim izni vermek için kullanılır.
    #async def ifadesi bu fonkdiyonun asenkron yani diğer işlemleri beklemeden çalışmasını sağlar. bu özellik fastApi de performans için önerilir
    if not authorization or not authorization.startswith("Bearer "):#başlık olarak authorization var mı ya da bearer ile mi başlıyor diye kontrol ederiz başlamıyorsa hata verir.Bearer token, genellikle OAuth 2.0 ve JWT (JSON Web Token) gibi kimlik doğrulama sistemlerinde kullanılan bir tür erişim belirtecidir. "Bearer" ifadesi, tokenın türünü belirtir ve genellikle HTTP Authorization başlığında kullanılır. güvenlik amacıyla kullanılır.
        raise HTTPException(401, "Missing Bearer token")
    token = authorization.split(" ", 1)[1]#normalde berarer <token> kısmını alırız. token yerine  split(" ", 1)[1] ifadesi alınır. [0] = "Bearer", [1] = "<token>" demektir.
    #split(" ", 1) ifadesi, authorization stringini ilk boşluk karakterinden böler. 1 parametresi, sadece bir kez bölme işlemi yapılacağını belirtir. Böylece, "Bearer <token>" ifadesi iki parçaya ayrılır
    try:
        return jwt.decode(token, settings.JWT_SECRET, algorithms=["HS256"])#ilk önce decode ile token açılır sonra bu tokenın doğruluğu JWT_SECRET ile kontrol edilir. algoritma olarak HS256 kullanılır sadece bu imzalamayla kabul edilir. eğer token geçerliyse içindeki payload döndürülür.
    except jwt.ExpiredSignatureError:
        raise HTTPException(401, "Token expired")  #token süresi geçmişse bu hatayı verir
    except Exception:
        raise HTTPException(401, "Invalid token")#geri kalan tüm hatalar için geçersiz token hatası verir
    
    
'''Bu dosya, sistemin “kimlik kontrol kapısı”dır.
Yani kullanıcı bir endpoint’e erişmek isterse:

Token göndermiş mi diye bakılır.

Token doğru formatta mı kontrol edilir.

Token geçerli mi (süresi dolmuş mu, imzası doğru mu) bakılır.

Eğer hepsi doğruysa, kullanıcı içeri alınır.

Eğer biri yanlışsa → sistem hemen 401 döndürür.'''