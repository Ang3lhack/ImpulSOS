package com.impulsos.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ShakeDetectorLogicTest {
    @Test fun restDoesNotTrigger() {
        val detector = ShakeDetectorLogic()
        assertFalse(feed(detector, durationMs = 5_000, frequencyHz = 0.0, amplitude = 0.0))
    }

    @Test fun walkingCadenceDoesNotTrigger() {
        val detector = ShakeDetectorLogic()
        assertFalse(feed(detector, durationMs = 6_000, frequencyHz = 2.0, amplitude = 2.2))
    }

    @Test fun runningCadenceDoesNotMeetShakeCadence() {
        val detector = ShakeDetectorLogic()
        assertFalse(feed(detector, durationMs = 6_000, frequencyHz = 2.5, amplitude = 2.5))
    }

    @Test fun isolatedJoltDoesNotTrigger() {
        val detector = ShakeDetectorLogic()
        detector.addSample(0f, 0f, 9.81f, 1L)
        detector.addSample(8f, 0f, 9.81f, 21L)
        assertFalse((2..100).any { i -> detector.addSample(0f, 0f, 9.81f, 21L + i * 20L) })
    }

    @Test fun sustainedThreeSecondShakeTriggers() {
        val detector = ShakeDetectorLogic()
        assertTrue(feed(detector, durationMs = 4_200, frequencyHz = 4.0, amplitude = 4.4))
    }

    @Test fun sustainedShakeTriggersAtUiSamplingRate() {
        val detector = ShakeDetectorLogic()
        assertTrue(feed(detector, durationMs = 4_800, frequencyHz = 4.0, amplitude = 4.4, samplePeriodMs = 60L))
    }

    @Test fun deliberateShakeCanTriggerWithinTheRecognitionWindow() {
        val detector = ShakeDetectorLogic()
        assertTrue(feed(detector, durationMs = 1_200, frequencyHz = 4.0, amplitude = 4.4))
    }

    @Test fun diagnosticSimulationSignalRecognizesAClosedPattern() {
        val detector = ShakeDetectorLogic()
        assertTrue(feed(detector, durationMs = 1_580, frequencyHz = 4.0, amplitude = 4.2))
    }

    @Test fun sensitivityPresetsChangeTheInitialThreshold() {
        val moreSensitive = ShakeDetectorLogic(ShakeSensitivity.MORE.detectorConfig())
        val lessSensitive = ShakeDetectorLogic(ShakeSensitivity.LESS.detectorConfig())
        assertTrue(feed(moreSensitive, durationMs = 4_800, frequencyHz = 4.0, amplitude = 3.8))
        assertFalse(feed(lessSensitive, durationMs = 4_800, frequencyHz = 4.0, amplitude = 3.8))
    }

    @Test fun freeFallAndImpactDoNotTrigger() {
        val detector = ShakeDetectorLogic()
        detector.addSample(0f, 0f, 9.81f, 1L)
        var triggered = false
        for (time in 21L..500L step 20L) triggered = detector.addSample(0f, 0f, 0f, time) || triggered
        for (time in 521L..2_000L step 20L) {
            val impact = if (time == 521L) 27f else 0f
            triggered = detector.addSample(impact, 0f, 9.81f, time) || triggered
        }
        assertFalse(triggered)
    }

    @Test fun highFrequencyVibrationDoesNotMeetShakeCadence() {
        val detector = ShakeDetectorLogic()
        assertFalse(feed(detector, durationMs = 5_000, frequencyHz = 20.0, amplitude = 4.0))
    }

    @Test fun interruptedShakeDoesNotCarryWindowAcrossPause() {
        val detector = ShakeDetectorLogic()
        detector.addSample(0f, 0f, 9.81f, 1L)
        var triggered = false
        // Two isolated partial bursts separated by a long pause must not combine into a gesture.
        for (time in 21L..121L step 20L) triggered = shakeSample(detector, time, 4.0, 4.4) || triggered
        for (time in 141L..1_400L step 20L) triggered = detector.addSample(0f, 0f, 9.81f, time) || triggered
        for (time in 1_420L..1_520L step 20L) triggered = shakeSample(detector, time, 4.0, 4.4) || triggered
        assertFalse(triggered)
    }

    @Test fun resetClearsAnIncompleteWindow() {
        val detector = ShakeDetectorLogic()
        detector.addSample(0f, 0f, 9.81f, 1L)
        for (time in 21L..121L step 20L) shakeSample(detector, time, 4.0, 4.4)
        detector.reset()
        assertFalse(feed(detector, durationMs = 100, frequencyHz = 4.0, amplitude = 4.4, startAt = 5_000L))
    }

    private fun feed(
        detector: ShakeDetectorLogic,
        durationMs: Long,
        frequencyHz: Double,
        amplitude: Double,
        startAt: Long = 1L,
        samplePeriodMs: Long = 20L,
    ): Boolean {
        detector.addSample(0f, 0f, 9.81f, startAt)
        var triggered = false
        for (elapsed in samplePeriodMs..durationMs step samplePeriodMs) {
            triggered = shakeSample(detector, startAt + elapsed, frequencyHz, amplitude) || triggered
        }
        return triggered
    }

    private fun shakeSample(detector: ShakeDetectorLogic, timestamp: Long, frequency: Double, amplitude: Double): Boolean {
        val x = (amplitude * sin(2 * PI * frequency * timestamp / 1_000.0)).toFloat()
        return detector.addSample(x, 0f, 9.81f, timestamp)
    }
}
