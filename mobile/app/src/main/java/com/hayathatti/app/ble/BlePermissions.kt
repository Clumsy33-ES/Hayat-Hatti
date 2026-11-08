package com.hayathatti.app.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * BLE'nin çalışma zamanında istediği izinler.
 *
 * Android 12 (API 31) ve sonrası: BLUETOOTH_SCAN ve BLUETOOTH_ADVERTISE. Manifest'te
 * BLUETOOTH_SCAN "neverForLocation" ile tanımlı olduğundan tarama için konum izni gerekmez.
 * Android 8–11: BLUETOOTH ve BLUETOOTH_ADMIN kurulumda verilir; tarama için konum izni gerekir.
 *
 * Ekranlar izin isterken [required] dizisini kullanabilir:
 * `launcher.launch(BlePermissions.required())`
 */
object BlePermissions {
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun canScan(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            granted(context, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun canAdvertise(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || granted(context, Manifest.permission.BLUETOOTH_ADVERTISE)

    fun allGranted(context: Context): Boolean = canScan(context) && canAdvertise(context)

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
