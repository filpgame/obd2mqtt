package com.frodrigues.odbmqtt.platform

import platform.Foundation.NSLog

actual object Logger {
    actual fun d(tag: String, message: String) { NSLog("D/$tag: $message") }
    actual fun i(tag: String, message: String) { NSLog("I/$tag: $message") }
    actual fun v(tag: String, message: String) { NSLog("V/$tag: $message") }
    actual fun w(tag: String, message: String, throwable: Throwable?) {
        NSLog("W/$tag: $message ${throwable?.message ?: ""}")
    }
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        NSLog("E/$tag: $message ${throwable?.message ?: ""}")
    }
}
