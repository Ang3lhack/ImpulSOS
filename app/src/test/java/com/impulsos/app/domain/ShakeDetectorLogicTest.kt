package com.impulsos.app.domain

import org.junit.Assert.assertEquals
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
        assertTrue(feed(detector, durationMs = 4_200, frequencyHz = 4.0, amplitude = 45.0))
    }

    @Test fun sustainedShakeTriggersAtUiSamplingRate() {
        val detector = ShakeDetectorLogic()
        assertTrue(feed(detector, durationMs = 4_800, frequencyHz = 4.0, amplitude = 45.0, samplePeriodMs = 60L))
    }

    @Test fun deliberateShakeCanTriggerWithinTheRecognitionWindow() {
        val detector = ShakeDetectorLogic()
        assertTrue(feed(detector, durationMs = 1_200, frequencyHz = 4.0, amplitude = 45.0))
    }

    @Test fun diagnosticSimulationSignalRecognizesAClosedPattern() {
        val detector = ShakeDetectorLogic(ShakeSensitivity.BALANCED.detectorConfig())
        val accepted = (0L..1_580L step 20L).count { time ->
            detector.addSample(syntheticChopAcceleration(time), 0f, 9.81f, time + 1L)
        }
        assertEquals(1, accepted)
    }

    @Test fun sensitivityPresetsChangeTheInitialThreshold() {
        val moreSensitive = ShakeDetectorLogic(ShakeSensitivity.MORE.detectorConfig())
        val lessSensitive = ShakeDetectorLogic(ShakeSensitivity.LESS.detectorConfig())
        assertTrue(feed(moreSensitive, durationMs = 4_800, frequencyHz = 4.0, amplitude = 31.0))
        assertFalse(feed(lessSensitive, durationMs = 4_800, frequencyHz = 4.0, amplitude = 31.0))
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

    @Test fun briefLowAccelerationDoesNotEraseAValidChopPair() {
        for (lowSamples in 1..3) {
            val detector = ShakeDetectorLogic()
            val count = (0..65).count { i ->
                val x = when (i) {
                    14 -> 80f
                    37 -> 90f
                    else -> 0f
                }
                val z = if (i in 18 until (18 + lowSamples)) 0f else 9.81f
                detector.addSample(x, 0f, z, 1L + i * 19L)
            }
            assertEquals("low samples=$lowSamples", 1, count)
        }
    }

    @Test fun sustainedLowAccelerationErasesThePairAndSuppressesImpacts() {
        val detector = ShakeDetectorLogic()
        val accepted = (0..80).count { i ->
            val x = when (i) {
                14, 31, 49 -> 90f
                else -> 0f
            }
            val z = if (i in 18..22) 0f else 9.81f
            detector.addSample(x, 0f, z, 1L + i * 19L)
        }
        assertEquals(0, accepted)
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
        for (time in 21L..121L step 20L) triggered = shakeSample(detector, time, 4.0, 45.0) || triggered
        for (time in 141L..1_400L step 20L) triggered = detector.addSample(0f, 0f, 9.81f, time) || triggered
        for (time in 1_420L..1_520L step 20L) triggered = shakeSample(detector, time, 4.0, 45.0) || triggered
        assertFalse(triggered)
    }

    @Test fun resetClearsAnIncompleteWindow() {
        val detector = ShakeDetectorLogic()
        detector.addSample(0f, 0f, 9.81f, 1L)
        for (time in 21L..121L step 20L) shakeSample(detector, time, 4.0, 45.0)
        detector.reset()
        assertFalse(feed(detector, durationMs = 100, frequencyHz = 4.0, amplitude = 45.0, startAt = 5_000L))
    }

    @Test fun invalidSampleDoesNotPoisonFollowingGesture() {
        val detector = ShakeDetectorLogic()
        assertFalse(detector.addSample(Float.NaN, 0f, 9.81f, 1L))
        assertTrue(feed(detector, 1200, 4.0, 45.0, startAt = 100L))
    }

    @Test fun batchingDoesNotChangeTimestampBasedRecognition() {
        for (period in listOf(10L, 20L, 40L, 60L)) {
            assertTrue("period=$period", feed(ShakeDetectorLogic(), 1200, 4.0, 45.0, samplePeriodMs = period))
        }
    }

    @Test fun singleChopAndRecoilAreNotTwoChops() {
        val d = ShakeDetectorLogic()
        d.addSample(0f, 0f, 9.81f, 1L)
        assertFalse(d.addSample(45f, 0f, 9.81f, 21L))
        for (t in 41L..101L step 20L) assertFalse(d.addSample(0f, 0f, 9.81f, t))
        assertFalse(d.addSample(-45f, 0f, 9.81f, 141L))
        for (t in 161L..900L step 20L) assertFalse(d.addSample(0f, 0f, 9.81f, t))
    }

    @Test fun perpendicularImpulsesDoNotFormRepeatedChop() {
        val d = ShakeDetectorLogic()
        d.addSample(0f, 0f, 9.81f, 1L)
        assertFalse(d.addSample(45f, 0f, 9.81f, 21L))
        for (t in 41L..261L step 20L) assertFalse(d.addSample(0f, 0f, 9.81f, t))
        assertFalse(d.addSample(0f, 45f, 9.81f, 281L))
        assertFalse(d.addSample(0f, 0f, 9.81f, 301L))
    }

    @Test fun strongRepeatedChopsDoNotRequireAlmostCompleteStillness() {
        for (amplitude in listOf(35.0, 45.0, 60.0, 90.0)) {
            assertTrue("amplitude=$amplitude", feed(ShakeDetectorLogic(), 1200, 4.0, amplitude))
        }
    }

    @Test fun strongVibrationIsStillRejected() {
        assertFalse(feed(ShakeDetectorLogic(), 5000, 20.0, 90.0))
    }

    @Test fun strongSingleChopWithRecoilIsStillRejected() {
        val d = ShakeDetectorLogic()
        d.addSample(0f, 0f, 9.81f, 1L)
        assertFalse(d.addSample(90f, 0f, 9.81f, 21L))
        for (t in 41L..101L step 20L) assertFalse(d.addSample(0f, 0f, 9.81f, t))
        assertFalse(d.addSample(-90f, 0f, 9.81f, 141L))
        for (t in 161L..1500L step 20L) assertFalse(d.addSample(0f, 0f, 9.81f, t))
    }

    @Test fun twoChopsWithResidualTransverseMotionRecognizeSecondImpulse() {
        val d = ShakeDetectorLogic()
        d.addSample(0f, 0f, 9.81f, 1L)
        assertFalse(d.addSample(90f, 0f, 9.81f, 21L))
        // The hand keeps moving between chops; it is not perfectly motionless.
        for (t in 41L..261L step 20L) assertFalse(d.addSample(0f, 4f, 9.81f, t))
        assertFalse(d.addSample(90f, 0f, 9.81f, 281L))
        assertTrue(d.addSample(0f, 4f, 9.81f, 301L))
    }

    @Test fun alignedEverydayImpulsesDoNotPassOnTimingAlone() {
        // Surrogate signals constrained by the Motorola summaries, not recorded traces.
        for ((gap, amplitude) in listOf(12 to 23f, 20 to 12f, 15 to 13f)) {
            val signal = mapOf(10 to amplitude, (10 + gap) to amplitude)
            assertEquals("gap=${gap * 19} ms, amplitude=$amplitude", 0, countImpulsePatterns(signal))
        }
    }

    @Test fun weakPrecursorsDoNotConsumeTheTwoDeliberateCrests() {
        val signal = mapOf(12 to 6f, 15 to 48f, 17 to -34f, 38 to 6f, 41 to 42f, 43 to -34f)
        assertEquals(1, countImpulsePatterns(signal))
    }

    @Test fun oneStrongChopAndWeakSettlingMotionDoNotPass() {
        val signal = mapOf(12 to 116f, 16 to -72f, 28 to 14f)
        assertEquals(0, countImpulsePatterns(signal))
    }

    @Test fun measuresFromActualCrestsAndReportsTheConfirmationDelay() {
        val detector = ShakeDetectorLogic()
        val samples = mapOf(8 to 8f, 9 to 40f, 10 to 55f, 30 to 7f, 31 to 38f, 32 to 51f)
        for (index in 0..32) {
            assertFalse(detector.addSample(samples[index] ?: 0f, 0f, 9.81f, 1L + index * 19L))
        }
        assertTrue(detector.addSample(0f, 0f, 9.81f, 628L))
        assertEquals(418L, detector.diagnostic.intervalMillis)
        assertEquals(19L, detector.diagnostic.crestConfirmationMillis)
        assertTrue(requireNotNull(detector.diagnostic.crestMagnitudeMps2) > 40f)
    }

    private fun countImpulsePatterns(signal: Map<Int, Float>): Int {
        val detector = ShakeDetectorLogic(ShakeSensitivity.BALANCED.detectorConfig())
        return (0..80).count { index ->
            detector.addSample(signal[index] ?: 0f, 0f, 9.81f, 1L + index * 19L)
        }
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
