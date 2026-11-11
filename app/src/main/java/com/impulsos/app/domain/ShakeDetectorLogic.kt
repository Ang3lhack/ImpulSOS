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
    val minimumHalfCycleMillis: Long = 200L,
    val maximumHalfCycleMillis: Long = 600L,
    val peakReleaseMps2: Float = 1.2f,
    val releaseRatio: Float = 0.60f,
    val minimumDirectionCosine: Float = 0.5f,
    val minimumCrestMps2: Float = 28f,
)

data class DetectorDiagnostic(
    val dynamicMagnitude: Float = 0f,
    val impulseCount: Int = 0,
    val intervalMillis: Long? = null,
    val message: String = "Esperando movimiento",
    val accepted: Boolean = false,
    val releaseThresholdMps2: Float = 0f,
    val crestMagnitudeMps2: Float? = null,
    val crestConfirmationMillis: Long? = null,
)

enum class ShakeSensitivity(
    val storageValue: String,
    val displayName: String,
    val thresholdMps2: Float,
    private val minimumCrestMps2: Float,
) {
    MORE("more", "Más sensible", 2.8f, 26f),
    BALANCED("balanced", "Equilibrada", 3.2f, 28f),
    LESS("less", "Menos sensible", 4.2f, 32f);

    fun detectorConfig() = ShakeDetectorConfig(
        dynamicThresholdMps2 = thresholdMps2,
        minimumCrestMps2 = minimumCrestMps2,
    )
    companion object { fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: BALANCED }
}

class ShakeDetectorLogic(private val config: ShakeDetectorConfig = ShakeDetectorConfig()) {
    var diagnostic: DetectorDiagnostic = DetectorDiagnostic()
        private set
    private var initialized = false
    private var gx = 0f; private var gy = 0f; private var gz = 0f
    private var lastSampleAt = 0L
    private var previousMagnitude = 0f
    private var crestMagnitude = 0f
    private var crestAt = 0L
    private var crestX = 0f; private var crestY = 0f; private var crestZ = 0f
    private var windowStartedAt = 0L
    private var lastPeakAt = 0L
    private var peaks = 0
    private var firstX = 0f; private var firstY = 0f; private var firstZ = 0f
    private var lastStrongCrestAt: Long? = null
    private var lowAccelerationStartedAt: Long? = null
    private var lowAccelerationSamples = 0
    private var dropUntil = 0L

    fun addSample(x: Float, y: Float, z: Float, timestampMillis: Long): Boolean {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) { reset(); return false }
        if (!initialized) {
            initialized = true; gx = x; gy = y; gz = z; lastSampleAt = timestampMillis
            return false
        }
        val dt = timestampMillis - lastSampleAt
        if (dt <= 0L) return false
        if (dt > config.interruptionMillis) {
            reset()
            return addSample(x, y, z, timestampMillis)
        }
        val lowAcceleration = x * x + y * y + z * z < LOW_ACCELERATION_SQUARED
        updateDropGuard(lowAcceleration, timestampMillis, dt)
        val alpha = exp(-dt / 450.0).toFloat()
        gx = alpha * gx + (1f - alpha) * x
        gy = alpha * gy + (1f - alpha) * y
        gz = alpha * gz + (1f - alpha) * z
        val dx = x - gx; val dy = y - gy; val dz = z - gz
        val magnitude = sqrt(dx * dx + dy * dy + dz * dz)
        diagnostic = diagnostic.copy(
            dynamicMagnitude = magnitude,
            accepted = false,
            crestMagnitudeMps2 = null,
            crestConfirmationMillis = null,
        )

        val rising = magnitude > previousMagnitude
        previousMagnitude = magnitude
        lastSampleAt = timestampMillis
        if (timestampMillis < dropUntil) {
            crestMagnitude = 0f
            diagnostic = diagnostic.copy(impulseCount = 0, intervalMillis = null, message = "Caída libre o impacto posterior: descartado")
            return false
        }
        if (lowAcceleration) {
            diagnostic = diagnostic.copy(intervalMillis = null, message = "Aceleración baja: esperando confirmación de caída")
            return false
        }

        if (crestMagnitude == 0f && (!rising || magnitude < config.dynamicThresholdMps2)) {
            if (magnitude < config.dynamicThresholdMps2) {
                diagnostic = diagnostic.copy(message = "Movimiento bajo el umbral; no cuenta como impulso")
            }
            return false
        }
        if (magnitude > crestMagnitude) {
            crestMagnitude = magnitude
            crestAt = timestampMillis
            crestX = dx; crestY = dy; crestZ = dz
            return false
        }
        // Confirm on the descent and use the actual crest's timestamp and direction.
        val release = maxOf(config.peakReleaseMps2, crestMagnitude * config.releaseRatio)
        if (magnitude > release) return false
        val peakMagnitude = crestMagnitude
        crestMagnitude = 0f
        diagnostic = diagnostic.copy(
            releaseThresholdMps2 = release,
            crestMagnitudeMps2 = peakMagnitude,
            crestConfirmationMillis = timestampMillis - crestAt,
        )
        if (peakMagnitude < config.minimumCrestMps2) {
            diagnostic = diagnostic.copy(intervalMillis = null, message = "Cresta bajo el umbral de tajo: descartada")
            return false
        }
        return registerCrest(crestAt, crestX / peakMagnitude, crestY / peakMagnitude, crestZ / peakMagnitude)
    }

    private fun updateDropGuard(lowAcceleration: Boolean, timestampMillis: Long, sampleGapMillis: Long) {
        if (!lowAcceleration) {
            lowAccelerationStartedAt = null
            lowAccelerationSamples = 0
            return
        }

        val startedAt = lowAccelerationStartedAt
        if (startedAt == null || sampleGapMillis > MAX_DROP_SAMPLE_GAP_MILLIS) {
            lowAccelerationStartedAt = timestampMillis
            lowAccelerationSamples = 1
            return
        }

        lowAccelerationSamples++
        if (lowAccelerationSamples >= MIN_DROP_SAMPLES && timestampMillis - startedAt >= MIN_DROP_MILLIS) {
            if (timestampMillis >= dropUntil) {
                lastStrongCrestAt = null
                resetPattern()
            }
            dropUntil = timestampMillis + DROP_SUPPRESSION_MILLIS
        }
    }

    private fun registerCrest(timestampMillis: Long, x: Float, y: Float, z: Float): Boolean {
        val previousStrongCrest = lastStrongCrestAt
        lastStrongCrestAt = timestampMillis
        // A nearby crest can be part of the same stroke. Continuous fast vibration keeps renewing this guard.
        if (previousStrongCrest != null && timestampMillis - previousStrongCrest < 110L) {
            diagnostic = diagnostic.copy(
                intervalMillis = timestampMillis - previousStrongCrest,
                message = "Rebote rápido o vibración: no cuenta como otro tajo",
            )
            return false
        }

        if (lastPeakAt == 0L || timestampMillis - lastPeakAt > config.maximumHalfCycleMillis ||
            (windowStartedAt != 0L && timestampMillis - windowStartedAt > config.windowMillis)) {
            resetPattern()
            windowStartedAt = timestampMillis
            lastPeakAt = timestampMillis
            peaks = 1
            firstX = x; firstY = y; firstZ = z
            diagnostic = diagnostic.copy(impulseCount = 1, intervalMillis = null, message = "Primer impulso registrado")
            return false
        }
        val separation = timestampMillis - lastPeakAt
        if (separation < config.minimumHalfCycleMillis) {
            diagnostic = diagnostic.copy(intervalMillis = separation, message = "Rebote temprano: esperando otro tajo")
            return false
        }
        val alignment = firstX * x + firstY * y + firstZ * z
        if (alignment < config.minimumDirectionCosine) {
            diagnostic = diagnostic.copy(intervalMillis = separation, message = "Dirección diferente: esperando repetición")
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
        previousMagnitude = 0f; crestMagnitude = 0f; lastStrongCrestAt = null
        lowAccelerationStartedAt = null
        lowAccelerationSamples = 0
        dropUntil = 0L
        resetPattern()
        diagnostic = DetectorDiagnostic(message = "Detector reiniciado")
    }

    private fun resetPattern() {
        windowStartedAt = 0L; lastPeakAt = 0L; peaks = 0
    }

    private companion object {
        const val LOW_ACCELERATION_SQUARED = 8.66f
        const val MIN_DROP_SAMPLES = 3
        const val MIN_DROP_MILLIS = 55L
        const val MAX_DROP_SAMPLE_GAP_MILLIS = 75L
        const val DROP_SUPPRESSION_MILLIS = 800L
    }
}
