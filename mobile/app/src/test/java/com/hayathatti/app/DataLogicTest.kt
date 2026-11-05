package com.hayathatti.app

import com.hayathatti.app.data.ApiErrors
import com.hayathatti.app.data.AppJson
import com.hayathatti.app.data.BleCreate
import com.hayathatti.app.data.BleDto
import com.hayathatti.app.data.ErrKind
import com.hayathatti.app.data.OutboxItem
import com.hayathatti.app.data.OutboxKind
import com.hayathatti.app.data.OutboxRequest
import com.hayathatti.app.data.OutboxRouting
import com.hayathatti.app.data.SignalCreate
import com.hayathatti.app.data.SignalDto
import com.hayathatti.app.data.Time
import com.hayathatti.app.data.toSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DataLogicTest {
    private fun item(kind: OutboxKind, short: String? = "ENKZ", note: String? = "2. kat") = OutboxItem(
        sid = "s1",
        kind = kind.name,
        type = "sel",
        shortMessage = short,
        note = note,
        lat = 38.6748,
        lon = 39.2225,
        accuracyM = 8.0,
        rssi = if (kind == OutboxKind.RELAY) -67 else null,
        timestamp = "2026-11-05T10:00:00Z",
        createdAtMillis = 0,
    )

    @Test
    fun accountSosGoesToSignals() {
        val r = OutboxRouting.route(item(OutboxKind.OWN_SOS), 14, "dev") as OutboxRequest.ToSignals
        assertEquals("/api/signals", r.path)
        assertEquals(14L, r.body.userId)
        assertEquals("sel", r.body.type)
        assertEquals("s1", r.body.deviceSignalId)
        assertEquals("ENKZ · 2. kat", r.body.notes)
    }

    @Test
    fun guestSosGoesToBleData() {
        val r = OutboxRouting.route(item(OutboxKind.OWN_SOS), null, "dev") as OutboxRequest.ToBle
        assertEquals("/api/ble-data", r.path)
        assertEquals("dev", r.body.deviceId)
        assertTrue(r.body.emergency)
    }

    @Test
    fun guestRequestJsonHasFieldsBackendRequires() {
        val r = OutboxRouting.route(item(OutboxKind.OWN_SOS), null, "dev") as OutboxRequest.ToBle
        val json = AppJson.encodeToString(BleCreate.serializer(), r.body)
        listOf("\"device_id\"", "\"latitude\"", "\"longitude\"", "\"emergency\":true").forEach {
            assertTrue("$it eksik: $json", json.contains(it))
        }
    }

    @Test
    fun accountRequestJsonHasFieldsBackendRequires() {
        val r = OutboxRouting.route(item(OutboxKind.OWN_SOS), 14, "dev") as OutboxRequest.ToSignals
        val json = AppJson.encodeToString(SignalCreate.serializer(), r.body)
        listOf("device_signal_id", "user_id", "type", "timestamp", "lon", "lat").forEach {
            assertTrue("$it eksik: $json", json.contains("\"$it\""))
        }
    }

    @Test
    fun relayUsesRelayType() {
        val r = OutboxRouting.route(item(OutboxKind.RELAY, note = null), 27, "dev") as OutboxRequest.ToSignals
        assertEquals("ble_relay", r.body.type)
        assertEquals("ENKZ | RSSI -67 | röle", r.body.notes)
    }

    @Test
    fun blankMessageAndNoteBecomeNull() {
        assertNull(OutboxRouting.noteFor(item(OutboxKind.OWN_SOS, short = null, note = " ")))
    }

    @Test
    fun sameSignalKeepsIdWithinTenMinutes() {
        val t = 1_793_000_000_000L - (1_793_000_000_000L % 600_000)
        val a = OutboxRouting.relayId(38.6748, 39.2225, "ENKZ", t)
        assertEquals(a, OutboxRouting.relayId(38.6748, 39.2225, "ENKZ", t + 599_000))
        assertNotEquals(a, OutboxRouting.relayId(38.6748, 39.2225, "ENKZ", t + 600_000))
        assertNotEquals(a, OutboxRouting.relayId(38.6748, 39.2225, "YARD", t))
        assertTrue(a.length <= 64)
    }

    @Test
    fun statusCodesMapToKinds() {
        assertEquals(ErrKind.UNAUTHORIZED, ApiErrors.fromStatus(401).kind)
        assertEquals("E-posta veya şifre hatalı.", ApiErrors.fromStatus(401, mapOf(401 to "E-posta veya şifre hatalı.")).message)
        assertEquals(ErrKind.REJECTED, ApiErrors.fromStatus(422).kind)
        assertEquals(ErrKind.SERVER, ApiErrors.fromStatus(500).kind)
    }

    @Test
    fun parsesTimesWithAndWithoutZone() {
        val expected = Instant.parse("2026-11-05T08:01:56.850Z")
        assertEquals(expected, Time.parse("2026-11-05T08:01:56.850000"))
        assertEquals(expected, Time.parse("2026-11-05T08:01:56.850000+00:00"))
        assertNull(Time.parse("gecersiz"))
        assertNull(Time.parse(null))
    }

    @Test
    fun decodesPostgresResponse() {
        val json = """{"id":104,"device_signal_id":"a","user_id":14,"type":"deprem","notes":"ENKZ","attachments":null,
            "location":{"lat":38.6,"lon":39.2,"accuracy_m":8.0},"timestamp":"2026-11-05T10:00:00+00:00","created_at":null,"source":"pg"}"""
        val s = AppJson.decodeFromString(SignalDto.serializer(), json).toSignal("PostgreSQL")
        assertEquals("104", s.id)
        assertEquals("14", s.userId)
        assertEquals(38.6, s.lat!!, 0.0)
    }

    @Test
    fun decodesMongoResponseWithTextIdAndNullUser() {
        val json = """{"id":"6aec37f4","device_signal_id":null,"user_id":null,"type":"sel","notes":null,"attachments":["x"],
            "location":{"lat":null,"lon":null,"accuracy_m":null},"timestamp":null,"created_at":null,"source":"mongo"}"""
        val s = AppJson.decodeFromString(SignalDto.serializer(), json).toSignal("MongoDB")
        assertEquals("6aec37f4", s.id)
        assertNull(s.userId)
        assertNull(s.lat)
    }

    @Test
    fun decodesBleListResponse() {
        val json = """{"id":"6aec","device_id":"d1","user_id":null,"latitude":38.6,"longitude":39.2,"emergency":true,"note":"YARD","timestamp":"2026-11-05T08:01:56.850000","source":"mongo"}"""
        val s = AppJson.decodeFromString(BleDto.serializer(), json).toSignal()
        assertEquals("ble", s.type)
        assertEquals("YARD", s.notes)
        assertEquals("d1", s.deviceRef)
    }
}
