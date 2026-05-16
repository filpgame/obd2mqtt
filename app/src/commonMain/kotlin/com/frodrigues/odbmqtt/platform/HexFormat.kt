package com.frodrigues.odbmqtt.platform

private const val HEX_CHARS = "0123456789ABCDEF"

fun Int.toHexUpper(width: Int = 0): String {
    val raw = this.toString(16).uppercase()
    return if (raw.length >= width) raw else "0".repeat(width - raw.length) + raw
}

fun Byte.toHexUpper(): String {
    val v = this.toInt() and 0xFF
    return "${HEX_CHARS[(v shr 4) and 0x0F]}${HEX_CHARS[v and 0x0F]}"
}

fun ByteArray.toHexUpper(separator: String = ""): String =
    joinToString(separator) { it.toHexUpper() }

/** Format a double similar to JVM "%.2f". Avoids depending on platform-specific String.format. */
fun Double.formatTwoDecimals(): String {
    val rounded = (this * 100.0).let { if (it >= 0) (it + 0.5).toLong() else (it - 0.5).toLong() }
    val whole = rounded / 100
    val frac = (if (rounded < 0) -rounded else rounded) % 100
    val sign = if (rounded < 0 && whole == 0L) "-" else ""
    val fracStr = if (frac < 10) "0$frac" else "$frac"
    return "$sign$whole.$fracStr"
}
