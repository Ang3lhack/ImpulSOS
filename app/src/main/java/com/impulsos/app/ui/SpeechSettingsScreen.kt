package com.impulsos.app.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SpeechSettingsScreen() {
    val context = LocalContext.current
    var prefs by remember { mutableStateOf(SpeechPreferenceStore.load(context)) }
    var ready by remember { mutableStateOf(false) }
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var voiceMenu by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) voices = engine?.voices?.sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired() }, { it.locale.displayName }, { it.name })).orEmpty()
        }
    }
    engine = tts
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown(); engine = null } }

    fun update(value: SpeechPreferences) {
        prefs = value
        SpeechPreferenceStore.save(context, value)
    }
    fun configure(engine: TextToSpeech, value: SpeechPreferences) {
        val selected = engine.voices?.firstOrNull { it.name == value.voiceName }
        if (selected != null) engine.voice = selected
        else engine.language = Locale.forLanguageTag(value.languageTag)
        engine.setSpeechRate(value.rate)
        engine.setPitch(value.pitch)
    }

    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lectura por voz en emergencia", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF191A32))
            Text("Usa TextToSpeech instalado en Android. No retrasa la pantalla visual y no comparte datos con ImpulSOS.", fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Leer automáticamente")
                Switch(checked = prefs.automatic, onCheckedChange = { update(prefs.copy(automatic = it)) })
            }
            Text(if (!ready) "Inicializando el motor de voz…" else if (voices.isEmpty()) "No se encontraron voces instaladas." else "Voz e idioma disponibles en este dispositivo:")
            val selectedVoice = voices.firstOrNull { it.name == prefs.voiceName } ?: if (ready) tts.defaultVoice else null
            if (voices.isNotEmpty()) {
                Column {
                    OutlinedButton(onClick = { voiceMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedVoice?.let { "${it.locale.getDisplayName(Locale.forLanguageTag("es"))} · ${if (it.isNetworkConnectionRequired()) "requiere red" else "sin conexión"}" } ?: "Voz del sistema · ${prefs.languageTag}")
                    }
                    DropdownMenu(expanded = voiceMenu, onDismissRequest = { voiceMenu = false }) {
                        voices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text("${voice.locale.getDisplayName(Locale.forLanguageTag("es"))} · ${if (voice.isNetworkConnectionRequired()) "red" else "local"}") },
                                onClick = {
                                    update(prefs.copy(voiceName = voice.name, languageTag = voice.locale.toLanguageTag()))
                                    voiceMenu = false
                                },
                            )
                        }
                    }
                }
            }
            Text("Velocidad: ${"%.2f".format(Locale.ROOT, prefs.rate)}")
            Slider(value = prefs.rate, onValueChange = { update(prefs.copy(rate = it)) }, valueRange = 0.65f..1.5f)
            Text("Tono: ${"%.2f".format(Locale.ROOT, prefs.pitch)}")
            Slider(value = prefs.pitch, onValueChange = { update(prefs.copy(pitch = it)) }, valueRange = 0.7f..1.4f)
            Text("Orden de lectura", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = prefs.instructionsFirst, onClick = { update(prefs.copy(instructionsFirst = true)) }, role = androidx.compose.ui.semantics.Role.RadioButton)) {
                androidx.compose.material3.RadioButton(selected = prefs.instructionsFirst, onClick = null)
                Text("Instrucciones primero")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = !prefs.instructionsFirst, onClick = { update(prefs.copy(instructionsFirst = false)) }, role = androidx.compose.ui.semantics.Role.RadioButton)) {
                androidx.compose.material3.RadioButton(selected = !prefs.instructionsFirst, onClick = null)
                Text("Otros datos primero")
            }
            Text("Información que se leerá", fontWeight = FontWeight.SemiBold)
            SpeechCheck("Nombre", prefs.readName) { update(prefs.copy(readName = it)) }
            SpeechCheck("Condiciones", prefs.readConditions) { update(prefs.copy(readConditions = it)) }
            SpeechCheck("Alergias", prefs.readAllergies) { update(prefs.copy(readAllergies = it)) }
            SpeechCheck("Medicamentos", prefs.readMedications) { update(prefs.copy(readMedications = it)) }
            SpeechCheck("Contactos de emergencia", prefs.readContacts) { update(prefs.copy(readContacts = it)) }
            SpeechCheck("Qué se debe evitar", prefs.readAvoid) { update(prefs.copy(readAvoid = it)) }
            SpeechCheck("Información adicional", prefs.readAdditionalInfo) { update(prefs.copy(readAdditionalInfo = it)) }
            Button(onClick = {
                if (ready) {
                    configure(tts, prefs)
                    tts.speak("Esta es una prueba de lectura de ImpulSOS.", TextToSpeech.QUEUE_FLUSH, null, "impulsos-tts-preview")
                }
            }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Escuchar prueba") }
            Text("Las voces marcadas ‘requiere red’ pueden no funcionar sin conexión. El núcleo de emergencia no depende del motor de voz.", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SpeechCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}
