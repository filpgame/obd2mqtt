package com.frodrigues.odbmqtt.bluetooth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * No-op transport so the iOS app can compile and the UI can be exercised before the
 * CoreBluetooth integration lands. Always reports "NO DATA" for any command.
 */
internal class IosStubTransport : BluetoothTransport {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override suspend fun connect() {
        throw NotImplementedError("iOS BLE transport not implemented — see BluetoothTransportProvider.ios.kt")
    }

    override suspend fun disconnect() {
        _isConnected.value = false
    }

    override suspend fun sendCommand(command: String): String = "NO DATA"
}
