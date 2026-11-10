# 🚨 Hayat Hattı – Acil Durum Destek Sistemi

**Hayat Hattı**, afet ve acil durum senaryolarında bireylerin konum ve yardım taleplerini hızlı şekilde iletebilmesini amaçlayan **mobil + backend** tabanlı bir destek sistemidir.  
Proje; **Bluetooth Low Energy (BLE)**, mobil uygulama ve **REST API** mimarisini birlikte kullanır.

## 🎯 Amaç

- Afet anında **internet olmasa bile** BLE üzerinden sinyal toplanması
- Mobil uygulama aracılığıyla verilerin **backend’e iletilmesi**
- Konum, mesaj ve zaman bilgisiyle **acil yardım taleplerinin kayıt altına alınması**
- Yetkililer için **takip edilebilir bir altyapı** oluşturulması

## 🧱 Genel Mimari

BLE Cihaz / Beacon → Kotlin Mobil Uygulama → FastAPI Backend → PostgreSQL / MongoDB

> Not: Backend, BLE ile doğrudan konuşmaz. Akış: **BLE → Mobil → Backend**


## 🛠️ Teknolojiler

### Backend
- Python (FastAPI)
- Pydantic
- SQLAlchemy
- PostgreSQL (Neon / Railway)
- MongoDB (opsiyonel)
- Railway (deploy)

### Mobil
- Kotlin, Jetpack Compose, Material 3
- Retrofit + kotlinx.serialization (REST API entegrasyonu)
- Room (çevrimdışı gönderim kuyruğu), WorkManager (otomatik yeniden gönderim)
- DataStore + Android Keystore (oturum bilgisinin şifreli saklanması)
- BLE (Bluetooth Low Energy)

### Diğer
- Swagger / OpenAPI
- Git & GitHub

## ✅ Temel Özellikler

- BLE üzerinden acil sinyal algılama
- Konum bilgisi gönderimi (latitude / longitude)
- Acil mesaj ve durum kaydı
- Zaman damgalı kayıt (timestamp)
- Swagger arayüzü ile API test edilebilirliği
- Modüler ve genişletilebilir yapı

## 📱 Mobil Uygulama (Kotlin Frontend)

> Kotlin mobil uygulamasının arayüzü ve backend bağlantısı **Neslihan LOKMAN** tarafından geliştirilmiştir.

Uygulama `mobile/` klasöründedir. Backend'deki giriş, kayıt, sinyal ve afet özeti uç noktalarını mobil arayüze taşır.

### Ekranlar

| Giriş | İzinler | Konum izni |
|:---:|:---:|:---:|
| <img src="docs/screenshots/01-giris.png" width="220" alt="Giriş ekranı"> | <img src="docs/screenshots/02-izinler.png" width="220" alt="İzinler ekranı"> | <img src="docs/screenshots/03-konum-izni.png" width="220" alt="Konum izni penceresi"> |

| SOS | SOS açık | Profil |
|:---:|:---:|:---:|
| <img src="docs/screenshots/04-sos.png" width="220" alt="SOS ekranı"> | <img src="docs/screenshots/05-sos-aktif.png" width="220" alt="SOS açıkken"> | <img src="docs/screenshots/06-profil.png" width="220" alt="Profil ekranı"> |

### Özellikler
- **Giriş ve kayıt:** `/api/auth/login` ve `/api/auth/register` ile çalışır; oturum bilgisi telefonda şifreli saklanır.
- **Hesapsız SOS:** Acil durumda hesap gerekmez. Hesapsız SOS `/api/ble-data`, hesaplı SOS `/api/signals` adresine gider.
- **SOS:** Butona basılı tutularak başlatılır; afet türü, en çok 4 karakterlik kısa mesaj ve isteğe bağlı not eklenebilir.
- **Çevrimdışı çalışma:** İnternet yokken gönderimler kuyrukta bekler, bağlantı gelince otomatik gönderilir.
- **Sinyal listesi:** PostgreSQL, MongoDB ve BLE kayıtları listelenir; türe göre filtrelenebilir, detayları açılabilir.
- **Afet özeti:** `/api/disasters` verisi gösterilir.
- **Sunucu ayarı:** Backend adresi uygulama içinden değiştirilebilir.

> BLE modülü bu dalda uygulamaya bağlandı. Ayrıntılar aşağıdaki **BLE** bölümünde.

### Backend tarafındaki değişiklikler
- `GET /api/ble-data` eklendi (BLE kayıtlarını listeler).
- `GET /api/users`, `GET /api/signals`, `GET /api/signals/mongo` ve `GET /api/ble-data` artık giriş ister (`Authorization: Bearer <token>`).
- SOS gönderimi (`POST /api/signals`, `POST /api/ble-data`) ve `/api/disasters` giriş istemez.

### Çalıştırma
1. Android Studio'da `mobile/` klasörünü açın (ana klasörü değil). Projede AGP 8.11.2 kullanıldığı için güncel bir Android Studio gerekir.
2. Backend'i başlatın: `uvicorn app.main:app --host 0.0.0.0 --port 8000` (`DATABASE_URL`, `MONGO_URI`, `MONGO_DB_NAME`, `JWT_SECRET_KEY` ortam değişkenleri ayarlı olmalı).
3. Emülatörde varsayılan adres `http://10.0.2.2:8000`'dir. Gerçek telefonda **Profil → Sunucu ayarı** bölümüne bilgisayarın yerel ağ adresini yazın.

### Ekip Çalışması Görev Dağılımı:
- Backend → ELİF SAKAR
- Veri Tabanı → FİRDEVS KÖSE
- Frontend (Kotlin mobil uygulama) → NESLİHAN LOKMAN
- BLE → BEYDA KIZILDAĞ
- Test → ŞEVAL PÖZE

## 📡 BLE – `feature/ble-entegrasyon` dalı

> BLE modülü **Beyda KIZILDAĞ** tarafından geliştirilmiştir. Paket formatı, gizlilik notları ve adım adım test rehberi: [`docs/BLE.md`](docs/BLE.md)

Bu dal BLE modülünü Kotlin uygulamasına bağlar. Main'e henüz birleştirilmedi; Firebase geçişi tamamlandıktan sonra entegre edilecek.

### Yapılanlar
- **Paket formatı v1:** SOS, BLE reklamının üretici verisi alanında taşınıyor (en çok 27 bayt): gönderen kimliği, konum (yaklaşık 11 cm hassasiyet), bayraklar ve 12 bayta kadar mesaj. Eski formattaki 31 bayt taşması (Android "hata 1") ve Türkçe harflerin ortadan bölünmesi giderildi.
- **Yayın ve tarama:** `AndroidBleGateway`, uygulamanın `BleGateway` arayüzünü gerçek Bluetooth ile uygular. Uygulama açıkken sürekli tarar, SOS başlatılınca konumu ve mesajı yayınlar.
- **Kendiliğinden toparlanma:** Bluetooth kapanıp açılınca ya da izin sonradan verilince tarama ve açık SOS yayını aynı gönderen kimliğiyle geri gelir.
- **Kuyruğa yazma:** Başka telefondan duyulan SOS'lar telefonun yerel kuyruğuna (Room) yazılır ve internet gelince mevcut senkron sistemiyle (WorkManager) gönderilir. Aynı sinyal 30 saniye içinde tekrar kaydedilmez.
- **Gizlilik:** Pakette ad, telefon numarası ya da cihaz kimliği yok; gönderen kimliği her SOS'ta rastgele seçilir. Konum ve mesaj yalnızca debug sürümünde loga yazılır.
- **Test:** 15 BLE birim testi (uygulamada toplam 27). Samsung Galaxy A34 (Android 16) üzerinde cihaz testi yapıldı: tarama, yayın, sinyal alma, Bluetooth kesintisi ve SOS durdurma çalışıyor.

### Nerede kaldık
- Dal GitHub'da, main'e birleştirilmedi.
- Cihaz testi bir telefonla yapıldı; ikinci cihaz olarak bir bilgisayarın Bluetooth'u kullanıldı. İki telefonda uygulamadan uygulamaya test henüz yapılmadı.
- **SOS kimliği uçtan uca korunmuyor.** Paketteki gönderen kimliği Room'a aktarılmıyor; Room'daki kayıt kimliği konum, mesaj ve 10 dakikalık zaman diliminden üretiliyor. Bu yüzden aynı SOS, farklı telefonlarda ya da 10 dakikadan uzun sürdüğünde birden çok kayıt oluşturabiliyor. SOS'u başlatan telefonun kendi kaydı da bu kimlikle eşleşmiyor.
- BLE izinleri ("Yakındaki cihazlar") uygulama ekranında henüz istenmiyor; testlerde elle verildi.

### Sıradaki adımlar
1. **SOS kimliğini uçtan uca taşımak:** Kimlik SOS başlatılırken bir kez üretilecek; hem telefonun kendi kaydında hem BLE paketinde kullanılacak, `BleIncoming` üzerinden Room'a ve Firestore'a kadar taşınacak. Gönderen kimliği 4 bayt olduğu için Firestore belge kimliğinde zaman bilgisiyle birlikte kullanılacak. Firebase veri yapısıyla birlikte netleştirilecek.
2. **Ekran kapalıyken çalışma:** Uzun süreli yayın ve dinleme için foreground service.
3. **Konumsuz SOS:** GPS yokken de yayın; sinyali duyan telefon kendi konumunu ve sinyal gücünü ekleyecek.
4. **Durum kodları:** "Yaralı", "enkaz altında", kişi sayısı gibi bilgiler paketteki bayraklar baytına.
5. **Kurtarıcı modu:** Sinyal gücüyle enkazdaki kişinin yönünü bulma.
6. **Şirket kimliği:** `0xFFFF` test kimliği yerine Bluetooth SIG'den alınacak kimlik.
