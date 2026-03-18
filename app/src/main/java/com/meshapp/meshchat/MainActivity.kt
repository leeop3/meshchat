package com.meshapp.meshchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        
        // Start RNS
        val rnsStatus = try {
            py.getModule("rns_manager").callAttr("start_mesh", configPath).toString()
        } catch (e: Exception) { "Error: ${e.message}" }

        setContent {
            var macAddress by remember { mutableStateOf("") }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MeshChat v0.3", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("RNS Status: $rnsStatus")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = macAddress,
                    onValueChange = { macAddress = it },
                    label = { Text("RNode MAC Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        val intent = Intent(this@MainActivity, MeshService::class.java)
                        intent.putExtra("device_address", macAddress)
                        startForegroundService(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Connect RNode")
                }
            }
        }
    }
}