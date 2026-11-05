package com.hayathatti.app

import android.app.Application
import android.content.Context
import com.hayathatti.app.ble.BleGateway
import com.hayathatti.app.ble.NotConnectedBle
import com.hayathatti.app.data.ApiFactory
import com.hayathatti.app.data.AppDatabase
import com.hayathatti.app.data.AuthRepository
import com.hayathatti.app.data.ConnectivityWatcher
import com.hayathatti.app.data.LocationProvider
import com.hayathatti.app.data.OutboxItem
import com.hayathatti.app.data.OutboxKind
import com.hayathatti.app.data.OutboxRepository
import com.hayathatti.app.data.OutboxRouting
import com.hayathatti.app.data.Prefs
import com.hayathatti.app.data.SignalRepository
import com.hayathatti.app.data.SosController
import com.hayathatti.app.data.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {
    val prefs = Prefs(context).also { runBlocking { it.load() } }
    val api = ApiFactory.create({ prefs.baseUrl.value }, { prefs.session.value?.token })
    val db = AppDatabase.create(context)
    val ble: BleGateway = NotConnectedBle
    val auth = AuthRepository(api, prefs)
    val signals = SignalRepository(api, prefs)
    val outbox = OutboxRepository(context, db.outbox())
    val connectivity = ConnectivityWatcher(context)
    val location = LocationProvider(context)
    val sos = SosController(outbox, location, ble)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // BLE bağlandığında duyulan sinyaller kuyruğa girer ve internet gelince taşınır.
        scope.launch {
            ble.incoming.collect { signal ->
                val now = System.currentTimeMillis()
                outbox.add(
                    OutboxItem(
                        sid = OutboxRouting.relayId(signal.latitude, signal.longitude, signal.message, now),
                        kind = OutboxKind.RELAY.name,
                        type = null,
                        shortMessage = signal.message,
                        note = null,
                        lat = signal.latitude,
                        lon = signal.longitude,
                        accuracyM = null,
                        rssi = signal.rssi,
                        timestamp = Time.nowIso(),
                        createdAtMillis = now,
                    ),
                )
            }
        }
    }
}

class HayatHattiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.outbox.scheduleSync()
    }
}
