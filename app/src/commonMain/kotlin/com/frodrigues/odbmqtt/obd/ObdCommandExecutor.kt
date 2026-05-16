package com.frodrigues.odbmqtt.obd

import com.frodrigues.odbmqtt.bluetooth.BluetoothTransport
import com.frodrigues.odbmqtt.platform.Logger
import kotlinx.coroutines.delay

class ObdInitException(message: String) : Exception(message)

class ObdCommandExecutor(private val transport: BluetoothTransport) {

    suspend fun initialize() {
        val atzResponse = transport.sendCommand("ATZ")
        Logger.d(TAG, "ATZ: $atzResponse")
        delay(1000)
        if (!atzResponse.contains("ELM", ignoreCase = true) &&
            !atzResponse.contains("OK", ignoreCase = true)) {
            throw ObdInitException("ELM327 not detected. Got: $atzResponse")
        }
        Logger.d(TAG, "ATE0: ${transport.sendCommand("ATE0")}")
        Logger.d(TAG, "ATL0: ${transport.sendCommand("ATL0")}")
        Logger.d(TAG, "ATH1: ${transport.sendCommand("ATH1")}")
        Logger.d(TAG, "ATSP6: ${transport.sendCommand("ATSP6")}")
        Logger.d(TAG, "ATAT1: ${transport.sendCommand("ATAT1")}")
    }

    suspend fun sendCommand(command: String): String = transport.sendCommand(command)

    companion object {
        private const val TAG = "ObdCommandExecutor"
    }
}
