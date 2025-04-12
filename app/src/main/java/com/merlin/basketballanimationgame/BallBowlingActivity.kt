package com.merlin.basketballanimationgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BasketballAnimation() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isAnimating by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var score by remember { mutableStateOf(0) }
    var isScored by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }

    // Ball controller for dragging
    val ballController = remember { BallController() }

    // Animation state
    val ballXPosition = remember { Animatable(ballController.parameters.value.startXPosition) }
    val ballYPosition = remember { Animatable(ballController.parameters.value.startYPosition) }
    val ballScale = remember { Animatable(1f) }
    var showBallInFrontOfHoop by remember { mutableStateOf(true) }

    // Basketball colors
    val basketballColor = Color(0xFFE65100)
    val basketballStripesColor = Color.Black
    val hoopColor = Color(0xFFD32F2F)
    val netColor = Color.White
    val backboardColor = Color(0xFFECEFF1)

    // Update ball controller canvas size whenever our canvas size changes
    LaunchedEffect(canvasSize) {
        if (canvasSize != Size.Zero) {
            ballController.canvasSizeState.value = canvasSize
        }
    }

    fun resetAnimation() {
        scope.launch {
            ballXPosition.snapTo(ballController.parameters.value.startXPosition)
            ballYPosition.snapTo(ballController.parameters.value.startYPosition)
            ballScale.snapTo(1f)
            showBallInFrontOfHoop = true
            isAnimating = false
            isScored = false
        }
    }

    val soundManager = rememberSoundManager()

    fun shootBall() {
        if (!isAnimating) {
            isAnimating = true
            scope.launch {
                // Get current parameters
                val params = ballController.parameters.value

                // X position animation (from current position to basket)
                launch {
                    ballXPosition.animateTo(
                        targetValue = params.endXPosition,
                        animationSpec = tween(
                            durationMillis = (1500 * params.shotPower).toInt(),
                            easing = LinearEasing
                        )
                    )
                }

                // Y position animation (up and then down)
                launch {
                    // Go up to peak
                    ballYPosition.animateTo(
                        targetValue = params.peakYPosition * params.shotArc,
                        animationSpec = tween(
                            durationMillis = (800 * params.shotPower).toInt(),
                            easing = EaseInOutQuad
                        )
                    )

                    // Small pause at the peak
                    delay(100)

                    // Check if ball will go through hoop
                    val willScore = willScoreBasket(
                        params.endXPosition,
                        0.75f, // Target hoop x position
                        0.15f  // Tolerance for scoring
                    )

                    // Go down through the basket or miss
                    // First down to the hoop level
                    ballYPosition.animateTo(
                        targetValue = 0.33f,
                        animationSpec = tween(
                            durationMillis = (300 * params.shotPower).toInt(),
                            easing = LinearEasing
                        )
                    )

                    if (willScore) {
                        // At this point we need to change the rendering order
                        showBallInFrontOfHoop = false

                        // Continue downward through the net
                        ballYPosition.animateTo(
                            targetValue = 0.45f,
                            animationSpec = tween(
                                durationMillis = (200 * params.shotPower).toInt(),
                                easing = LinearEasing
                            )
                        )

                        soundManager.playApplauseSound()
                        score++
                        isScored = true
                    } else {
                        // Ball misses - angle away from the hoop
                        val missDirection = if (params.endXPosition > 0.8f) -1f else 1f

                        ballXPosition.animateTo(
                            targetValue = params.endXPosition + 0.2f * missDirection,
                            animationSpec = tween(
                                durationMillis = (300 * params.shotPower).toInt(),
                                easing = LinearEasing
                            )
                        )
                    }

                    // Continue falling
                    ballYPosition.animateTo(
                        targetValue = 1.2f,
                        animationSpec = tween(
                            durationMillis = (600 * params.shotPower).toInt(),
                            easing = LinearEasing
                        )
                    )

                    // Reset after a delay
                    delay(800)
                    resetAnimation()
                }

                // Scale animation (ball appears smaller at apex of shot)
                launch {
                    // Decrease size as ball goes up
                    ballScale.animateTo(
                        targetValue = 0.7f,
                        animationSpec = tween(
                            durationMillis = (800 * params.shotPower).toInt(),
                            easing = EaseInOutQuad
                        )
                    )

                    // Small pause at the peak
                    delay(100)

                    // Increase size as ball comes down
                    ballScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (1100 * params.shotPower).toInt(),
                            easing = EaseInOutQuad
                        )
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp)
        ) {
            // Canvas for drawing the basketball court elements
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        canvasSize = size.toSize()
                    }
                    .then(
                        if (!isAnimating)
                            Modifier.draggableBall(
                                controller = ballController,
                                ballRadius = canvasSize.width * 0.05f,
                                onDragStart = {},
                                onDragEnd = {}
                            )
                        else
                            Modifier
                    )
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
                val ballCenterX = size.width * (if (isAnimating) ballXPosition.value else ballController.position.value.x)
                val ballCenterY = size.height * (if (isAnimating) ballYPosition.value else ballController.position.value.y)

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

                // Handle z-order for proper ball-through-hoop effect
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
                }

                // Game status text
                if (isAnimating) {
                    Text(
                        text = "Ball in motion...",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Success/failure message
                if (isScored) {
                    Text(
                        text = "Hurray!",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Green.copy(alpha = 0.7f))
                            .padding(8.dp),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
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

                // Toggle controls button
                OutlinedButton(
                    onClick = { showControls = !showControls },
                    modifier = Modifier.shadow(2.dp)
                ) {
                    Text(if (showControls) "Hide Controls" else "Show Controls")
                }

                if (!isAnimating) {
                    Text(
                        text = "Drag the ball to position it",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Controls section with sliders
        if (showControls) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Shot Parameters",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Target X position slider
                    Text("Target X Position: ${ballController.parameters.value.endXPosition}")
                    Slider(
                        value = ballController.parameters.value.endXPosition,
                        onValueChange = {
                            ballController.parameters.value = ballController.parameters.value.copy(
                                endXPosition = it
                            )
                        },
                        valueRange = 0.5f..0.95f,
                        steps = 10,
                        enabled = !isAnimating
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Peak Y position slider
                    Text("Shot Arc Height: ${ballController.parameters.value.peakYPosition}")
                    Slider(
                        value = ballController.parameters.value.peakYPosition,
                        onValueChange = {
                            ballController.parameters.value = ballController.parameters.value.copy(
                                peakYPosition = it
                            )
                        },
                        valueRange = 0.1f..0.4f,
                        steps = 10,
                        enabled = !isAnimating
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shot power slider
                    Text("Shot Power: ${ballController.parameters.value.shotPower}")
                    Slider(
                        value = ballController.parameters.value.shotPower,
                        onValueChange = {
                            ballController.parameters.value = ballController.parameters.value.copy(
                                shotPower = it
                            )
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 10,
                        enabled = !isAnimating
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shot arc slider
                    Text("Arc Multiplier: ${ballController.parameters.value.shotArc}")
                    Slider(
                        value = ballController.parameters.value.shotArc,
                        onValueChange = {
                            ballController.parameters.value = ballController.parameters.value.copy(
                                shotArc = it
                            )
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 10,
                        enabled = !isAnimating
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reset to Default button
                    OutlinedButton(
                        onClick = {
                            ballController.parameters.value = BallParameters(
                                startXPosition = 0.2f,
                                startYPosition = 0.8f,
                                endXPosition = 0.75f,
                                peakYPosition = 0.2f,
                                shotPower = 1.0f,
                                shotArc = 1.0f
                            )
                            ballController.position.value = Offset(0.2f, 0.8f)
                            resetAnimation()
                        },
                        enabled = !isAnimating,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to Default")
                    }
                }
            }
        }
    }
}

// Function to determine if the ball will score based on how close it is to the target
fun willScoreBasket(ballXPosition: Float, targetXPosition: Float, tolerance: Float): Boolean {
    return (ballXPosition > targetXPosition - tolerance &&
            ballXPosition < targetXPosition + tolerance)
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun BasketballAnimationPreview() {
    MaterialTheme {
        BasketballAnimation()
    }
}