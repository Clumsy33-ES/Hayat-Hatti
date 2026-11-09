🔥 Hayat Hattı – Firebase Veritabanı

📌 Genel Bakış

Hayat Hattı projesinin veritabanı altyapısı, Firebase Cloud Firestore kullanılarak hazırlanmıştır.

Firebase'in ücretsiz Spark planı tercih edilmiş, kullanıcı bilgileri ve acil durum sinyalleri için iki temel koleksiyon oluşturulmuştur.

🗂️ Firestore Koleksiyonları

1. users – Kullanıcı Bilgileri

Kullanıcıların profil bilgilerini saklamak için oluşturulmuştur.

Alan

Veri Türü

Açıklama

firstName

string

Kullanıcı adı

lastName

string

Kullanıcı soyadı

email

string

E-posta adresi

phone

string

Telefon numarası

createdAt

timestamp

Oluşturulma tarihi

Kullanıcı şifreleri Firestore'da saklanmaz. Kullanıcı belgelerinin kimliği Firebase Authentication UID'siyle eşleşecek şekilde tasarlanmıştır.

2. signals – Acil Durum Sinyalleri

Normal SOS ve BLE üzerinden alınan sinyaller aynı koleksiyonda tutulacak şekilde tasarlanmıştır.

Alan

Veri Türü

Açıklama

signalId

string

Sinyalin benzersiz kimliği

userId

string / null

Kullanıcı kimliği

latitude

double

Enlem

longitude

double

Boylam

message

string

Yardım mesajı

disasterType

string / null

Afet türü

source

string

Sinyalin kaynağı (app / ble)

status

string

Sinyalin durumu

createdAt

timestamp

Oluşturulma zamanı

receivedAt

timestamp

BLE sinyalinin alınma zamanı

originSignalId

string

İlk SOS kaydının kimliği

uploadedBy

string

Kaydı yükleyen oturumun kimliği

uploadedAt

timestamp

Buluta kaydedilme zamanı

Bazı alanlar sinyal türüne göre isteğe bağlıdır.

🚨 Afet Türleri ve Test Kayıtları

Firestore üzerinde aşağıdaki test kayıtları oluşturulmuştur:

Kayıt ID

Afet Türü

Kaynak

test_signal_001

Deprem (earthquake)

app

test_signal_002

Sel (flood)

app

test_signal_003

Yangın (fire)

app

test_ble_001

BLE SOS

ble

Bu kayıtlar yalnızca test amaçlıdır.

🔎 Sorgu ve İndeks Kontrolleri

Firestore üzerinde source alanına göre filtreleme yapılmış ve BLE kayıtlarının ayrı görüntülenebildiği doğrulanmıştır.

Otomatik tek alanlı indekslerin etkin olduğu kontrol edilmiştir:

Ascending

Descending

Arrays
