package com.example.myapplication.bluetooth
//bir mesaj aldığında internet varsa MongoDB, yoksa postgreSQL'e (local mock database)
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.example.myapplication.data.LocalRepository

class BleScanner(private val repository: LocalRepository) {

    private val scanner: BluetoothLeScanner? = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

    // 🔹 Tek bir ScanCallback örneği tanımla (start/stop aynı instance)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val data = result?.scanRecord?.serviceData
            data?.forEach { (_, value) ->
                val message = String(value)
                Log.d("BLE", "Received message: $message")

                // 🔹 Artık sadece log değil, repository’ye kaydet
                repository.saveMessage(message)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Scan failed: $errorCode")
        }
    }

    fun startScanning() {
        if (scanner == null) {
            Log.e("BLE", "Scanner not supported.")
            return
        }

        scanner.startScan(scanCallback)
        Log.d("BLE", "Scanning started...")
    }

    fun stopScanning() {
        scanner?.stopScan(scanCallback)
        Log.d("BLE", "Scanning stopped.")
    }
}
