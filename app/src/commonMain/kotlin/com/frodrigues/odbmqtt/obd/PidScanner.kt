package com.frodrigues.odbmqtt.obd

import com.frodrigues.odbmqtt.platform.Logger
import com.frodrigues.odbmqtt.platform.toHexUpper
import com.frodrigues.odbmqtt.settings.AppSettings
import kotlinx.coroutines.flow.first

class PidScanner(
    private val executor: ObdCommandExecutor,
    private val settings: AppSettings,
    private val onProgress: suspend (scanned: Int, total: Int) -> Unit = { _, _ -> }
) {
    private val supportPids = setOf(0x00, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0)
    private val scanRange = (0x01..0xFF).filter { it !in supportPids }

    suspend fun scan(): Set<Int> {
        val cached = settings.cachedPids.first()
        if (cached.isNotEmpty()) {
            Logger.d(TAG, "Cache hit: ${cached.size} PIDs")
            return cached
        }
        return bruteForce()
    }

    private suspend fun bruteForce(): Set<Int> {
        val supported = mutableSetOf<Int>()
        val total = scanRange.size
        Logger.d(TAG, "Brute-force scan: probing $total PIDs (0x01–0xFF)...")

        scanRange.forEachIndexed { index, pid ->
            onProgress(index + 1, total)

            val pidHex = pid.toHexUpper(2)
            val response = executor.sendCommand("01$pidHex")
            val bytes = ObdResponseParser.extractDataBytes(response, pid)

            if (bytes != null && bytes.isNotEmpty()) {
                supported.add(pid)
                val name = PidRegistry.definitions[pid]?.name ?: "Unknown"
                val preview = bytes.take(4).joinToString(" ") { (it.toInt() and 0xFF).toHexUpper(2) }
                Logger.d(TAG, "✓ 0x$pidHex $name  [${bytes.size}B raw=$preview]")
            } else {
                Logger.v(TAG, "✗ 0x$pidHex: ${response.trim().take(20)}")
            }
        }

        Logger.d(TAG, "Brute-force done: ${supported.size}/$total PIDs respond")
        settings.savePidCache(supported)
        return supported
    }

    companion object {
        private const val TAG = "PidScanner"
    }
}
