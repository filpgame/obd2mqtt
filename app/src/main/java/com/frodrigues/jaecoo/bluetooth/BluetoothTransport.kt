package com.frodrigues.jaecoo.bluetooth

import kotlinx.coroutines.flow.StateFlow

interface BluetoothTransport {
    suspend fun connect()
    suspend fun disconnect()
    suspend fun sendCommand(command: String): String
    val isConnected: StateFlow<Boolean>
}
