package com.frodrigues.odbmqtt.obd

typealias PidFormula = (ByteArray) -> Double

data class PidDefinition(
    val pid: Int,
    val name: String,
    val unit: String,
    val haDeviceClass: String?,
    val isFast: Boolean = false,
    val formula: PidFormula
)

fun ByteArray.u(i: Int): Int = this[i].toInt() and 0xFF
