# iosApp — Xcode project shell

This folder is the Xcode side of the iOS target. The `iosApp.xcodeproj/project.pbxproj`
is intentionally **not committed** — generating it by hand is brittle and Xcode rewrites
the file on every change, producing massive diffs. Instead, create it once locally:

## One-time setup

1. Open Xcode → **File → New → Project** → iOS → **App**.
2. Set:
   - Product name: `iosApp`
   - Organization Identifier: `com.frodrigues`
   - Bundle Identifier: `com.frodrigues.odbmqtt`
   - Interface: **SwiftUI**, Language: **Swift**
   - Save it **inside this `iosApp/` directory** (so the project file lives at
     `iosApp/iosApp.xcodeproj`). Replace the auto-generated `iOSApp.swift`,
     `ContentView.swift`, and `Info.plist` with the ones already in this folder.

2. Add the KMP framework to the iosApp target:
   - In the project's Build Phases → **+ → New Run Script Phase**, paste:
     ```sh
     cd "$SRCROOT/.."
     ./gradlew :app:embedAndSignAppleFrameworkForXcode
     ```
     Move this script **before** the "Compile Sources" phase.
   - In **Build Settings → Framework Search Paths**, add:
     `$(SRCROOT)/../app/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
   - In **Build Settings → Other Linker Flags**, add: `-framework ComposeApp`
   - In **General → Frameworks, Libraries, and Embedded Content**, add the
     `ComposeApp.framework` (mark as **Embed & Sign**).

3. Build & run on a real iPhone (BLE doesn't work in the iOS Simulator).

## What still needs implementation

- **BluetoothTransportProvider.ios.kt** — currently a stub. See its docs for the
  CoreBluetooth wiring required (CBCentralManager / CBPeripheral / chars 0xFFF1/0xFFF2).
- **MqttPublisher.ios.kt** — currently a stub. Either add a KMP MQTT client (KMQTT) or
  use CocoaMQTT via cinterop.

Once both are implemented and an OBD2 BLE adapter is paired with the iPhone, the app
should function during active use (foreground only — no background polling on iOS,
per the project's chosen scope).
