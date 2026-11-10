package com.impulsos.app.domain

import kotlin.math.exp
import kotlin.math.sqrt

/** Experimental deliberate-pulse recognizer. Thresholds are engineering starting points only. */
data class ShakeDetectorConfig(
    val dynamicThresholdMps2: Float = 3.0f,
    val windowMillis: Long = 1_200L,
    val interruptionMillis: Long = 650L,
    val minimumActiveRatio: Float = 0f,
    val minimumAlternations: Int = 2,
    val minimumHalfCycleMillis: Long = 110L,
    val maximumHalfCycleMillis: Long = 600L,
    val peakReleaseMps2: Float = 1.2f,
)

data class DetectorDiagnostic(
    val dynamicMagnitude: Float = 0f,
    val impulseCount: Int = 0,
    val intervalMillis: Long? = null,
    val message: String = "Esperando movimiento",
    val accepted: Boolean = false,
)

enum class ShakeSensitivity(val storageValue: String, val displayName: String, val thresholdMps2: Float) {
    MORE("more", "Más sensible", 2.8f), BALANCED("balanced", "Equilibrada", 3.2f), LESS("less", "Menos sensible", 4.2f);
    fun detectorConfig() = ShakeDetectorConfig(dynamicThresholdMps2 = thresholdMps2)
    companion object { fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: BALANCED }
}

class ShakeDetectorLogic(private val config: ShakeDetectorConfig = ShakeDetectorConfig()) {
    var diagnostic: DetectorDiagnostic = DetectorDiagnostic()
        private set
    private var initialized = false
    private var gx = 0f; private var gy = 0f; private var gz = 0f
    private var lastSampleAt = 0L
    private var previousMagnitude = 0f
    private var armed = true
    private var windowStartedAt = 0L
    private var lastPeakAt = 0L
    private var peaks = 0

    fun addSample(x: Float, y: Float, z: Float, timestampMillis: Long): Boolean {
        if (!initialized) {
            initialized = true; gx = x; gy = y; gz = z; lastSampleAt = timestampMillis
            return false
        }
        val dt = timestampMillis - lastSampleAt
        if (dt <= 0L) return false
        if (dt > config.interruptionMillis) resetPattern()
        val alpha = exp(-dt / 450.0).toFloat()
        gx = alpha * gx + (1f - alpha) * x
        gy = alpha * gy + (1f - alpha) * y
        gz = alpha * gz + (1f - alpha) * z
        val dx = x - gx; val dy = y - gy; val dz = z - gz
        val magnitude = sqrt(dx * dx + dy * dy + dz * dz)
        diagnostic = diagnostic.copy(dynamicMagnitude = magnitude, accepted = false)

        if (magnitude <= config.peakReleaseMps2) armed = true
        val peak = armed && magnitude >= config.dynamicThresholdMps2 && magnitude > previousMagnitude
        previousMagnitude = magnitude
        lastSampleAt = timestampMillis
        if (!peak) {
            if (armed && magnitude < config.dynamicThresholdMps2) {
                diagnostic = diagnostic.copy(message = "Movimiento bajo el umbral; no cuenta como impulso")
            }
            return false
        }
        armed = false

        if (lastPeakAt == 0L || timestampMillis - lastPeakAt > config.maximumHalfCycleMillis ||
            (windowStartedAt != 0L && timestampMillis - windowStartedAt > config.windowMillis)) {
            resetPattern()
            windowStartedAt = timestampMillis
            lastPeakAt = timestampMillis
            peaks = 1
            diagnostic = diagnostic.copy(impulseCount = 1, intervalMillis = null, message = "Primer impulso registrado")
            return false
        }
        val separation = timestampMillis - lastPeakAt
        if (separation < config.minimumHalfCycleMillis) {
            // A too-fast peak is a new candidate, not an extension of the earlier pulse.
            windowStartedAt = timestampMillis
            lastPeakAt = timestampMillis
            peaks = 1
            diagnostic = diagnostic.copy(impulseCount = 1, intervalMillis = separation, message = "Impulso demasiado próximo; secuencia reiniciada")
            return false
        }
        lastPeakAt = timestampMillis
        peaks++
        diagnostic = diagnostic.copy(impulseCount = peaks, intervalMillis = separation, message = if (peaks >= config.minimumAlternations) "Patrón aceptado" else "Segundo impulso registrado", accepted = peaks >= config.minimumAlternations)
        if (peaks >= config.minimumAlternations) {
            resetPattern()
            return true
        }
        return false
    }

    fun reset() {
        initialized = false; gx = 0f; gy = 0f; gz = 0f; lastSampleAt = 0L
        previousMagnitude = 0f; armed = true; resetPattern()
        diagnostic = DetectorDiagnostic(message = "Detector reiniciado")
    }

    private fun resetPattern() {
        windowStartedAt = 0L; lastPeakAt = 0L; peaks = 0
    }
}
