package com.merlin.basketballanimationgame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawNetBack(centerX: Float, centerY: Float, radius: Float, netColor: Color) {
    val netLength = radius * 1.5f
    val segments = 8

    // Draw only the back half of the net
    for (i in segments / 2 until segments) {
        val angle = (i.toFloat() / segments) * 360f
        val radians = Math.toRadians(angle.toDouble()).toFloat()
        val x = centerX + radius * kotlin.math.cos(radians)
        val y = centerY + radius * kotlin.math.sin(radians)

        // Draw vertical net strings
        drawLine(
            color = netColor,
            start = Offset(x, y),
            end = Offset(
                x + (if (i % 2 == 0) radius * 0.2f else -radius * 0.2f) * 0.5f,
                y + netLength
            ),
            strokeWidth = 1f,
            alpha = 0.7f
        )
    }

    // Draw horizontal net rings (back half)
    for (i in 1..3) {
        val ringRadius = radius - (i - 1) * (radius / 4)

        drawArc(
            color = netColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - ringRadius, centerY + (i - 1) * (netLength / 4) - ringRadius),
            size = Size(ringRadius * 2, ringRadius * 2),
            style = Stroke(width = 1f),
            alpha = 0.5f
        )
    }
}

fun DrawScope.drawNetFront(centerX: Float, centerY: Float, radius: Float, netColor: Color) {
    val netLength = radius * 1.5f
    val segments = 8

    // Draw only the front half of the net
    for (i in 0 until segments / 2) {
        val angle = (i.toFloat() / segments) * 360f
        val radians = Math.toRadians(angle.toDouble()).toFloat()
        val x = centerX + radius * kotlin.math.cos(radians)
        val y = centerY + radius * kotlin.math.sin(radians)

        // Draw vertical net strings
        drawLine(
            color = netColor,
            start = Offset(x, y),
            end = Offset(
                x + (if (i % 2 == 0) radius * 0.2f else -radius * 0.2f) * 0.5f,
                y + netLength
            ),
            strokeWidth = 1f,
            alpha = 0.7f
        )
    }

    // Draw horizontal net rings (front half)
    for (i in 1..3) {
        val ringRadius = radius - (i - 1) * (radius / 4)

        drawArc(
            color = netColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - ringRadius, centerY + (i - 1) * (netLength / 4) - ringRadius),
            size = Size(ringRadius * 2, ringRadius * 2),
            style = Stroke(width = 1f),
            alpha = 0.5f
        )
    }
}

fun DrawScope.drawNetFull(centerX: Float, centerY: Float, radius: Float, netColor: Color) {
    val netLength = radius * 1.5f
    val segments = 12

    // Draw full net
    for (i in 0 until segments) {
        val angle = (i.toFloat() / segments) * 360f
        val radians = Math.toRadians(angle.toDouble()).toFloat()
        val x = centerX + radius * kotlin.math.cos(radians)
        val y = centerY + radius * kotlin.math.sin(radians)

        // Draw vertical net strings
        drawLine(
            color = netColor,
            start = Offset(x, y),
            end = Offset(
                x + (if (i % 2 == 0) radius * 0.2f else -radius * 0.2f) * 0.5f,
                y + netLength
            ),
            strokeWidth = 1f,
            alpha = 0.7f
        )
    }

    // Draw horizontal net rings
    for (i in 1..3) {
        val ringRadius = radius - (i - 1) * (radius / 4)

        drawCircle(
            color = netColor,
            radius = ringRadius,
            center = Offset(centerX, centerY + (i - 1) * (netLength / 4)),
            style = Stroke(width = 1f),
            alpha = 0.5f
        )
    }
}

fun DrawScope.drawBasketball(
    centerX: Float,
    centerY: Float,
    radius: Float,
    basketballColor: Color,
    stripesColor: Color
) {
    // Draw the main ball
    drawCircle(
        color = basketballColor,
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Draw basketball stripes
    // Horizontal stripe
    drawLine(
        color = stripesColor,
        start = Offset(centerX - radius, centerY),
        end = Offset(centerX + radius, centerY),
        strokeWidth = 2f
    )

    // Vertical stripe
    drawLine(
        color = stripesColor,
        start = Offset(centerX, centerY - radius),
        end = Offset(centerX, centerY + radius),
        strokeWidth = 2f
    )

    // Curved stripes
    val path1 = Path().apply {
        moveTo(centerX - radius * 0.7f, centerY - radius * 0.7f)
        quadraticBezierTo(
            centerX, centerY - radius * 0.2f,
            centerX + radius * 0.7f, centerY - radius * 0.7f
        )
    }

    val path2 = Path().apply {
        moveTo(centerX - radius * 0.7f, centerY + radius * 0.7f)
        quadraticBezierTo(
            centerX, centerY + radius * 0.2f,
            centerX + radius * 0.7f, centerY + radius * 0.7f
        )
    }

    drawPath(
        path = path1,
        color = stripesColor,
        style = Stroke(width = 2f)
    )

    drawPath(
        path = path2,
        color = stripesColor,
        style = Stroke(width = 2f)
    )
}