package com.hayathatti.app.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.hayathatti.app.BuildConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

/**
 * [BleGateway]'in gerçek Android uygulaması (eski feature/ble-stable-final dalındaki
 * BleAdvertiser + BleScanner + BleManager'ın yerini alır).
 *
 * - Uygulama açıkken sürekli tarar ve çevredeki Hayat Hattı yayınlarını [incoming] ile verir.
 *   AppContainer bunları kuyruğa RELAY olarak yazar; internet gelince sunucuya gider.
 * - [startSos] konumu ve kısa mesajı yayınlar. [stopSos] yalnızca yayını durdurur, tarama sürer.
 * - Bluetooth kapanıp açılırsa ya da izin sonradan verilirse kendini yeniden başlatır.
 *
 * Logcat'te "HH-BLE" etiketiyle izlenebilir. Konum ve mesaj kişisel veri olduğu için yalnızca
 * debug sürümünde loga yazılır; release sürümünde sadece gönderen kimliği görünür.
 */
@SuppressLint("MissingPermission") // Her Bluetooth çağrısından önce BlePermissions ile izin denetleniyor.
class AndroidBleGateway(context: Context) : BleGateway {
    private val app = context.applicationContext
    private val bluetooth: BluetoothManager? = app.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetooth?.adapter
    private val main = Handler(Looper.getMainLooper())
    private val random = SecureRandom()
    private val throttle = SignalThrottle()
    private val lock = Any()

    private val _incoming = MutableSharedFlow<BleIncoming>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val incoming: Flow<BleIncoming> = _incoming.asSharedFlow()

    private val _ready = MutableStateFlow(false)

    /** Bluetooth açık ve izinler tamam mı. Durumu canlı izlemek isteyen ekranlar bunu dinleyebilir. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** Bluetooth açık ve BLE izinleri verilmişse true. Her okunduğunda yeniden hesaplanır. */
    override val isConnected: Boolean
        get() = isBluetoothOn() && BlePermissions.allGranted(app)

    // Aşağıdaki dört alan lock ile korunur.
    private var scanning = false
    private var advertiseCallback: AdvertiseCallback? = null
    private var activeSos: BlePacket? = null
    private var bluetoothOff = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)

        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::handle)

        override fun onScanFailed(errorCode: Int) {
            if (errorCode == SCAN_FAILED_ALREADY_STARTED) return
            synchronized(lock) { scanning = false }
            Log.e(TAG, "Tarama başarısız: ${scanError(errorCode)}")
        }
    }

    /** Android uzun süren taramaları zamanla kısabiliyor; tarama belirli aralıklarla tazelenir. */
    private val scanRestart = Runnable {
        synchronized(lock) { stopScanningLocked() }
        startScanning()
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    synchronized(lock) { bluetoothOff = false }
                    Log.i(TAG, "Bluetooth açıldı")
                    refresh()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> onBluetoothOff()
            }
        }
    }

    /** Kullanıcı ayarlardan izin verip uygulamaya dönünce taramanın kendiliğinden başlaması için. */
    private val resumeWatcher = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) = refresh()
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    init {
        ContextCompat.registerReceiver(
            app,
            stateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        (app as? Application)?.registerActivityLifecycleCallbacks(resumeWatcher)
        refresh()
    }

    override fun startSos(message: String, latitude: Double, longitude: Double) {
        val packet = try {
            BlePacket(senderId = random.nextInt(), latitude = latitude, longitude = longitude, message = message.trim())
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "SOS paketi oluşturulamadı: ${e.message}")
            return
        }
        synchronized(lock) { activeSos = packet }
        startAdvertising(packet)
        startScanning()
    }

    override fun stopSos() {
        synchronized(lock) {
            activeSos = null
            stopAdvertisingLocked()
        }
        Log.i(TAG, "SOS yayını durduruldu; tarama sürüyor")
    }

    /** Bluetooth ve izin durumunu yeniden okur; gerekiyorsa taramayı ve bekleyen SOS yayınını başlatır. */
    fun refresh() {
        _ready.value = isConnected
        if (!isBluetoothOn()) return
        startScanning()
        val pending = synchronized(lock) { activeSos?.takeIf { advertiseCallback == null } }
        if (pending != null) startAdvertising(pending)
    }

    private fun startAdvertising(packet: BlePacket) {
        synchronized(lock) {
            stopAdvertisingLocked()
            if (!BlePermissions.canAdvertise(app)) {
                Log.w(TAG, "BLUETOOTH_ADVERTISE izni yok; izin verilince SOS yayını başlayacak")
                return
            }
            val enabled = adapter?.takeIf { isBluetoothOn() } ?: run {
                Log.w(TAG, "Bluetooth kapalı; açılınca SOS yayını başlayacak")
                return
            }
            val advertiser = enabled.bluetoothLeAdvertiser ?: run {
                Log.e(TAG, "Bu cihaz BLE yayınını desteklemiyor")
                return
            }
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build()
            val data = AdvertiseData.Builder()
                .addManufacturerData(BlePacket.MANUFACTURER_ID, packet.encode())
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()
            val callback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.i(TAG, "Yayın başladı: ${describe(packet)}")
                }

                override fun onStartFailure(errorCode: Int) {
                    if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) return
                    synchronized(lock) { if (advertiseCallback === this) advertiseCallback = null }
                    Log.e(TAG, "Yayın başlatılamadı: ${advertiseError(errorCode)}")
                }
            }
            try {
                advertiser.startAdvertising(settings, data, callback)
                advertiseCallback = callback
            } catch (e: RuntimeException) {
                Log.e(TAG, "Yayın başlatılamadı: ${e.message}")
            }
        }
    }

    private fun stopAdvertisingLocked() {
        val callback = advertiseCallback ?: return
        advertiseCallback = null
        if (!BlePermissions.canAdvertise(app) || !isBluetoothOn()) return
        try {
            adapter?.bluetoothLeAdvertiser?.stopAdvertising(callback)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Yayın durdurulamadı: ${e.message}")
        }
    }

    private fun startScanning() {
        synchronized(lock) {
            if (scanning) return
            if (!BlePermissions.canScan(app)) {
                Log.w(TAG, "Tarama izni yok; izin verilince tarama başlayacak")
                return
            }
            val scanner = adapter?.takeIf { isBluetoothOn() }?.bluetoothLeScanner ?: return
            // Filtre üretici verisinin ilk iki baytına (işaret + sürüm) bakar. Filtreli tarama
            // ekran kapanınca da sürer; filtresiz tarama Android 8.1+ sürümlerde durdurulur.
            val filter = ScanFilter.Builder()
                .setManufacturerData(BlePacket.MANUFACTURER_ID, BlePacket.FILTER_PREFIX, BlePacket.FILTER_MASK)
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            try {
                scanner.startScan(listOf(filter), settings, scanCallback)
                scanning = true
                Log.i(TAG, "Tarama başladı")
            } catch (e: RuntimeException) {
                Log.e(TAG, "Tarama başlatılamadı: ${e.message}")
                return
            }
        }
        main.removeCallbacks(scanRestart)
        main.postDelayed(scanRestart, SCAN_RESTART_MILLIS)
    }

    private fun stopScanningLocked() {
        if (!scanning) return
        scanning = false
        if (!BlePermissions.canScan(app) || !isBluetoothOn()) return
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Tarama durdurulamadı: ${e.message}")
        }
    }

    private fun handle(result: ScanResult) {
        val data = result.scanRecord?.getManufacturerSpecificData(BlePacket.MANUFACTURER_ID)
        val packet = BlePacket.decode(data) ?: return
        val ownId = synchronized(lock) { activeSos?.senderId }
        if (packet.senderId == ownId) return
        if (!throttle.shouldEmit(packet, SystemClock.elapsedRealtime())) return
        Log.i(TAG, "Sinyal alındı: ${describe(packet)} rssi=${result.rssi}")
        _incoming.tryEmit(BleIncoming(packet.latitude, packet.longitude, packet.message, result.rssi))
    }

    /** Hem "kapanıyor" hem "kapandı" durumunda çağrılır; uyarı yalnızca ilkinde yazılır. */
    private fun onBluetoothOff() {
        val firstNotice = synchronized(lock) {
            // Sistem tarama ve yayını kendisi durdurur; burada yalnızca kayıtlar sıfırlanır.
            // SOS isteği (activeSos) korunur, Bluetooth açılınca yayın yeniden başlar.
            scanning = false
            advertiseCallback = null
            val first = !bluetoothOff
            bluetoothOff = true
            first
        }
        main.removeCallbacks(scanRestart)
        _ready.value = false
        if (firstNotice) Log.w(TAG, "Bluetooth kapandı; açılınca tarama ve SOS yayını kendiliğinden sürecek")
    }

    /** Konum ve mesaj kişisel veri (KVKK): yalnızca debug sürümünde loga yazılır. */
    private fun describe(packet: BlePacket): String =
        if (BuildConfig.DEBUG) {
            "gönderen=${packet.senderHex} konum=${packet.latitude},${packet.longitude} mesaj='${packet.message}'"
        } else {
            "gönderen=${packet.senderHex}"
        }

    private fun isBluetoothOn(): Boolean =
        try {
            adapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }

    private fun advertiseError(code: Int) = when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "paket 31 baytı aşıyor (1)"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "cihazda boş yayın yuvası kalmadı (2)"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Bluetooth iç hatası (4)"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "cihaz BLE yayınını desteklemiyor (5)"
        else -> "kod $code"
    }

    private fun scanError(code: Int) = when (code) {
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "kayıt başarısız; tarama çok sık başlatılıp durdurulmuş olabilir (2)"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Bluetooth iç hatası (3)"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "cihaz BLE taramasını desteklemiyor (4)"
        else -> "kod $code"
    }

    private companion object {
        const val TAG = "HH-BLE"
        const val SCAN_RESTART_MILLIS = 20 * 60 * 1000L
    }
}
