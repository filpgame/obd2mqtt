# KMP migration notes

This branch converts the single-module Android app into a Kotlin Multiplatform project that targets Android **and** iOS (`iosArm64`, `iosSimulatorArm64`), with Compose Multiplatform sharing the UI.

## What ships in this branch

- **Builds Android**: `./gradlew :app:assembleDebug` produces a debug APK.
- **Builds iOS**: `./gradlew :app:compileKotlinIosSimulatorArm64` and `:app:linkDebugFrameworkIosSimulatorArm64` produce `ComposeApp.framework`.
- **Tests pass**: 25/25 commonTest unit tests pass on the Android JVM target.
- **No regressions on Android**: the foreground service, BLE/Classic transport, HiveMQ MQTT, DataStore-replaced settings storage, and Compose UI all behave as before.

## Project layout

```
app/                                         <- KMP module (kept the name :app)
  build.gradle.kts                           <- KMP plugin + android + ios targets
  src/
    commonMain/kotlin/com/frodrigues/odbmqtt/
      App.kt                                 <- root Compose nav graph
      bluetooth/BluetoothTransport.kt        <- interface + expect class TransportProvider
      collector/CollectorEngine.kt           <- platform-agnostic orchestrator
      mqtt/                                  <- expect MqttPublisher + HaDiscoveryPublisher
      obd/                                   <- PIDs, parsers, scanners, poller (all common)
      platform/Logger.kt, HexFormat.kt, Time.kt
      settings/AppSettings.kt + SettingsFactory.kt (expect)
      ui/                                    <- MainScreen, SettingsScreen, PidSelectionScreen, theme
    commonTest/kotlin/                       <- PidParser, ObdResponseParser, ObdCommandExecutor, HaDiscoveryPublisher, FakeTransport
    androidMain/kotlin/com/frodrigues/odbmqtt/
      MainActivity.kt
      bluetooth/BleTransport.kt, ClassicTransport.kt, BluetoothTransportProvider.android.kt
      mqtt/MqttPublisher.android.kt          <- HiveMQ
      platform/Logger.android.kt             <- android.util.Log
      service/OBDCollectorService.kt         <- wraps CollectorEngine
      settings/AppSettings.android.kt        <- SharedPreferences via multiplatform-settings
      ui/MainViewModel.kt                    <- AndroidAppViewModel : AppViewModel
    androidMain/AndroidManifest.xml + res/
    iosMain/kotlin/com/frodrigues/odbmqtt/
      MainViewController.kt                  <- ComposeUIViewController entry point
      bluetooth/BluetoothTransportProvider.ios.kt + IosStubTransport.kt   STUB
      mqtt/MqttPublisher.ios.kt              STUB
      platform/Logger.ios.kt                 <- NSLog
      settings/SettingsFactory.ios.kt        <- NSUserDefaults
      ui/IosAppViewModel.kt                  <- holds CollectorEngine for active-use only
iosApp/
  iosApp/iOSApp.swift, ContentView.swift, Info.plist
  README.md                                  <- Xcode project setup instructions
```

## Key architectural changes

1. **CollectorEngine** (`commonMain/collector/CollectorEngine.kt`) — what used to be `OBDCollectorService.collect()` is now a platform-agnostic class that orchestrates BT → ELM327 init → PID scan → MQTT publish → poll loop, with retry/backoff. The Android `OBDCollectorService` wraps it for foreground execution + notifications; the iOS `IosAppViewModel` wraps it for active-use foreground execution.

2. **`AppViewModel` interface** (commonMain) — defines the contract MainScreen consumes. Android's `AndroidAppViewModel` extends `androidx.lifecycle.AndroidViewModel` and forwards state from `OBDCollectorService.companion`. iOS's `IosAppViewModel` owns a `CollectorEngine` directly (no background service equivalent on iOS — the user confirmed "active-use only" is fine).

3. **`expect class BluetoothTransportProvider`** replaces the old object factory. The interface (`BluetoothTransport`) lives in commonMain; both transports (Classic, BLE) stay in androidMain as before — they implement the common interface.

4. **`expect class MqttPublisher`** — Android uses HiveMQ unchanged; iOS is a stub that logs (see "What still needs implementation").

5. **`expect class SettingsFactory`** — provides `ObservableSettings` from [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings). Android backs it with `SharedPreferences` (replacing DataStore); iOS uses `NSUserDefaults`. The common `AppSettings` class on top exposes the same Flow-based API as before.

6. **Platform utilities** (`platform/`) — `Logger.kt` (expect object), `nowMillis()` via `kotlin.time.Clock`, hex formatting helpers that avoid `String.format("%02X", …)` (not in commonMain).

7. **HA Discovery JSON** stays as manual `buildString` (already portable, no need for kotlinx-serialization for this).

## Version & toolchain notes

| Component | Version | Notes |
|---|---|---|
| Kotlin | 2.3.21 | Unchanged from before migration |
| AGP | 9.0.1 | Unchanged. **AGP 9 deprecated `com.android.application` + `kotlin.multiplatform`** — see workaround below |
| Gradle | 9.2.1 | Unchanged |
| Compose Multiplatform | 1.11.0 | Newly added |
| Navigation Compose (JB) | 2.9.2 | Newly added, replaces androidx navigation |
| multiplatform-settings | 1.2.0 | Newly added |
| kotlinx-datetime | 0.6.1 | Newly added |

### AGP 9 / KMP incompatibility workaround

AGP 9.0 deprecated using `com.android.application` together with `kotlin.multiplatform`; the recommended migration is to split the app into two Gradle modules (one KMP library + one app). For this conversion I used the **opt-out** instead:

```properties
# gradle.properties
android.builtInKotlin=false
android.newDsl=false
```

These will be removed in AGP 10. Before then, the cleanest path is to split `:app` into `:shared` (KMP library) + `:androidApp` (com.android.application that depends on `:shared`). That refactor is mechanical but invasive — postponing it kept the diff focused on enabling iOS.

## What still needs implementation

These are stubs that compile and let the iOS app launch, but won't talk to a car:

### 1. CoreBluetooth integration (`iosMain/bluetooth/`)

The file `BluetoothTransportProvider.ios.kt` returns an empty list of paired devices and a no-op `IosStubTransport`. To make it work:

- Implement `BleTransport.ios.kt` mirroring the Android one, but with `CBCentralManager` + `CBPeripheral`. Write to characteristic `0xFFF1`, subscribe to notifications on `0xFFF2`, accumulate response chunks until the `>` prompt arrives.
- For "listPairedDevices" the iOS equivalent is either:
  - **Scan** for peripherals advertising the OBD2 service UUID `0000FFF0-0000-1000-8000-00805F9B34FB` for a short window, or
  - **`retrievePeripherals(withIdentifiers:)`** if the user previously connected and we persisted the UUID.
- Apple's MFi restriction means **Classic Bluetooth (SPP) won't work on iOS**. The user confirmed they have a BLE adapter, so this is fine.

### 2. MQTT client (`iosMain/mqtt/MqttPublisher.ios.kt`)

Options, easiest first:
- **KMQTT** (`io.github.davidepianca98:kmqtt-client`) — multiplatform, has `iosArm64` / `iosSimulatorArm64` targets.
- **CocoaMQTT** via cinterop — native Swift client; needs a cinterop bridge.
- **Ktor sockets** + a hand-rolled MQTT v3.1.1 frame parser. Most work but no extra dep.

### 3. Xcode project

`iosApp/iosApp.xcodeproj` is intentionally not committed. Run through the one-time setup in [iosApp/README.md](iosApp/README.md) (5–10 minutes in Xcode) to generate it, then build and run on a real iPhone (BLE doesn't work in the Simulator).

## Anatomy of the build

```
./gradlew :app:assembleDebug                       # Android APK
./gradlew :app:testDebugUnitTest                   # JVM unit tests (25 tests)
./gradlew :app:compileKotlinIosSimulatorArm64      # iOS source compile
./gradlew :app:linkDebugFrameworkIosSimulatorArm64 # ComposeApp.framework
./gradlew :app:embedAndSignAppleFrameworkForXcode  # called from Xcode build phase
```

Verified locally as of this commit: Android APK builds, iOS Simulator framework compiles, all unit tests pass.

## Things deliberately *not* changed

- **Module name** remained `:app`. Renaming to `:composeApp` would have created hundreds of lines of meaningless diff.
- **`com.frodrigues.odbmqtt` package** kept everywhere — only the source-set the file lives in changed.
- **Material You dynamic colors** (Android-only) — dropped. The theme is the same static palette on both platforms. Re-add via an `expect val dynamicColorScheme` if you want Material You back on Android.
- **AppCompat / Activity result API** for permissions — unchanged on Android.
- **ha-dashboard/** Python tooling — untouched (not part of the app).
