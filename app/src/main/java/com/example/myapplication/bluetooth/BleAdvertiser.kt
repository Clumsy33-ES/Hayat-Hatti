package com.example.myapplication.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID
import android.util.Log
import android.widget.Toast

class BleAdvertiser(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    // 🔹 Aynı callback örneğini saklıyoruz
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BLE", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "Advertising failed: $errorCode")
        }
    }

    fun startAdvertising(message: String) {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth kapalı, lütfen açın", Toast.LENGTH_SHORT).show()
            return
        }
        if (advertiser == null) {
            advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        }



        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(UUID.randomUUID()), message.toByteArray())
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
        Toast.makeText(context, "SOS sinyali gönderiliyor...", Toast.LENGTH_SHORT).show()
    }

    fun stopAdvertising() {
        // Bluetooth durumu kontrolü
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w("BLE", "Bluetooth kapalı veya desteklenmiyor, durdurma atlandı.")
            return
        }

        // Gerekli izin kontrolü (UI bağımlılığı olmadan)
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_ADVERTISE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e("BLE", "İzin yok: BLUETOOTH_ADVERTISE. Durdurma işlemi atlandı.")
            return
        }

        // Güvenli durdurma
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            Log.i("BLE", "Advertising başarıyla durduruldu.")
        } catch (e: SecurityException) {
            Log.e("BLE", "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e("BLE", "Beklenmeyen hata: ${e.message}")
        }
    }

}
