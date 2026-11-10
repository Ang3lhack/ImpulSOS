package com.impulsos.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impulsos.app.domain.DetectorDiagnostic
import com.impulsos.app.domain.ShakeDetectorLogic
import com.impulsos.app.domain.ShakeSensitivity
import com.impulsos.app.sensor.MonitoringService
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private data class SensorReadout(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val time: Long = 0L, val sampleInterval: Long? = null)

/** Independent diagnostic path: this composable never starts MonitoringService or emergency actions. */
@Composable
fun DetectorDiagnosticScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val detector = remember { ShakeDetectorLogic(ShakeSensitivity.BALANCED.detectorConfig()) }
    var serviceActive by remember { mutableStateOf(MonitoringService.isActive(context)) }
    var reading by remember { mutableStateOf(SensorReadout()) }
    var state by remember { mutableStateOf(DetectorDiagnostic()) }
    var running by remember { mutableStateOf(false) }
    var simulation by remember { mutableStateOf(false) }
    var simTime by remember { mutableStateOf(0L) }
    var previousSampleAt by remember { mutableStateOf(0L) }
    var acceptedCount by remember { mutableStateOf(0) }
    var lastAcceptedAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            serviceActive = MonitoringService.isActive(context)
            delay(500L)
        }
    }

    DisposableEffect(running, simulation, serviceActive) {
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = event.timestamp / 1_000_000L
                val triggered = detector.addSample(event.values[0], event.values[1], event.values[2], now)
                val delta = if (previousSampleAt == 0L) null else now - previousSampleAt
                previousSampleAt = now
                reading = SensorReadout(event.values[0], event.values[1], event.values[2], now, delta)
                if (triggered) { acceptedCount++; lastAcceptedAt = now }
                state = detector.diagnostic.copy(accepted = triggered)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (running && !simulation && !serviceActive && sensor != null) manager.registerListener(listener, sensor, 20_000)
        onDispose { manager.unregisterListener(listener) }
    }

    LaunchedEffect(running, simulation) {
        if (running && simulation) {
            detector.reset()
            var t = 1L
            repeat(80) {
                val x = (4.2 * sin(2 * PI * 4.0 * t / 1_000.0)).toFloat()
                val accepted = detector.addSample(x, 0f, 9.81f, t)
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Diagnóstico seguro", fontSize = 24.sp)
        Text("No activa alarma, pantalla de emergencia ni servicio. Detén el monitoreo normal antes de usar el sensor aquí.")
        Text("Fuente: ${if (running && simulation) "señal sintética" else if (running) "acelerómetro físico" else if (simulation) "última prueba sintética" else "inactiva"}")
        Text("X ${"%.2f".format(reading.x)} · Y ${"%.2f".format(reading.y)} · Z ${"%.2f".format(reading.z)} m/s²")
        Text("Magnitud dinámica: ${"%.2f".format(state.dynamicMagnitude)} m/s²")
        Text("Impulsos: ${state.impulseCount} · Intervalo: ${state.intervalMillis?.let { "$it ms" } ?: "—"}")
        Text(if (state.accepted) "Patrón aceptado · sin emergencia" else state.message, fontSize = 18.sp)
        Text("Timestamp de muestra: ${reading.time} ms${if (simulation) " · simulación t=$simTime ms" else ""}")
        Text("Intervalo entre muestras: ${reading.sampleInterval?.let { "$it ms" } ?: "esperando segunda muestra"}")
        Text("Patrones aceptados: $acceptedCount${lastAcceptedAt?.let { " · último a ${it} ms" } ?: ""}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { detector.reset(); acceptedCount = 0; lastAcceptedAt = null; previousSampleAt = 0L; running = true; simulation = false }, enabled = !running && !serviceActive, modifier = Modifier.weight(1f).height(54.dp)) { Text("Leer sensor") }
            OutlinedButton(onClick = { running = false; detector.reset(); state = detector.diagnostic }, modifier = Modifier.weight(1f).height(54.dp)) { Text("Detener") }
        }
        OutlinedButton(onClick = { detector.reset(); acceptedCount = 0; lastAcceptedAt = null; simulation = true; running = true }, enabled = !running, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Ejecutar señal simulada") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Volver") }
    }
}
