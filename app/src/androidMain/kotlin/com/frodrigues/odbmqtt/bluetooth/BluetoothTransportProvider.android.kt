package com.frodrigues.odbmqtt.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

actual class BluetoothTransportProvider(private val context: Context) {

    actual suspend fun listPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return emptyList()
        return try {
            adapter.bondedDevices.map { dev ->
                BluetoothDeviceInfo(
                    address = dev.address,
                    name = runCatching { dev.name }.getOrDefault(dev.address),
                    isBle = dev.type == BluetoothDevice.DEVICE_TYPE_LE
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    actual fun createTransport(address: String): BluetoothTransport {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: error("Bluetooth not available")
        val device = adapter.getRemoteDevice(address)
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> BleTransport(context, device)
            else -> ClassicTransport(device)
        }
    }
}
