# 🔥 Hayat Hattı – Firebase Veritabanı

## 📌 Genel Bakış

Hayat Hattı projesinin veritabanı altyapısı **Firebase Cloud Firestore** kullanılarak hazırlanmıştır.

Firebase'in ücretsiz **Spark planı** tercih edilmiş, kullanıcı bilgileri ve acil durum sinyallerinin saklanması için iki temel koleksiyon oluşturulmuştur.

## 🗂️ Firestore Koleksiyonları

### 1. users – Kullanıcı Bilgileri

Kullanıcıların profil bilgilerini saklamak amacıyla oluşturulmuştur.

| Alan | Veri Türü | Açıklama |
|---|---|---|
| `firstName` | string | Kullanıcının adı |
| `lastName` | string | Kullanıcının soyadı |
| `email` | string | E-posta adresi |
| `phone` | string | Telefon numarası |
| `createdAt` | timestamp | Hesap oluşturulma tarihi |

> **Not:** Kullanıcı şifreleri Firestore'da saklanmaz. Gerçek kullanıcı belgelerinin kimliği, Firebase Authentication tarafından oluşturulan UID ile eşleşecek şekilde tasarlanmıştır.

### 2. signals – Acil Durum Sinyalleri

Normal SOS ve Bluetooth Low Energy (BLE) üzerinden alınan yardım sinyallerini ortak bir koleksiyonda saklamak amacıyla oluşturulmuştur.

| Alan | Veri Türü | Açıklama |
|---|---|---|
| `signalId` | string | Sinyalin benzersiz kimliği |
| `userId` | string / null | SOS'u oluşturan kullanıcının kimliği |
| `latitude` | double | Konumun enlem bilgisi |
| `longitude` | double | Konumun boylam bilgisi |
| `message` | string | Yardım mesajı |
| `disasterType` | string / null | Afet türü |
| `source` | string | Sinyalin kaynağı (`app` / `ble`) |
| `status` | string | Sinyalin durumu |
| `createdAt` | timestamp / null | SOS oluşturulma zamanı |
| `receivedAt` | timestamp / null | BLE sinyalinin alınma zamanı |
| `originSignalId` | string | İlk SOS kaydının kimliği |
| `uploadedBy` | string | Kaydı Firebase'e yükleyen oturumun kimliği |
| `uploadedAt` | timestamp | Buluta kaydedilme zamanı |

> **Not:** Bazı alanlar sinyalin kaynağına göre isteğe bağlıdır.

## 🚨 Afet Türleri ve Test Kayıtları

Cloud Firestore üzerinde farklı afet türlerini temsil eden örnek SOS kayıtları oluşturulmuştur.

| Kayıt ID | Afet Türü | Kaynak |
|---|---|---|
| `test_signal_001` | Deprem (`earthquake`) | `app` |
| `test_signal_002` | Sel (`flood`) | `app` |
| `test_signal_003` | Yangın (`fire`) | `app` |
| `test_ble_001` | Belirtilmemiş (`null`) | `ble` |

Ayrıca `users` koleksiyonunda `test_user_001` kimliğiyle örnek bir kullanıcı kaydı oluşturulmuştur.

**Bu kayıtların tamamı test amaçlıdır ve gerçek yardım taleplerini temsil etmemektedir.**

## 🔎 Sorgu ve İndeks Kontrolleri

Firestore üzerinde `source` alanına göre filtreleme gerçekleştirilmiştir.

BLE kayıtlarını listelemek için kullanılan örnek sorgu:

```javascript
db.collection("signals")
  .where("source", "==", "ble")
```

Bu sorgu sonucunda yalnızca `test_ble_001` kaydının görüntülendiği doğrulanmıştır.

### Otomatik İndeksler

Firestore'un otomatik indeks ayarları kontrol edilmiştir.

| İndeks Türü | Durum |
|---|---|
| Ascending | ✅ Enabled |
| Descending | ✅ Enabled |
| Arrays | ✅ Enabled |

Bu aşamada manuel birleşik indeks oluşturulmamıştır.
