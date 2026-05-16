package com.frodrigues.odbmqtt.obd

import com.frodrigues.odbmqtt.platform.Logger
import com.frodrigues.odbmqtt.platform.toHexUpper
import com.frodrigues.odbmqtt.settings.AppSettings
import kotlinx.coroutines.flow.first

class Mode22Scanner(
    private val executor: ObdCommandExecutor,
    private val settings: AppSettings,
    private val onProgress: suspend (scanned: Int, total: Int) -> Unit = { _, _ -> }
) {
    private val scanRanges = listOf(
        0xF400..0xF4FF,
    )

    suspend fun scan(): Map<Int, ByteArray> {
        val cached = settings.cachedMode22Pids.first()
        if (cached.isNotEmpty()) {
            Logger.d(TAG, "Mode22 cache hit: ${cached.size} PIDs")
            return cached
        }
        return bruteForce()
    }

    private suspend fun bruteForce(): Map<Int, ByteArray> {
        val found = mutableMapOf<Int, ByteArray>()
        val allPids = scanRanges.flatMap { it.toList() }
        val total = allPids.size

        Logger.d(TAG, "Mode22 brute-force: probing $total PIDs across ${scanRanges.size} ranges")

        allPids.forEachIndexed { index, pid ->
            onProgress(index + 1, total)

            val high = (pid shr 8) and 0xFF
            val low = pid and 0xFF
            val cmd = "22${high.toHexUpper(2)}${low.toHexUpper(2)}"

            val response = executor.sendCommand(cmd)
            val bytes = extractMode22Bytes(response, pid)

            if (bytes != null && bytes.isNotEmpty()) {
                found[pid] = bytes
                val name = Mode22Registry.definitions[pid]?.name ?: "Unknown"
                val hexBytes = bytes.take(8).joinToString(" ") { (it.toInt() and 0xFF).toHexUpper(2) }
                Logger.d(TAG, "✓ 0x${pid.toHexUpper(4)} $name  [$hexBytes]")
            } else if (response.contains("7F", ignoreCase = true) &&
                       !response.contains("NO DATA", ignoreCase = true)) {
                Logger.v(TAG, "~ 0x${pid.toHexUpper(4)} NRC: ${response.trim().take(30)}")
            } else {
                Logger.v(TAG, "✗ 0x${pid.toHexUpper(4)}: ${response.trim().take(20)}")
            }
        }

        Logger.d(TAG, "Mode22 done: ${found.size}/$total PIDs responded with data")
        settings.saveMode22Cache(found)
        return found
    }

    companion object {
        private const val TAG = "Mode22Scanner"

        fun extractMode22Bytes(response: String, pid: Int): ByteArray? {
            val upper = response.uppercase().replace(Regex("[\\s\\r\\n]+"), "")
            if (upper.isBlank() ||
                upper.contains("NODATA") ||
                upper.contains("ERROR") ||
                upper.contains("UNABLE")) return null

            val marker = "62${pid.toHexUpper(4)}"
            val idx = upper.indexOf(marker)
            if (idx == -1) return null

            val dataHex = upper.substring(idx + marker.length)
            return dataHex.chunked(2)
                .take(8)
                .mapNotNull { it.toIntOrNull(16)?.toByte() }
                .toByteArray()
                .takeIf { it.isNotEmpty() }
        }
    }
}
