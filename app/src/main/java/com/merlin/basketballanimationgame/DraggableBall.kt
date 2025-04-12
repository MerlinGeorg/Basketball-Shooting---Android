package com.merlin.basketballanimationgame


import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Data class to hold the parameters for ball positioning and shooting
 */
data class BallParameters(
    val startXPosition: Float,  // Starting X position (0-1)
    val startYPosition: Float,  // Starting Y position (0-1)
    val endXPosition: Float,    // Target X position for shot (0-1)
    val peakYPosition: Float,   // Peak height of the arc (0-1)
    val shotPower: Float,       // Shot power multiplier (0.5-2.0)
    val shotArc: Float          // Arc height multiplier (0.5-2.0)
)

/**
 * Class to handle dragging ball and managing ball parameters
 */
class BallController {
    // Ball position states for dragging
    val position = mutableStateOf(Offset(0.2f, 0.8f))
    var isDragging = mutableStateOf(false)
    var canvasSizeState = mutableStateOf(Size.Zero)

    // Shooting parameters with default values for successful basket
    var parameters = mutableStateOf(
        BallParameters(
            startXPosition = 0.2f,
            startYPosition = 0.8f,
            endXPosition = 0.75f,
            peakYPosition = 0.2f,
            shotPower = 1.0f,
            shotArc = 1.0f
        )
    )

    // Convert normalized position (0-1) to actual canvas pixels
    fun getPixelPosition(): Offset {
        return Offset(
            position.value.x * canvasSizeState.value.width,
            position.value.y * canvasSizeState.value.height
        )
    }

    // Update position based on dragging
    fun updatePosition(newPosition: Offset) {
        // Clamp values to ensure ball stays within canvas
        val x = newPosition.x.coerceIn(0f, canvasSizeState.value.width)
        val y = newPosition.y.coerceIn(0f, canvasSizeState.value.height)

        // Convert to normalized coordinates (0-1)
        position.value = Offset(
            x / canvasSizeState.value.width,
            y / canvasSizeState.value.height
        )

        // Update start position in parameters
        parameters.value = parameters.value.copy(
            startXPosition = position.value.x,
            startYPosition = position.value.y
        )
    }
}

/**
 * Extension function to make any Modifier draggable for the ball
 */
fun Modifier.draggableBall(
    controller: BallController,
    ballRadius: Float,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
): Modifier {
    return this.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                controller.isDragging.value = true
                onDragStart()
            },
            onDragEnd = {
                controller.isDragging.value = false
                onDragEnd()
            },
            onDrag = { change, dragAmount ->
                change.consume()

                // Get current position in pixels
                val currentPosition = controller.getPixelPosition()

                // Calculate new position
                val newPosition = Offset(
                    currentPosition.x + dragAmount.x,
                    currentPosition.y + dragAmount.y
                )

                // Update position in controller
                controller.updatePosition(newPosition)
            }
        )
    }
}