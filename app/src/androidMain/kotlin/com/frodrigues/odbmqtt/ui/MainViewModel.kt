package com.frodrigues.odbmqtt.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.frodrigues.odbmqtt.collector.ConnectionStatus
import com.frodrigues.odbmqtt.collector.ServiceStatus
import com.frodrigues.odbmqtt.service.OBDCollectorService
import com.frodrigues.odbmqtt.settings.AppSettings
import com.frodrigues.odbmqtt.settings.SettingsFactory
import kotlinx.coroutines.flow.StateFlow

class AndroidAppViewModel(app: Application) : AndroidViewModel(app), AppViewModel {

    override val settings: AppSettings = AppSettings(SettingsFactory(app).create())

    override val serviceStatus: StateFlow<ServiceStatus> = OBDCollectorService.status
    override val activePidCount: StateFlow<Int> = OBDCollectorService.activePidCount
    override val lastUpdateTime: StateFlow<Long> = OBDCollectorService.lastUpdateTime
    override val btStatus: StateFlow<ConnectionStatus> = OBDCollectorService.btStatus
    override val mqttStatus: StateFlow<ConnectionStatus> = OBDCollectorService.mqttStatus
    override val pidReadings: StateFlow<Map<Int, Double>> = OBDCollectorService.pidReadings

    override fun startService() {
        val intent = Intent(getApplication(), OBDCollectorService::class.java).apply {
            action = OBDCollectorService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
    }

    override fun stopService() {
        val intent = Intent(getApplication(), OBDCollectorService::class.java).apply {
            action = OBDCollectorService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
}
