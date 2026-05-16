package com.frodrigues.odbmqtt.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun create(): ObservableSettings {
        val defaults = NSUserDefaults(suiteName = "com.frodrigues.odbmqtt")
        return NSUserDefaultsSettings(defaults)
    }
}
