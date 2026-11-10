package com.impulsos.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impulsos.app.R
import kotlinx.coroutines.delay

private val Ink = Color(0xFF191A32)
private val Violet = Color(0xFF5553D7)

@Composable
fun WelcomeScreen(initializing: Boolean, onContinue: () -> Unit) {
    var pulse by remember { mutableFloatStateOf(0.96f) }
    val scale by animateFloatAsState(pulse, label = "welcome-logo")
    LaunchedEffect(Unit) {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            pulse = 1f
            return@LaunchedEffect
        }
        while (true) {
            pulse = if (pulse < 1f) 1.02f else 0.97f
            delay(1_300L)
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F6FA)).verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.impulsos_isotipo), contentDescription = "Isotipo de ImpulSOS", modifier = Modifier.size(116.dp).scale(scale))
        Spacer(Modifier.height(18.dp))
        Text("ImpulSOS", color = Violet, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Tu información. A mano cuando importa.", color = Ink, fontSize = 18.sp)
        Spacer(Modifier.height(32.dp))
        if (initializing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Violet)
            Text("Preparando tu información local…", color = Ink, modifier = Modifier.padding(top = 12.dp))
        } else {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                Text("Toca para comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Funciona con información guardada en este dispositivo. No sustituye los servicios de emergencia.", color = Color(0xFF65677E), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

data class TutorialStep(val title: String, val body: String, val icon: String)

private val tutorialSteps = listOf(
    TutorialStep("ImpulSOS, ayuda local", "Prepara información elegida por ti para que esté a mano en una emergencia. No reemplaza servicios médicos.", "SOS"),
    TutorialStep("Completa tu perfil", "Añade tu nombre y solo las condiciones que quieras mostrar. La información se guarda cifrada en el teléfono.", "♥"),
    TutorialStep("Registra alergias y medicamentos", "Incluye alergias, medicamentos y dispositivo de rescate si los usas. Revisa que tus datos sigan vigentes.", "＋"),
    TutorialStep("Configura contactos", "Añade personas de confianza manualmente o elige una desde el selector del teléfono.", "♡"),
    TutorialStep("Escribe instrucciones", "Prepara frases breves con indicaciones elegidas por ti. ImpulSOS no genera consejos médicos.", "≡"),
    TutorialStep("Añade una foto opcional", "Puedes seleccionar o tomar una fotografía. Confirma la vista previa; se cifra y guarda solo en este dispositivo.", "▧"),
    TutorialStep("Activa el monitoreo", "Inícialo cuando estés listo. La notificación persistente indica que el acelerómetro está escuchando.", "●"),
    TutorialStep("Practica el gesto", "Dos impulsos vigorosos relacionados pueden activar el reconocimiento. Es experimental; usa Diagnóstico seguro para probarlo.", "↔"),
    TutorialStep("Qué ocurre al activarse", "Se muestra tu información local y comienza la alarma. No se envían llamadas, SMS ni ubicación.", "!"),
    TutorialStep("Finaliza con seguridad", "Mantén pulsado cinco segundos para finalizar. Si sueltas antes, la acción se cancela. Simulación no activa la alarma.", "✓"),
)

@Composable
fun TutorialScreen(onExit: () -> Unit, onFinish: () -> Unit) {
    var index by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val step = tutorialSteps[index]
    val progress by animateFloatAsState((index + 1f) / tutorialSteps.size, label = "tutorial-progress")
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F6FA)).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Tutorial", color = Violet, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Paso ${index + 1} de ${tutorialSteps.size}", color = Color(0xFF65677E), fontSize = 14.sp)
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), color = Violet)
        Card(Modifier.fillMaxWidth().height(210.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFECEBFF))) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(step.icon, color = Violet, fontSize = 88.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "Ilustración: ${step.title}" })
            }
        }
        Text(step.title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(step.body, color = Color(0xFF3D3E58), fontSize = 18.sp, lineHeight = 26.sp)
        Spacer(Modifier.weight(1f, fill = false))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f).height(54.dp)) { Text("Salir") }
            if (index > 0) OutlinedButton(onClick = { index-- }, modifier = Modifier.weight(1f).height(54.dp)) { Text("Atrás") }
            Button(onClick = { if (index == tutorialSteps.lastIndex) onFinish() else index++ }, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                Text(if (index == tutorialSteps.lastIndex) "Terminar" else "Siguiente")
            }
        }
    }
}
