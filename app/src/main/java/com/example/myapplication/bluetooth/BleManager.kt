package com.example.myapplication.bluetooth

import com.example.myapplication.data.LocalRepository

class BleManager(
    private val advertiser: BleAdvertiser,
    private val scanner: BleScanner,
    private val repository: LocalRepository
) {

    // artık lat/lon alıyoruz
    fun startSos(message: String, latitude: Double, longitude: Double) {
        advertiser.startAdvertising(message, latitude, longitude)
        scanner.startScanning()
        // local kaydetme
        repository.saveMessage("OUTGOING:$latitude,$longitude|$message")
    }

    fun stopSos() {
        advertiser.stopAdvertising()
        scanner.stopScanning()
    }
}
