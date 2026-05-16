package com.frodrigues.odbmqtt.ui

import com.frodrigues.odbmqtt.collector.ConnectionStatus
import com.frodrigues.odbmqtt.collector.ServiceStatus
import com.frodrigues.odbmqtt.settings.AppSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Common contract for the screen's view-model surface.
 *
 *  - Android: backed by OBDCollectorService (foreground service); StateFlows mirror the
 *    service's flows.
 *  - iOS: backed by a CollectorEngine owned by the view controller (active-use only).
 */
interface AppViewModel {
    val settings: AppSettings
    val serviceStatus: StateFlow<ServiceStatus>
    val activePidCount: StateFlow<Int>
    val lastUpdateTime: StateFlow<Long>
    val btStatus: StateFlow<ConnectionStatus>
    val mqttStatus: StateFlow<ConnectionStatus>
    val pidReadings: StateFlow<Map<Int, Double>>

    fun startService()
    fun stopService()
}
