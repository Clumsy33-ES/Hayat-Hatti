package com.example.myapplication.bluetooth

interface BleScanListener {
    // Yeni bir sinyal yakalandığında tetiklenir
    fun onSignalReceived(message: String)

    // Tarama başlatılamazsa veya hata verirse tetiklenir
    fun onScanFailed(errorCode: Int)
}