package com.frodrigues.odbmqtt.mqtt

import com.frodrigues.odbmqtt.settings.AppConfig

expect class MqttPublisher(config: AppConfig) {
    suspend fun connect()
    suspend fun publish(topic: String, payload: String, retain: Boolean = false)
    suspend fun publishEmpty(topic: String)
    suspend fun disconnect()
}

expect suspend fun testMqttConnection(config: AppConfig): Result<Unit>
