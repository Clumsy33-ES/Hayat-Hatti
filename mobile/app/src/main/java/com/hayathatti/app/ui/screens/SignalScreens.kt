@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hayathatti.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hayathatti.app.data.DisasterTypes
import com.hayathatti.app.data.Signal
import com.hayathatti.app.data.SignalSource
import com.hayathatti.app.data.Time
import com.hayathatti.app.ui.AppCard
import com.hayathatti.app.ui.Banner
import com.hayathatti.app.ui.EmptyState
import com.hayathatti.app.ui.FormField
import com.hayathatti.app.ui.Header
import com.hayathatti.app.ui.NewSignalViewModel
import com.hayathatti.app.ui.PrimaryButton
import com.hayathatti.app.ui.SegmentRow
import com.hayathatti.app.ui.SignalsViewModel
import com.hayathatti.app.ui.StatusPill
import com.hayathatti.app.ui.Tone
import com.hayathatti.app.ui.coords
import com.hayathatti.app.ui.openMap

fun typeIcon(code: String?): ImageVector = when (code) {
    "deprem" -> Icons.Rounded.Vibration
    "sel" -> Icons.Rounded.Water
    "yangin" -> Icons.Rounded.LocalFireDepartment
    "ble", "ble_relay" -> Icons.Rounded.Bluetooth
    else -> Icons.Rounded.Air
}

@Composable
private fun SignalRow(signal: Signal, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(typeIcon(signal.type), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(DisasterTypes.label(signal.type), style = MaterialTheme.typography.titleMedium)
                Text(Time.show(signal.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (signal.id.length > 8) "" else "#${signal.id}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!signal.notes.isNullOrBlank()) Text(signal.notes, style = MaterialTheme.typography.bodyMedium)
        Text(coords(signal.lat, signal.lon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SignalsScreen(vm: SignalsViewModel, onOpen: () -> Unit, onNew: () -> Unit, onLogin: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val session by vm.session.collectAsState()
    // Son kopya veya hata gösteriliyorsa sekmeye dönüşte yeniden denenir.
    LaunchedEffect(Unit) { if (ui.stale || ui.error != null) vm.refresh() }

    Column(Modifier.fillMaxSize()) {
        Header("Sinyaller", "Kayıtlı yardım çağrıları")
        if (session == null) {
            EmptyState(
                Icons.Rounded.Lock,
                "Giriş gerekiyor",
                "Sinyaller konum bilgisi içerdiği için listeyi yalnızca giriş yapan kullanıcılar görebilir. SOS göndermek için giriş gerekmez.",
            ) {
                PrimaryButton("Giriş yap", onLogin)
            }
            return@Column
        }
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(isRefreshing = ui.loading, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        SegmentRow(SignalSource.entries.map { it to it.label }, ui.source, vm::setSource)
                    }
                    if (ui.source != SignalSource.BLE) {
                        item {
                            val base = listOf<Pair<String?, String>>(null to "Hepsi") + DisasterTypes.selectable
                            val options = if (ui.type != null && base.none { it.first == ui.type }) base + (ui.type to DisasterTypes.label(ui.type)) else base
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(options) { (code, label) ->
                                    FilterChip(selected = ui.type == code, onClick = { vm.setType(code) }, label = { Text(label) })
                                }
                            }
                        }
                    }
                    if (ui.stale) {
                        item {
                            val at = ui.cachedAt?.let { " Son güncelleme: ${Time.clock(it)}." } ?: ""
                            Banner("Sunucuya ulaşılamıyor, son kopya gösteriliyor.$at")
                        }
                    }
                    when {
                        ui.error != null -> item {
                            EmptyState(Icons.Rounded.CloudOff, "Liste alınamadı", ui.error!!) {
                                OutlinedButton(onClick = vm::refresh, shape = MaterialTheme.shapes.medium) { Text("Yeniden dene") }
                            }
                        }
                        ui.items.isEmpty() && !ui.loading -> item {
                            EmptyState(Icons.Rounded.Inbox, "Kayıt yok", "Bu kaynakta gösterilecek sinyal bulunmuyor.")
                        }
                        else -> items(ui.items, key = { it.source + it.id }) { signal ->
                            SignalRow(signal) {
                                vm.select(signal)
                                onOpen()
                            }
                        }
                    }
                    if (ui.hasMore) {
                        item {
                            OutlinedButton(
                                onClick = vm::loadMore,
                                enabled = !ui.loadingMore,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                if (ui.loadingMore) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Daha fazla yükle")
                            }
                        }
                    }
                    ui.moreError?.let { message -> item { Banner(message, Tone.BAD) } }
                }
            }
            ExtendedFloatingActionButton(
                onClick = onNew,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Yeni sinyal", Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
fun SignalDetailScreen(signal: Signal?, onBack: () -> Unit) {
    val context = LocalContext.current
    var mapError by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Header(if (signal == null) "Sinyal" else DisasterTypes.label(signal.type), signal?.let { Time.show(it.timestamp) }, onBack = onBack)
        if (signal == null) {
            EmptyState(Icons.Rounded.Inbox, "Sinyal bulunamadı", "Listeye dönüp yeniden seçin.")
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!signal.notes.isNullOrBlank()) {
                AppCard {
                    Text("Not", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(signal.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
            AppCard {
                InfoRow("Konum", coords(signal.lat, signal.lon))
                signal.accuracyM?.let { InfoRow("Doğruluk", "${it.toInt()} m") }
                InfoRow("Sinyal zamanı", Time.show(signal.timestamp))
                signal.createdAt?.let { InfoRow("Kayıt zamanı", Time.show(it)) }
                signal.userId?.let { InfoRow("Kullanıcı no", it) }
                InfoRow("Kaynak", signal.source)
                InfoRow("Kayıt no", signal.id)
                signal.deviceRef?.let { InfoRow("Cihaz kaydı", it) }
            }
            if (mapError) Banner("Harita uygulaması bulunamadı.", Tone.BAD)
        }
        if (signal.lat != null && signal.lon != null) {
            Column(Modifier.padding(20.dp)) {
                PrimaryButton("Haritada aç", { mapError = !openMap(context, signal.lat, signal.lon) })
            }
        }
    }
}

@Composable
fun NewSignalScreen(vm: NewSignalViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val type by vm.type.collectAsState()
    val note by vm.note.collectAsState()
    val fix by vm.fix.collectAsState()
    val locating by vm.locating.collectAsState()
    val sending by vm.sending.collectAsState()
    var triedLocating by remember { mutableStateOf(false) }

    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        triedLocating = true
        vm.locate()
    }

    fun locate() {
        if (granted()) {
            triedLocating = true
            vm.locate()
        } else {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(Unit) { locate() }

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Header("Yeni sinyal", "Bluetooth yayını açmadan ayrıntılı bildirim", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SegmentRow(DisasterTypes.selectable, type, { vm.type.value = it })
            FormField(note, { vm.note.value = it }, "Not", singleLine = false, minLines = 4, helper = "Ne olduğunu ve nerede olduğunuzu yazın.")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("  Konum", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    when {
                        locating -> StatusPill("Alınıyor", Tone.WARN)
                        fix != null -> StatusPill(fix?.accuracyM?.let { "GPS, ${it.toInt()} m" } ?: "GPS", Tone.OK)
                        else -> StatusPill("Alınamadı", Tone.BAD)
                    }
                }
                fix?.let { Text(coords(it.lat, it.lon), style = MaterialTheme.typography.bodyMedium) }
                if (fix == null && !locating && triedLocating) {
                    Text("Konum izninin verildiğinden ve GPS'in açık olduğundan emin olun.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = ::locate) { Text("Yeniden dene") }
                }
            }
            Text(
                "İnternet yoksa kayıt kuyrukta bekler ve bağlantı gelince gönderilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
        }
        Column(Modifier.padding(20.dp)) {
            PrimaryButton("Gönder", { vm.submit(onDone) }, loading = sending, enabled = fix != null)
        }
    }
}

@Composable
fun SummaryScreen(
    vm: com.hayathatti.app.ui.SummaryViewModel,
    onPick: (SignalSource, String) -> Unit,
) {
    val ui by vm.ui.collectAsState()
    // Sekmeye her dönüşte güncel sayılar alınır; sunucu geri geldiyse eski hata kalmaz.
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        Header("Afet özeti", "Türlere göre sinyal sayıları")
        PullToRefreshBox(isRefreshing = ui.loading, onRefresh = vm::load, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SegmentRow(
                    listOf(SignalSource.POSTGRES, SignalSource.MONGO).map { it to it.label },
                    ui.source,
                    vm::setSource,
                )
                when {
                    ui.error != null -> EmptyState(Icons.Rounded.CloudOff, "Özet alınamadı", ui.error!!) {
                        OutlinedButton(onClick = vm::load, shape = MaterialTheme.shapes.medium) { Text("Yeniden dene") }
                    }
                    ui.items.isEmpty() && !ui.loading -> EmptyState(Icons.Rounded.Inbox, "Kayıt yok", "Bu kaynakta henüz afet kaydı bulunmuyor.")
                    else -> {
                        if (ui.items.isNotEmpty()) {
                            AppCard {
                                Text("Toplam sinyal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(ui.items.sumOf { it.count }.toString(), style = MaterialTheme.typography.headlineLarge)
                            }
                        }
                        ui.items.forEach { d ->
                            AppCard(onClick = { onPick(ui.source, d.code) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                            Icon(typeIcon(d.code), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                    Text(
                                        if (d.code == "ble_relay") DisasterTypes.label(d.code) else d.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                    )
                                    Text(d.count.toString(), style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
