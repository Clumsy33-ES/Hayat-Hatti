package com.hayathatti.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hayathatti.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.store by preferencesDataStore("hayat_hatti")

/** Token'ı Android Keystore'daki anahtarla şifreler. */
private object TokenCipher {
    private const val ALIAS = "hayat_hatti_token"

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val body = cipher.doFinal(plain.toByteArray())
        return Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
    }

    fun decrypt(stored: String): String? = runCatching {
        val all = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all.copyOfRange(0, 12)))
        String(cipher.doFinal(all.copyOfRange(12, all.size)))
    }.getOrNull()
}

data class Session(val token: String, val user: UserDto)

class Prefs(context: Context) {
    private val ds = context.applicationContext.store

    private val tokenKey = stringPreferencesKey("token")
    private val userKey = stringPreferencesKey("user")
    private val urlKey = stringPreferencesKey("base_url")
    private val enteredKey = booleanPreferencesKey("entered")
    private val permsKey = booleanPreferencesKey("perms_seen")
    private val deviceKey = stringPreferencesKey("device_id")

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    private val _baseUrl = MutableStateFlow(BuildConfig.DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl

    private val _entered = MutableStateFlow(false)
    val entered: StateFlow<Boolean> = _entered

    private val _permsSeen = MutableStateFlow(false)
    val permsSeen: StateFlow<Boolean> = _permsSeen

    var deviceId: String = ""
        private set

    suspend fun load() {
        val p = runCatching { ds.data.first() }.getOrDefault(emptyPreferences())
        _baseUrl.value = p[urlKey] ?: BuildConfig.DEFAULT_BASE_URL
        _entered.value = p[enteredKey] ?: false
        _permsSeen.value = p[permsKey] ?: false
        val token = p[tokenKey]?.let(TokenCipher::decrypt)
        val user = p[userKey]?.let { raw ->
            runCatching { AppJson.decodeFromString(UserDto.serializer(), raw) }.getOrNull()
        }
        _session.value = if (token != null && user != null) Session(token, user) else null
        deviceId = p[deviceKey] ?: UUID.randomUUID().toString().also { id -> ds.edit { it[deviceKey] = id } }
    }

    suspend fun saveSession(session: Session) {
        ds.edit {
            it[tokenKey] = TokenCipher.encrypt(session.token)
            it[userKey] = AppJson.encodeToString(UserDto.serializer(), session.user)
            it[enteredKey] = true
        }
        _session.value = session
        _entered.value = true
    }

    /** Oturum süresi dolunca giriş bilgisi silinir, uygulama açık kalır. */
    suspend fun clearSession() {
        ds.edit {
            it.remove(tokenKey)
            it.remove(userKey)
        }
        _session.value = null
    }

    suspend fun logout() {
        clearSession()
        ds.edit { it[enteredKey] = false }
        _entered.value = false
    }

    suspend fun enterAsGuest() {
        ds.edit { it[enteredKey] = true }
        _entered.value = true
    }

    suspend fun markPermsSeen() {
        ds.edit { it[permsKey] = true }
        _permsSeen.value = true
    }

    suspend fun saveBaseUrl(url: String) {
        ds.edit { it[urlKey] = url }
        _baseUrl.value = url
    }

    suspend fun saveCache(key: String, signals: List<Signal>) {
        ds.edit {
            it[stringPreferencesKey("cache_$key")] =
                AppJson.encodeToString(ListSerializer(Signal.serializer()), signals)
            it[longPreferencesKey("cache_time_$key")] = System.currentTimeMillis()
        }
    }

    suspend fun readCache(key: String): Pair<List<Signal>, Long>? {
        val p = ds.data.first()
        val raw = p[stringPreferencesKey("cache_$key")] ?: return null
        val list = runCatching {
            AppJson.decodeFromString(ListSerializer(Signal.serializer()), raw)
        }.getOrNull() ?: return null
        return list to (p[longPreferencesKey("cache_time_$key")] ?: 0L)
    }
}
