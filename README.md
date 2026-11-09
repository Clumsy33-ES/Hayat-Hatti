
# 🚨 Hayat Hattı – Acil Durum Destek Sistemi

**Hayat Hattı**, afet ve acil durum senaryolarında bireylerin konum ve yardım taleplerini hızlı şekilde iletebilmesini amaçlayan **Android tabanlı bir acil durum destek sistemi** prototipidir.

Proje; **Bluetooth Low Energy (BLE)**, **Kotlin mobil uygulaması** ve **Firebase** teknolojilerini bir araya getirerek internet bağlantısının bulunmadığı durumlarda da yardım sinyallerinin saklanmasını ve yakındaki cihazlara iletilmesini hedefler.

## 🎯 Amaç

- Afet anında **internet bağlantısı olmasa bile** BLE üzerinden yardım sinyallerinin iletilmesi
- Acil durum kayıtlarının telefonun yerel veritabanında saklanması
- İnternet bağlantısı sağlandığında bekleyen SOS kayıtlarının Firebase'e aktarılması
- Konum, mesaj ve zaman bilgisiyle **acil yardım taleplerinin kayıt altına alınması**
- Yardım taleplerinin güvenli ve takip edilebilir şekilde yönetilmesine yönelik altyapı oluşturulması

## 🧱 Genel Mimari

**BLE Cihaz / Telefon → Kotlin Mobil Uygulaması → Room / WorkManager → Firebase Cloud Firestore**

- **BLE:** Yakındaki cihazlar arasında internet olmadan SOS sinyallerinin iletilmesini sağlar.
- **Kotlin Mobil Uygulaması:** Kullanıcı arayüzünü, SOS işlemlerini ve cihaz özelliklerini yönetir.
- **Room:** İnternet olmadığında SOS kayıtlarını telefonun yerel veritabanında saklar.
- **WorkManager:** Bekleyen kayıtların internet bağlantısı sağlandığında gönderilmesini yönetir.
- **Firebase Authentication:** Kullanıcı kimlik doğrulama işlemleri için kullanılacaktır.
- **Cloud Firestore:** Kullanıcı bilgilerini ve SOS kayıtlarını merkezi olarak saklayacaktır.


## 🛠️ Teknolojiler

### Firebase / Veritabanı
- Firebase Cloud Firestore
- Firebase Authentication
- Firestore Security Rules
- Firebase Spark (Free) Plan

### Mobil
- Kotlin
- Jetpack Compose
- Material 3
- Firebase Android SDK (entegrasyon aşamasında)
- Room Database (çevrimdışı kayıt)
- WorkManager (otomatik senkronizasyon)
- DataStore + Android Keystore
- Bluetooth Low Energy (BLE)

### Diğer
- Git & GitHub
- Android Studio

## ✅ Temel Özellikler

- BLE üzerinden acil yardım sinyali gönderme ve alma altyapısı
- Konum bilgisi (latitude / longitude)
- SOS mesajı ve afet türü bilgisi
- Deprem, sel ve yangın gibi afet senaryoları
- Çevrimdışı SOS kayıtları için Room altyapısı
- İnternet bağlantısı sağlandığında kayıtların senkronize edilmesine yönelik WorkManager altyapısı
- Firebase Firestore üzerinde kullanıcı ve SOS veri modeli
- Zaman damgalı kayıtlar (timestamp)
- Modüler ve genişletilebilir mimari

## 📱 Mobil Uygulama (Kotlin Frontend)

> Kotlin mobil uygulamasının arayüzü ve mevcut API bağlantısı **Neslihan LOKMAN** tarafından geliştirilmiştir.

Uygulama `mobile/` klasöründe bulunmaktadır.

### Ekranlar

| Giriş | İzinler | Konum İzni |
|:---:|:---:|:---:|
| <img src="docs/screenshots/01-giris.png" width="220" alt="Giriş ekranı"> | <img src="docs/screenshots/02-izinler.png" width="220" alt="İzinler ekranı"> | <img src="docs/screenshots/03-konum-izni.png" width="220" alt="Konum izni penceresi"> |

| SOS | SOS Açık | Profil |
|:---:|:---:|:---:|
| <img src="docs/screenshots/04-sos.png" width="220" alt="SOS ekranı"> | <img src="docs/screenshots/05-sos-aktif.png" width="220" alt="SOS açıkken"> | <img src="docs/screenshots/06-profil.png" width="220" alt="Profil ekranı"> |

## 👥 Ekip Çalışması Görev Dağılımı

- **Backend / Firebase Güvenlik** → ELİF SAKAR
- **Firebase Veritabanı** → FİRDEVS KÖSE
- **Frontend (Kotlin Mobil Uygulama)** → NESLİHAN LOKMAN
- **BLE** → BEYDA KIZILDAĞ
- **Test** → ŞEVAL PÖZE
