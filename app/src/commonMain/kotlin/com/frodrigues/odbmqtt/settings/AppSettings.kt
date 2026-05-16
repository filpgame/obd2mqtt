package com.frodrigues.odbmqtt.settings

import com.frodrigues.odbmqtt.platform.toHexUpper
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppConfig(
    val btDeviceMac: String,
    val mqttHost: String,
    val mqttPort: Int,
    val mqttUser: String,
    val mqttPassword: String,
    val deviceName: String,
    val deviceModel: String,
    val deviceManufacturer: String
) {
    val mqttClientId: String get() = "obd2_${btDeviceMac.replace(":", "")}"
}

@OptIn(ExperimentalSettingsApi::class)
class AppSettings(private val settings: ObservableSettings) {

    companion object {
        const val BT_DEVICE_MAC = "btDeviceMac"
        const val MQTT_HOST = "mqttHost"
        const val MQTT_PORT = "mqttPort"
        const val MQTT_USER = "mqttUser"
        const val MQTT_PASSWORD = "mqttPassword"
        const val DEVICE_NAME = "deviceName"
        const val DEVICE_MODEL = "deviceModel"
        const val DEVICE_MANUFACTURER = "deviceManufacturer"
        const val CACHED_PIDS = "cachedPids"
        const val SELECTED_PIDS = "selectedPids"
        const val CACHED_MODE22_PIDS = "cachedMode22Pids"
    }

    val btDeviceMac: Flow<String> = settings.getStringFlow(BT_DEVICE_MAC, "")
    val mqttHost: Flow<String> = settings.getStringFlow(MQTT_HOST, "")
    val mqttPort: Flow<Int> = settings.getIntFlow(MQTT_PORT, 1883)
    val mqttUser: Flow<String> = settings.getStringFlow(MQTT_USER, "")
    val mqttPassword: Flow<String> = settings.getStringFlow(MQTT_PASSWORD, "")
    val deviceName: Flow<String> = settings.getStringFlow(DEVICE_NAME, "Jaecoo 7")
    val deviceModel: Flow<String> = settings.getStringFlow(DEVICE_MODEL, "Jaecoo 7")
    val deviceManufacturer: Flow<String> = settings.getStringFlow(DEVICE_MANUFACTURER, "Jaecoo")

    suspend fun snapshot(): AppConfig = AppConfig(
        btDeviceMac = btDeviceMac.first(),
        mqttHost = mqttHost.first(),
        mqttPort = mqttPort.first(),
        mqttUser = mqttUser.first(),
        mqttPassword = mqttPassword.first(),
        deviceName = deviceName.first(),
        deviceModel = deviceModel.first(),
        deviceManufacturer = deviceManufacturer.first()
    )

    fun setString(key: String, value: String) { settings.putString(key, value) }
    fun setInt(key: String, value: Int) { settings.putInt(key, value) }

    // ── Mode 01 PID cache ─────────────────────────────────────────────────────
    val cachedPids: Flow<Set<Int>> = settings.getStringFlow(CACHED_PIDS, "").map { decodePidSet(it) }

    suspend fun savePidCache(pids: Set<Int>) {
        settings.putString(CACHED_PIDS, pids.joinToString(",") { it.toHexUpper() })
    }

    suspend fun clearPidCache() {
        settings.remove(CACHED_PIDS)
    }

    // ── Selected PIDs ─────────────────────────────────────────────────────────
    val selectedPids: Flow<Set<Int>?> = settings.getStringFlow(SELECTED_PIDS, "__null__").map { raw ->
        when {
            raw == "__null__" -> null
            raw.isBlank() -> null
            else -> decodePidSet(raw)
        }
    }

    suspend fun setSelectedPids(pids: Set<Int>?) {
        if (pids == null) settings.remove(SELECTED_PIDS)
        else settings.putString(SELECTED_PIDS, pids.joinToString(",") { it.toHexUpper() })
    }

    // ── Mode 22 cache ─────────────────────────────────────────────────────────
    val cachedMode22Pids: Flow<Map<Int, ByteArray>> =
        settings.getStringFlow(CACHED_MODE22_PIDS, "").map { decodeMode22Map(it) }

    suspend fun saveMode22Cache(pids: Map<Int, ByteArray>) {
        settings.putString(CACHED_MODE22_PIDS, encodeMode22Map(pids))
    }

    suspend fun clearMode22Cache() {
        settings.remove(CACHED_MODE22_PIDS)
    }

    private fun decodePidSet(raw: String): Set<Int> =
        if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull(16) }.toSet()

    private fun encodeMode22Map(pids: Map<Int, ByteArray>): String =
        pids.entries.joinToString(",") { (pid, bytes) ->
            "${pid.toHexUpper(4)}:${bytes.joinToString("") { b -> b.toHexUpperByte() }}"
        }

    private fun decodeMode22Map(raw: String): Map<Int, ByteArray> =
        if (raw.isBlank()) emptyMap()
        else raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val pid = parts[0].toIntOrNull(16) ?: return@mapNotNull null
            val bytes = parts[1].chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
            pid to bytes
        }.toMap()

    private fun Byte.toHexUpperByte(): String {
        val v = this.toInt() and 0xFF
        val chars = "0123456789ABCDEF"
        return "${chars[(v shr 4) and 0x0F]}${chars[v and 0x0F]}"
    }
}
