package com.hayathatti.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

enum class OutboxKind { OWN_SOS, REPORT, RELAY }
enum class OutboxStatus { PENDING, SENT, FAILED }

/** Sunucuya gidecek her kayıt önce buraya yazılır; telefon kapansa da kaybolmaz. */
@Entity(tableName = "outbox")
data class OutboxItem(
    @PrimaryKey val sid: String,
    val kind: String,
    val type: String?,
    val shortMessage: String?,
    val note: String?,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double?,
    val rssi: Int?,
    val timestamp: String,
    val createdAtMillis: Long,
    val status: String = OutboxStatus.PENDING.name,
    val attempts: Int = 0,
    val sentTo: String? = null,
    val lastError: String? = null,
)

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: OutboxItem): Long

    @Query("SELECT * FROM outbox ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<OutboxItem>>

    @Query("SELECT * FROM outbox WHERE kind = 'RELAY' ORDER BY createdAtMillis DESC")
    fun observeRelays(): Flow<List<OutboxItem>>

    @Query("SELECT * FROM outbox WHERE status = 'PENDING' ORDER BY createdAtMillis ASC")
    suspend fun pending(): List<OutboxItem>

    @Query("UPDATE outbox SET status = 'SENT', sentTo = :path, lastError = NULL, attempts = attempts + 1 WHERE sid = :sid")
    suspend fun markSent(sid: String, path: String)

    @Query("UPDATE outbox SET status = 'FAILED', lastError = :error, attempts = attempts + 1 WHERE sid = :sid")
    suspend fun markFailed(sid: String, error: String)

    @Query("UPDATE outbox SET lastError = :error, attempts = attempts + 1 WHERE sid = :sid")
    suspend fun markRetry(sid: String, error: String)

    @Query("UPDATE outbox SET status = 'PENDING', lastError = NULL WHERE status = 'FAILED'")
    suspend fun requeueFailed()
}

@Database(entities = [OutboxItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outbox(): OutboxDao

    companion object {
        fun create(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, "hayat_hatti.db").build()
    }
}

/** Kaydın hangi endpoint'e nasıl gideceği. Hesap varsa /api/signals, yoksa /api/ble-data. */
sealed interface OutboxRequest {
    val path: String

    data class ToSignals(val body: SignalCreate) : OutboxRequest {
        override val path = "/api/signals"
    }

    data class ToBle(val body: BleCreate) : OutboxRequest {
        override val path = "/api/ble-data"
    }
}

object OutboxRouting {
    const val RELAY_TYPE = "ble_relay"

    fun noteFor(item: OutboxItem): String? = when (item.kind) {
        OutboxKind.RELAY.name ->
            listOfNotNull(item.shortMessage, item.rssi?.let { "RSSI $it" }, "röle").joinToString(" | ")
        else ->
            listOf(item.shortMessage, item.note).filterNot { it.isNullOrBlank() }.joinToString(" · ").ifBlank { null }
    }

    fun route(item: OutboxItem, userId: Long?, deviceId: String): OutboxRequest {
        val note = noteFor(item)
        return if (userId != null) {
            OutboxRequest.ToSignals(
                SignalCreate(
                    deviceSignalId = item.sid,
                    userId = userId,
                    type = if (item.kind == OutboxKind.RELAY.name) RELAY_TYPE else item.type ?: "deprem",
                    timestamp = item.timestamp,
                    lat = item.lat,
                    lon = item.lon,
                    accuracyM = item.accuracyM,
                    notes = note,
                ),
            )
        } else {
            OutboxRequest.ToBle(
                BleCreate(
                    deviceId = deviceId,
                    latitude = item.lat,
                    longitude = item.lon,
                    note = note,
                    timestamp = item.timestamp,
                ),
            )
        }
    }

    /** Aynı sinyal 10 dakika içinde tekrar duyulursa aynı kimliği alır, ikinci kayıt açılmaz. */
    fun relayId(lat: Double, lon: Double, message: String, epochMillis: Long): String {
        val key = "${(lat * 1e6).toLong()}|${(lon * 1e6).toLong()}|$message|${epochMillis / 600_000}"
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return "ble-" + hash.joinToString("") { "%02x".format(it) }.take(32)
    }
}
