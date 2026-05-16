package com.frodrigues.odbmqtt.mqtt

import com.frodrigues.odbmqtt.platform.Logger
import com.frodrigues.odbmqtt.settings.AppConfig

/**
 * iOS MQTT publisher — STUB.
 *
 * TODO: Replace with a real MQTT client. Options:
 *  - KMQTT (io.github.davidepianca98:kmqtt-client) — multiplatform, has iOS targets
 *  - CocoaMQTT via cinterop — native Swift library, requires bridge
 *  - Write a minimal MQTT v3.1.1 client on top of Ktor Sockets (NSURLSession streams)
 *
 * Until then, this stub logs and no-ops so the app can compile and the UI can be exercised.
 */
actual class MqttPublisher actual constructor(private val config: AppConfig) {

    actual suspend fun connect() {
        Logger.w(TAG, "iOS MQTT not implemented — connect() is a no-op. Host=${config.mqttHost}:${config.mqttPort}")
    }

    actual suspend fun publish(topic: String, payload: String, retain: Boolean) {
        Logger.v(TAG, "publish[stub] $topic = $payload retain=$retain")
    }

    actual suspend fun publishEmpty(topic: String) {
        Logger.v(TAG, "publishEmpty[stub] $topic")
    }

    actual suspend fun disconnect() {
        Logger.v(TAG, "disconnect[stub]")
    }

    companion object {
        private const val TAG = "MqttPublisher.ios"
    }
}

actual suspend fun testMqttConnection(config: AppConfig): Result<Unit> =
    Result.failure(NotImplementedError("iOS MQTT client not implemented yet — see MqttPublisher.ios.kt"))
