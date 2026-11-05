package com.hayathatti.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val password: String,
    val phone: String? = null,
)

@Serializable
data class UserDto(
    val id: Long,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String,
    val phone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable data class AuthResponse(val token: String, val user: UserDto)

@Serializable data class HealthDto(val status: String, val time: String? = null)

@Serializable
data class LocationDto(
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
)

/** /api/signals ve /api/signals/mongo yanıtı. Postgres'te id sayı, Mongo'da metindir. */
@Serializable
data class SignalDto(
    val id: JsonPrimitive,
    @SerialName("device_signal_id") val deviceSignalId: String? = null,
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    val type: String? = null,
    val notes: String? = null,
    val location: LocationDto? = null,
    val timestamp: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val source: String? = null,
)

@Serializable
data class BleDto(
    val id: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val emergency: Boolean? = null,
    val note: String? = null,
    val timestamp: String? = null,
    val source: String? = null,
)

@Serializable data class DisasterDto(val code: String, val name: String, val count: Int)

@Serializable
data class SignalCreate(
    @SerialName("device_signal_id") val deviceSignalId: String,
    @SerialName("user_id") val userId: Long,
    val type: String,
    val timestamp: String,
    val lat: Double,
    val lon: Double,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    val notes: String? = null,
)

@Serializable
data class BleCreate(
    @SerialName("device_id") val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val emergency: Boolean = true,
    val note: String? = null,
    val timestamp: String,
)

enum class SignalSource(val label: String, val apiValue: String) {
    POSTGRES("PostgreSQL", "pg"),
    MONGO("MongoDB", "mongo"),
    BLE("BLE", "ble"),
}

/** Ekranların kullandığı tek sinyal biçimi. */
@Serializable
data class Signal(
    val id: String,
    val type: String?,
    val notes: String?,
    val lat: Double?,
    val lon: Double?,
    val accuracyM: Double?,
    val timestamp: String?,
    val createdAt: String?,
    val userId: String?,
    val deviceRef: String?,
    val source: String,
)

private fun JsonPrimitive?.valueOrNull(): String? = this?.content?.takeUnless { it == "null" }

fun SignalDto.toSignal(sourceLabel: String) = Signal(
    id = id.content,
    type = type,
    notes = notes,
    lat = location?.lat,
    lon = location?.lon,
    accuracyM = location?.accuracyM,
    timestamp = timestamp,
    createdAt = createdAt,
    userId = userId.valueOrNull(),
    deviceRef = deviceSignalId,
    source = sourceLabel,
)

fun BleDto.toSignal() = Signal(
    id = id,
    type = "ble",
    notes = note,
    lat = latitude,
    lon = longitude,
    accuracyM = null,
    timestamp = timestamp,
    createdAt = null,
    userId = userId.valueOrNull(),
    deviceRef = deviceId,
    source = SignalSource.BLE.label,
)

object DisasterTypes {
    val selectable = listOf("deprem" to "Deprem", "sel" to "Sel", "yangin" to "Yangın")

    fun label(code: String?): String = when (code) {
        "deprem" -> "Deprem"
        "sel" -> "Sel"
        "yangin" -> "Orman Yangını"
        "ble", "ble_relay" -> "BLE sinyali"
        null -> "Sinyal"
        else -> code
    }
}
