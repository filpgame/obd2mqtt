package com.frodrigues.odbmqtt

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.frodrigues.odbmqtt.bluetooth.BluetoothTransportProvider
import com.frodrigues.odbmqtt.ui.AppViewModel
import com.frodrigues.odbmqtt.ui.MainScreen
import com.frodrigues.odbmqtt.ui.PidSelectionScreen
import com.frodrigues.odbmqtt.ui.SettingsScreen
import com.frodrigues.odbmqtt.ui.theme.OdbMqttTheme

@Composable
fun App(viewModel: AppViewModel, bluetoothProvider: BluetoothTransportProvider) {
    OdbMqttTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings = viewModel.settings,
                    bluetoothProvider = bluetoothProvider,
                    onBack = { navController.popBackStack() },
                    onNavigateToPidSelection = { navController.navigate("pid_selection") }
                )
            }
            composable("pid_selection") {
                PidSelectionScreen(
                    settings = viewModel.settings,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
