package com.hayathatti.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hayathatti.app.ui.AppCard
import com.hayathatti.app.ui.AuthViewModel
import com.hayathatti.app.ui.Banner
import com.hayathatti.app.ui.FormField
import com.hayathatti.app.ui.Header
import com.hayathatti.app.ui.PrimaryButton
import com.hayathatti.app.ui.SplashViewModel
import com.hayathatti.app.ui.StatusPill
import com.hayathatti.app.ui.Tone
import com.hayathatti.app.ui.theme.SosRed
import com.hayathatti.app.ui.theme.SosRedDeep
import com.hayathatti.app.ui.theme.SosRedLight

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(Modifier.size(72.dp)) {
            val r = size.minDimension / 2
            drawCircle(SosRed.copy(alpha = 0.18f), r)
            drawCircle(SosRed.copy(alpha = 0.5f), r * 0.78f, style = Stroke(2.dp.toPx()))
            drawCircle(
                Brush.radialGradient(listOf(SosRedLight, SosRed, SosRedDeep), center = Offset(r * 0.8f, r * 0.7f), radius = r),
                r * 0.5f,
            )
        }
        Text("Hayat Hattı", style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun SplashScreen(vm: SplashViewModel, onDone: (Boolean) -> Unit) {
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.check(onDone) }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Brand()
        Spacer(Modifier.height(10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, error: String?, helper: String? = null) {
    var visible by rememberSaveable { mutableStateOf(false) }
    FormField(
        value = value,
        onChange = onChange,
        label = label,
        error = error,
        helper = helper,
        keyboard = KeyboardType.Password,
        transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Şifreyi gizle" else "Şifreyi göster",
                )
            }
        },
    )
}

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit,
    onServer: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Brand(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp))
        FormField(email, { email = it }, "E-posta", error = ui.emailError, keyboard = KeyboardType.Email)
        PasswordField(password, { password = it }, "Şifre", ui.passwordError)
        ui.error?.let { Banner(it, Tone.BAD) }
        PrimaryButton("Giriş yap", { vm.login(email, password, onSignedIn) }, loading = ui.busy)
        TextButton(onClick = { vm.clearErrors(); onRegister() }, Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text("Hesabım yok, kayıt ol")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onGuest,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Rounded.Sos, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("  Hesapsız SOS", color = MaterialTheme.colorScheme.primary)
        }
        Text(
            "Acil durumda hesap gerekmez.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onServer, Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Icon(Icons.Rounded.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Sunucu ayarı")
        }
    }
}

@Composable
fun RegisterScreen(vm: AuthViewModel, onSignedIn: () -> Unit, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Header("Kayıt ol", "Kayıtlarınız adınıza bağlansın", onBack = { vm.clearErrors(); onBack() })
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(first, { first = it }, "Ad", Modifier.weight(1f), error = ui.firstError)
                FormField(last, { last = it }, "Soyad", Modifier.weight(1f), error = ui.lastError)
            }
            FormField(email, { email = it }, "E-posta", error = ui.emailError, keyboard = KeyboardType.Email)
            FormField(phone, { phone = it }, "Telefon (isteğe bağlı)", keyboard = KeyboardType.Phone)
            PasswordField(password, { password = it }, "Şifre", ui.passwordError, helper = "En az 8 karakter")
            ui.error?.let { Banner(it, Tone.BAD) }
            PrimaryButton("Kayıt ol", { vm.register(first, last, email, phone, password, onSignedIn) }, loading = ui.busy)
        }
    }
}

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    var hasLocation by rememberSaveable { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocation = granted()
        onContinue()
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Header("İzinler", "Yardım çağrısının yerinizi bildirebilmesi için")
        Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("  Konum", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusPill(if (hasLocation) "Verildi" else "Bekliyor", if (hasLocation) Tone.OK else Tone.WARN)
                }
                Text(
                    "SOS ve sinyal kayıtlarına enlem ve boylam eklemek için gerekir. Konum yalnızca siz gönderdiğinizde paylaşılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton(
                if (hasLocation) "Devam et" else "Konum iznini ver",
                {
                    if (hasLocation) {
                        onContinue()
                    } else {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
            )
            if (!hasLocation) {
                TextButton(onClick = onContinue, Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text("Şimdilik geç") }
            }
        }
    }
}
