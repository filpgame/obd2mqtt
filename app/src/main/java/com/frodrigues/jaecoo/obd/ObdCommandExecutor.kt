package com.frodrigues.jaecoo.obd

import com.frodrigues.jaecoo.bluetooth.BluetoothTransport
import kotlinx.coroutines.delay

class ObdCommandExecutor(private val transport: BluetoothTransport) {

    suspend fun initialize() {
        transport.sendCommand("ATZ")
        delay(1000)
        transport.sendCommand("ATE0")
        transport.sendCommand("ATL0")
        transport.sendCommand("ATH1")
        transport.sendCommand("ATSP6")
        transport.sendCommand("ATAT1")
    }

    suspend fun sendCommand(command: String): String = transport.sendCommand(command)
}
