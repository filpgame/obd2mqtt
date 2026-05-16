package com.frodrigues.odbmqtt.collector

import com.frodrigues.odbmqtt.bluetooth.BluetoothTransport
import com.frodrigues.odbmqtt.bluetooth.BluetoothTransportProvider
import com.frodrigues.odbmqtt.mqtt.HaDiscoveryPublisher
import com.frodrigues.odbmqtt.mqtt.MqttPublisher
import com.frodrigues.odbmqtt.obd.Mode22Registry
import com.frodrigues.odbmqtt.obd.Mode22Scanner
import com.frodrigues.odbmqtt.obd.ObdCommandExecutor
import com.frodrigues.odbmqtt.obd.PidPoller
import com.frodrigues.odbmqtt.obd.PidRegistry
import com.frodrigues.odbmqtt.obd.PidScanner
import com.frodrigues.odbmqtt.platform.Logger
import com.frodrigues.odbmqtt.platform.toHexUpper
import com.frodrigues.odbmqtt.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

enum class ServiceStatus { IDLE, CONNECTING, CONNECTED, RECONNECTING }
enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED }

/**
 * Platform-agnostic orchestrator: connect BT → init ELM327 → scan PIDs → connect MQTT →
 * publish HA discovery → poll PIDs and publish state, with retry/backoff.
 *
 * Platform shells (Android foreground service / iOS view model) own the lifecycle and the
 * notification UI; this engine only manages the data pipeline.
 */
class CollectorEngine(
    private val settings: AppSettings,
    private val transportProvider: BluetoothTransportProvider,
    private val onStatus: (String) -> Unit = {}
) {
    private val _status = MutableStateFlow(ServiceStatus.IDLE)
    val status: StateFlow<ServiceStatus> = _status.asStateFlow()

    private val _activePidCount = MutableStateFlow(0)
    val activePidCount: StateFlow<Int> = _activePidCount.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow(0L)
    val lastUpdateTime: StateFlow<Long> = _lastUpdateTime.asStateFlow()

    private val _btStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val btStatus: StateFlow<ConnectionStatus> = _btStatus.asStateFlow()

    private val _mqttStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val mqttStatus: StateFlow<ConnectionStatus> = _mqttStatus.asStateFlow()

    private val _pidReadings = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val pidReadings: StateFlow<Map<Int, Double>> = _pidReadings.asStateFlow()

    private var mqttPublisherRef: MqttPublisher? = null
    private var currentStateTopics: List<String> = emptyList()

    suspend fun runWithRetry() {
        val backoffMs = longArrayOf(5_000L, 10_000L, 30_000L, 60_000L)
        var attempt = 0
        while (true) {
            try {
                runOnce()
                attempt = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, "Collector failed: ${e.message}", e)
                _status.value = ServiceStatus.RECONNECTING
                _btStatus.value = ConnectionStatus.DISCONNECTED
                _mqttStatus.value = ConnectionStatus.DISCONNECTED
                val ms = backoffMs.getOrNull(attempt) ?: 60_000L
                attempt = (attempt + 1).coerceAtMost(backoffMs.size - 1)
                onStatus("Reconnecting in ${ms / 1000}s...")
                delay(ms)
            }
        }
    }

    private suspend fun runOnce() {
        val config = settings.snapshot()
        require(config.btDeviceMac.isNotBlank()) { "No BT device configured" }
        require(config.mqttHost.isNotBlank()) { "No MQTT host configured" }

        _status.value = ServiceStatus.CONNECTING
        _btStatus.value = ConnectionStatus.CONNECTING
        onStatus("Connecting to Bluetooth...")

        val transport = transportProvider.createTransport(config.btDeviceMac)
        transport.connect()

        try {
            onStatus("Initializing ELM327...")
            val executor = ObdCommandExecutor(transport)
            executor.initialize()

            onStatus("Scanning PIDs...")
            val supportedPids = PidScanner(
                executor = executor,
                settings = settings,
                onProgress = { scanned, total -> onStatus("Scanning PIDs ($scanned/$total)...") }
            ).scan()
            _activePidCount.value = supportedPids.size
            _btStatus.value = ConnectionStatus.CONNECTED

            _mqttStatus.value = ConnectionStatus.CONNECTING
            onStatus("Connecting to MQTT...")
            val mqtt = MqttPublisher(config)
            mqtt.connect()
            _mqttStatus.value = ConnectionStatus.CONNECTED
            mqttPublisherRef = mqtt

            val mac = config.btDeviceMac.replace(":", "")
            currentStateTopics = supportedPids.map { pid -> "obd2/$mac/${pid.toHexUpper(2)}/state" }

            HaDiscoveryPublisher(
                publish = { t, p -> mqtt.publish(t, p, retain = true) },
                config = config
            ).publishDiscovery(supportedPids, mac)

            onStatus("Scanning Mode 22 PIDs...")
            val mode22Pids = Mode22Scanner(
                executor = executor,
                settings = settings,
                onProgress = { scanned, total -> onStatus("Scanning Mode 22 ($scanned/$total)...") }
            ).scan()

            mode22Pids.keys.forEach { pid ->
                val def = Mode22Registry.getOrUnknown(pid)
                val pidHex = pid.toHexUpper(4)
                val topic = "homeassistant/sensor/obd2_${mac}/m22_$pidHex/config"
                val stateTopic = "obd2/$mac/m22_$pidHex/state"
                val payload = buildString {
                    append("{")
                    append("\"name\":\"${config.deviceName} ${def.name}\"")
                    append(",\"state_topic\":\"$stateTopic\"")
                    if (def.unit.isNotBlank()) append(",\"unit_of_measurement\":\"${def.unit}\"")
                    append(",\"unique_id\":\"${mac}_m22_$pidHex\"")
                    def.haDeviceClass?.let { append(",\"device_class\":\"$it\"") }
                    append(",\"device\":{\"identifiers\":[\"obd2_$mac\"],\"name\":\"${config.deviceName}\"}")
                    append("}")
                }
                mqtt.publish(topic, payload, retain = true)
            }

            val userSelected: Set<Int>? = settings.selectedPids.first()
            val activePids: Set<Int> =
                if (userSelected == null) supportedPids else supportedPids.intersect(userSelected)
            val fastCount = activePids.count { PidRegistry.getOrUnknown(it).isFast }
            val slowCount = activePids.size - fastCount

            _status.value = ServiceStatus.CONNECTED
            onStatus("Connected — $fastCount fast (${PidPoller.FAST_INTERVAL_SECONDS}s) + $slowCount slow (${PidPoller.SLOW_INTERVAL_SECONDS}s)")

            val currentReadings = mutableMapOf<Int, Double>()

            PidPoller(executor = executor, pids = activePids).readings().collect { reading ->
                currentReadings[reading.pid] = reading.value
                _pidReadings.value = currentReadings.toMap()
                val pidHex = reading.pid.toHexUpper(2)
                val value = formatNumber(reading.value)
                mqtt.publish("obd2/$mac/$pidHex/state", value, retain = true)
                _lastUpdateTime.value = reading.timestamp
            }
        } finally {
            runCatching { publishUnavailable() }
            runCatching { mqttPublisherRef?.disconnect() }
            mqttPublisherRef = null
            runCatching { transport.disconnect() }
            _btStatus.value = ConnectionStatus.DISCONNECTED
            _mqttStatus.value = ConnectionStatus.DISCONNECTED
            _pidReadings.value = emptyMap()
        }
    }

    fun reset() {
        _status.value = ServiceStatus.IDLE
        _activePidCount.value = 0
        _lastUpdateTime.value = 0L
        _btStatus.value = ConnectionStatus.DISCONNECTED
        _mqttStatus.value = ConnectionStatus.DISCONNECTED
        _pidReadings.value = emptyMap()
    }

    private suspend fun publishUnavailable() {
        val mqtt = mqttPublisherRef ?: return
        currentStateTopics.forEach { topic ->
            runCatching { mqtt.publishEmpty(topic) }
        }
    }

    private fun formatNumber(value: Double): String {
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        val rounded = ((value * 1000.0) + if (value >= 0) 0.5 else -0.5).toLong() / 1000.0
        val s = rounded.toString()
        // Trim trailing zeros, then trailing dot
        return s.trimEnd('0').trimEnd('.')
    }

    companion object {
        private const val TAG = "CollectorEngine"
    }
}
