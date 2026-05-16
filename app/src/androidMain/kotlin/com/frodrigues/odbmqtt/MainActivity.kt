package com.frodrigues.odbmqtt

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frodrigues.odbmqtt.bluetooth.BluetoothTransportProvider
import com.frodrigues.odbmqtt.ui.AndroidAppViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* the service handles missing permissions gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        val bluetoothProvider = BluetoothTransportProvider(applicationContext)

        setContent {
            val vm: AndroidAppViewModel = viewModel()
            App(viewModel = vm, bluetoothProvider = bluetoothProvider)
        }
    }
}
