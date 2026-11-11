package com.impulsos.app.domain

import org.junit.Assert.*
import org.junit.Test

class DiagnosticSessionTest {
    @Test fun quietSamplesPreservePeakAndRejectionEvidence() {
        val session = DiagnosticSession()
        session.record(DetectorDiagnostic(dynamicMagnitude = 8f, message = "Primer impulso registrado"), 100L)
        session.record(DetectorDiagnostic(dynamicMagnitude = 6f, intervalMillis = 150, message = "Rebote temprano"), 250L)
        repeat(100) { session.record(DetectorDiagnostic(message = "Movimiento bajo el umbral"), 300L + it) }
        assertEquals(8f, session.maximumMagnitude, 0f)
        assertEquals(2, session.events.size)
        assertTrue(session.events.last().contains("150 ms"))
    }

    @Test fun sessionHistoryIsBoundedAndClearStartsFreshTimeline() {
        val session = DiagnosticSession()
        repeat(40) { session.record(DetectorDiagnostic(message = "Evento $it"), 1000L + it) }
        assertEquals(12, session.events.size)
        session.clear()
        session.record(DetectorDiagnostic(message = "Primer impulso registrado"), 9000L)
        assertEquals(1, session.events.size)
        assertTrue(session.events.single().startsWith("0 ms:"))
    }

    @Test fun completedResultsRemainSeparateWhenTheNextReadingStarts() {
        val session = DiagnosticSession()
        val counts = listOf(1, 1, 0, 0)
        var history = emptyList<String>()
        counts.forEach { count ->
            session.record(DetectorDiagnostic(message = "Primer impulso registrado"), 100L)
            session.record(DetectorDiagnostic(message = "Movimiento bajo el umbral"), 119L)
            val result = requireNotNull(session.completedSummary(count, "Intervalo 243 ms", 19L))
            history = DiagnosticSession.appendCompletedRun(history, result)
            session.clear()
        }
        assertEquals(counts.map { "Patrones aceptados: $it" }, history.map { it.substringBefore('\n') })
        assertTrue(history.all { it.contains("Duración de lectura: 19 ms") })
        assertTrue(history.first().contains("Intervalo 243 ms"))
        assertFalse(history.last().contains("Intervalo 243 ms"))
    }

    @Test fun incompleteReadingCannotProduceAResult() {
        val session = DiagnosticSession()
        assertNull(session.completedSummary(0, "", null))
        session.record(DetectorDiagnostic(), 100L)
        assertNull(session.completedSummary(0, "", null))
        session.record(DetectorDiagnostic(), 119L)
        assertNotNull(session.completedSummary(0, "", 19L))
        session.clear()
        assertNull(session.completedSummary(0, "", null))
    }

    @Test fun keepsOnlyTheEightMostRecentResultsInOrder() {
        var history = emptyList<String>()
        (1..10).forEach { number ->
            history = DiagnosticSession.appendCompletedRun(history, "Resultado $number")
        }
        assertEquals((3..10).map { "Resultado $it" }, history)
    }
}
