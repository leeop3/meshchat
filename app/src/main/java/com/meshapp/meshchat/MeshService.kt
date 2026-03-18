package com.meshapp.meshchat

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

class MeshService : Service() {
    private val TAG = "MeshService"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var btSocket: BluetoothSocket? = null
    private var tcpServer: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceAddress = intent?.getStringExtra("device_address")
        startForegroundService()
        
        if (!deviceAddress.isNullOrBlank()) {
            connectToRNode(deviceAddress.uppercase().trim())
        }
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "mesh_channel"
        val channel = NotificationChannel(channelId, "Mesh Network", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MeshChat Active")
            .setContentText("Linking RNode to Reticulum...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
            
        startForeground(1, notification)
    }

    private fun connectToRNode(address: String) {
        thread {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.getRemoteDevice(address)
            
            try {
                Log.d(TAG, "Attempting standard connection to $address")
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                btSocket?.connect()
                Log.d(TAG, "Standard connection successful")
                startTcpBridge()
            } catch (e: Exception) {
                Log.e(TAG, "Standard connection failed, trying fallback...")
                try {
                    // Fallback: Using reflection to call the hidden createRfcommSocket method
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    btSocket = method.invoke(device, 1) as BluetoothSocket
                    btSocket?.connect()
                    Log.d(TAG, "Fallback connection successful")
                    startTcpBridge()
                } catch (e2: Exception) {
                    Log.e(TAG, "All connection attempts failed: ${e2.message}")
                }
            }
        }
    }

    private fun startTcpBridge() {
        if (tcpServer == null) {
            tcpServer = ServerSocket(50001)
        }
        thread {
            while (true) {
                try {
                    val client = tcpServer?.accept() ?: break
                    Log.d(TAG, "Reticulum (Python) connected to Bridge")
                    handleBridge(client)
                } catch (e: Exception) { break }
            }
        }
    }

    private fun handleBridge(tcpSocket: Socket) {
        thread {
            val btIn = btSocket?.inputStream
            val tcpOut = tcpSocket.getOutputStream()
            val buffer = ByteArray(2048)
            try {
                while (true) {
                    val bytes = btIn?.read(buffer) ?: -1
                    if (bytes > 0) {
                        tcpOut.write(buffer, 0, bytes)
                        tcpOut.flush()
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "BT -> TCP Bridge Error") }
        }
        
        thread {
            val tcpIn = tcpSocket.getInputStream()
            val btOut = btSocket?.outputStream
            val buffer = ByteArray(2048)
            try {
                while (true) {
                    val bytes = tcpIn.read(buffer) ?: -1
                    if (bytes > 0) {
                        btOut?.write(buffer, 0, bytes)
                        btOut?.flush()
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "TCP -> BT Bridge Error") }
        }
    }

    override fun onDestroy() {
        btSocket?.close()
        tcpServer?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}