package com.example.myapplication.bluetooth
//advertiser ve scanner sınıflarını yöneten kısım.
import com.example.myapplication.data.LocalRepository

class BleManager(private val advertiser: BleAdvertiser, private val scanner: BleScanner){

    fun startSos(message: String) {
        advertiser.startAdvertising(message)
        scanner.startScanning()
    }

    fun stopSos() {
        advertiser.stopAdvertising()
        scanner.stopScanning()
    }

}