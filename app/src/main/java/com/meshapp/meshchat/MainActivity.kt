package com.meshapp.meshchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Start Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        
        val py = Python.getInstance()
        
        // 2. Call our python script 'main.py' and its function 'get_info'
        val meshInfo = try {
            val module = py.getModule("main")
            module.callAttr("get_info").toString()
        } catch (e: Exception) {
            "Error loading Mesh Engine: ${e.message}"
        }

        setContent {
            Column {
                Text("Native Android UI Loaded")
                Text(meshInfo)
            }
        }
    }
}