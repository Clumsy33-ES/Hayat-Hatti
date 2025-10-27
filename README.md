# ⚙️ Hayat Hattı – BLE Modülü (feature/ble-module)

Bu branch, **Hayat Hattı** projesinin **Bluetooth Low Energy (BLE)** modülünü içerir.  
Amaç, afet durumlarında internet bağlantısı olmadan **SOS sinyali** yaymak ve yakındaki cihazlardan bu sinyalleri **algılayıp veritabanına aktarmaktır**.

---

## 🚨 Modülün Amacı

Bu modül, göçük altında veya sinyalin kesildiği durumlarda cihazların Bluetooth aracılığıyla
yardım çağrısı göndermesini sağlar.  

  Android uygulaması BLE sinyali algılar veya acil durum yayını yapar.
	Uygulama bu veriyi JSONObject olarak hazırlar.
	Eğer internet varsa, backend’e HTTP POST isteğiyle gönderir → MongoDB Atlas’a kaydedilir.
	Eğer internet yoksa, veriler lokal PostgreSQL’e (offline DB) kaydedilir.
	Bağlantı yeniden sağlandığında, lokal PostgreSQL’deki kayıtlar otomatik olarak MongoDB’ye senkronize edilir.


---


## 🧩 Mimarinin Genel Akışı

```mermaid
graph TD
    A[MainActivity] --> B[PermissionHelper (Bluetooth izinleri)]
    B --> C[BleManager]
    C --> D[BleAdvertiser - SOS mesajı yayınlar]
    C --> E[BleScanner - Yakındaki SOS sinyallerini algılar]
    E --> F[LocalRepository - Verileri PostgreSQL'e kaydeder]
    F --> G[Backend REST API (Spring Boot / PostgreSQL)]
    style A fill:#b3e5fc,stroke:#0277bd,stroke-width:2px
    style C fill:#fff9c4,stroke:#fbc02d,stroke-width:2px
    style G fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px

---
## 📱 Test Adımları

1️⃣ Uygulamayı iki Android cihazda (Android 12+) çalıştır.  
2️⃣ Bir cihazda **“SOS BAŞLAT”** butonuna tıkla.  
3️⃣ Logcat çıktısı aşağıdaki gibi olmalıdır:  

BLE: Advertising started successfully
BLE: Scanning started...
DB: Saved locally (PostgreSQL mock): SOS:37.4,38.5
4️⃣ Diğer cihaz, sinyali algıladığında şunu görürsün: 
5️⃣ “SOS DURDUR” butonu, hem advertise hem scan işlemlerini durdurur.

---

## 🔒 İzinler

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
🔸 BLE işlemleri Android 12 (API 31) ve üzeri sürümlerde runtime izin gerektirir.
🔸 PermissionHelper sınıfı bu izinleri dinamik olarak ister.

---

🧱 Katmanlar
Sınıf	Görevi
BleAdvertiser	SOS mesajını BLE üzerinden yayınlar.
BleScanner	Yakındaki SOS sinyallerini tarar.
BleManager	Advertiser & Scanner'ı birlikte yönetir.
PermissionHelper	Android izinlerini kontrol eder.
LocalRepository	Alınan mesajları mock veritabanına kaydeder.
 
 ---
 
🧠 Backend Entegrasyonu (Yapılacak)

Backend tarafı, BLE sinyallerinin REST API aracılığıyla PostgreSQL’e kaydedilmesini sağlayacak.
Mevcut durumda LocalRepository, mock olarak çalışmaktadır.

Beklenen örnek API:POST /api/sos
Content-Type: application/json

{
  "deviceId": "ABC123",
  "latitude": 37.4,
  "longitude": 38.5,
  "timestamp": "2025-10-27T08:54:00Z"
}

---

🧾 Branch Bilgisi

Bu dosya ve BLE modülü şu anda
👉 feature/ble-module branch’indedir.
Farklı bir cihazla test edilmemiştir.

Kodlar henüz main branch’e merge edilmemiştir.
Ekip arkadaşlarım bu branch üzerinden inceleme, test ve pull request review işlemlerini yapabilir.

👩‍💻 Geliştirici
Beyda Kızıldağ
📱 Android BLE – SOS Acil Yardım Modülü
💡 Hayat Hattı Projesi (Afet Sonrası Yardım Ağı)

