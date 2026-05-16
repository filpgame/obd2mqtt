package com.frodrigues.odbmqtt.settings

import com.russhwolf.settings.ObservableSettings

expect class SettingsFactory {
    fun create(): ObservableSettings
}
