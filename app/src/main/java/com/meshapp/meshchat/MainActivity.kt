package com.meshapp.meshchat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        
        val py = Python.getInstance()
        val configPath = File(filesDir, ".reticulum").absolutePath
        
        val rnsStatus = try {
            py.getModule("rns_manager").callAttr("start_mesh", configPath).toString()
        } catch (e: Exception) { "Error: ${e.message}" }

        setContent {
            var pairedDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
            var hasPermission by remember { mutableStateOf(false) }
            val context = LocalContext.current

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                hasPermission = perms.values.all { it }
                if (hasPermission) {
                    pairedDevices = getPairedDevices()
                }
            }

            LaunchedEffect(Unit) {
                val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                } else {
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
                }
                
                if (required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                    hasPermission = true
                    pairedDevices = getPairedDevices()
                } else {
                    permissionLauncher.launch(required)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("MeshChat v0.4", style = MaterialTheme.typography.headlineMedium)
                Text("RNS Status: $rnsStatus")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select RNode (Paired Devices):", style = MaterialTheme.typography.titleMedium)
                
                if (pairedDevices.isEmpty()) {
                    Text("No paired devices found or permission missing.", style = MaterialTheme.typography.bodySmall)
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(pairedDevices) { device ->
                        BluetoothDeviceItem(device) {
                            val intent = Intent(context, MeshService::class.java)
                            intent.putExtra("device_address", device.address)
                            context.startForegroundService(intent)
                        }
                    }
                }
                
                Button(
                    onClick = { pairedDevices = getPairedDevices() },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Refresh Device List")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun BluetoothDeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
        ) {
            Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.bodyLarge)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
    }

    private fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}