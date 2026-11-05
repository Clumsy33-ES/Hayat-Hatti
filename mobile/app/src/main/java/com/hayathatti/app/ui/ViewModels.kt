package com.hayathatti.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayathatti.app.AppContainer
import com.hayathatti.app.data.ApiResult
import com.hayathatti.app.data.DisasterDto
import com.hayathatti.app.data.GeoFix
import com.hayathatti.app.data.OutboxItem
import com.hayathatti.app.data.OutboxKind
import com.hayathatti.app.data.OutboxStatus
import com.hayathatti.app.data.Signal
import com.hayathatti.app.data.SignalSource
import com.hayathatti.app.data.Time
import com.hayathatti.app.data.apiCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

object Validators {
    private val email = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun email(value: String): String? = if (email.matches(value.trim())) null else "Geçerli bir e-posta yazın."

    /** Backend şifreyi 8-72 karakter ve en çok 72 byte kabul eder. */
    fun password(value: String): String? = when {
        value.length < 8 -> "Şifre en az 8 karakter olmalı."
        value.toByteArray().size > 72 -> "Şifre çok uzun, en çok 72 byte olabilir."
        else -> null
    }

    fun required(value: String, label: String): String? = if (value.isBlank()) "$label boş bırakılamaz." else null
}

data class AuthUi(
    val busy: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val firstError: String? = null,
    val lastError: String? = null,
)

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(AuthUi())
    val ui = _ui.asStateFlow()

    fun clearErrors() = _ui.update { AuthUi() }

    fun login(email: String, password: String, onDone: () -> Unit) {
        val checked = AuthUi(emailError = Validators.email(email), passwordError = Validators.password(password))
        if (checked.emailError != null || checked.passwordError != null) {
            _ui.value = checked
            return
        }
        run(onDone) { c.auth.login(email.trim(), password) }
    }

    fun register(first: String, last: String, email: String, phone: String, password: String, onDone: () -> Unit) {
        val checked = AuthUi(
            firstError = Validators.required(first, "Ad"),
            lastError = Validators.required(last, "Soyad"),
            emailError = Validators.email(email),
            passwordError = Validators.password(password),
        )
        if (listOf(checked.firstError, checked.lastError, checked.emailError, checked.passwordError).any { it != null }) {
            _ui.value = checked
            return
        }
        run(onDone) { c.auth.register(first.trim(), last.trim(), email.trim(), password, phone.trim().ifBlank { null }) }
    }

    private fun run(onDone: () -> Unit, block: suspend () -> ApiResult<Unit>) {
        viewModelScope.launch {
            _ui.value = AuthUi(busy = true)
            when (val r = block()) {
                is ApiResult.Ok -> {
                    _ui.value = AuthUi()
                    onDone()
                }
                is ApiResult.Err -> _ui.value = AuthUi(error = r.message)
            }
        }
    }
}

class SosViewModel(private val c: AppContainer) : ViewModel() {
    val type = MutableStateFlow("deprem")
    val shortMessage = MutableStateFlow("")
    val note = MutableStateFlow("")
    val starting = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val active = c.sos.active
    val bleConnected = c.ble.isConnected
    val online = c.connectivity.online.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val outbox = c.outbox.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start() {
        if (starting.value) return
        viewModelScope.launch {
            starting.value = true
            error.value = c.sos.start(type.value, shortMessage.value, note.value)
            starting.value = false
        }
    }

    fun stop() = c.sos.stop()
}

data class SignalsUi(
    val source: SignalSource = SignalSource.POSTGRES,
    val type: String? = null,
    val items: List<Signal> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val moreError: String? = null,
    val hasMore: Boolean = false,
    val stale: Boolean = false,
    val cachedAt: Long? = null,
    val selected: Signal? = null,
)

class SignalsViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(SignalsUi())
    val ui = _ui.asStateFlow()
    val session = c.prefs.session

    init {
        // Giriş yapılınca liste yenilenir; oturum kapanınca eski kayıtlar ekranda kalmaz.
        viewModelScope.launch {
            c.prefs.session.map { it?.user?.id }.distinctUntilChanged().drop(1).collect { id ->
                if (id != null) refresh() else _ui.update { it.copy(items = emptyList(), selected = null, stale = false, error = null) }
            }
        }
        // Kuyruktan bir kayıt gidince liste güncellenir.
        viewModelScope.launch {
            c.outbox.all.map { list -> list.count { it.status == OutboxStatus.SENT.name } }
                .distinctUntilChanged().drop(1).collect { refresh() }
        }
        if (c.prefs.session.value != null) refresh()
    }

    fun setSource(source: SignalSource) {
        _ui.update { it.copy(source = source, type = if (source == SignalSource.BLE) null else it.type) }
        refresh()
    }

    fun setType(type: String?) {
        _ui.update { it.copy(type = type) }
        refresh()
    }

    fun select(signal: Signal) = _ui.update { it.copy(selected = signal) }

    fun refresh() {
        if (c.prefs.session.value == null) return
        viewModelScope.launch {
            val s = _ui.value
            _ui.update { it.copy(loading = true, error = null, moreError = null) }
            when (val r = c.signals.load(s.source, s.type, 0)) {
                is ApiResult.Ok -> _ui.update {
                    it.copy(items = r.value.items, hasMore = r.value.hasMore, stale = r.value.stale, cachedAt = r.value.cachedAt, loading = false)
                }
                is ApiResult.Err -> _ui.update { it.copy(items = emptyList(), error = r.message, loading = false, stale = false, hasMore = false) }
            }
        }
    }

    fun loadMore() {
        val s = _ui.value
        if (s.loadingMore || !s.hasMore) return
        viewModelScope.launch {
            _ui.update { it.copy(loadingMore = true, moreError = null) }
            when (val r = c.signals.load(s.source, s.type, s.items.size)) {
                is ApiResult.Ok -> _ui.update {
                    it.copy(items = it.items + r.value.items, hasMore = r.value.hasMore, loadingMore = false)
                }
                is ApiResult.Err -> _ui.update { it.copy(loadingMore = false, moreError = r.message) }
            }
        }
    }
}

data class SummaryUi(
    val source: SignalSource = SignalSource.POSTGRES,
    val items: List<DisasterDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class SummaryViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(SummaryUi())
    val ui = _ui.asStateFlow()

    fun setSource(source: SignalSource) {
        _ui.update { it.copy(source = source) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = c.signals.disasters(_ui.value.source)) {
                is ApiResult.Ok -> _ui.update { it.copy(items = r.value, loading = false) }
                is ApiResult.Err -> _ui.update { it.copy(items = emptyList(), error = r.message, loading = false) }
            }
        }
    }
}

class NewSignalViewModel(private val c: AppContainer) : ViewModel() {
    val type = MutableStateFlow("deprem")
    val note = MutableStateFlow("")
    val fix = MutableStateFlow<GeoFix?>(null)
    val locating = MutableStateFlow(false)
    val sending = MutableStateFlow(false)

    fun locate() {
        viewModelScope.launch {
            locating.value = true
            fix.value = c.location.current()
            locating.value = false
        }
    }

    fun submit(onDone: () -> Unit) {
        val position = fix.value ?: return
        viewModelScope.launch {
            sending.value = true
            c.outbox.add(
                OutboxItem(
                    sid = UUID.randomUUID().toString(),
                    kind = OutboxKind.REPORT.name,
                    type = type.value,
                    shortMessage = null,
                    note = note.value.ifBlank { null },
                    lat = position.lat,
                    lon = position.lon,
                    accuracyM = position.accuracyM,
                    rssi = null,
                    timestamp = Time.nowIso(),
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            sending.value = false
            onDone()
        }
    }
}

sealed interface ServerState {
    data object Idle : ServerState
    data object Testing : ServerState
    data class Ok(val status: String) : ServerState
    data class Failed(val message: String) : ServerState
}

class ServerViewModel(private val c: AppContainer) : ViewModel() {
    val url = MutableStateFlow(c.prefs.baseUrl.value)
    val state = MutableStateFlow<ServerState>(ServerState.Idle)
    val urlError = MutableStateFlow<String?>(null)

    fun saveAndTest() {
        val value = url.value.trim().trimEnd('/')
        if (value.toHttpUrlOrNull() == null) {
            urlError.value = "Adres http:// veya https:// ile başlamalı."
            return
        }
        urlError.value = null
        viewModelScope.launch {
            state.value = ServerState.Testing
            c.prefs.saveBaseUrl(value)
            url.value = value
            state.value = when (val r = apiCall { c.api.health() }) {
                is ApiResult.Ok -> ServerState.Ok(r.value.status)
                is ApiResult.Err -> ServerState.Failed(r.message)
            }
            if (state.value is ServerState.Ok) c.outbox.scheduleSync()
        }
    }
}

class SplashViewModel(private val c: AppContainer) : ViewModel() {
    private val _message = MutableStateFlow("Sunucu kontrol ediliyor…")
    val message: StateFlow<String> = _message

    fun check(onDone: (signedIn: Boolean) -> Unit) {
        viewModelScope.launch {
            val job = launch {
                val r = apiCall { c.api.health() }
                _message.value = if (r is ApiResult.Ok) "Sunucu çalışıyor" else "Sunucuya ulaşılamadı, SOS yine çalışır"
            }
            delay(1_200)
            job.cancel()
            onDone(c.prefs.entered.value)
        }
    }
}
