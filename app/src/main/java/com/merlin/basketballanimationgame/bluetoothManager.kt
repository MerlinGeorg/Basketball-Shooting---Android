package com.merlin.basketballanimationgame


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth communication for the basketball game.
 * Handles device discovery, connection establishment, and data transmission.
 */
class BluetoothManager(private val context: Context) {
    companion object {
        private const val APP_NAME = "BasketballScoreShare"
        private val MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

        // Message protocol constants
        private const val MSG_SCORE_PREFIX = "SCORE:"
    }

    // Bluetooth components
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    // Connection components
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null

    // Communication streams
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    // State management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _receivedScore = MutableStateFlow<Int?>(null)
    val receivedScore: StateFlow<Int?> = _receivedScore.asStateFlow()

    // Check if Bluetooth is available and enabled
    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    // Permission check helper
    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // For older versions, we'll check at runtime
        }
    }

    // Get list of paired devices
    suspend fun updatePairedDevices() {
        if (bluetoothAdapter == null || !checkBluetoothPermission()) {
            _pairedDevices.value = emptyList()
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val devices = bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
                _pairedDevices.value = devices
            } catch (se: SecurityException) {
                // Handle security exception - permission denied
                _pairedDevices.value = emptyList()
            } catch (e: Exception) {
                // Handle other exceptions
                _pairedDevices.value = emptyList()
            }
        }
    }

    // Start server to listen for connections
    suspend fun startServer() {
        if (bluetoothAdapter == null || !checkBluetoothPermission()) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.LISTENING

                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)

                try {
                    // Wait for a connection
                    _connectionState.value = ConnectionState.CONNECTING
                    connectedSocket = serverSocket?.accept()

                    if (connectedSocket != null) {
                        // Connection established
                        serverSocket?.close()
                        setupCommunication(connectedSocket!!)
                        _connectionState.value = ConnectionState.CONNECTED

                        // Start listening for incoming messages
                        startListening()
                    }
                } catch (se: SecurityException) {
                    _connectionState.value = ConnectionState.ERROR
                } catch (e: IOException) {
                    _connectionState.value = ConnectionState.ERROR
                }
            } catch (se: SecurityException) {
                _connectionState.value = ConnectionState.ERROR
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    // Connect to a specific device
    suspend fun connectToDevice(device: BluetoothDevice) {
        if (bluetoothAdapter == null || !checkBluetoothPermission()) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.CONNECTING

                // Cancel discovery before connecting
                try {
                    bluetoothAdapter.cancelDiscovery()
                } catch (se: SecurityException) {
                    // Handle permission issue for cancelDiscovery
                }

                try {
                    // Create socket and connect
                    clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID)

                    // Connect to the remote device
                    clientSocket?.connect()
                    connectedSocket = clientSocket

                    if (connectedSocket != null) {
                        setupCommunication(connectedSocket!!)
                        _connectionState.value = ConnectionState.CONNECTED

                        // Start listening for incoming messages
                        startListening()
                    }
                } catch (se: SecurityException) {
                    _connectionState.value = ConnectionState.ERROR
                } catch (e: IOException) {
                    try {
                        clientSocket?.close()
                        _connectionState.value = ConnectionState.DISCONNECTED
                    } catch (e2: IOException) {
                        _connectionState.value = ConnectionState.ERROR
                    } catch (se: SecurityException) {
                        _connectionState.value = ConnectionState.ERROR
                    }
                }
            } catch (se: SecurityException) {
                _connectionState.value = ConnectionState.ERROR
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    // Set up input/output streams
    private fun setupCommunication(socket: BluetoothSocket) {
        try {
            outputStream = socket.outputStream
            inputStream = socket.inputStream
        } catch (se: SecurityException) {
            _connectionState.value = ConnectionState.ERROR
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.ERROR
        }
    }

    // Send score to connected device
    suspend fun sendScore(score: Int): Boolean {
        if (outputStream == null || _connectionState.value != ConnectionState.CONNECTED) {
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val scoreMessage = "$MSG_SCORE_PREFIX$score"
                outputStream?.write(scoreMessage.toByteArray())
                true
            } catch (se: SecurityException) {
                _connectionState.value = ConnectionState.ERROR
                false
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.ERROR
                false
            }
        }
    }

    // Listen for incoming messages
    private suspend fun startListening() {
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1

                    if (bytes > 0) {
                        val readMessage = String(buffer, 0, bytes)

                        // Process the message based on protocol
                        if (readMessage.startsWith(MSG_SCORE_PREFIX)) {
                            val scoreValue = readMessage.substringAfter(MSG_SCORE_PREFIX).toIntOrNull()
                            if (scoreValue != null) {
                                _receivedScore.value = scoreValue
                            }
                        }
                    }
                } catch (se: SecurityException) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    break
                } catch (e: IOException) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    break
                }
            }
        }
    }

    // Close all connections and streams
    suspend fun close() {
        withContext(Dispatchers.IO) {
            try {
                serverSocket?.close()
                clientSocket?.close()
                connectedSocket?.close()
                outputStream?.close()
                inputStream?.close()

                serverSocket = null
                clientSocket = null
                connectedSocket = null
                outputStream = null
                inputStream = null

                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (se: SecurityException) {
                _connectionState.value = ConnectionState.ERROR
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    // Connection states
    enum class ConnectionState {
        DISCONNECTED,
        LISTENING,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}