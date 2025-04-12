package com.merlin.basketballanimationgame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A utility class to detect shake motion using the device's accelerometer.
 * @param context The application context
 * @param shakeThreshold The threshold for determining a shake (default: 12.0f)
 * @param cooldownPeriod The minimum time between shake detections in milliseconds (default: 1000)
 * @param onShakeDetected Callback to be invoked when a shake is detected
 */


class ShakeDetector(
    context: Context,
    private val shakeThreshold: Float = 2.5f,
    private val cooldownPeriod: Long = 500L,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var lastRotationX = 0.0f
    private var lastRotationY = 0.0f
    private var lastRotationZ = 0.0f
    private var initialized = false
    private var lastShakeTime = 0L

    fun start(): Boolean {
        return if (gyroscope != null) {
            sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST
            )
            true
        } else {
            println("Gyroscope not available on this device.")
            false
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                val rotationX = it.values[0]
                val rotationY = it.values[1]
                val rotationZ = it.values[2]

                if (!initialized) {
                    lastRotationX = rotationX
                    lastRotationY = rotationY
                    lastRotationZ = rotationZ
                    initialized = true
                    println("Gyroscope initialized: lastRotationX=$lastRotationX, lastRotationY=$lastRotationY, lastRotationZ=$lastRotationZ")
                    return
                }

                val deltaRotationX = abs(lastRotationX - rotationX)
                val deltaRotationY = abs(lastRotationY - rotationY)
                val deltaRotationZ = abs(lastRotationZ - rotationZ)

                val rotationAcceleration = sqrt(deltaRotationX * deltaRotationX + deltaRotationY * deltaRotationY + deltaRotationZ * deltaRotationZ)

                if (rotationAcceleration > shakeThreshold) {
                    println("Rotation Acceleration exceeds threshold")
                    println("Rotation Acceleration: $rotationAcceleration")

                    // Prevent multiple triggers by using a cooldown period
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastShakeTime > cooldownPeriod) {
                        println("Shake detected!")
                        lastShakeTime = currentTime
                        onShakeDetected()
                    } else {
                        println("Shake ignored due to cooldown")
                    }
                }

                lastRotationX = rotationX
                lastRotationY = rotationY
                lastRotationZ = rotationZ
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}
