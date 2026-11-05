package com.hayathatti.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hayathatti.app.BuildConfig
import com.hayathatti.app.data.DisasterTypes
import com.hayathatti.app.data.OutboxItem
import com.hayathatti.app.data.OutboxKind
import com.hayathatti.app.data.OutboxStatus
import com.hayathatti.app.data.Session
import com.hayathatti.app.data.Time
import com.hayathatti.app.ui.AppCard
import com.hayathatti.app.ui.Banner
import com.hayathatti.app.ui.EmptyState
import com.hayathatti.app.ui.FormField
import com.hayathatti.app.ui.Header
import com.hayathatti.app.ui.PrimaryButton
import com.hayathatti.app.ui.ServerState
import com.hayathatti.app.ui.ServerViewModel
import com.hayathatti.app.ui.StatusPill
import com.hayathatti.app.ui.Tone

@Composable
private fun LinkRow(icon: ImageVector, title: String, trailing: String?, tone: Tone = Tone.NEUTRAL, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
            if (trailing != null) StatusPill(trailing, tone)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun ProfileScreen(
    session: Session?,
    waiting: Int,
    onQueue: () -> Unit,
    onServer: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Profil", null)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (session != null) {
                val u = session.user
                AppCard {
                    Text(listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank { "Kullanıcı" }, style = MaterialTheme.typography.titleLarge)
                    Text(u.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    u.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Kullanıcı no ${u.id}" + (u.createdAt?.let { " · Kayıt: ${Time.show(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                AppCard {
                    Text("Hesapsız kullanıyorsunuz", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "SOS çalışır. Sinyal listesini görmek ve kayıtları adınıza bağlamak için giriş yapın.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton("Giriş yap veya kayıt ol", onLogin)
                }
            }
            LinkRow(Icons.Rounded.CloudUpload, "Gönderim kuyruğu", if (waiting > 0) "$waiting bekliyor" else null, Tone.WARN, onQueue)
            LinkRow(Icons.Rounded.Dns, "Sunucu ayarı", null, onClick = onServer)
            if (session != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp), shape = MaterialTheme.shapes.medium) {
                    Text("Çıkış yap")
                }
            }
            Text(
                "Sürüm ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

private fun kindLabel(item: OutboxItem): String = when (item.kind) {
    OutboxKind.OWN_SOS.name -> "SOS çağrım"
    OutboxKind.REPORT.name -> "Bildirimim"
    else -> "Taşınan sinyal"
}

@Composable
fun QueueScreen(items: List<OutboxItem>, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Gönderim kuyruğu", "İnternet gelince otomatik gönderilir", onBack = onBack)
        if (items.isEmpty()) {
            EmptyState(Icons.Rounded.Inbox, "Kuyruk boş", "SOS ve bildirimleriniz burada izlenir.")
            return@Column
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (items.any { it.status != OutboxStatus.SENT.name }) {
                OutlinedButton(onClick = onRetry, Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp), shape = MaterialTheme.shapes.medium) {
                    Text("Şimdi gönder")
                }
            }
            items.forEach { item ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(kindLabel(item), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(Time.clock(item.createdAtMillis), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val detail = listOfNotNull(
                        item.type?.let(DisasterTypes::label),
                        item.shortMessage,
                        item.note,
                    ).joinToString(" · ")
                    if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodyMedium)
                    when (item.status) {
                        OutboxStatus.SENT.name -> StatusPill("Gönderildi: ${item.sentTo}", Tone.OK)
                        OutboxStatus.FAILED.name -> StatusPill("Kabul edilmedi", Tone.BAD)
                        else -> StatusPill(if (item.attempts > 0) "Bekliyor, ${item.attempts} deneme" else "Bekliyor", Tone.WARN)
                    }
                    item.lastError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
fun ServerScreen(vm: ServerViewModel, onBack: () -> Unit) {
    val url by vm.url.collectAsState()
    val state by vm.state.collectAsState()
    val urlError by vm.urlError.collectAsState()

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Header("Sunucu ayarı", "Backend'in adresi", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(url, { vm.url.value = it }, "Sunucu adresi", error = urlError, keyboard = KeyboardType.Uri)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.url.value = "http://10.0.2.2:8000" }, label = { Text("Emülatör") })
                AssistChip(onClick = { vm.url.value = "http://192.168.1.20:8000" }, label = { Text("Yerel ağ örneği") })
            }
            when (val s = state) {
                ServerState.Idle -> Unit
                ServerState.Testing -> Banner("Bağlantı deneniyor…", Tone.NEUTRAL)
                is ServerState.Ok -> Banner("Bağlantı çalışıyor. Sunucu yanıtı: ${s.status}", Tone.OK)
                is ServerState.Failed -> Banner(s.message, Tone.BAD)
            }
            Text(
                "Telefondan bilgisayardaki backend'e bağlanırken bilgisayarın yerel ağ adresini yazın. Canlı sunucu için https:// adresini kullanın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.padding(20.dp)) {
            PrimaryButton("Kaydet ve dene", vm::saveAndTest, loading = state == ServerState.Testing)
        }
    }
}
