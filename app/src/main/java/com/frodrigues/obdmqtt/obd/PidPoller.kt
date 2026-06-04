package com.frodrigues.obdmqtt.obd

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

data class PidReading(
    val pid: Int,
    val value: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class PidPoller(
    private val executor: ObdCommandExecutor,
    private val fastPids: Set<Int>,
    private val slowPids: Set<Int>
) {
    private val slowEvery = SLOW_INTERVAL_SECONDS / FAST_INTERVAL_SECONDS

    fun readings(): Flow<PidReading> = flow {
        var consecutiveFailures = 0
        var cycle = 0
        while (true) {
            val cycleStart = System.currentTimeMillis()

            for (pid in fastPids) {
                try {
                    val response = executor.sendCommand("01${pid.toString(16).padStart(2, '0').uppercase()}")
                    PidParser.parse(pid, response)?.let {
                        consecutiveFailures = 0
                        emit(PidReading(pid, it))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.w(TAG, "PID 0x${pid.toString(16).uppercase()} failed ($consecutiveFailures): ${e.message}")
                    if (consecutiveFailures >= MAX_FAILURES)
                        throw IOException("$consecutiveFailures consecutive failures — connection dead", e)
                }
            }

            if (cycle % slowEvery == 0) {
                for (pid in slowPids) {
                    try {
                        val response = executor.sendCommand("01${pid.toString(16).padStart(2, '0').uppercase()}")
                        PidParser.parse(pid, response)?.let {
                            consecutiveFailures = 0
                            emit(PidReading(pid, it))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        consecutiveFailures++
                        Log.w(TAG, "PID 0x${pid.toString(16).uppercase()} (slow) failed ($consecutiveFailures): ${e.message}")
                        if (consecutiveFailures >= MAX_FAILURES)
                            throw IOException("$consecutiveFailures consecutive failures — connection dead", e)
                    }
                }
            }

            cycle++
            val remaining = FAST_INTERVAL_SECONDS * 1000L - (System.currentTimeMillis() - cycleStart)
            if (remaining > 0) delay(remaining)
        }
    }

    companion object {
        private const val TAG = "PidPoller"
        const val FAST_INTERVAL_SECONDS = 1
        const val SLOW_INTERVAL_SECONDS = 30
        const val FAST_PID_LIMIT = 10
        private const val MAX_FAILURES = 5
    }
}
