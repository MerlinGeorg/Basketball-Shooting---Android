package com.merlin.basketballanimationgame

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * Listens for basketball scores sent via Bluetooth.
 * Can be used in a separate app to display received scores.
 */
class ScoreReceiver(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val APP_NAME = "BasketballScoreShare"
        private val MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val MSG_SCORE_PREFIX = "SCORE:"
    }

    // Bluetooth components
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    // Connection components
    private var serverSocket: BluetoothServerSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null

    // State management
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _receivedScore = MutableStateFlow<Int?>(null)
    val receivedScore: StateFlow<Int?> = _receivedScore.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    // Permission check helper
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // For older versions, we'll check at runtime
        }
    }

    // Start listening for incoming connections
    fun startListening() {
        if (bluetoothAdapter == null || !hasBluetoothPermissions()) {
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        _isListening.value = true
        _connectionStatus.value = ConnectionStatus.LISTENING

        scope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)

                while (_isListening.value) {
                    try {
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        connectedSocket = serverSocket?.accept(10000) // 10 second timeout

                        if (connectedSocket != null) {
                            // Connected with a device
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                            inputStream = connectedSocket?.inputStream

                            val buffer = ByteArray(1024)
                            var bytes: Int

                            // Keep listening until stopped or disconnected
                            while (_isListening.value && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                                try {
                                    bytes = inputStream?.read(buffer) ?: -1

                                    if (bytes > 0) {
                                        val readMessage = String(buffer, 0, bytes)

                                        if (readMessage.startsWith(MSG_SCORE_PREFIX)) {
                                            val scoreValue = readMessage.substringAfter(MSG_SCORE_PREFIX).toIntOrNull()
                                            if (scoreValue != null) {
                                                _receivedScore.value = scoreValue
                                            }
                                        }
                                    }
                                } catch (e: IOException) {
                                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                                    break
                                }
                            }
                        }
                    } catch (e: IOException) {
                        if (_isListening.value) {
                            // Try to restart the server socket if it was an error
                            _connectionStatus.value = ConnectionStatus.LISTENING
                            continue
                        } else {
                            break
                        }
                    }
                }
            } catch (e: IOException) {
                _connectionStatus.value = ConnectionStatus.ERROR
            } finally {
                close()
            }
        }
    }

    // Stop listening and close all connections
    fun close() {
        _isListening.value = false

        scope.launch(Dispatchers.IO) {
            try {
                serverSocket?.close()
                connectedSocket?.close()
                inputStream?.close()

                serverSocket = null
                connectedSocket = null
                inputStream = null

                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } catch (e: IOException) {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }

    // Connection status states
    enum class ConnectionStatus {
        DISCONNECTED,
        LISTENING,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}