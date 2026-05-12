package com.frodrigues.odbmqtt.obd

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class PidReading(
    val pid: Int,
    val value: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class PidPoller(
    private val executor: ObdCommandExecutor,
    private val fastPids: Set<Int>,   // polled every cycle
    private val slowPids: Set<Int>,   // polled every slowCycleInterval cycles
    private val intervalSeconds: Int,
    private val slowCycleInterval: Int = 6  // slow PIDs every 6 fast cycles (~30s at 5s interval)
) {
    fun readings(): Flow<PidReading> = flow {
        var cycle = 0
        while (true) {
            val cycleStart = System.currentTimeMillis()

            for (pid in fastPids) {
                val response = executor.sendCommand("01${pid.toString(16).padStart(2, '0').uppercase()}")
                PidParser.parse(pid, response)?.let { emit(PidReading(pid, it)) }
            }

            if (cycle % slowCycleInterval == 0) {
                for (pid in slowPids) {
                    val response = executor.sendCommand("01${pid.toString(16).padStart(2, '0').uppercase()}")
                    PidParser.parse(pid, response)?.let { emit(PidReading(pid, it)) }
                }
            }

            cycle++
            val remaining = intervalSeconds * 1000L - (System.currentTimeMillis() - cycleStart)
            if (remaining > 0) delay(remaining)
        }
    }

    companion object {
        /**
         * PIDs with high-frequency changes — polled every cycle.
         * Anything not in this set is treated as slow.
         */
        val FAST_PIDS: Set<Int> = setOf(
            0x0C, // RPM
            0x0D, // Speed
            0x04, // Engine Load
            0x0B, // Intake MAP
            0x0E, // Timing Advance
            0x11, // Throttle Position
            0x45, // Relative Throttle
            0x47, // Absolute Throttle B
            0x49, // Accel Pedal D
            0x4C, // Commanded Throttle
            0x2D, // EGR Error
            0x44, // Commanded AFR
            0x34, // O2 Lambda Ratio
            0x15, // O2 Sensor 2 Voltage
            0x42, // Control Module Voltage
        )
    }
}
