package com.tubes.nimons360.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs

interface OrientationCallback {
    fun onOrientationChanged(bearing: Float)
}

class OrientationHandler(context: Context) : SensorEventListener {
    private val TAG = "OrientationHandler"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    private var callback: OrientationCallback? = null
    private var isListening = false
    private var lastBearing = 0f

    fun setCallback(callback: OrientationCallback) {
        this.callback = callback
    }

    fun startListening() {
        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
            Log.d(TAG, "Orientation listener started")
        } else {
            Log.w(TAG, "Required sensors not available")
        }
    }

    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
            Log.d(TAG, "Orientation listener stopped")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerData, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerData, 0, 3)
            }
        }

        // Calculate orientation
        SensorManager.getRotationMatrix(
            rotationMatrix, null,
            accelerometerData, magnetometerData
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Azimuth is at index 0 (bearing/compass direction)
        // Convert dari radian ke derajat (0-360)
        var bearing = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        bearing = if (bearing < 0) bearing + 360f else bearing

        // Only update jika ada perubahan signifikan (mengurangi noise)
        if (abs(bearing - lastBearing) > 1f) {
            lastBearing = bearing
            callback?.onOrientationChanged(bearing)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Accuracy changes
    }

    fun getCurrentBearing(): Float {
        return lastBearing
    }
}
