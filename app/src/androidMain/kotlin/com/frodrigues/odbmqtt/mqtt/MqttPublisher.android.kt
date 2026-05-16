package com.frodrigues.odbmqtt.mqtt

import com.frodrigues.odbmqtt.platform.nowMillis
import com.frodrigues.odbmqtt.settings.AppConfig
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

actual class MqttPublisher actual constructor(private val config: AppConfig) {

    @Volatile private var client: Mqtt3AsyncClient? = null

    actual suspend fun connect() = withContext(Dispatchers.IO) {
        val newClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier(config.mqttClientId)
            .serverHost(config.mqttHost)
            .serverPort(config.mqttPort)
            .buildAsync()

        val connectBuilder = newClient.connectWith()
        if (config.mqttUser.isNotBlank()) {
            connectBuilder
                .simpleAuth()
                .username(config.mqttUser)
                .password(config.mqttPassword.toByteArray(Charsets.UTF_8))
                .applySimpleAuth()
        }
        connectBuilder.send().get()
        client = newClient
    }

    actual suspend fun publish(topic: String, payload: String, retain: Boolean) {
        withContext(Dispatchers.IO) {
            val c = client ?: throw IllegalStateException("Not connected")
            c.publishWith()
                .topic(topic)
                .payload(payload.toByteArray(Charsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .retain(retain)
                .send()
                .get()
        }
    }

    actual suspend fun publishEmpty(topic: String) {
        withContext(Dispatchers.IO) {
            val c = client ?: return@withContext
            c.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .retain(true)
                .send()
                .get()
        }
    }

    actual suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { client?.disconnect()?.get() }
            client = null
        }
    }
}

actual suspend fun testMqttConnection(config: AppConfig): Result<Unit> = runCatching {
    withContext(Dispatchers.IO) {
        val testClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier("obd2_test_${nowMillis()}")
            .serverHost(config.mqttHost)
            .serverPort(config.mqttPort)
            .buildAsync()

        val connectBuilder = testClient.connectWith()
        if (config.mqttUser.isNotBlank()) {
            connectBuilder
                .simpleAuth()
                .username(config.mqttUser)
                .password(config.mqttPassword.toByteArray(Charsets.UTF_8))
                .applySimpleAuth()
        }
        connectBuilder.send().get(5, TimeUnit.SECONDS)
        runCatching { testClient.disconnect().get(2, TimeUnit.SECONDS) }
        Unit
    }
}
