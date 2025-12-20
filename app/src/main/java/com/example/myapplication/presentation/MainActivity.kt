package com.example.myapplication.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import android.widget.Toast
import com.example.myapplication.bluetooth.*
import com.example.myapplication.utils.PermissionHelper
import com.example.myapplication.data.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

private var isProcessing = false

class MainActivity : AppCompatActivity(), BleScanListener {
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val cancellationTokenSource = CancellationTokenSource()

    private lateinit var bleManager: BleManager
    private lateinit var textStatus: TextView
    private lateinit var buttonSos: Button
    private lateinit var textSignals: TextView // Alınan mesajları listeleyeceğimiz alan
    private var isSosActive = false

    private val requestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textStatus = findViewById(R.id.textStatus)
        buttonSos = findViewById(R.id.buttonSos)
        textSignals = findViewById(R.id.textSignals)

        // 🔹 Data katmanını oluştur
        val dbHelper = DatabaseHelper()
        val remoteRepo = RemoteRepository()
        val localRepo = LocalRepository(this, dbHelper, remoteRepo)

        // 🔹 BLE bileşenlerini oluştur
        val advertiser = BleAdvertiser(this)
        val scanner = BleScanner(this, localRepo, this)

        // 🔹 BLE yöneticisi (advertiser + scanner)
        bleManager = BleManager(advertiser, scanner, localRepo)

        // 🔹 Gerekli izinler kontrolü
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            PermissionHelper.requestBluetoothPermissions(this, requestCode)
        }
        else {
            Toast.makeText(this, "İzinler mevcut, butona basarak SOS başlatabilirsiniz.", Toast.LENGTH_SHORT).show()
        }

        buttonSos.setOnClickListener {
            if (!isSosActive) {
                startSosMode()
            } else {
                stopSosMode()
            }
        }
    }
    override fun onSignalReceived(message: String) {
        // BLE tarayıcı arka planda çalıştığı için UI güncellemesini 'runOnUiThread' ile yapıyoruz
        runOnUiThread {
            val currentText = textSignals.text.toString()
            textSignals.text = "$message\n$currentText"

            textSignals.setOnClickListener {
                try {
                    // Mesaj formatımız: "SOS: 41.0, 28.9 | Mesaj"
                    // Koordinatları ayıklıyoruz
                    val coordsPart = message.substringAfter("SOS: ").substringBefore("|").trim()
                    val latLon = coordsPart.split(",")

                    if (coordsPart.isNotEmpty()) {

                        if (latLon.size == 2) {
                            val lat = latLon[0].trim()
                            val lon = latLon[1].trim()

                            // Google Maps URI'si oluşturma
                            val gmmIntentUri = android.net.Uri.parse("geo:$lat,$lon?q=$lat,$lon(Acil Durum Konumu)")
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)

                            // Google Maps uygulamasını hedefle
                            mapIntent.setPackage("com.google.android.apps.maps")

                            // Hata aldığınız startContext yerine doğrudan startActivity kullanıyoruz
                            startActivity(mapIntent)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MAP_ERROR", "Harita acilamadi: ${e.message}")
                    Toast.makeText(this, "Harita acilamadi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onScanFailed(errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, "Tarama hatası: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startSosMode() {
        if (isProcessing) return // Eğer zaten bir işlem sürüyorsa durdur
        isProcessing = true // Kilidi kapat

        val rawMessage = findViewById<EditText>(R.id.editMessage)?.text?.toString() ?: ""
        // Kullanıcıdan mesaj al (örnek: bir EditText'ten)
        val userMessage = if (rawMessage.length > 4) rawMessage.substring(0, 4) else rawMessage
        // Önce izin kontrolü
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            PermissionHelper.requestBluetoothPermissions(this, requestCode)
            isProcessing = false // İzin yoksa kilidi aç
            return
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    // BleManager.startSos artık (message, lat, lon) parametrelerini alacak
                    bleManager.startSos(userMessage, location.latitude, location.longitude)
                    textStatus.text = "Durum: Aktif 🔴"
                    buttonSos.text = "SOS DURDUR"
                    buttonSos.setBackgroundColor(getColor(android.R.color.holo_red_dark))
                    isSosActive = true

                    Toast.makeText(this, "SOS sinyali gönderilmeye başlandı", Toast.LENGTH_SHORT).show()
                }

                else {
                    Toast.makeText(this, "Konum alınamadı. Lütfen GPS'in açık olduğundan emin olun.", Toast.LENGTH_SHORT).show()
                }
                isProcessing = false // İşlem bitti, kilidi aç
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, "Konum hatası: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
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
