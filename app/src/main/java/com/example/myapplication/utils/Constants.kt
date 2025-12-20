package com.example.myapplication.utils

import java.util.UUID
object Constants {
    // Tüm projenin dinleyeceği ve yayın yapacağı tek servis UUID'si
    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-567890abcdef")

    // Konum hassasiyeti çarpanı (Her iki tarafta da aynı olmalı)
    const val LOCATION_FACTOR = 1_000_000.0

    // BLE paketindeki maksimum mesaj uzunluğu (Byte sınırı nedeniyle)
    const val MAX_MESSAGE_BYTES = 5
}