package com.frodrigues.odbmqtt

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.frodrigues.odbmqtt.bluetooth.BluetoothTransportProvider
import com.frodrigues.odbmqtt.settings.AppSettings
import com.frodrigues.odbmqtt.settings.SettingsFactory
import com.frodrigues.odbmqtt.ui.IosAppViewModel

/**
 * Entry point consumed by the Xcode `iosApp` target.
 *
 * Swift usage:
 *   import ComposeApp
 *   struct ContentView: UIViewControllerRepresentable {
 *       func makeUIViewController(context: Context) -> UIViewController {
 *           MainViewControllerKt.MainViewController()
 *       }
 *       func updateUIViewController(_ vc: UIViewController, context: Context) {}
 *   }
 */
fun MainViewController() = ComposeUIViewController {
    val bluetoothProvider = remember { BluetoothTransportProvider() }
    val viewModel = remember {
        IosAppViewModel(
            settings = AppSettings(SettingsFactory().create()),
            transportProvider = bluetoothProvider
        )
    }
    App(viewModel = viewModel, bluetoothProvider = bluetoothProvider)
}
