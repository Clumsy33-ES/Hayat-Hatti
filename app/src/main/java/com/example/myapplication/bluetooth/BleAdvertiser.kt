package com.example.myapplication.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.example.myapplication.utils.Constants
import android.util.Log
import android.widget.Toast
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BleAdvertiser(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    // 🔹 ÖNEMLİ: Sabit callback yerine değişken (null olabilir) bir callback tutuyoruz
    private var currentAdvertiseCallback: AdvertiseCallback? = null

   /* private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BLE", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "Advertising failed: $errorCode")
        }
    }
   */
   private var isActuallyAdvertising = false

    fun startAdvertising(message: String, lat: Double, lon: Double) {
        val adapter = bluetoothAdapter ?: run {
            Toast.makeText(context, "Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            Toast.makeText(context, "Bluetooth kapalı, lütfen açın", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Önce varsa mevcut yayını durdur ve callback'i temizle
        stopAdvertising()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({


            if (bluetoothAdapter?.isEnabled != true) {
                Log.e("BLE", "Gecikme sonrası Bluetooth'un kapalı olduğu saptandı.")
                return@postDelayed
            }

            if (advertiser == null && currentAdvertiseCallback != null) {
                advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            }
            currentAdvertiseCallback = null

            // 🔹 3. HER SEFERİNDE YENİ BİR CALLBACK OLUŞTURUYORUZ (Çakışmayı önler)
            currentAdvertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    isActuallyAdvertising = true // Kilitlendi: Yayın başarılı!
                    Log.i("BLE", "YAYIN BAŞARILI: Diğer cihazlar artık sizi görebilir.")
                }
                override fun onStartFailure(errorCode: Int) {
                    if (errorCode == 1 && isActuallyAdvertising) {
                        // Eğer zaten yayın başarılı olduysa ve sistem 1 hatası veriyorsa GÖRMEZDEN GEL
                        Log.d("BLE", "Mükerrer hata 1 alındı ama yayın zaten aktif. Devam ediliyor.")
                    }
                    else {
                        isActuallyAdvertising = false
                        Log.e("BLE", "YAYIN BAŞLATILAMADI: Hata kodu $errorCode")
                    }
                }
            }

            // İzin kontrolü
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_ADVERTISE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.e("BLE", "BLUETOOTH_ADVERTISE izni yok!")
                return@postDelayed
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build()

            val messageBytes = message.toByteArray(Charsets.UTF_8)
                .take(Constants.MAX_MESSAGE_BYTES)
                .toByteArray()

            val payload = ByteBuffer.allocate(8 + 1 + messageBytes.size).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                putInt((lat * Constants.LOCATION_FACTOR).toInt())
                putInt((lon * Constants.LOCATION_FACTOR).toInt())
                put(messageBytes.size.toByte())
                put(messageBytes)
            }

            val serviceParcel = ParcelUuid(Constants.SERVICE_UUID)

            val data = AdvertiseData.Builder()

                .addServiceData(serviceParcel, payload.array())
                .setIncludeTxPowerLevel(false) //TRUE DAN DEĞİŞİM
                .setIncludeDeviceName(false)
                .build()

            try {
                advertiser?.startAdvertising(settings, data, currentAdvertiseCallback)
                Log.i("BLE", "Reklam başarıyla başlatıldı. Payload: ${payload.capacity()}")
                Toast.makeText(context, "SOS sinyali gönderiliyor...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("BLE", "Başlatma sırasında kritik hata: ${e.message}")
            }
        }, 1000)
    }

    fun stopAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BLUETOOTH_ADVERTISE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        try {
            // 🔹 5. Eğer aktif bir callback varsa onunla durdur
            currentAdvertiseCallback?.let {
                advertiser?.stopAdvertising(it)
            }
            currentAdvertiseCallback = null // Temizle
            Log.i("BLE", "Reklam tamamen durduruldu ve temizlendi.")
        } catch (e: Exception) {
            Log.e("BLE", "Durdurma hatası: ${e.message}")
        }
    }
}