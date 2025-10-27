package com.example.myapplication.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import android.widget.Toast
import com.example.myapplication.bluetooth.BleAdvertiser
import com.example.myapplication.bluetooth.BleScanner
import com.example.myapplication.bluetooth.BleManager
import com.example.myapplication.utils.PermissionHelper
import com.example.myapplication.data.*

class MainActivity : AppCompatActivity() {

    private lateinit var bleManager: BleManager
    private lateinit var textStatus: TextView
    private lateinit var buttonSos: Button
    private var isSosActive = false

    private val requestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔹 Data katmanını oluştur
        val dbHelper = DatabaseHelper()
        val remoteRepo = RemoteRepository()
        val localRepo = LocalRepository(this, dbHelper, remoteRepo)

        // 🔹 BLE bileşenlerini oluştur
        val advertiser = BleAdvertiser(this)
        val scanner = BleScanner(localRepo)

        // 🔹 BLE yöneticisi (advertiser + scanner)
        bleManager = BleManager(advertiser, scanner)

        // 🔹 Gerekli izinler kontrolü
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            PermissionHelper.requestBluetoothPermissions(this, requestCode)
        } else {
            Toast.makeText(this, "İzinler mevcut, butona basarak SOS başlatabilirsiniz.", Toast.LENGTH_SHORT).show()
        }

        // UI elemanlarını bağla
        textStatus = findViewById(R.id.textStatus)
        buttonSos = findViewById(R.id.buttonSos)


        buttonSos.setOnClickListener {
            if (!isSosActive) {
                startSosMode()
            } else {
                stopSosMode()
            }
        }

    }
    private fun startSosMode() {
        bleManager.startSos("SOS:37.4,38.5")
        textStatus.text = "Durum: Aktif 🔴"
        buttonSos.text = "SOS DURDUR"
        buttonSos.setBackgroundColor(getColor(android.R.color.holo_red_dark))
        isSosActive = true
        Toast.makeText(this, "SOS sinyali gönderilmeye başlandı", Toast.LENGTH_SHORT).show()
    }

    private fun stopSosMode() {
        bleManager.stopSos()
        textStatus.text = "Durum: Kapalı ⚪"
        buttonSos.text = "SOS BAŞLAT"
        buttonSos.setBackgroundColor(getColor(android.R.color.holo_green_dark))
        isSosActive = false
        Toast.makeText(this, "SOS sinyali durduruldu", Toast.LENGTH_SHORT).show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == this.requestCode &&
            PermissionHelper.hasBluetoothPermissions(this)
        ) {
            Toast.makeText(this, "Bluetooth izinleri verildi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth izinleri reddedildi!", Toast.LENGTH_SHORT).show()
        }
    }
}
