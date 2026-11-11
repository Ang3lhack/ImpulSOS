package com.impulsos.app.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.impulsos.app.sensor.MotionSensorPolicy
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impulsos.app.domain.DetectorDiagnostic
import com.impulsos.app.domain.DiagnosticSession
import com.impulsos.app.domain.ShakeDetectorLogic
import com.impulsos.app.domain.ShakeSensitivity
import com.impulsos.app.domain.syntheticChopAcceleration
import com.impulsos.app.sensor.MonitoringService
import kotlinx.coroutines.delay

private data class SensorReadout(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val time: Long = 0L, val sampleInterval: Long? = null)

/** Independent diagnostic path: this composable never starts MonitoringService or emergency actions. */
@Composable
fun DetectorDiagnosticScreen(
    onBack: () -> Unit,
    sensitivity: ShakeSensitivity = ShakeSensitivity.BALANCED,
    guided: Boolean = false,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val detector = remember(sensitivity) { ShakeDetectorLogic(sensitivity.detectorConfig()) }
    val session = remember { DiagnosticSession() }
    var completedRuns by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var summary by remember { mutableStateOf("") }

    fun updateSummary() {
        summary = "Máxima dinámica: ${"%.2f".format(session.maximumMagnitude)} m/s²\n" +
            session.events.joinToString("\n").ifBlank { "Ningún impulso registrado" }
    }
    var serviceActive by remember { mutableStateOf(MonitoringService.isActive(context)) }
    var reading by remember { mutableStateOf(SensorReadout()) }
    var state by remember { mutableStateOf(DetectorDiagnostic()) }
    var running by remember { mutableStateOf(false) }
    var simulation by remember { mutableStateOf(false) }
    var simTime by remember { mutableStateOf(0L) }
    var previousSampleAt by remember { mutableStateOf(0L) }
    var acceptedCount by remember { mutableStateOf(0) }
    var acceptedDetail by remember { mutableStateOf("Sin patrón aceptado en esta sesión") }
    var lastAcceptedAt by remember { mutableStateOf<Long?>(null) }
    var stoppedPractice by remember { mutableStateOf(false) }
    var sensorError by remember { mutableStateOf(false) }

    fun startReading(synthetic: Boolean) {
        if (!synthetic && MonitoringService.isActive(context)) {
            serviceActive = true
            return
        }
        session.clear()
        detector.reset()
        summary = ""
        reading = SensorReadout()
        state = DetectorDiagnostic()
        acceptedDetail = "Sin patrón aceptado en esta sesión"
        acceptedCount = 0
        lastAcceptedAt = null
        previousSampleAt = 0L
        simTime = 0L
        simulation = synthetic
        stoppedPractice = false
        sensorError = false
        running = true
    }

    fun stopReading() {
        if (!running) return
        running = false
        if (guided) {
            stoppedPractice = true
            return
        }
        state = detector.diagnostic
        updateSummary()
        if (!simulation && !serviceActive) {
            session.completedSummary(acceptedCount, acceptedDetail, reading.sampleInterval)?.let { result ->
                completedRuns = ArrayList(DiagnosticSession.appendCompletedRun(completedRuns, result))
            }
        }
        android.util.Log.i("ImpulSOS-Diagnostic", "Fuente=${if (simulation) "sintética" else "física"}\n$summary")
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) running = false }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            serviceActive = MonitoringService.isActive(context)
            if (serviceActive && !simulation) running = false
            delay(500L)
        }
    }

    DisposableEffect(running, simulation, serviceActive) {
        val sensor = MotionSensorPolicy.sensor(manager)
        var lastUiUpdate = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!running || simulation || serviceActive) return
                val now = event.timestamp / 1_000_000L
                val triggered = detector.addSample(event.values[0], event.values[1], event.values[2], now)
                if (!guided) session.record(detector.diagnostic, now)
                val delta = if (previousSampleAt == 0L) null else now - previousSampleAt
                previousSampleAt = now
                if (triggered && !guided) {
                    val confirmation = detector.diagnostic.crestConfirmationMillis
                    val delivery = (SystemClock.elapsedRealtime() - now).coerceAtLeast(0)
                    acceptedDetail = "Último patrón físico: intervalo ${detector.diagnostic.intervalMillis} ms · confirmación $confirmation ms · entrega $delivery ms"
                    android.util.Log.i("ImpulSOS-Diagnostic", acceptedDetail)
                }
                if (!triggered && now - lastUiUpdate < 100L) return
                lastUiUpdate = now
                reading = SensorReadout(event.values[0], event.values[1], event.values[2], now, delta)
                if (triggered) { acceptedCount++; lastAcceptedAt = now }
                state = detector.diagnostic.copy(accepted = triggered)
                if (!guided) updateSummary()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (running && !simulation && !serviceActive) {
            val registered = sensor != null &&
                manager.registerListener(listener, sensor, MotionSensorPolicy.SAMPLE_PERIOD_US, MotionSensorPolicy.latency(sensor))
            if (!registered) {
                running = false
                sensorError = true
                state = DetectorDiagnostic(message = "No se pudo registrar el sensor")
            }
        }
        onDispose { manager.unregisterListener(listener) }
    }

    LaunchedEffect(running, simulation) {
        if (running && simulation) {
            detector.reset()
            var t = 1L
            repeat(80) {
                val x = syntheticChopAcceleration(t - 1L)
                val accepted = detector.addSample(x, 0f, 9.81f, t)
                session.record(detector.diagnostic, t)
                updateSummary()
                reading = SensorReadout(x, 0f, 9.81f, t, if (t == 1L) null else 20L)
                if (accepted) { acceptedCount++; lastAcceptedAt = t }
                state = detector.diagnostic.copy(accepted = accepted)
                simTime = t
                t += 20L
                delay(20L)
            }
            running = false
        }
    }

    if (guided) {
        val result = when {
            serviceActive -> "Pausa el monitoreo en Inicio o Sensor para practicar."
            sensorError -> "No se pudo leer el acelerómetro. Vuelve a intentarlo."
            acceptedCount > 0 -> "Doble tajo reconocido."
            running && state.impulseCount == 1 -> "Primer impulso reconocido."
            running -> "Escuchando. Haz el doble tajo."
            stoppedPractice -> "El gesto quedó sin reconocer. Repite la secuencia cuando estés listo."
            else -> "Pulsa Empezar práctica cuando estés listo."
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Práctica del doble tajo", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            Text("Practica sin alarma. Al empezar, sujeta el teléfono quieto un segundo. Luego haz dos tajos hacia el mismo lado, separados por cerca de medio segundo.")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (acceptedCount > 0) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Text(
                    result,
                    modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (running) {
                Button(onClick = ::stopReading, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                    Text("Detener práctica")
                }
            } else {
                Button(
                    onClick = { startReading(synthetic = false) },
                    enabled = !serviceActive,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) { Text(if (stoppedPractice) "Volver a practicar" else "Empezar práctica") }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text("Volver a la guía")
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Diagnóstico seguro", fontSize = 24.sp)
        Text("No activa alarma, pantalla de emergencia ni servicio. Detén el monitoreo normal antes de usar el sensor aquí.")
        Text("Fuente: ${if (running && simulation) "señal sintética" else if (running) "acelerómetro físico" else if (simulation) "última prueba sintética" else "inactiva"}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { startReading(synthetic = false) },
                enabled = !running && !serviceActive,
                modifier = Modifier.weight(1f).heightIn(min = 54.dp),
            ) { Text("Leer sensor") }
            OutlinedButton(
                onClick = { stopReading() },
                enabled = running,
                modifier = Modifier.weight(1f).heightIn(min = 54.dp),
            ) { Text("Detener") }
        }
        Text("Patrones aceptados: $acceptedCount${lastAcceptedAt?.let { " · último a ${it} ms" } ?: ""}")
        Text(acceptedDetail)
        if (!running) {
            Text(
                "Historial de pruebas físicas",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            if (completedRuns.isEmpty()) {
                Text("El resumen de cada lectura aparece aquí al pulsar Detener.")
            } else {
                completedRuns.forEachIndexed { index, result ->
                    Text("Prueba ${index + 1} · ${result.substringBefore('\n')}")
                }
                OutlinedButton(
                    onClick = {
                        val report = completedRuns.mapIndexed { index, result ->
                            "Prueba ${index + 1}\n$result"
                        }.joinToString("\n\n")
                        clipboard.setText(AnnotatedString("ImpulSOS · Pruebas físicas\n\n$report"))
                        Toast.makeText(context, "Historial copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) { Text("Copiar historial") }
            }
        }
        Text("X ${"%.2f".format(reading.x)} · Y ${"%.2f".format(reading.y)} · Z ${"%.2f".format(reading.z)} m/s²")
        Text("Magnitud dinámica: ${"%.2f".format(state.dynamicMagnitude)} m/s²")
        Text("Impulsos: ${state.impulseCount} · Intervalo: ${state.intervalMillis?.let { "$it ms" } ?: "—"}")
        Text(if (state.accepted) "Patrón aceptado · sin emergencia" else state.message, fontSize = 18.sp)
        Text("Timestamp de muestra: ${reading.time} ms${if (simulation) " · simulación t=$simTime ms" else ""}")
        Text("Intervalo entre muestras: ${reading.sampleInterval?.let { "$it ms" } ?: "esperando segunda muestra"}")
        OutlinedButton(
            onClick = { startReading(synthetic = true) },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        ) { Text("Ejecutar señal simulada") }
        Text(summary)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Volver") }
    }
}
