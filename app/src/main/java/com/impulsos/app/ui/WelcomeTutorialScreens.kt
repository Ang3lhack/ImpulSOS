package com.impulsos.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.impulsos.app.R
import com.impulsos.app.domain.ShakeSensitivity

@Composable
fun WelcomeScreen(initializing: Boolean, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(R.drawable.impulsos_isotipo), contentDescription = null, modifier = Modifier.size(116.dp))
        Text("ImpulSOS", color = colors.primary, style = MaterialTheme.typography.headlineLarge)
        Text("Tu información de emergencia a mano", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (initializing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Preparando tu información local…")
        } else {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Comenzar")
            }
        }
        Text(
            "Guarda la información en este dispositivo. En una emergencia, pide ayuda a los servicios de emergencia.",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private data class TutorialStep(
    val title: String,
    val body: String,
    val hint: String,
    val icon: ImageVector?,
)

private const val GESTURE_STEP = 7
private const val SIMULATION_STEP = 8

private val tutorialSteps = listOf(
    TutorialStep("Prepara tu plan de ayuda", "ImpulSOS muestra la información que tú eliges cuando alguien necesita ayudarte.", "Los datos permanecen en este teléfono.", Icons.Default.Info),
    TutorialStep("Completa tu perfil", "Añade tu nombre y las condiciones que quieras compartir.", "Abre Perfil y pulsa Guardar al terminar.", Icons.Default.Person),
    TutorialStep("Añade alergias y medicamentos", "Registra tus alergias en Perfil y tus medicamentos en la sección Medicamentos.", "Abre Medicamentos desde Inicio para revisar lo que guardaste.", Icons.Default.Add),
    TutorialStep("Elige tus contactos", "Añade a una persona de confianza. Puedes escribir sus datos o elegirla en el selector del teléfono.", "Abre Contactos de emergencia desde Inicio.", Icons.Default.Call),
    TutorialStep("Escribe tus indicaciones", "Deja instrucciones breves para quien te ayude. Usa solo indicaciones que hayas revisado.", "Abre Mensaje de emergencia desde Inicio.", Icons.Default.Edit),
    TutorialStep("Añade una foto si te sirve", "Elige una imagen o toma una foto. Revísala y confirma para guardarla con tus indicaciones.", "La foto es opcional y se guarda cifrada.", Icons.Default.AccountBox),
    TutorialStep("Activa el monitoreo cuando estés listo", "Pulsa Activar monitoreo en Inicio. La notificación te indica que ImpulSOS está escuchando el gesto.", "Puedes pausarlo desde Inicio o Sensor.", Icons.Default.Notifications),
    TutorialStep("Aprende el doble tajo", "Sujeta el teléfono con firmeza. Haz dos movimientos cortos hacia el mismo lado.", "Entre tajos, vuelve suavemente al inicio y deja cerca de medio segundo.", null),
    TutorialStep("Conoce la pantalla de emergencia", "Cuando se reconoce el gesto con monitoreo activo, ImpulSOS muestra tu información y reproduce una alarma local.", "En la simulación puedes recorrer la pantalla sin activar la alarma.", Icons.Default.Warning),
    TutorialStep("Finaliza la emergencia", "Mantén pulsado el botón Mantén 5 s para finalizar. Si sueltas antes, se cancela la acción.", "Puedes volver a esta guía desde Ajustes.", Icons.Default.CheckCircle),
)

@Composable
fun TutorialScreen(
    startAtGesture: Boolean,
    sensitivity: ShakeSensitivity,
    onSimulation: () -> Unit,
    onExit: () -> Unit,
    onFinish: () -> Unit,
) {
    val firstStep = if (startAtGesture) GESTURE_STEP else 0
    var index by rememberSaveable(startAtGesture) { mutableIntStateOf(firstStep) }
    var practicing by rememberSaveable { mutableStateOf(false) }
    val step = tutorialSteps[index]

    fun previous() {
        if (practicing) practicing = false
        else if (index > firstStep) index--
        else onExit()
    }

    BackHandler(onBack = ::previous)
    if (practicing) {
        DetectorDiagnosticScreen(onBack = { practicing = false }, sensitivity = sensitivity, guided = true)
        return
    }

    key(index) {
        val colors = MaterialTheme.colorScheme
        Column(
            Modifier.fillMaxSize().background(colors.background)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Paso ${index + 1} de ${tutorialSteps.size}", color = colors.onSurfaceVariant)
                TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 52.dp)) { Text("Salir de la guía") }
            }
            LinearProgressIndicator(
                progress = (index + 1f) / tutorialSteps.size,
                modifier = Modifier.fillMaxWidth(),
                trackColor = colors.surfaceVariant,
            )
            Text(step.title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            Text(step.body, style = MaterialTheme.typography.bodyLarge)

            if (index == GESTURE_STEP) {
                GestureDemonstration()
            } else {
                val isEmergencyStep = index == SIMULATION_STEP
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEmergencyStep) colors.errorContainer else colors.primaryContainer,
                    ),
                ) {
                    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                        step.icon?.let {
                            Icon(
                                it,
                                contentDescription = null,
                                tint = if (isEmergencyStep) colors.onErrorContainer else colors.onPrimaryContainer,
                                modifier = Modifier.size(60.dp),
                            )
                        }
                    }
                }
            }
            Text(step.hint, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (index == GESTURE_STEP || index == SIMULATION_STEP) {
                OutlinedButton(
                    onClick = if (index == GESTURE_STEP) ({ practicing = true }) else onSimulation,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) {
                    Text(if (index == GESTURE_STEP) "Practicar sin alarma" else "Ver simulación de emergencia")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (index > firstStep) {
                    OutlinedButton(onClick = ::previous, modifier = Modifier.weight(1f).heightIn(min = 54.dp)) { Text("Atrás") }
                }
                Button(
                    onClick = { if (index == tutorialSteps.lastIndex) onFinish() else index++ },
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                ) {
                    Text(if (index == tutorialSteps.lastIndex) "Terminar" else "Siguiente")
                }
            }
        }
    }
}
