package com.frodrigues.odbmqtt.bluetooth

import com.frodrigues.odbmqtt.platform.Logger

/**
 * iOS Bluetooth transport — STUB.
 *
 * TODO: Implement with CoreBluetooth:
 *  - listPairedDevices: iOS has no concept of "paired BLE" — instead either scan for
 *    peripherals advertising the OBD service UUID (0xFFF0) for a short window, or call
 *    CBCentralManager.retrievePeripherals(withIdentifiers:) once the user has paired one.
 *  - createTransport: return a BleTransport.ios.kt that owns a CBCentralManager and a
 *    CBPeripheral, mirroring the Android BleTransport. Write to char 0xFFF1, subscribe
 *    to notifications on 0xFFF2, accumulate response chunks until the ">" prompt arrives.
 *  - Add Info.plist NSBluetoothAlwaysUsageDescription string (done in iosApp/Info.plist).
 *  - Apple's MFi program restricts Classic Bluetooth (SPP) to certified devices, so this
 *    will only support BLE OBD2 adapters (Vgate iCar Pro BLE, OBDLink MX+ BLE, etc.) —
 *    which is fine because the user confirmed they have a BLE adapter.
 */
actual class BluetoothTransportProvider {

    actual suspend fun listPairedDevices(): List<BluetoothDeviceInfo> {
        Logger.w(TAG, "listPairedDevices[stub] — CoreBluetooth scan not implemented")
        return emptyList()
    }

    actual fun createTransport(address: String): BluetoothTransport {
        Logger.w(TAG, "createTransport[stub] for $address — returning no-op transport")
        return IosStubTransport()
    }

    companion object {
        private const val TAG = "BluetoothProvider.ios"
    }
}
