package com.frodrigues.odbmqtt.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.frodrigues.odbmqtt.settings.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val btMac by settings.btDeviceMac.collectAsState(initial = "")
    val mqttHost by settings.mqttHost.collectAsState(initial = "")
    val mqttPort by settings.mqttPort.collectAsState(initial = 1883)
    val mqttUser by settings.mqttUser.collectAsState(initial = "")
    val mqttPassword by settings.mqttPassword.collectAsState(initial = "")
    val pollInterval by settings.pollIntervalSeconds.collectAsState(initial = 5)
    val deviceName by settings.deviceName.collectAsState(initial = "Jaecoo 7")
    val deviceModel by settings.deviceModel.collectAsState(initial = "Jaecoo 7")
    val deviceManufacturer by settings.deviceManufacturer.collectAsState(initial = "Jaecoo")

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var pairedDevices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                pairedDevices = try {
                    context.getSystemService(BluetoothManager::class.java)
                        ?.adapter?.bondedDevices?.toList() ?: emptyList()
                } catch (_: SecurityException) {
                    emptyList()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Bluetooth Device ──────────────────────────────────────────────
            SectionHeader(text = "Bluetooth Device")

            if (pairedDevices.isEmpty()) {
                Text(
                    text = "No paired devices found. Pair your ELM327 adapter in Android Settings first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            pairedDevices.forEach { device ->
                val name = runCatching { device.name }.getOrDefault(device.address)
                val selected = device.address == btMac

                if (selected) {
                    FilledTonalButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✓  $name")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                settings.update { this[AppSettings.BT_DEVICE_MAC] = device.address }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(name)
                    }
                }
            }

            // ── MQTT Broker ───────────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader(text = "MQTT Broker")

            OutlinedTextField(
                value = mqttHost,
                onValueChange = { v ->
                    scope.launch { settings.update { this[AppSettings.MQTT_HOST] = v } }
                },
                label = { Text("Host") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mqttPort.toString(),
                onValueChange = { v ->
                    v.toIntOrNull()?.let { port ->
                        scope.launch { settings.update { this[AppSettings.MQTT_PORT] = port } }
                    }
                },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mqttUser,
                onValueChange = { v ->
                    scope.launch { settings.update { this[AppSettings.MQTT_USER] = v } }
                },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mqttPassword,
                onValueChange = { v ->
                    scope.launch { settings.update { this[AppSettings.MQTT_PASSWORD] = v } }
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Polling ───────────────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader(text = "Polling")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Interval",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${pollInterval}s",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = pollInterval.toFloat(),
                onValueChange = { v ->
                    scope.launch {
                        settings.update { this[AppSettings.POLL_INTERVAL_SECONDS] = v.toInt() }
                    }
                },
                valueRange = 1f..60f,
                steps = 58,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Device Info (HA) ──────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader(text = "Device Info (Home Assistant)")

            OutlinedTextField(
                value = deviceName,
                onValueChange = { v ->
                    scope.launch { settings.update { this[AppSettings.DEVICE_NAME] = v } }
                },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = deviceModel,
                onValueChange = { v ->
                    scope.launch { settings.update { this[AppSettings.DEVICE_MODEL] = v } }
                },
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = deviceManufacturer,
                onValueChange = { v ->
                    scope.launch { settings.update { this[AppSettings.DEVICE_MANUFACTURER] = v } }
                },
                label = { Text("Manufacturer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
