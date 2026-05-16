package com.frodrigues.odbmqtt.ui

import com.frodrigues.odbmqtt.bluetooth.BluetoothTransportProvider
import com.frodrigues.odbmqtt.collector.CollectorEngine
import com.frodrigues.odbmqtt.collector.ConnectionStatus
import com.frodrigues.odbmqtt.collector.ServiceStatus
import com.frodrigues.odbmqtt.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * iOS active-use ViewModel. Holds a CollectorEngine and starts/stops it from the UI;
 * there is no background service equivalent, which matches the user's "active use only"
 * requirement.
 */
class IosAppViewModel(
    override val settings: AppSettings,
    transportProvider: BluetoothTransportProvider
) : AppViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val engine = CollectorEngine(
        settings = settings,
        transportProvider = transportProvider,
        onStatus = { /* iOS surfaces status via state flows, not notifications */ }
    )
    private var job: Job? = null

    override val serviceStatus: StateFlow<ServiceStatus> = engine.status
    override val activePidCount: StateFlow<Int> = engine.activePidCount
    override val lastUpdateTime: StateFlow<Long> = engine.lastUpdateTime
    override val btStatus: StateFlow<ConnectionStatus> = engine.btStatus
    override val mqttStatus: StateFlow<ConnectionStatus> = engine.mqttStatus
    override val pidReadings: StateFlow<Map<Int, Double>> = engine.pidReadings

    override fun startService() {
        if (job?.isActive == true) return
        job = scope.launch { engine.runWithRetry() }
    }

    override fun stopService() {
        job?.cancel()
        job = null
        engine.reset()
    }
}
