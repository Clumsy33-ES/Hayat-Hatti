package com.hayathatti.app.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePacketTest {
    private val elazig = BlePacket(senderId = 0x01020304, latitude = 38.6748, longitude = 39.2225, message = "YRDM")

    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun encodesToTheDocumentedBytes() {
        // Python struct ile ayrıca hesaplandı; nRF Connect testinde de bu dizi kullanılabilir.
        assertArrayEquals(hex("48010403020170214e02e47c5602005952444d"), elazig.encode())
    }

    @Test
    fun docsExamplePacketDecodes() {
        // docs/BLE.md'deki nRF Connect örneği.
        val p = BlePacket.decode(hex("4801EFBEAD0B70214E02E47C560200454E4B5A"))!!
        assertEquals(0x0BADBEEF, p.senderId)
        assertEquals(38.6748, p.latitude, 1e-9)
        assertEquals(39.2225, p.longitude, 1e-9)
        assertEquals("ENKZ", p.message)
        assertEquals(0, p.flags)
    }

    @Test
    fun roundTripKeepsAllFields() {
        val back = BlePacket.decode(elazig.encode())
        assertEquals(elazig, back)
    }

    @Test
    fun negativeCoordinatesRoundTrip() {
        val p = BlePacket(-7, -33.868820, -151.209296, "SOS")
        val back = BlePacket.decode(p.encode())!!
        assertEquals(p.latitude, back.latitude, 1e-6)
        assertEquals(p.longitude, back.longitude, 1e-6)
        assertEquals(-7, back.senderId)
    }

    @Test
    fun longestPacketFitsInLegacyAdvertisement() {
        val p = BlePacket(1, 89.999999, 179.999999, "ABCDEFGHIJKLMNOP")
        val payload = p.encode()
        assertEquals(BlePacket.MAX_PAYLOAD_BYTES, payload.size)
        assertTrue(payload.size + BlePacket.AD_OVERHEAD_BYTES <= BlePacket.LEGACY_ADVERTISING_LIMIT)
    }

    @Test
    fun fourTurkishLettersFitWithoutCutting() {
        val p = BlePacket(1, 38.0, 39.0, "ŞĞÜİ")
        assertEquals(15 + 8, p.encode().size)
        assertEquals("ŞĞÜİ", BlePacket.decode(p.encode())!!.message)
    }

    @Test
    fun truncationNeverSplitsALetter() {
        // 7 iki baytlık harf = 14 bayt; 12 bayta kırpınca 6 harf kalmalı, yarım harf olmamalı.
        assertEquals("şşşşşş", String(BlePacket.truncateUtf8("şşşşşşş", 12), Charsets.UTF_8))
        // 'a' + 'ş' = 3 bayt; 2 bayta kırpınca sadece 'a' kalır.
        assertEquals("a", String(BlePacket.truncateUtf8("aş", 2), Charsets.UTF_8))
        // Emoji 4 bayttır; 3 baytlık yere sığmazsa hiç yazılmaz.
        assertEquals(0, BlePacket.truncateUtf8("🆘", 3).size)
    }

    @Test
    fun emptyMessageIsAllowed() {
        val back = BlePacket.decode(BlePacket(5, 1.0, 2.0, "").encode())!!
        assertEquals("", back.message)
    }

    @Test
    fun rejectsForeignOrBrokenData() {
        val good = elazig.encode()
        assertNull(BlePacket.decode(null))
        assertNull(BlePacket.decode(ByteArray(0)))
        assertNull(BlePacket.decode(good.copyOf(14)))
        assertNull(BlePacket.decode(good.copyOf().also { it[0] = 0x00 }))
        assertNull(BlePacket.decode(good.copyOf().also { it[1] = 2 }))
        val tooLong = ByteArray(BlePacket.MAX_PAYLOAD_BYTES + 1).also { it[0] = BlePacket.MAGIC; it[1] = BlePacket.VERSION }
        assertNull(BlePacket.decode(tooLong))
    }

    @Test
    fun rejectsImpossibleCoordinates() {
        val bytes = elazig.encode()
        // Enlem alanını 95.000000 yap.
        java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(6, 95_000_000)
        assertNull(BlePacket.decode(bytes))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesToEncodeInvalidLatitude() {
        BlePacket(1, 91.0, 0.0, "X")
    }

    @Test
    fun filterPrefixMatchesEncodedPacket() {
        val payload = elazig.encode()
        for (i in BlePacket.FILTER_PREFIX.indices) {
            assertEquals(BlePacket.FILTER_PREFIX[i], payload[i])
        }
        assertEquals(BlePacket.FILTER_PREFIX.size, BlePacket.FILTER_MASK.size)
    }
}

class SignalThrottleTest {
    private val p = BlePacket(1, 38.0, 39.0, "YRDM")

    @Test
    fun samePacketPassesOncePerWindow() {
        val t = SignalThrottle(windowMillis = 30_000)
        assertTrue(t.shouldEmit(p, 0))
        assertFalse(t.shouldEmit(p, 1_000))
        assertFalse(t.shouldEmit(p, 29_999))
        assertTrue(t.shouldEmit(p, 30_000))
    }

    @Test
    fun differentSendersAreIndependent() {
        val t = SignalThrottle()
        assertTrue(t.shouldEmit(p, 0))
        assertTrue(t.shouldEmit(p.copy(senderId = 2), 0))
        assertTrue(t.shouldEmit(p.copy(message = "ENKZ"), 0))
    }

    @Test
    fun staysBoundedWhenManySendersAppear() {
        val t = SignalThrottle(windowMillis = 30_000, maxEntries = 4)
        repeat(4) { assertTrue(t.shouldEmit(p.copy(senderId = it), 0)) }
        // Liste doluyken süresi geçmiş kayıtlar temizlenir ve yeni gönderen kabul edilir.
        assertTrue(t.shouldEmit(p.copy(senderId = 99), 31_000))
        // Temizlikten sonra da tekrarı engellemeye devam eder.
        assertFalse(t.shouldEmit(p.copy(senderId = 99), 32_000))
    }
}
