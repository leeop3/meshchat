package com.meshapp.meshchat

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

class MeshService : Service() {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var btSocket: BluetoothSocket? = null
    private var tcpServer: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceAddress = intent?.getStringExtra("device_address")
        startForegroundService()
        
        if (deviceAddress != null) {
            connectToRNode(deviceAddress)
        }
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "mesh_channel"
        val channel = NotificationChannel(channelId, "Mesh Network", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MeshChat Active")
            .setContentText("Connected to RNode Mesh")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
            
        startForeground(1, notification)
    }

    private fun connectToRNode(address: String) {
        thread {
            try {
                val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                btSocket?.connect()
                
                startTcpBridge()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTcpBridge() {
        tcpServer = ServerSocket(50001)
        thread {
            while (true) {
                val client = tcpServer?.accept() ?: break
                handleBridge(client)
            }
        }
    }

    private fun handleBridge(tcpSocket: Socket) {
        thread {
            val btIn = btSocket?.inputStream
            val tcpOut = tcpSocket.getOutputStream()
            val buffer = ByteArray(1024)
            try {
                while (true) {
                    val bytes = btIn?.read(buffer) ?: -1
                    if (bytes > 0) tcpOut.write(buffer, 0, bytes)
                }
            } catch (e: Exception) {}
        }
        
        thread {
            val tcpIn = tcpSocket.getInputStream()
            val btOut = btSocket?.outputStream
            val buffer = ByteArray(1024)
            try {
                while (true) {
                    val bytes = tcpIn.read(buffer) ?: -1
                    if (bytes > 0) btOut?.write(buffer, 0, bytes)
                }
            } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}