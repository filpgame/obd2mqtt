package com.frodrigues.odbmqtt.platform

expect object Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun v(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
