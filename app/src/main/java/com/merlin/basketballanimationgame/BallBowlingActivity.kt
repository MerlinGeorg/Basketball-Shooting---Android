package com.merlin.basketballanimationgame

import android.bluetooth.BluetoothAdapter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.merlin.basketballanimationgame.BluetoothManager
import com.merlin.basketballanimationgame.BluetoothPermissionsHelper
import com.merlin.basketballanimationgame.ShakeDetector
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.window.Dialog


@Composable
fun BasketballAnimation() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isAnimating by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var score by remember { mutableStateOf(0)}

    // Bluetooth manager setup
    val bluetoothManager = remember { BluetoothManager(context) }
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val pairedDevices by bluetoothManager.pairedDevices.collectAsState()

    // Bluetooth UI state
    var showBluetoothUI by remember { mutableStateOf(false) }
    var showDevicesList by remember { mutableStateOf(false) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            scope.launch {
                bluetoothManager.updatePairedDevices()
            }
        } else {
            Toast.makeText(context, "Bluetooth permissions are required to share score", Toast.LENGTH_SHORT).show()
        }
    }

    // Enable Bluetooth launcher
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (bluetoothManager.isBluetoothEnabled) {
            scope.launch {
                bluetoothManager.updatePairedDevices()
            }
        }
    }

    // Request required permissions on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(BluetoothPermissionsHelper.getRequiredPermissions())
    }

    // Animation state
    val ballXPosition = remember { Animatable(0.2f) }
    val ballYPosition = remember { Animatable(0.8f) }
    val ballScale = remember { Animatable(1f) }
    val ballZIndex = remember { Animatable(0f) } // Controls whether ball is in front or behind net
    var showBallInFrontOfHoop by remember { mutableStateOf(true) }

    // Basketball colors
    val basketballColor = Color(0xFFE65100)
    val basketballStripesColor = Color.Black
    val hoopColor = Color(0xFFD32F2F)
    val netColor = Color.White
    val backboardColor = Color(0xFFECEFF1)


    fun resetAnimation() {
        scope.launch {
            ballXPosition.snapTo(0.2f)
            ballYPosition.snapTo(0.8f)
            ballScale.snapTo(1f)
            ballZIndex.snapTo(0f)
            showBallInFrontOfHoop = true
            isAnimating = false
        }
    }

    val soundManager = rememberSoundManager()

    fun shootBall() {
        if (!isAnimating) {
            isAnimating = true
            scope.launch {
                // Create a curved trajectory animation
                // X position animation (from left side to basket)
                launch {
                    ballXPosition.animateTo(
                        targetValue = 0.75f,
                        animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
                    )
                }

                // Y position animation (up and then down)
                launch {
                    // Go up
                    ballYPosition.animateTo(
                        targetValue = 0.2f,
                        animationSpec = tween(durationMillis = 800, easing = EaseInOutQuad)
                    )

                    // Small pause at the peak
                    delay(100)

                    // Go down through the basket
                    // First down to the hoop level
                    ballYPosition.animateTo(
                        targetValue = 0.33f,
                        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                    )

                    // At this point we need to change the rendering order
                    showBallInFrontOfHoop = false

                    // Continue downward through the net
                    ballYPosition.animateTo(
                        targetValue = 0.45f,
                        animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                    )

                    // Continue falling after going through net
                    ballYPosition.animateTo(
                        targetValue = 1.2f,
                        animationSpec = tween(durationMillis = 600, easing = LinearEasing)
                    )

                    soundManager.playApplauseSound()
                    score++
                    // Reset after a delay
                    delay(800)
                    resetAnimation()

                }

                // Scale animation (ball appears smaller at apex of shot)
                launch {
                    // Decrease size as ball goes up
                    ballScale.animateTo(
                        targetValue = 0.7f,
                        animationSpec = tween(durationMillis = 800, easing = EaseInOutQuad)
                    )

                    // Small pause at the peak
                    delay(100)

                    // Increase size as ball comes down
                    ballScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1100, easing = EaseInOutQuad)
                    )
                }
            }
        }
    }

    // Setup shake detector
    val shakeContext = LocalContext.current
    val shakeDetector = remember {
        ShakeDetector(
            context = shakeContext,
            shakeThreshold = 5.0f,
            cooldownPeriod = 1000L,
            onShakeDetected = {
                scope.launch {
                    shootBall()
                }
            }
        )
    }

    // Register and unregister the shake detector
    DisposableEffect(Unit) {
        shakeDetector.start()

        onDispose {
            shakeDetector.stop()

            // Clean up Bluetooth connections
            runBlocking {
                bluetoothManager.close()
            }
        }
    }



    Box(
        modifier = Modifier.fillMaxSize() // Outer Box takes full screen
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                //   .background(Color(0xFF90CAF9))
                .padding(16.dp)
                .align(Alignment.Center)
        ) {
            // Canvas for drawing the basketball court elements
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        canvasSize = size.toSize()
                    }
            ) {
                // Draw court background
                drawRect(
                    color = Color(0xFFd6ccc2),
                    size = Size(size.width, size.height)
                )

                // Calculate positions
                val backboardWidth = size.width * 0.2f
                val backboardHeight = size.height * 0.2f
                val backboardLeft = size.width * 0.7f - backboardWidth / 2
                val backboardTop = size.height * 0.15f

                val hoopRadius = size.width * 0.06f
                val hoopCenterX = backboardLeft + backboardWidth / 2
                val hoopCenterY = backboardTop + backboardHeight * 0.7f + hoopRadius

                val ballRadius = size.width * 0.05f * ballScale.value
                val ballCenterX = size.width * ballXPosition.value
                val ballCenterY = size.height * ballYPosition.value

                // Draw backboard
                drawRect(
                    color = backboardColor,
                    topLeft = Offset(backboardLeft, backboardTop),
                    size = Size(backboardWidth, backboardHeight),
                    style = Stroke(width = 4f)
                )

                // Draw the target square on backboard
                val targetSize = backboardWidth * 0.4f
                val targetLeft = backboardLeft + (backboardWidth - targetSize) / 2
                val targetTop = backboardTop + backboardHeight * 0.6f - targetSize / 2

                drawRect(
                    color = Color(0xFF1976D2),
                    topLeft = Offset(targetLeft, targetTop),
                    size = Size(targetSize, targetSize),
                    style = Stroke(width = 2f)
                )

                // Draw hoop mount
                drawLine(
                    color = hoopColor,
                    start = Offset(hoopCenterX - hoopRadius, hoopCenterY),
                    end = Offset(
                        backboardLeft + backboardWidth / 2,
                        backboardTop + backboardHeight * 0.7f
                    ),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Here we handle the z-order for proper ball-through-hoop effect

                // The ball starts on top of everything
                if (showBallInFrontOfHoop) {
                    // First draw hoop back portion
                    drawArc(
                        color = hoopColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(hoopCenterX - hoopRadius, hoopCenterY - hoopRadius),
                        size = Size(hoopRadius * 2, hoopRadius * 2),
                        style = Stroke(width = 3f)
                    )

                    // Draw net back portion
                    drawNetBack(
                        centerX = hoopCenterX,
                        centerY = hoopCenterY,
                        radius = hoopRadius,
                        netColor = netColor
                    )

                    // Draw basketball
                    drawBasketball(
                        centerX = ballCenterX,
                        centerY = ballCenterY,
                        radius = ballRadius,
                        basketballColor = basketballColor,
                        stripesColor = basketballStripesColor
                    )

                    // Draw front part of the hoop
                    drawArc(
                        color = hoopColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(hoopCenterX - hoopRadius, hoopCenterY - hoopRadius),
                        size = Size(hoopRadius * 2, hoopRadius * 2),
                        style = Stroke(width = 3f)
                    )

                    // Draw net front portion
                    drawNetFront(
                        centerX = hoopCenterX,
                        centerY = hoopCenterY,
                        radius = hoopRadius,
                        netColor = netColor
                    )
                } else {
                    // Ball is behind the net/hoop (ball is passing through)

                    // Draw basketball first (it's behind everything now)
                    drawBasketball(
                        centerX = ballCenterX,
                        centerY = ballCenterY,
                        radius = ballRadius,
                        basketballColor = basketballColor,
                        stripesColor = basketballStripesColor
                    )

                    // Draw complete hoop
                    drawCircle(
                        color = hoopColor,
                        radius = hoopRadius,
                        center = Offset(hoopCenterX, hoopCenterY),
                        style = Stroke(width = 3f)
                    )

                    // Draw complete net
                    drawNetFull(
                        centerX = hoopCenterX,
                        centerY = hoopCenterY,
                        radius = hoopRadius,
                        netColor = netColor
                    )
                }

                // Draw floor line (always on top)
                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height * 0.85f),
                    end = Offset(size.width, size.height * 0.85f),
                    strokeWidth = 2f
                )
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Display the score with share icon
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedScoreDisplay(score = score)

                    Spacer(modifier = Modifier.size(8.dp)) // Add space between score and icon

                    Icon(
                        imageVector = Icons.Default.Share, // Use a share icon from Material Icons
                        contentDescription = "Share Score",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                // Trigger Bluetooth sharing functionality here
                                if (bluetoothManager.isBluetoothEnabled) {
                                    scope.launch {
                                        bluetoothManager.updatePairedDevices()
                                        showBluetoothUI = true // Show Bluetooth UI if needed
                                    }
                                } else {
                                    enableBluetoothLauncher.launch(BluetoothPermissionsHelper.getEnableBluetoothIntent())
                                }
                            },
                        tint = Color.Black
                    )

                }
            }

            // Button to trigger the animation
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { shootBall() },
                    enabled = !isAnimating,
                    modifier = Modifier.shadow(4.dp)
                ) {
                    Text("Shoot Basketball")
                }


                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isAnimating) "Ball in motion..." else "Ready to shoot!",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Bluetooth UI Dialog
    if (showBluetoothUI) {
        Dialog(onDismissRequest = { showBluetoothUI = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Basketball Score Sharing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Bluetooth Status
                    Text(
                        "Status: ${connectionState.name}",
                        color = when (connectionState) {
                            BluetoothManager.ConnectionState.CONNECTED -> Color.Green
                            BluetoothManager.ConnectionState.ERROR -> Color.Red
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // First show device list if requested
                    if (showDevicesList) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            // When displaying device names in the list
                            items(pairedDevices) { device ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                bluetoothManager.connectToDevice(device)
                                                showDevicesList = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Safely access device name with proper permission check
                                        val deviceName = try {
                                            if (BluetoothPermissionsHelper.hasRequiredPermissions(context)) {
                                                device.name ?: "Unknown Device"
                                            } else {
                                                "Unknown Device"
                                            }
                                        } catch (e: SecurityException) {
                                            "Unknown Device"
                                        }

                                        Text(deviceName)
                                    }

                                    Text(
                                        text = device.address,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Divider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }

                            item {
                                if (pairedDevices.isEmpty()) {
                                    Text(
                                        "No paired devices found. Please pair a device in Bluetooth settings.",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Button(
                                    onClick = { showDevicesList = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    } else {
                        // Permission Check
                        if (!BluetoothPermissionsHelper.hasRequiredPermissions(context)) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(BluetoothPermissionsHelper.getRequiredPermissions())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Bluetooth Permissions")
                            }
                        } else if (!bluetoothManager.isBluetoothEnabled) {
                            // Enable Bluetooth
                            Button(
                                onClick = {
                                    try {
                                        val enableBtIntent = BluetoothAdapter.getDefaultAdapter()?.let {
                                            android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        }
                                        if (enableBtIntent != null) {
                                            enableBluetoothLauncher.launch(enableBtIntent)
                                        } else {
                                            Toast.makeText(context, "Bluetooth not available on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to enable Bluetooth", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Bluetooth")
                            }
                        } else {
                            // Connection Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        showDevicesList = true
                                        scope.launch {
                                            bluetoothManager.updatePairedDevices()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Connect to Device")
                                }

                                Spacer(modifier = Modifier.size(8.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            bluetoothManager.startServer()
                                        }
                                        Toast.makeText(context, "Waiting for connection...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Wait for Connection")
                                }
                            }

                            // Connected device actions
                            if (connectionState == BluetoothManager.ConnectionState.CONNECTED) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val success = bluetoothManager.sendScore(score)
                                            val message = if (success) "Score sent!" else "Failed to send score"
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Send Current Score: $score")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            bluetoothManager.close()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Close button
                        OutlinedButton(
                            onClick = { showBluetoothUI = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

}



@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun BasketballAnimationPreview() {
    MaterialTheme {
        BasketballAnimation()
    }
}