package com.frodrigues.odbmqtt.obd

import android.util.Log

class PidScanner(private val executor: ObdCommandExecutor) {

    /**
     * Probes each PID in PidRegistry directly instead of relying on OBD2 support bitmasks.
     *
     * Bitmask scanning (0100, 0120, ...) is unreliable on many vehicles — they declare PIDs
     * as supported in the bitmask but return NO DATA when polled. Direct probing ensures
     * supportedPids only contains PIDs that actually respond with valid data.
     */
    suspend fun scan(): Set<Int> {
        val supported = mutableSetOf<Int>()
        Log.d(TAG, "Probing ${PidRegistry.definitions.size} known PIDs...")

        for ((pid, def) in PidRegistry.definitions) {
            val pidHex = pid.toString(16).padStart(2, '0').uppercase()
            val response = executor.sendCommand("01$pidHex")
            val value = PidParser.parse(pid, response)

            if (value != null) {
                supported.add(pid)
                Log.d(TAG, "✓ 0x$pidHex ${def.name}: $value ${def.unit}")
            } else {
                Log.v(TAG, "✗ 0x$pidHex ${def.name}: no data (response: ${response.take(40)})")
            }
        }

        Log.d(TAG, "Scan complete — ${supported.size}/${PidRegistry.definitions.size} PIDs responding")
        return supported
    }

    companion object {
        private const val TAG = "PidScanner"
    }
}
