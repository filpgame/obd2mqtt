package com.frodrigues.odbmqtt.obd

import com.frodrigues.odbmqtt.platform.nowMillis
import com.frodrigues.odbmqtt.platform.toHexUpper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class PidReading(
    val pid: Int,
    val value: Double,
    val timestamp: Long = nowMillis()
)

class PidPoller(
    private val executor: ObdCommandExecutor,
    private val pids: Set<Int>
) {
    private val fastPids = pids.filter { PidRegistry.getOrUnknown(it).isFast }.toSet()
    private val slowPids = pids - fastPids
    private val slowEvery = SLOW_INTERVAL_SECONDS / FAST_INTERVAL_SECONDS

    fun readings(): Flow<PidReading> = flow {
        var cycle = 0
        while (true) {
            val cycleStart = nowMillis()

            for (pid in fastPids) {
                val response = executor.sendCommand("01${pid.toHexUpper(2)}")
                PidParser.parse(pid, response)?.let { emit(PidReading(pid, it)) }
            }

            if (cycle % slowEvery == 0) {
                for (pid in slowPids) {
                    val response = executor.sendCommand("01${pid.toHexUpper(2)}")
                    PidParser.parse(pid, response)?.let { emit(PidReading(pid, it)) }
                }
            }

            cycle++
            val remaining = FAST_INTERVAL_SECONDS * 1000L - (nowMillis() - cycleStart)
            if (remaining > 0) delay(remaining)
        }
    }

    companion object {
        const val FAST_INTERVAL_SECONDS = 1
        const val SLOW_INTERVAL_SECONDS = 30
    }
}
