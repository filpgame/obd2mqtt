package com.frodrigues.obdmqtt.obd

/**
 * Known Mode 22 (manufacturer-specific) PID definitions.
 *
 * These are discovered by brute-force scan + manual analysis of raw bytes.
 * Add entries here as PIDs are identified. Key = 2-byte PID as Int.
 *
 * Formula receives the data bytes AFTER the 62 XXYY header.
 */
object Mode22Registry {

    val definitions: Map<Int, PidDefinition> = mapOf(
        // ── Entries will be populated after brute-force scan + reverse-engineering ──
        // Example format:
        // 0x1234 to PidDefinition(0x1234, "Name", "unit", "haDeviceClass") { b -> b.u(0) - 40.0 }
    )

    fun getOrUnknown(pid: Int): PidDefinition = definitions[pid] ?: PidDefinition(
        pid = pid,
        name = "M22 0x${pid.toString(16).padStart(4, '0').uppercase()}",
        unit = "",
        haDeviceClass = null,
        formula = { b ->
            when {
                b.size >= 2 -> (b.u(0) * 256 + b.u(1)).toDouble()
                b.isNotEmpty() -> b.u(0).toDouble()
                else -> 0.0
            }
        }
    )
}
