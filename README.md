# 🚨 Hayat Hattı – Acil Durum Destek Sistemi

**Hayat Hattı**, afet ve acil durum senaryolarında bireylerin konum ve yardım taleplerini hızlı şekilde iletebilmesini amaçlayan **mobil + backend** tabanlı bir destek sistemidir.  
Proje; **Bluetooth Low Energy (BLE)**, mobil uygulama ve **REST API** mimarisini birlikte kullanır.

## 🎯 Amaç

- Afet anında **internet olmasa bile** BLE üzerinden sinyal toplanması
- Mobil uygulama aracılığıyla verilerin **backend’e iletilmesi**
- Konum, mesaj ve zaman bilgisiyle **acil yardım taleplerinin kayıt altına alınması**
- Yetkililer için **takip edilebilir bir altyapı** oluşturulması

## 🧱 Genel Mimari

BLE Cihaz / Beacon → Flutter Mobil Uygulama → FastAPI Backend → PostgreSQL / MongoDB

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
- Flutter
- BLE (Bluetooth Low Energy)
- REST API entegrasyonu

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

Ekip Çalışması Görev Dağılımı:
-Backend --> ELİF SAKAR
-Veri Tabanı --> FİRDEVS KÖSE
-Fontend --> NESLİHAN LOKMAN
-BLE --> BEYDA KIZILDAĞ
-Test --> ŞEVAL PÖZE
