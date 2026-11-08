package com.hayathatti.app.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Hayat Hattı BLE yayın paketi, sürüm 1.
 *
 * Paket, BLE reklamının "üretici verisi" (manufacturer specific data) alanında taşınır.
 * Şirket kimliği 0xFFFF, Bluetooth SIG'in test ve geliştirme için ayırdığı değerdir.
 *
 * Klasik BLE reklamı en çok 31 bayttır. Üretici verisinin başlığı 4 bayt tutar
 * (uzunluk 1 + tür 1 + şirket kimliği 2), geriye paket için 27 bayt kalır.
 * Eski sürümde 128 bit UUID'li servis verisi kullanılıyordu; onun başlığı 18 bayt olduğundan
 * 5 baytlık mesajda paket 32 bayta çıkıyor ve Android "hata 1" (DATA_TOO_LARGE) veriyordu.
 *
 * Bayt sırası little endian:
 *
 * | Bayt | Boyut | Alan                                                       |
 * |------|-------|------------------------------------------------------------|
 * | 0    | 1     | İşaret 0x48 ('H'); başka uygulamaların paketlerini ayırır   |
 * | 1    | 1     | Sürüm (1)                                                  |
 * | 2    | 4     | Gönderen kimliği; her SOS başlatıldığında rastgele seçilir  |
 * | 6    | 4     | Enlem × 1.000.000 (int32)                                  |
 * | 10   | 4     | Boylam × 1.000.000 (int32)                                 |
 * | 14   | 1     | Bayraklar; şimdilik 0 (afet türü ve röle sayacı için ayrıldı) |
 * | 15   | 0–12  | Kısa mesaj, UTF-8; harf ortasından kesilmez                 |
 */
data class BlePacket(
    val senderId: Int,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val flags: Int = 0,
) {
    init {
        require(latitude in -90.0..90.0) { "Enlem -90 ile 90 arasında olmalı: $latitude" }
        require(longitude in -180.0..180.0) { "Boylam -180 ile 180 arasında olmalı: $longitude" }
        require(flags in 0..0xFF) { "Bayraklar tek bayta sığmalı: $flags" }
    }

    fun encode(): ByteArray {
        val text = truncateUtf8(message, MAX_MESSAGE_BYTES)
        return ByteBuffer.allocate(HEADER_SIZE + text.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(MAGIC)
            put(VERSION)
            putInt(senderId)
            putInt((latitude * COORD_FACTOR).roundToInt())
            putInt((longitude * COORD_FACTOR).roundToInt())
            put(flags.toByte())
            put(text)
        }.array()
    }

    /** Loglarda ve testlerde gönderen kimliğini okunur göstermek için. */
    val senderHex: String get() = "%08x".format(senderId)

    companion object {
        const val MANUFACTURER_ID = 0xFFFF
        const val MAGIC: Byte = 0x48
        const val VERSION: Byte = 1
        const val HEADER_SIZE = 15
        const val MAX_MESSAGE_BYTES = 12
        const val MAX_PAYLOAD_BYTES = HEADER_SIZE + MAX_MESSAGE_BYTES

        /** Reklamdaki üretici verisi başlığı: uzunluk + tür + şirket kimliği. */
        const val AD_OVERHEAD_BYTES = 4
        const val LEGACY_ADVERTISING_LIMIT = 31

        private const val COORD_FACTOR = 1_000_000.0

        /** Tarama filtresi bu iki bayta bakar; yalnızca Hayat Hattı paketleri uygulamaya gelir. */
        val FILTER_PREFIX = byteArrayOf(MAGIC, VERSION)
        val FILTER_MASK = byteArrayOf(-1, -1)

        /** Bozuk, kısa veya başka bir uygulamaya ait veride null döner; hiçbir durumda istisna atmaz. */
        fun decode(data: ByteArray?): BlePacket? {
            if (data == null || data.size < HEADER_SIZE || data.size > MAX_PAYLOAD_BYTES) return null
            if (data[0] != MAGIC || data[1] != VERSION) return null
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(2)
            val sender = buffer.int
            val lat = buffer.int / COORD_FACTOR
            val lon = buffer.int / COORD_FACTOR
            val flags = buffer.get().toInt() and 0xFF
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            val text = String(data, HEADER_SIZE, data.size - HEADER_SIZE, Charsets.UTF_8)
            return BlePacket(sender, lat, lon, text, flags)
        }

        /**
         * Metni UTF-8'e çevirip en çok [maxBytes] bayta kırpar. Kırpma harf sınırında yapılır:
         * "ş", "ğ", "İ" gibi iki baytlık harfler ya da emojiler yarım kalıp bozuk görünmez.
         */
        fun truncateUtf8(text: String, maxBytes: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream(maxBytes)
            var i = 0
            while (i < text.length) {
                val codePoint = text.codePointAt(i)
                val bytes = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8)
                if (out.size() + bytes.size > maxBytes) break
                out.write(bytes)
                i += Character.charCount(codePoint)
            }
            return out.toByteArray()
        }
    }
}

/**
 * Aynı yayın tarayıcıya saniyede birkaç kez gelir. Bu sınıf aynı paketi [windowMillis]
 * içinde yalnızca bir kez geçirir; böylece kuyruğa ve veritabanına gereksiz yazma yapılmaz.
 */
class SignalThrottle(
    private val windowMillis: Long = 30_000,
    private val maxEntries: Int = 256,
) {
    private val lastSeen = HashMap<BlePacket, Long>()

    @Synchronized
    fun shouldEmit(packet: BlePacket, nowMillis: Long): Boolean {
        val last = lastSeen[packet]
        if (last != null && nowMillis - last < windowMillis) return false
        if (lastSeen.size >= maxEntries) {
            lastSeen.entries.removeAll { nowMillis - it.value >= windowMillis }
            if (lastSeen.size >= maxEntries) lastSeen.clear()
        }
        lastSeen[packet] = nowMillis
        return true
    }
}
