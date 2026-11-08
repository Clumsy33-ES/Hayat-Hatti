# 📡 BLE Modülü

> BLE modülü **Beyda KIZILDAĞ** tarafından geliştirilmiştir.

İnternet olmadan, telefonlar arasında Bluetooth Low Energy (BLE) yayınıyla SOS sinyali taşır.

- SOS başlatılınca telefon konumunu ve kısa mesajı çevreye yayınlar. Paket 12 bayta kadar mesaj taşır; SOS ekranındaki alan şimdilik 4 karakterle sınırlı.
- Uygulama açıkken telefon sürekli dinler. Duyduğu sinyalleri **Yakındaki sinyaller** ekranında gösterir, kuyruğa yazar ve internet gelince `/api/ble-data` (hesap varsa `/api/signals`) adresine gönderir.
- Backend BLE ile doğrudan konuşmaz. Akış: **BLE → Mobil → Backend**.

## Dosyalar

Hepsi `mobile/app/src/main/java/com/hayathatti/app/ble/` altında:

| Dosya | Görevi |
|---|---|
| `BleGateway.kt` | Ekranların kullandığı arayüz (Neslihan'ın yazdığı bağlantı noktası, değişmedi) |
| `AndroidBleGateway.kt` | Gerçek uygulama: yayın, tarama, Bluetooth aç/kapa ve izin takibi |
| `BlePacket.kt` | Paketin baytlara çevrilmesi ve geri okunması; tekrar eden sinyalleri süzen `SignalThrottle` |
| `BlePermissions.kt` | Android sürümüne göre gereken izinler (`BlePermissions.required()`) |

Testler: `mobile/app/src/test/java/com/hayathatti/app/ble/BlePacketTest.kt`

## Paket formatı (sürüm 1)

Paket, BLE reklamının **üretici verisi** (manufacturer specific data) alanında taşınır. Şirket kimliği `0xFFFF`, Bluetooth SIG'in test ve geliştirme için ayırdığı değerdir.

Klasik reklam en çok 31 bayttır. Üretici verisinin başlığı 4 bayt tutar, geriye paket için **27 bayt** kalır. Bayt sırası little endian:

| Bayt | Boyut | Alan |
|---|---|---|
| 0 | 1 | İşaret `0x48` ('H'). Başka uygulamaların paketlerini ayırır |
| 1 | 1 | Sürüm (`1`) |
| 2 | 4 | Gönderen kimliği. Her SOS başlatıldığında rastgele seçilir |
| 6 | 4 | Enlem × 1.000.000 (int32) |
| 10 | 4 | Boylam × 1.000.000 (int32) |
| 14 | 1 | Bayraklar. Şimdilik `0`; afet türü ve röle sayacı için ayrıldı |
| 15 | 0–12 | Kısa mesaj, UTF-8. Harf ortasından kesilmez |

**Eski sürümden farkı:** Eski sürüm 128 bit UUID'li servis verisi kullanıyordu. Onun başlığı 18 bayt olduğu için 5 baytlık bir mesajda paket 32 bayta çıkıyor ve Android "hata 1" (`ADVERTISE_FAILED_DATA_TOO_LARGE`) veriyordu. Türkçe harfler (ş, ğ, İ) UTF-8'de 2 bayt olduğundan 4 harflik bir mesaj bile sınırı aşabiliyordu. Yeni formatta 4 Türkçe harf (8 bayt) rahatça sığar.

> Eski test uygulaması (`com.example.myapplication`) yeni paketi okuyamaz. Karışıklık olmaması için telefondan kaldırın.

**Örnek paket** (birim testindeki değerlerle aynı): gönderen `0x0BADBEEF`, konum 38.6748, 39.2225 (Elazığ), mesaj `ENKZ`:

```
4801EFBEAD0B70214E02E47C560200454E4B5A
```

## Gizlilik ve güvenlik

- **Yayın şifresiz ve herkese açıktır.** Yakındaki her BLE tarayıcı (ör. nRF Connect) SOS'taki konumu ve mesajı okuyabilir. Yardım sinyalinin herkese ulaşması için bu bilinçli bir tercihtir.
- **Pakette kimlik bilgisi yoktur.** Ad, telefon numarası ya da cihaz kimliği taşınmaz. Gönderen kimliği her SOS'ta rastgele seçilir ve kişiyle eşleştirilemez. Android de cihazın Bluetooth adresini düzenli olarak değiştirir.
- **Konum ve mesaj loga yazılmaz.** Bu bilgiler yalnızca debug sürümünde Logcat'e düşer.
- **Paket imzasızdır.** Herkes sahte SOS yayınlayabilir; aşağıdaki Test 2 tam olarak bunu yapar.
- **`0xFFFF` yalnızca test içindir.** Bu şirket kimliği Bluetooth SIG tarafından test ve geliştirme için ayrılmıştır. Ürün sürümünde Bluetooth SIG'den alınacak şirket kimliğiyle değiştirilecek (`BlePacket.MANUFACTURER_ID`).

## Test rehberi

### Backend gerekli mi?

**BLE testleri için gerekli değil.** BLE telefondan telefona çalışır. Duyulan sinyaller internet yokken de **Yakındaki sinyaller** ekranında görünür ve kuyrukta "bekliyor" olarak durur. Backend yalnızca son aşamada (sunucuya aktarım) gerekir.

### Hazırlık

1. Android Studio'da **`mobile/` klasörünü** açın (ana klasörü değil). Proje AGP 8.11.2 kullanıyor, Android Studio'nun güncel olması gerekir.
2. Telefonda **Geliştirici seçenekleri → USB hata ayıklama**'yı açın ve Type-C kabloyla bağlayın.
3. Android Studio'da cihazı seçip **Run** (▶) ile kurun. Kablosuz hata ayıklama da olur.
4. Telefonda **Bluetooth açık** olmalı.

### İzinler

Uygulamanın izin ekranı şimdilik yalnızca konum izni istiyor. BLE izinlerini test için elle verin:

- **Android 12 ve üstü:** Ayarlar → Uygulamalar → Hayat Hattı → İzinler → **Yakındaki cihazlar → İzin ver**
- **Android 8–11:** Konum iznini verin (uygulama zaten istiyor) ve **konum servisini (GPS) açık** tutun. Bu sürümlerde tarama için bu şart.

Komutla vermek isterseniz (Android 12+, telefon kabloyla bağlıyken, PowerShell):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm grant com.hayathatti.app android.permission.BLUETOOTH_SCAN
```

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm grant com.hayathatti.app android.permission.BLUETOOTH_ADVERTISE
```

İzni verip uygulamaya dönünce tarama kendiliğinden başlar, uygulamayı yeniden başlatmak gerekmez.

### Logcat

Android Studio → **Logcat** → filtre: `tag:HH-BLE`. Beklenen satırlar:

```
Tarama başladı
Yayın başladı: gönderen=… konum=38.6748,39.2225 mesaj='ENKZ'
Sinyal alındı: gönderen=… konum=… mesaj='…' rssi=-62
```

Konum ve mesaj kişisel veri olduğu için yalnızca **debug** sürümünde loga yazılır. Release sürümünde bu satırlarda sadece gönderen kimliği görünür.

### Test 1: Telefon yayın yapıyor mu? (tek telefon + nRF Connect)

Bir telefon kendi yayınını göremez. Doğrulamak için ikinci bir telefona ücretsiz **nRF Connect for Mobile** (Nordic Semiconductor) uygulamasını kurun. iPhone da olur.

1. Kendi telefonunuzda SOS'u basılı tutarak başlatın, mesaja örneğin `YRDM` yazın. SOS konum alamazsa başlamaz; GPS açık olsun, gerekirse pencere kenarına geçin.
2. İkinci telefonda nRF Connect → **Scanner** → **Scan**.
3. Listede adı olmayan bir cihazda **Manufacturer data** satırı görünmeli: şirket `0xFFFF`, veri `0x4801…` ile başlar.
4. Verinin sonunda mesajın baytları olmalı (`YRDM` = `5952444D`).

### Test 2: Telefon sinyal alıyor mu? (tek telefon + başka bir Android'de nRF Connect)

nRF Connect, Android'de sahte bir SOS yayını da yapabilir (iPhone'da bu özellik yok).

1. İkinci Android telefonda nRF Connect → **Advertiser** sekmesi → **+** (yeni paket).
2. **Add record → Manufacturer data**: Company ID `0xFFFF`, Data: `4801EFBEAD0B70214E02E47C560200454E4B5A`
3. Yayını açın.
4. Kendi telefonunuzda **Yakındaki sinyaller** ekranında `ENKZ` ve `38.6748, 39.2225` görünmeli. Karta dokununca harita açılır.

### Test 3: İki telefonda uygulama

1. Uygulamayı iki telefona da kurun (ikisini sırayla kabloyla bağlayıp Run).
2. A telefonunda **interneti kapatın** (uçak modu + Bluetooth açık) ve SOS başlatın.
3. B telefonunda **Yakındaki sinyaller** ekranında sinyal birkaç saniye içinde görünmeli.
4. A'da SOS'u durdurun. B'de yeni kayıt gelmemeli; eski kayıt listede kalır.
5. Bluetooth'u kapatıp açın: tarama ve SOS yayını kendiliğinden geri gelmeli (Logcat'te görülür).

### Test 4: Sunucuya aktarım (isteğe bağlı, backend çalışırken)

1. B telefonu internete bağlıyken **Profil → Sunucu ayarı** bölümüne backend adresini yazın (bilgisayardaki backend için aynı Wi-Fi'deki yerel IP, örn. `http://192.168.1.20:8000`).
2. Test 3'ü tekrarlayın. B'deki kayıt "gönderildi" durumuna geçmeli; Swagger'da `GET /api/ble-data` ile görülebilir (giriş gerekir).

### Sık görülen sorunlar

| Logcat'te | Anlamı / çözüm |
|---|---|
| `Tarama izni yok` / `BLUETOOTH_ADVERTISE izni yok` | Yukarıdaki izinleri verin |
| `Bluetooth kapalı` | Bluetooth'u açın; açılınca kendiliğinden devam eder |
| `Bu cihaz BLE yayınını desteklemiyor` | Cihaz donanımı yayın yapamıyor; bu telefon yalnızca alıcı olarak kullanılabilir |
| `Yayın başlatılamadı: paket 31 baytı aşıyor (1)` | Olmamalı; görülürse paket formatını kontrol edin |
| Android 8–11'de sinyal gelmiyor | Konum izni ve konum servisi (GPS) açık mı? |

## Cihaz testi (8 Kasım 2026)

Samsung Galaxy A34 (Android 16), debug sürümü. İkinci cihaz olarak bir Windows bilgisayarın Bluetooth'u kullanıldı: bilgisayar hem sahte SOS yayınladı hem de telefonun yayınını dinledi.

| Test | Sonuç |
|---|---|
| Uygulama açılınca tarama (filtre `0xFFFF` / `48 01`) | Geçti |
| SOS → yayın; bilgisayar paketi duydu ve doğru çözdü (1–2 m'de RSSI yaklaşık −50) | Geçti |
| Bilgisayardan gelen SOS'un **Yakındaki sinyaller** ekranına düşmesi | Geçti |
| Aynı sinyalin 30 sn içinde tekrar kaydedilmemesi | Geçti |
| SOS açıkken Bluetooth kapat-aç: yayının aynı gönderen kimliğiyle geri gelmesi | Geçti |
| Bluetooth kapalıyken SOS başlatma ve durdurma | Geçti |
| SOS durdurulunca yayının durması, taramanın sürmesi | Geçti |

Denenmedi: iki telefonda uygulamadan uygulamaya, ekran uzun süre kapalıyken çalışma, Android 8–11 cihazlar, sunucuya aktarım.

## Bilinen eksikler ve sonraki adımlar

- **Şirket kimliği:** `0xFFFF` yerine Bluetooth SIG'den alınacak kimliğe geçilecek (bkz. Gizlilik ve güvenlik).
- **Arka planda çalışma:** Şu an uygulama açıkken (ve kısa süre arka plandayken) çalışır. Ekran uzun süre kapalıyken de yayın ve dinleme için bir *foreground service* (bildirimde "SOS yayında") eklenecek.
- **Konumsuz SOS:** Konum alınamazsa SOS hiç başlamıyor. Enkaz altında sık görülebilecek bu durumda da yayın yapılmalı; sinyali duyan telefon kendi konumunu ve sinyal gücünü ekleyebilir.
- **Gönderen kimliğinin iletilmesi:** `BleIncoming`'e gönderen kimliği eklenecek. Böylece aynı SOS'u duyan farklı telefonların kayıtları tek SOS olarak eşleştirilebilecek.
- **Afet türü:** Paket içinde yer ayrıldı (bayraklar baytı). `BleGateway.startSos` türü almadığı için şimdilik `0` gönderiliyor.
- **Mesh (röle):** Duyulan sinyalin başka telefonlarca yeniden yayınlanması. Bayraklar baytı röle sayacı için kullanılabilir.
- **iPhone:** iOS üretici verisi yayınına izin vermediği için bu paket iPhone'dan yayınlanamaz. iPhone desteği ayrı bir tasarım gerektirir.
