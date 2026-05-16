# OBD Scanner MQTT — KMP edition

Kotlin Multiplatform app (Android + iOS) that reads OBD2 data from a vehicle via an ELM327 Bluetooth adapter and publishes sensor readings to Home Assistant using MQTT Discovery. UI built with **Compose Multiplatform**.

> **iOS status:** the project structure, common code, and Compose UI are KMP-ready and compile for `iosArm64`/`iosSimulatorArm64`. CoreBluetooth and the iOS MQTT client are stubbed — see [MIGRATION.md](MIGRATION.md) for the remaining work.

## Features

- Connects to ELM327 adapters via **Classic Bluetooth (SPP)** or **BLE**
- Automatically scans all supported OBD2 PIDs (Mode 01)
- Polls 39 mapped PIDs including RPM, speed, temperatures, fuel level, torque, and more
- Publishes to **Home Assistant** via MQTT with auto-discovery — sensors appear automatically, no manual configuration needed
- Runs as a persistent **ForegroundService** with exponential backoff reconnection (5s → 10s → 30s → 60s)
- Configurable poll interval (default: 5 seconds)
- Marks sensors `unavailable` in HA when the car disconnects

## Requirements

- Android 13+ (minSdk 33)
- ELM327 OBD2 Bluetooth adapter (Classic or BLE)
- Vehicle using ISO 15765-4 CAN protocol (11-bit ID, 500 kbaud) — confirmed working on Jaecoo 7 / OMODA platform
- MQTT broker (e.g. Mosquitto running alongside Home Assistant)

## Setup

1. Pair your ELM327 adapter in Android **Settings → Bluetooth**
2. Open the app → **Settings**
3. Select the paired ELM327 device
4. Enter your MQTT broker IP, port, and credentials (if any)
5. Optionally set device name/model/manufacturer (used in HA entity names)
6. Tap **Start** on the main screen

The app will connect to the adapter, initialize ELM327, scan supported PIDs, and start publishing to MQTT. Home Assistant will auto-create a device with all supported sensors.

## MQTT Topics

| Topic | Description |
|---|---|
| `homeassistant/sensor/obd2_<mac>/<PID>/config` | HA MQTT Discovery config (retained) |
| `obd2/<mac>/<PID>/state` | Sensor value, updated every poll cycle (retained) |

When the app stops or disconnects, all state topics receive an empty payload, marking sensors as `unavailable` in HA.

## Mapped PIDs

| PID | Sensor | Unit |
|---|---|---|
| 0x04 | Engine Load | % |
| 0x05 | Coolant Temp | °C |
| 0x0C | RPM | rpm |
| 0x0D | Speed | km/h |
| 0x0F | Intake Air Temp | °C |
| 0x11 | Throttle Position | % |
| 0x2F | Fuel Level | % |
| 0x33 | Barometric Pressure | kPa |
| 0x42 | Control Module Voltage | V |
| 0x46 | Ambient Air Temp | °C |
| 0x5C | Oil Temp | °C |
| 0x5E | Fuel Rate | L/h |
| 0x62 | Actual Torque | % |
| 0xA6 | Odometer | km |
| ... | [39 PIDs total](app/src/main/java/com/frodrigues/jaecoo/obd/PidRegistry.kt) | |

## Architecture

```
ELM327 ──BT──► BluetoothTransport (Classic/BLE)
                      │
                      ▼
              ObdCommandExecutor (AT init)
                      │
               PidScanner (bitmask scan)
               PidPoller  (round-robin loop)
                      │
                      ▼
             MqttPublisher ──► Home Assistant
                      ▲
              OBDCollectorService (ForegroundService)
```

## Tech Stack

- Kotlin Multiplatform 2.3 + Compose Multiplatform 1.11
- AGP 9.0 (Android target) / Kotlin/Native (iOS targets)
- HiveMQ MQTT client 1.3 (Android) — iOS MQTT pending
- multiplatform-settings 1.2 — SharedPreferences (Android) / NSUserDefaults (iOS)
- kotlinx-datetime, kotlinx-serialization-json, kotlinx-coroutines
- Compose Multiplatform Navigation 2.9

## Building

```bash
# Android
./gradlew :app:assembleDebug                       # debug APK
./gradlew :app:testDebugUnitTest                   # JVM unit tests

# iOS (compiles on macOS only)
./gradlew :app:compileKotlinIosSimulatorArm64
./gradlew :app:linkDebugFrameworkIosSimulatorArm64 # ComposeApp.framework for the simulator
```

To run on iPhone, open `iosApp/iosApp.xcodeproj` after one-time setup (see [iosApp/README.md](iosApp/README.md)).

## License

MIT
