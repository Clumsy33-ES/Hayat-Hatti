package com.hayathatti.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.hayathatti.app.ble.BleGateway
import com.hayathatti.app.work.SyncWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

class AuthRepository(private val api: ApiService, private val prefs: Prefs) {
    suspend fun login(email: String, password: String): ApiResult<Unit> =
        finish(apiCall(mapOf(401 to "E-posta veya şifre hatalı.")) { api.login(LoginRequest(email, password)) })

    suspend fun register(first: String, last: String, email: String, password: String, phone: String?): ApiResult<Unit> =
        finish(
            apiCall(mapOf(400 to "Bu e-posta zaten kayıtlı. Giriş yapmayı deneyin.")) {
                api.register(RegisterRequest(first, last, email, password, phone))
            },
        )

    private suspend fun finish(result: ApiResult<AuthResponse>): ApiResult<Unit> = when (result) {
        is ApiResult.Ok -> {
            prefs.saveSession(Session(result.value.token, result.value.user))
            ApiResult.Ok(Unit)
        }
        is ApiResult.Err -> result
    }
}

data class SignalPage(val items: List<Signal>, val hasMore: Boolean, val stale: Boolean, val cachedAt: Long?)

class SignalRepository(private val api: ApiService, private val prefs: Prefs) {
    private val pageSize = 20

    suspend fun load(source: SignalSource, type: String?, offset: Int): ApiResult<SignalPage> {
        val cacheKey = source.apiValue + "_" + (type ?: "all")
        val result = apiCall {
            when (source) {
                SignalSource.POSTGRES -> api.signals(pageSize, offset, type).map { it.toSignal(source.label) }
                // Mongo ve BLE endpoint'lerinde tür filtresi yok, burada süzülür.
                SignalSource.MONGO -> api.signalsMongo(pageSize, offset).map { it.toSignal(source.label) }
                SignalSource.BLE -> api.bleData(pageSize, offset).map { it.toSignal() }
            }
        }
        return when (result) {
            is ApiResult.Ok -> {
                val fullPage = result.value.size >= pageSize
                val items = if (source == SignalSource.MONGO && type != null) result.value.filter { it.type == type } else result.value
                if (offset == 0) prefs.saveCache(cacheKey, items)
                ApiResult.Ok(SignalPage(items, fullPage, stale = false, cachedAt = null))
            }
            is ApiResult.Err -> {
                if (result.kind == ErrKind.UNAUTHORIZED) prefs.clearSession()
                val cached = if (offset == 0 && result.kind != ErrKind.UNAUTHORIZED) prefs.readCache(cacheKey) else null
                if (cached != null) {
                    ApiResult.Ok(SignalPage(cached.first, hasMore = false, stale = true, cachedAt = cached.second))
                } else {
                    result
                }
            }
        }
    }

    suspend fun disasters(source: SignalSource): ApiResult<List<DisasterDto>> =
        apiCall { api.disasters(source.apiValue) }
}

class OutboxRepository(context: Context, private val dao: OutboxDao) {
    private val work = WorkManager.getInstance(context.applicationContext)
    val all: Flow<List<OutboxItem>> = dao.observeAll()
    val relays: Flow<List<OutboxItem>> = dao.observeRelays()

    suspend fun add(item: OutboxItem): Boolean {
        val inserted = dao.insert(item) != -1L
        if (inserted) scheduleSync()
        return inserted
    }

    suspend fun retryNow() {
        dao.requeueFailed()
        scheduleSync()
    }

    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Yeni kayıt, önceki denemenin bekleme süresine takılmasın diye eski iş değiştirilir.
        work.enqueueUniqueWork(SyncWorker.NAME, ExistingWorkPolicy.REPLACE, request)
    }
}

data class GeoFix(val lat: Double, val lon: Double, val accuracyM: Double?)

class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val system = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** Hiçbir adım SOS'u bekletmesin diye her birinin süre sınırı var. Toplamda en çok 11 saniye sürer. */
    @SuppressLint("MissingPermission") // İzin yoksa SecurityException yakalanır ve null döner.
    suspend fun current(): GeoFix? {
        val token = CancellationTokenSource()
        val fresh = runCatching {
            withTimeoutOrNull(8_000) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            }
        }.getOrNull()
        token.cancel()
        val location = fresh
            ?: runCatching { withTimeoutOrNull(3_000) { client.lastLocation.await() } }.getOrNull()
            ?: lastKnownFromSystem() // Google Play Hizmetleri olmayan telefonlar için de çalışır.
        return location?.let { GeoFix(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy.toDouble() else null) }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownFromSystem(): Location? = runCatching {
        system.getProviders(true).mapNotNull { system.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }.getOrNull()
}

class ConnectivityWatcher(context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val online: Flow<Boolean> = callbackFlow {
        // Kayıp anında activeNetwork eski değeri dönebildiği için ağlar burada izlenir.
        val networks = mutableSetOf<Network>()
        val initial = manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(initial)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                networks -= network
                trySend(networks.isNotEmpty())
            }
        }
        manager.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

data class ActiveSos(
    val sid: String,
    val type: String,
    val shortMessage: String,
    val note: String?,
    val fix: GeoFix,
    val startedAt: Long,
)

class SosController(
    private val outbox: OutboxRepository,
    private val location: LocationProvider,
    private val ble: BleGateway,
) {
    private val _active = MutableStateFlow<ActiveSos?>(null)
    val active: StateFlow<ActiveSos?> = _active

    /** Hata varsa kullanıcıya gösterilecek metni döndürür. */
    suspend fun start(type: String, shortMessage: String, note: String?): String? {
        val fix = location.current()
            ?: return "Konum alınamadı. Konum izninin verildiğinden ve GPS'in açık olduğundan emin olun."
        val now = System.currentTimeMillis()
        val sid = UUID.randomUUID().toString()
        outbox.add(
            OutboxItem(
                sid = sid,
                kind = OutboxKind.OWN_SOS.name,
                type = type,
                shortMessage = shortMessage.ifBlank { null },
                note = note?.ifBlank { null },
                lat = fix.lat,
                lon = fix.lon,
                accuracyM = fix.accuracyM,
                rssi = null,
                timestamp = Time.nowIso(),
                createdAtMillis = now,
            ),
        )
        ble.startSos(shortMessage, fix.lat, fix.lon)
        _active.value = ActiveSos(sid, type, shortMessage, note, fix, now)
        return null
    }

    fun stop() {
        ble.stopSos()
        _active.value = null
    }
}
