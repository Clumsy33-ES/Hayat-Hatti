package com.hayathatti.app.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** BLE dalındaki bir sinyalin uygulamaya geliş biçimi. */
data class BleIncoming(val latitude: Double, val longitude: Double, val message: String, val rssi: Int?)

/**
 * BLE modülünün bağlanacağı yer. Birleştirme yapılırken feature/ble-stable-final dalındaki
 * BleManager.startSos / stopSos ve BleScanner bu arayüzün arkasına alınır, ekranlar değişmez.
 */
interface BleGateway {
    val isConnected: Boolean
    val incoming: Flow<BleIncoming>
    fun startSos(message: String, latitude: Double, longitude: Double)
    fun stopSos()
}

/** Birleştirmeye kadar kullanılan boş uygulama: yayın yapmaz, sinyal üretmez. */
object NotConnectedBle : BleGateway {
    override val isConnected = false
    override val incoming: Flow<BleIncoming> = emptyFlow()
    override fun startSos(message: String, latitude: Double, longitude: Double) = Unit
    override fun stopSos() = Unit
}
