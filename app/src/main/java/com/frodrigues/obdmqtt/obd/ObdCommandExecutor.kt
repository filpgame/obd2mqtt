package com.frodrigues.obdmqtt.obd

import android.util.Log
import com.frodrigues.obdmqtt.bluetooth.BluetoothTransport
import kotlinx.coroutines.delay
import java.io.IOException

class ObdCommandExecutor(private val transport: BluetoothTransport) {

    suspend fun initialize() {
        val atzResponse = transport.sendCommand("ATZ")
        Log.d(TAG, "ATZ: $atzResponse")
        delay(1000)
        if (!atzResponse.contains("ELM", ignoreCase = true) &&
            !atzResponse.contains("OK", ignoreCase = true)) {
            throw IOException("ELM327 not detected. Got: $atzResponse")
        }
        Log.d(TAG, "ATE0: ${transport.sendCommand("ATE0")}")
        Log.d(TAG, "ATL0: ${transport.sendCommand("ATL0")}")
        Log.d(TAG, "ATH1: ${transport.sendCommand("ATH1")}")
        Log.d(TAG, "ATSP6: ${transport.sendCommand("ATSP6")}")
        Log.d(TAG, "ATAT1: ${transport.sendCommand("ATAT1")}")
    }

    suspend fun sendCommand(command: String): String = transport.sendCommand(command)

    companion object {
        private const val TAG = "ObdCommandExecutor"
    }
}
