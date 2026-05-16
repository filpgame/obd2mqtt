package com.frodrigues.odbmqtt.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.frodrigues.odbmqtt.MainActivity
import com.frodrigues.odbmqtt.bluetooth.BluetoothTransportProvider
import com.frodrigues.odbmqtt.collector.CollectorEngine
import com.frodrigues.odbmqtt.collector.ConnectionStatus
import com.frodrigues.odbmqtt.collector.ServiceStatus
import com.frodrigues.odbmqtt.settings.AppSettings
import com.frodrigues.odbmqtt.settings.SettingsFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OBDCollectorService : LifecycleService() {

    inner class LocalBinder : Binder() {
        fun getService(): OBDCollectorService = this@OBDCollectorService
    }

    private val binder = LocalBinder()
    private lateinit var engine: CollectorEngine
    private var collectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val settings = AppSettings(SettingsFactory(applicationContext).create())
        val provider = BluetoothTransportProvider(applicationContext)
        engine = CollectorEngine(
            settings = settings,
            transportProvider = provider,
            onStatus = { text -> updateNotification(text) }
        )
        // Mirror engine flows to service-level companion flows for the UI.
        lifecycleScope.launch { engine.status.collect { status.value = it } }
        lifecycleScope.launch { engine.btStatus.collect { btStatus.value = it } }
        lifecycleScope.launch { engine.mqttStatus.collect { mqttStatus.value = it } }
        lifecycleScope.launch { engine.activePidCount.collect { activePidCount.value = it } }
        lifecycleScope.launch { engine.lastUpdateTime.collect { lastUpdateTime.value = it } }
        lifecycleScope.launch { engine.pidReadings.collect { pidReadings.value = it } }
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START, null -> startCollection()
            ACTION_STOP -> stopCollection()
        }
        return START_STICKY
    }

    private fun startCollection() {
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Starting..."),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        collectorJob?.cancel()
        collectorJob = lifecycleScope.launch { engine.runWithRetry() }
    }

    private fun stopCollection() {
        val job = collectorJob ?: return
        collectorJob = null
        job.invokeOnCompletion { engine.reset() }
        job.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OBD2 Collector",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OBD2 Bridge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.frodrigues.odbmqtt.START"
        const val ACTION_STOP = "com.frodrigues.odbmqtt.STOP"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "obd_collector"

        val status = MutableStateFlow(ServiceStatus.IDLE)
        val activePidCount = MutableStateFlow(0)
        val lastUpdateTime = MutableStateFlow(0L)
        val btStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        val mqttStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        val pidReadings = MutableStateFlow<Map<Int, Double>>(emptyMap())
    }
}
