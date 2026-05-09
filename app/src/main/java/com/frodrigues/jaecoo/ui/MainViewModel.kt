package com.frodrigues.jaecoo.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.frodrigues.jaecoo.service.OBDCollectorService
import com.frodrigues.jaecoo.service.ServiceStatus
import com.frodrigues.jaecoo.settings.AppSettings
import com.frodrigues.jaecoo.settings.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = AppSettings(app.dataStore)

    private val _serviceStatus = MutableStateFlow(ServiceStatus.IDLE)
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()

    private val _activePidCount = MutableStateFlow(0)
    val activePidCount: StateFlow<Int> = _activePidCount.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow(0L)
    val lastUpdateTime: StateFlow<Long> = _lastUpdateTime.asStateFlow()

    fun startService() {
        val intent = Intent(getApplication(), OBDCollectorService::class.java).apply {
            action = OBDCollectorService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopService() {
        val intent = Intent(getApplication(), OBDCollectorService::class.java).apply {
            action = OBDCollectorService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
}
