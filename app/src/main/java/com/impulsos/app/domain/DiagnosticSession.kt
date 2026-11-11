package com.impulsos.app.domain

import kotlin.math.abs

/** Synthetic lateral double chop with a return after each stroke. */
internal fun syntheticChopAcceleration(elapsedMillis: Long): Float {
    fun pulse(centerMillis: Long, amplitude: Float): Float =
        amplitude * (1f - abs(elapsedMillis - centerMillis) / 60f).coerceAtLeast(0f)

    return pulse(300L, 56f) + pulse(440L, -21f) +
        pulse(720L, 52f) + pulse(860L, -21f)
}

/** Bounded, in-memory engineering evidence; no profile data or raw motion recording. */
class DiagnosticSession {
    var maximumMagnitude = 0f
        private set
    var events: List<String> = emptyList()
        private set
    private var firstSample: Long? = null
    private var lastSample: Long? = null
    private var previousMessage = ""

    fun record(sample: DetectorDiagnostic, timestamp: Long) {
        if (firstSample == null) firstSample = timestamp
        lastSample = timestamp
        if (sample.dynamicMagnitude.isFinite()) maximumMagnitude = maxOf(maximumMagnitude, sample.dynamicMagnitude)
        val message = sample.message
        if ((message != previousMessage || sample.accepted) &&
            !message.startsWith("Movimiento bajo") && !message.startsWith("Esperando") &&
            message != "Detector reiniciado") {
            val interval = sample.intervalMillis?.let { " · intervalo $it ms" }.orEmpty()
            val valley = if (message.startsWith("Valle")) " · nivel ${sample.releaseThresholdMps2} m/s²" else ""
            val crest = sample.crestMagnitudeMps2?.let { " · cresta ${"%.2f".format(it)} m/s²" }.orEmpty()
            events = (events + "${timestamp - firstSample!!} ms: $message$interval$valley$crest").takeLast(12)
        }
        previousMessage = message
    }

    fun completedSummary(acceptedCount: Int, acceptedDetail: String, sampleInterval: Long?): String? {
        val first = firstSample ?: return null
        val last = lastSample ?: return null
        if (last <= first) return null
        return buildString {
            appendLine("Patrones aceptados: $acceptedCount")
            appendLine("Duración de lectura: ${last - first} ms")
            appendLine("Máxima dinámica: ${"%.2f".format(maximumMagnitude)} m/s²")
            sampleInterval?.let { appendLine("Último intervalo entre muestras: $it ms") }
            if (acceptedCount > 0) appendLine(acceptedDetail)
            append(events.joinToString("\n").ifBlank { "Ningún impulso registrado" })
        }
    }

    fun clear() {
        maximumMagnitude = 0f
        events = emptyList()
        firstSample = null
        lastSample = null
        previousMessage = ""
    }

    companion object {
        const val MAX_COMPLETED_RUNS = 8

        fun appendCompletedRun(history: List<String>, result: String): List<String> =
            (history + result).takeLast(MAX_COMPLETED_RUNS)
    }
}
