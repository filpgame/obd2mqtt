package com.frodrigues.odbmqtt.bluetooth

import kotlinx.coroutines.flow.StateFlow

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isBle: Boolean
)

interface BluetoothTransport {
    suspend fun connect()
    suspend fun disconnect()
    suspend fun sendCommand(command: String): String
    val isConnected: StateFlow<Boolean>
}

/**
 * Creates a transport for the given device address.
 * iOS: only BLE peripherals are supported (Apple's MFi restriction excludes Classic SPP).
 */
expect class BluetoothTransportProvider {
    suspend fun listPairedDevices(): List<BluetoothDeviceInfo>
    fun createTransport(address: String): BluetoothTransport
}
