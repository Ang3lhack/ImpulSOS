package com.impulsos.app.sensor

import android.hardware.Sensor
import android.hardware.SensorManager

/** Shared delivery policy for production and safe physical diagnostics. No permanent wake lock. */
object MotionSensorPolicy {
    const val SAMPLE_PERIOD_US = 20_000
    const val MAX_BATCH_LATENCY_US = 100_000
    fun sensor(manager: SensorManager): Sensor? =
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
            ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    fun latency(sensor: Sensor): Int =
        if (sensor.isWakeUpSensor && sensor.fifoMaxEventCount > 0) MAX_BATCH_LATENCY_US else 0
}
