package com.meshapp.meshchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
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
        
        val status = try {
            val module = py.getModule("rns_manager")
            module.callAttr("start_mesh", configPath).toString()
        } catch (e: Exception) {
            "RNS Error: ${e.message}"
        }

        setContent {
            Column {
                Text("MeshChat v0.2")
                Text(status)
                Text("Waiting for Bluetooth Bridge...")
            }
        }
    }
}