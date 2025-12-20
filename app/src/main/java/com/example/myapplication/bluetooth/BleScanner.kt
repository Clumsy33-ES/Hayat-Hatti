package com.example.myapplication.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context // Context import edildi
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.myapplication.data.LocalRepository
import com.example.myapplication.utils.Constants
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BleScanner(
    private val context: Context, // 1. Buraya context eklendi
    private val repository: LocalRepository,
    private val listener: BleScanListener
) {

    private val scanner: BluetoothLeScanner? = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
           // super.onScanResult(callbackType, result)

            val record = result?.scanRecord ?: return
            val serviceParcel = ParcelUuid(Constants.SERVICE_UUID)
            val sd = record.getServiceData(serviceParcel) ?: return

            try {
                val buffer = ByteBuffer.wrap(sd).order(ByteOrder.LITTLE_ENDIAN)
                val lat = buffer.int / Constants.LOCATION_FACTOR
                val lon = buffer.int / Constants.LOCATION_FACTOR
                val msgLen = buffer.get().toInt() and 0xFF
                val msgBytes = ByteArray(msgLen)
                buffer.get(msgBytes)
                val message = String(msgBytes, Charsets.UTF_8)

                val resultString = "SOS: $lat, $lon | $message"
                repository.saveMessage(resultString)
                listener.onSignalReceived(resultString)

            } catch (e: Exception) {
                Log.e("BLE_SCAN", "Parse error: ${e.message}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            listener.onScanFailed(errorCode)
        }
    }

    fun startScanning() {
      //  if (scanner == null) return

        // 2. Hata veren getContext() yerine yukarıdaki context kullanıldı
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(Constants.SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(null, settings, scanCallback)
            Log.d("BLE", "Scanning started with NULL filters (safe mode)")
        } catch (e: SecurityException) {
            Log.e("BLE", "Permission error: ${e.message}")
        }
    }

    fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) { }
    }
}