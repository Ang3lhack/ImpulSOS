package com.impulsos.app.emergency

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.impulsos.app.data.MedicalProfile
import com.impulsos.app.data.ProfileRepository
import com.impulsos.app.data.EmergencyPhotoRepository
import com.impulsos.app.sensor.MonitoringService
import com.impulsos.app.ui.ImpulSOSTheme
import com.impulsos.app.ui.SpeechPreferenceStore
import com.impulsos.app.ui.SpeechPreferences
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
class EmergencyActivity : ComponentActivity() {
    private val simulation = mutableStateOf(false)
    private val screenProfile = mutableStateOf(MedicalProfile())
    private var previousWindowBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private var speechEngine: TextToSpeech? = null
    private var speechProfile: MedicalProfile = MedicalProfile()
    private var profileLoaded = false
    private var speechPreferences: SpeechPreferences = SpeechPreferences()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simulation.value = intent.getBooleanExtra(EXTRA_SIMULATION, false)
        prepareWindow()
        speechPreferences = SpeechPreferenceStore.load(this)
        setContent {
            ImpulSOSTheme {
                EmergencyScreen(screenProfile.value, simulation.value, speechPreferences.automatic,
                    onFinish = { finishEmergency() },
                    onStopSpeech = { speechEngine?.stop() },
                    onRepeatSpeech = { speakEmergencyText() })
                LaunchedEffect(Unit) {
                    val loaded = withContext(Dispatchers.IO) {
                        runCatching { ProfileRepository.load(this@EmergencyActivity) }.getOrDefault(MedicalProfile())
                    }
                    speechProfile = loaded
                    profileLoaded = true
                    screenProfile.value = loaded
                    if (!simulation.value && speechPreferences.automatic) initializeSpeech()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        simulation.value = intent.getBooleanExtra(EXTRA_SIMULATION, false)
        if (!simulation.value && profileLoaded && speechPreferences.automatic) initializeSpeech()
    }

    override fun onDestroy() {
        window.attributes = window.attributes.apply { screenBrightness = previousWindowBrightness }
        speechEngine?.stop()
        speechEngine?.shutdown()
        speechEngine = null
        super.onDestroy()
    }

    private fun initializeSpeech() {
        if (speechEngine != null) return
        speechEngine = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val engine = speechEngine ?: return@TextToSpeech
            val selected = engine.voices?.firstOrNull { it.name == speechPreferences.voiceName }
            if (selected != null) engine.voice = selected else engine.language = Locale.forLanguageTag(speechPreferences.languageTag)
            engine.setSpeechRate(speechPreferences.rate)
            engine.setPitch(speechPreferences.pitch)
            speakEmergencyText()
        }
    }

    private fun speakEmergencyText() {
        val p = speechProfile
        val s = speechPreferences
        val instructions = p.instructions.trim().takeIf { it.isNotBlank() }?.let { "Instrucciones. $it" }
        val other = buildList {
            if (s.readName && p.name.isNotBlank()) add("Nombre. ${p.name}.")
            if (s.readConditions && p.conditions.isNotBlank()) add("Condiciones. ${p.conditions}.")
            if (s.readAllergies && p.allergies.isNotBlank()) add("Alergias. ${p.allergies}.")
            if (s.readMedications) {
                p.rescueMedication.takeIf(String::isNotBlank)?.let { add("Medicamento de rescate. $it.") }
                p.medications.takeIf(String::isNotBlank)?.let { add("Medicamentos. $it.") }
            }
            if (s.readContacts) p.contacts.forEach { add("Contacto. ${it.name}. Teléfono. ${it.phone}.") }
            if (s.readContacts && p.contacts.isEmpty() && p.trustedContact.isNotBlank()) add("Contacto. ${p.trustedContact}.")
            if (s.readAvoid && p.avoid.isNotBlank()) add("Evitar. ${p.avoid}.")
            if (s.readAdditionalInfo && p.additionalInfo.isNotBlank()) add("Información adicional. ${p.additionalInfo}.")
        }.joinToString(" ").ifBlank { null }
        val pieces = if (s.instructionsFirst) listOfNotNull(instructions, other) else listOfNotNull(other, instructions)
        val chunks = splitForSpeech(pieces.joinToString(" "))
        chunks.forEachIndexed { index, chunk ->
            speechEngine?.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "impulsos-emergency-$index",
            )
        }
    }

    private fun splitForSpeech(text: String, maximum: Int = 3_500): List<String> {
        if (text.length <= maximum) return listOf(text)
        val result = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                result += current.toString()
                current.clear()
            }
        }
        text.split(Regex("(?<=[.!?])\\s+")).forEach { sentence ->
            if (current.isNotEmpty() && current.length + sentence.length + 1 > maximum) flush()
            if (sentence.length <= maximum) {
                if (current.isNotEmpty()) current.append(' ')
                current.append(sentence)
            } else {
                sentence.split(Regex("\\s+")).forEach { word ->
                    if (current.isNotEmpty() && current.length + word.length + 1 > maximum) flush()
                    if (word.length <= maximum) {
                        if (current.isNotEmpty()) current.append(' ')
                        current.append(word)
                    } else {
                        flush()
                        word.chunked(maximum).forEach(result::add)
                    }
                }
            }
        }
        flush()
        return result
    }

    private fun prepareWindow() {
        previousWindowBrightness = window.attributes.screenBrightness
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        window.statusBarColor = android.graphics.Color.rgb(198, 45, 66)
        window.navigationBarColor = android.graphics.Color.rgb(198, 45, 66)
        window.decorView.systemUiVisibility = 0
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (android.os.Build.VERSION.SDK_INT >= 26 && keyguard.isKeyguardLocked) {
            // Show over the lock screen; do not dismiss or bypass the user's device authentication.
            setShowWhenLocked(true)
        }
    }

    private fun finishEmergency() {
        window.attributes = window.attributes.apply { screenBrightness = previousWindowBrightness }
        if (!simulation.value) {
            startService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.ACTION_DISARM))
            getSystemService(android.app.NotificationManager::class.java)
                .cancel(4102)
        }
        finish()
    }

    companion object { const val EXTRA_SIMULATION = "simulation" }
}

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
private fun EmergencyScreen(profile: MedicalProfile, simulation: Boolean, speechEnabled: Boolean, onFinish: () -> Unit, onStopSpeech: () -> Unit, onRepeatSpeech: () -> Unit) {
    val red = Color(0xFFC62D42)
    val ink = Color(0xFF17182E)
    val context = LocalContext.current
    val photo by produceState<Bitmap?>(initialValue = null, profile.emergencyPhotoSet) {
        if (profile.emergencyPhotoSet) {
            value = withContext(Dispatchers.IO) { EmergencyPhotoRepository.load(context) }
        }
    }
    var held by remember { mutableFloatStateOf(0f) }
    var isPressing by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(isPressing) {
        if (isPressing) {
            val start = android.os.SystemClock.elapsedRealtime()
            while (isPressing) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - start
                held = (elapsed / 5_000f).coerceIn(0f, 1f)
                if (elapsed >= 5_000L) {
                    onFinish()
                    isPressing = false
                    break
                }
                delay(40L)
            }
        } else {
            held = 0f
        }
    }
    Surface(color = red, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(color = ink, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (simulation) "SIMULACIÓN · NO ES UNA EMERGENCIA REAL" else "EMERGENCIA MÉDICA",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            Text(profile.name.ifBlank { "Información médica" }.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            photo?.let { loadedPhoto -> Image(loadedPhoto.asImageBitmap(), contentDescription = "Fotografía de referencia elegida por la persona", modifier = Modifier.fillMaxWidth().height(132.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit) }
            if (profile.conditions.isNotBlank()) Text(profile.conditions, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            EmergencyInfo(profile.instructions.ifBlank { "No hay instrucciones personales guardadas." }, ink, Color.White)
            if (profile.avoid.isNotBlank()) EmergencyInfo("Evitar: ${profile.avoid}", Color.White, Color(0xFF951F31))
            if (profile.rescueMedication.isNotBlank() || profile.medications.isNotBlank()) {
                EmergencyInfo(
                    listOf(profile.rescueMedication, profile.medications).filter(String::isNotBlank).joinToString("\n"),
                    Color.White,
                    Color(0xFF951F31),
                )
            }
            if (profile.allergies.isNotBlank()) EmergencyInfo("Alergias: ${profile.allergies}", ink, Color.White)
            profile.contacts.forEach { contact ->
                val relation = contact.relation.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()
                EmergencyInfo("Contacto$relation: ${contact.name} · ${contact.phone}", ink, Color.White)
                if (contact.additionalInfo.isNotBlank()) EmergencyInfo(contact.additionalInfo, ink, Color.White)
            }
            if (profile.contacts.isEmpty() && profile.trustedContact.isNotBlank()) EmergencyInfo("Contacto: ${profile.trustedContact}", ink, Color.White)
            if (profile.additionalInfo.isNotBlank()) EmergencyInfo(profile.additionalInfo, ink, Color.White)
            if (!simulation && speechEnabled) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onRepeatSpeech, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ink)) { Text("Repetir lectura") }
                    Button(onClick = onStopSpeech, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ink)) { Text("Detener voz") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .semantics {
                    contentDescription = if (simulation) {
                        "Mantén presionado cinco segundos para salir de la simulación."
                    } else {
                        "Mantén presionado cinco segundos para finalizar la emergencia."
                    }
                    customActions = listOf(
                        CustomAccessibilityAction("Mantener cinco segundos para finalizar") {
                            isPressing = true
                            true
                        },
                    )
                }.pointerInput(simulation) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val start = android.os.SystemClock.elapsedRealtime()
                        var finished = false
                        while (!finished) {
                            val elapsed = android.os.SystemClock.elapsedRealtime() - start
                            held = (elapsed / 5_000f).coerceIn(0f, 1f)
                            if (elapsed >= 5_000L) {
                                onFinish()
                                finished = true
                            } else {
                                val event = withTimeoutOrNull(50L) { awaitPointerEvent() }
                                if (event?.changes?.none { it.pressed } == true) finished = true
                            }
                        }
                        held = 0f
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (simulation) "MANTÉN 5 S PARA SALIR" else "MANTÉN 5 S PARA FINALIZAR", color = red, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = held, modifier = Modifier.fillMaxWidth(), color = Color(0xFFBEF54D), trackColor = ink)
            Text(if (simulation) "Práctica · sin alarma ni llamadas" else "Mantén presionado durante cinco segundos", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmergencyInfo(text: String, foreground: Color, background: Color) {
    Surface(color = background, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = foreground, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
    }
}
