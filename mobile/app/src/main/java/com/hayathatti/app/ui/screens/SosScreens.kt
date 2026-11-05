package com.hayathatti.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.hayathatti.app.data.ActiveSos
import com.hayathatti.app.data.DisasterTypes
import com.hayathatti.app.data.OutboxItem
import com.hayathatti.app.data.OutboxStatus
import com.hayathatti.app.data.Time
import com.hayathatti.app.ui.AppCard
import com.hayathatti.app.ui.Banner
import com.hayathatti.app.ui.EmptyState
import com.hayathatti.app.ui.FormField
import com.hayathatti.app.ui.Header
import com.hayathatti.app.ui.SegmentRow
import com.hayathatti.app.ui.SosViewModel
import com.hayathatti.app.ui.StatusPill
import com.hayathatti.app.ui.Tone
import com.hayathatti.app.ui.coords
import com.hayathatti.app.ui.openMap
import com.hayathatti.app.ui.theme.SosRed
import com.hayathatti.app.ui.theme.SosRedDeep
import com.hayathatti.app.ui.theme.SosRedLight
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val HOLD_MILLIS = 1_500

@Composable
private fun HoldSosButton(enabled: Boolean, busy: Boolean, onTriggered: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sweepColor = Color.White

    Box(
        modifier = Modifier
            .size(216.dp)
            .semantics {
                role = Role.Button
                contentDescription = "SOS başlat. Başlatmak için basılı tutun."
                onClick(label = "SOS başlat") {
                    if (enabled) onTriggered()
                    enabled
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    val animation = scope.launch {
                        progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTriggered()
                    }
                    waitForUpOrCancellation()
                    animation.cancel()
                    scope.launch { progress.animateTo(0f, tween(180)) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(SosRed.copy(alpha = 0.10f), r)
            drawCircle(SosRed.copy(alpha = 0.18f), r * 0.87f)
            drawCircle(
                Brush.radialGradient(
                    listOf(SosRedLight, SosRed, SosRedDeep),
                    center = Offset(center.x - r * 0.25f, center.y - r * 0.3f),
                    radius = r * 1.1f,
                ),
                r * 0.74f,
            )
            if (progress.value > 0f) {
                val stroke = 6.dp.toPx()
                val inset = r * 0.26f + stroke
                drawArc(
                    color = sweepColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOS", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                Text("Basılı tut", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PulsingBeacon() {
    val transition = rememberInfiniteTransition(label = "beacon")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            listOf(0f, 0.5f).forEach { shift ->
                val t = (phase + shift) % 1f
                drawCircle(SosRed.copy(alpha = (1f - t) * 0.35f), r * (0.5f + 0.5f * t))
            }
            drawCircle(
                Brush.radialGradient(listOf(SosRedLight, SosRed, SosRedDeep), center = Offset(center.x - r * 0.2f, center.y - r * 0.2f), radius = r * 0.8f),
                r * 0.5f,
            )
        }
        Text("SOS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ConnectionPills(online: Boolean, bleConnected: Boolean, waiting: Int) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusPill(if (online) "Çevrimiçi" else "İnternet yok", if (online) Tone.OK else Tone.WARN, if (online) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff)
        StatusPill(if (bleConnected) "Bluetooth" else "Bluetooth bağlı değil", if (bleConnected) Tone.OK else Tone.NEUTRAL, if (bleConnected) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled)
        if (waiting > 0) StatusPill("$waiting bekliyor", Tone.WARN, Icons.Rounded.CloudUpload)
    }
}

@Composable
fun SosScreen(vm: SosViewModel, onNearby: () -> Unit, nearbyCount: StateFlow<Int>) {
    val active by vm.active.collectAsState()
    val online by vm.online.collectAsState()
    val outbox by vm.outbox.collectAsState()
    val waiting = outbox.count { it.status == OutboxStatus.PENDING.name }
    val nearby by nearbyCount.collectAsState()

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Header("Hayat Hattı", if (active != null) "Yardım çağrınız açık" else "Acil durumda basılı tutun")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ConnectionPills(online, vm.bleConnected, waiting)
            val current = active
            if (current != null) {
                ActiveSosPanel(current, outbox.firstOrNull { it.sid == current.sid }, online, vm.bleConnected, vm::stop)
            } else {
                IdleSosPanel(vm)
            }
            AppCard(onClick = onNearby) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Yakındaki sinyaller", style = MaterialTheme.typography.titleMedium)
                        Text(if (nearby > 0) "$nearby sinyal duyuldu" else "Henüz sinyal yok", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun IdleSosPanel(vm: SosViewModel) {
    val context = LocalContext.current
    val type by vm.type.collectAsState()
    val short by vm.shortMessage.collectAsState()
    val note by vm.note.collectAsState()
    val starting by vm.starting.collectAsState()
    val error by vm.error.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.start() }

    fun begin() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.start()
        } else {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        HoldSosButton(enabled = !starting, busy = starting, onTriggered = ::begin)
    }
    error?.let { Banner(it, Tone.BAD) }
    SegmentRow(DisasterTypes.selectable, type, { vm.type.value = it })
    FormField(
        value = short,
        onChange = { vm.shortMessage.value = it.take(4) },
        label = "Kısa mesaj",
        helper = "En çok 4 karakter. Bluetooth ile yayınlanır.",
    )
    FormField(
        value = note,
        onChange = { vm.note.value = it },
        label = "Not (isteğe bağlı)",
        helper = "İnternet gelince sunucuya gider.",
        singleLine = false,
        minLines = 2,
    )
}

@Composable
private fun ActiveSosPanel(sos: ActiveSos, item: OutboxItem?, online: Boolean, bleConnected: Boolean, onStop: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PulsingBeacon() }
    AppCard {
        InfoRow("Afet türü", DisasterTypes.label(sos.type))
        if (sos.shortMessage.isNotBlank()) InfoRow("Mesaj", sos.shortMessage)
        if (!sos.note.isNullOrBlank()) InfoRow("Not", sos.note)
        InfoRow("Konum", coords(sos.fix.lat, sos.fix.lon))
        InfoRow("Başlangıç", Time.clock(sos.startedAt))
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sunucu", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (item?.status) {
                OutboxStatus.SENT.name -> StatusPill("İletildi", Tone.OK)
                OutboxStatus.FAILED.name -> StatusPill("Kabul edilmedi", Tone.BAD)
                else -> StatusPill(if (online) "Gönderiliyor" else "İnternet bekleniyor", Tone.WARN)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bluetooth yayını", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (bleConnected) StatusPill("Yayında", Tone.OK) else StatusPill("Modül bağlı değil", Tone.NEUTRAL)
        }
    }
    OutlinedButton(onClick = onStop, Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp), shape = MaterialTheme.shapes.medium) {
        Text("SOS'u durdur")
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}

@Composable
fun NearbyScreen(relays: List<OutboxItem>, bleConnected: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Header("Yakındaki sinyaller", if (bleConnected) "Bluetooth dinleniyor" else "Bluetooth modülü bağlı değil", onBack = onBack)
        if (relays.isEmpty()) {
            EmptyState(
                Icons.Rounded.Sensors,
                "Henüz sinyal yok",
                if (bleConnected) {
                    "Yakındaki bir telefon SOS başlattığında burada görünür."
                } else {
                    "BLE modülü uygulamayla birleştirildiğinde duyulan sinyaller burada listelenir."
                },
            )
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                relays.forEach { item ->
                    AppCard(onClick = { openMap(context, item.lat, item.lon) }) {
                        Text(item.shortMessage ?: "Sinyal", style = MaterialTheme.typography.titleMedium)
                        Text(coords(item.lat, item.lon), style = MaterialTheme.typography.bodySmall)
                        Text("${Time.clock(item.createdAtMillis)} · Haritada açmak için dokunun", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
