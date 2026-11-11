package com.impulsos.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.app.Activity
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.media.AudioManager
import android.media.ToneGenerator
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.impulsos.app.data.MedicalProfile
import com.impulsos.app.data.ProfileRepository
import com.impulsos.app.data.EmergencyContact
import com.impulsos.app.data.EmergencyPhotoRepository
import com.impulsos.app.domain.ShakeSensitivity
import com.impulsos.app.domain.EmergencyContactRules
import com.impulsos.app.emergency.EmergencyActivity
import com.impulsos.app.sensor.MonitoringService
import com.impulsos.app.ui.ImpulSOSTheme
import com.impulsos.app.ui.AppAppearance
import com.impulsos.app.ui.WelcomeScreen
import com.impulsos.app.ui.TutorialScreen
import com.impulsos.app.ui.SpeechPreferences
import com.impulsos.app.ui.SpeechPreferenceStore
import com.impulsos.app.ui.SpeechSettingsScreen
import com.impulsos.app.ui.DetectorDiagnosticScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("impulsos-ui", MODE_PRIVATE)
        setContent {
            var appearance by remember {
                val stored = preferences.getString("appearance", null)
                mutableStateOf(AppAppearance.entries.firstOrNull { it.name == stored } ?: AppAppearance.SYSTEM)
            }
            val dark = appearance == AppAppearance.DARK ||
                (appearance == AppAppearance.SYSTEM && isSystemInDarkTheme())
            ImpulSOSTheme(darkTheme = dark) {
                val barColor = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    window.statusBarColor = barColor
                    window.navigationBarColor = barColor
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ImpulSOSApp(appearance) { selected ->
                        appearance = selected
                        preferences.edit().putString("appearance", selected.name).apply()
                    }
                }
            }
        }
    }
}

private enum class Destination { HOME, PROFILE, MESSAGE, MEDICATIONS, CONTACTS, SENSOR, SETTINGS, TUTORIAL }
private enum class TutorialEntry { FIRST_SETUP, SETTINGS, SENSOR }

@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun ImpulSOSApp(appearance: AppAppearance, onAppearanceChange: (AppAppearance) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("impulsos-ui", android.content.Context.MODE_PRIVATE) }
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding-complete", false)) }
    var welcomeSeen by remember { mutableStateOf(prefs.getBoolean("welcome-seen", false)) }
    var initializing by remember { mutableStateOf(true) }
    var tutorialEntry by rememberSaveable { mutableStateOf(TutorialEntry.SETTINGS) }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var profile by remember { mutableStateOf(MedicalProfile()) }
    var photoRevision by remember { mutableStateOf(0) }
    var monitoring by remember { mutableStateOf(MonitoringService.isActive(context)) }
    var pendingGalleryPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cameraOutputPath by rememberSaveable { mutableStateOf<String?>(null) }
    var sensitivity by remember {
        mutableStateOf(
            ShakeSensitivity.fromStorage(
                context.getSharedPreferences(MonitoringService.PREFS, android.content.Context.MODE_PRIVATE)
                    .getString(MonitoringService.KEY_SENSITIVITY, null),
            ),
        )
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        profile = withContext(Dispatchers.IO) { runCatching { ProfileRepository.load(context) }.getOrDefault(MedicalProfile()) }
        initializing = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            monitoring = MonitoringService.isActive(context)
            delay(700L)
        }
    }

    fun openSimulation() {
        context.startActivity(Intent(context, EmergencyActivity::class.java).putExtra(EmergencyActivity.EXTRA_SIMULATION, true))
    }

    fun openTutorial(entry: TutorialEntry) {
        tutorialEntry = entry
        destination = Destination.TUTORIAL
    }

    fun endTutorial(completed: Boolean) {
        destination = when (tutorialEntry) {
            TutorialEntry.FIRST_SETUP -> if (completed) Destination.PROFILE else Destination.HOME
            TutorialEntry.SETTINGS -> Destination.SETTINGS
            TutorialEntry.SENSOR -> Destination.SENSOR
        }
    }

    fun saveProfile(updated: MedicalProfile, message: String = "Cambios guardados en este dispositivo.") {
        profile = updated
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { ProfileRepository.save(context, updated) } }
            snackbar.showSnackbar(if (saved.isSuccess) message else "No se pudo guardar. Revisa el almacenamiento del dispositivo.")
        }
    }

    fun setMonitoring(enabled: Boolean) {
        val sensor = (context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager)
            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (enabled && sensor == null) {
            scope.launch { snackbar.showSnackbar("Este dispositivo no informa un acelerómetro disponible.") }
            return
        }
        val intent = Intent(context, MonitoringService::class.java).setAction(
            if (enabled) MonitoringService.ACTION_START else MonitoringService.ACTION_STOP,
        )
        if (enabled) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
        monitoring = enabled
    }

    fun setSensitivity(value: ShakeSensitivity) {
        context.getSharedPreferences(MonitoringService.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(MonitoringService.KEY_SENSITIVITY, value.storageValue).apply()
        sensitivity = value
        if (MonitoringService.isActive(context)) {
            context.startService(Intent(context, MonitoringService::class.java).setAction(MonitoringService.ACTION_SET_SENSITIVITY))
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pendingGalleryPhoto = uri }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val output = cameraOutputPath?.let(::File)
        if (saved && output != null && output.isFile) {
            pendingGalleryPhoto = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
        } else {
            output?.delete() // cancellation keeps any existing saved photo intact.
            cameraOutputPath = null
        }
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            val selected = runCatching {
                context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) EmergencyContact(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty()) else null }
            }.getOrNull()
            if (selected != null && selected.phone.isNotBlank()) {
                val duplicate = profile.contacts.any { it.phone.filter(Char::isDigit) == selected.phone.filter(Char::isDigit) }
                if (duplicate) scope.launch { snackbar.showSnackbar("Ese número ya está en la lista.") }
                else saveProfile(profile.copy(contacts = profile.contacts + selected.copy(name = selected.name.ifBlank { "Contacto" })), "Contacto guardado en este dispositivo.")
            } else scope.launch { snackbar.showSnackbar("No se pudo leer el número. Puedes introducirlo manualmente.") }
        }
    }

    if (initializing) {
        WelcomeScreen(initializing = true, onContinue = {})
        return
    }
    if (!welcomeSeen) {
        WelcomeScreen(initializing = false, onContinue = {
            prefs.edit().putBoolean("welcome-seen", true).apply()
            welcomeSeen = true
            if (!onboardingDone) destination = Destination.HOME
        })
        return
    }
    if (!onboardingDone) {
        OnboardingScreen(onContinue = {
            prefs.edit().putBoolean("onboarding-complete", true).apply()
            onboardingDone = true
            openTutorial(TutorialEntry.FIRST_SETUP)
        })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (destination !in setOf(Destination.MESSAGE, Destination.MEDICATIONS, Destination.CONTACTS, Destination.TUTORIAL)) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface), horizontalArrangement = Arrangement.SpaceEvenly) {
                NavItem("Inicio", Icons.Default.Home, destination == Destination.HOME) { destination = Destination.HOME }
                NavItem("Perfil", Icons.Default.Person, destination == Destination.PROFILE) { destination = Destination.PROFILE }
                NavItem("Sensor", Icons.Default.Settings, destination == Destination.SENSOR) { destination = Destination.SENSOR }
                NavItem("Ajustes", Icons.Default.Settings, destination == Destination.SETTINGS) { destination = Destination.SETTINGS }
            }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = destination,
            modifier = Modifier.padding(padding),
            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
            label = "main-navigation",
        ) { screen ->
        when (screen) {
            Destination.HOME -> HomeScreen(
                profile, monitoring, Modifier,
                onMonitoring = ::setMonitoring,
                onSimulation = ::openSimulation,
                onProfile = { destination = Destination.PROFILE },
                onMessage = { destination = Destination.MESSAGE },
                onMedications = { destination = Destination.MEDICATIONS },
                onContacts = { destination = Destination.CONTACTS },
            )
            Destination.PROFILE -> ProfileScreen(profile, Modifier, onSave = { saveProfile(it) })
            Destination.MESSAGE -> MessageScreen(profile, photoRevision, Modifier, onPickPhoto = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, onTakePhoto = {
                runCatching {
                    val cameraDir = File(context.cacheDir, "impulsos-camera").apply { mkdirs() }
                    val file = File.createTempFile("capture-", ".jpg", cameraDir)
                    cameraOutputPath = file.absolutePath
                    cameraCapture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                }.onFailure { cameraOutputPath?.let { File(it).delete() }; cameraOutputPath = null; scope.launch { snackbar.showSnackbar("No se pudo abrir la cámara disponible.") } }
            }, onRemovePhoto = {
                EmergencyPhotoRepository.delete(context)
                photoRevision++
                saveProfile(profile.copy(emergencyPhotoSet = false), "Fotografía eliminada.")
            }, onSave = { saveProfile(profile.copy(instructions = it)) }, onBack = { destination = Destination.HOME })
            Destination.MEDICATIONS -> MedicationsScreen(profile, Modifier, onSave = { updated -> saveProfile(updated) }, onBack = { destination = Destination.HOME })
            Destination.CONTACTS -> ContactsScreen(profile.contacts, Modifier, onPick = {
                contactPicker.launch(
                    Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE),
                )
            }, onSave = { updated -> saveProfile(profile.copy(contacts = updated)) }, onBack = { destination = Destination.HOME })
            Destination.SENSOR -> SensorScreen(
                monitoring,
                (context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
                sensitivity, Modifier, ::setMonitoring, ::openSimulation, ::setSensitivity, { playAlarmPreview() },
                onTutorial = { openTutorial(TutorialEntry.SENSOR) },
            )
            Destination.SETTINGS -> SettingsScreen(
                Modifier, appearance, onAppearanceChange, ::openSimulation,
                onTutorial = { openTutorial(TutorialEntry.SETTINGS) },
            )
            Destination.TUTORIAL -> TutorialScreen(
                startAtGesture = tutorialEntry == TutorialEntry.SENSOR,
                sensitivity = sensitivity,
                onSimulation = ::openSimulation,
                onExit = { endTutorial(completed = false) },
                onFinish = { endTutorial(completed = true) },
            )
        }
        }
    }
    pendingGalleryPhoto?.let { uri ->
        var preview by remember(uri) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(uri) { preview = withContext(Dispatchers.IO) { EmergencyPhotoRepository.preview(context, uri) } }
        AlertDialog(
            onDismissRequest = { pendingGalleryPhoto = null; cameraOutputPath?.let { File(it).delete() }; cameraOutputPath = null },
            title = { Text("Vista previa de la fotografía") },
            text = { Column { preview?.let { Image(it.asImageBitmap(), contentDescription = "Vista previa antes de guardar", modifier = Modifier.fillMaxWidth().height(220.dp)) } ?: Text("No se pudo abrir esta imagen.") } },
            confirmButton = { TextButton(enabled = preview != null, onClick = {
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { EmergencyPhotoRepository.save(context, uri) } }
                    if (result.isSuccess) {
                        photoRevision++
                        saveProfile(profile.copy(emergencyPhotoSet = true), "Fotografía guardada de forma cifrada.")
                    }
                    else snackbar.showSnackbar("No se pudo guardar la fotografía.")
                    pendingGalleryPhoto = null
                    cameraOutputPath?.let { File(it).delete() }
                    cameraOutputPath = null
                }
            }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { pendingGalleryPhoto = null; cameraOutputPath?.let { File(it).delete() }; cameraOutputPath = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.impulsos_isotipo),
                contentDescription = "Símbolo ImpulSOS",
                modifier = Modifier.size(54.dp),
            )
            Text("ImpulSOS", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("Tu ayuda, siempre cerca", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Text("Prepara la información que quieres compartir y activa el monitoreo cuando estés listo.", fontSize = 16.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        InfoCard("Tu información permanece en el teléfono", "El perfil se guarda cifrado en el almacenamiento privado de esta aplicación.")
        Spacer(Modifier.height(12.dp))
        InfoCard("La función crítica trabaja sin internet", "La activación usa el acelerómetro y presenta localmente tus instrucciones guardadas.")
        Spacer(Modifier.height(28.dp))
        PrimaryAction("Comenzar configuración", onContinue)
        Spacer(Modifier.height(10.dp))
        Text("ImpulSOS orienta a quienes te acompañan. No sustituye atención médica profesional.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun HomeScreen(
    profile: MedicalProfile,
    monitoring: Boolean,
    modifier: Modifier,
    onMonitoring: (Boolean) -> Unit,
    onSimulation: () -> Unit,
    onProfile: () -> Unit,
    onMessage: () -> Unit,
    onMedications: () -> Unit,
    onContacts: () -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(8.dp)); PageTitle("Hola${profile.name.trim().takeIf(String::isNotEmpty)?.let { ", $it" } ?: ""}", "Tu plan de ayuda está listo para revisarse.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (monitoring) "MONITOREO ACTIVO" else "PROTECCIÓN CONFIGURADA", color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (monitoring) "Lista cuando hace falta" else "Prepara tu plan de ayuda", color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(if (monitoring) "El acelerómetro escucha el gesto de activación." else "El perfil permanece en este dispositivo.", color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { onMonitoring(!monitoring) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (monitoring) "Pausar monitoreo" else "Activar monitoreo", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { SummaryRow("Perfil médico", profile.conditions.ifBlank { if (profile.name.isBlank()) "Por completar" else "Sin condiciones registradas" }, Icons.Default.Person, onProfile) }
        item { SummaryRow("Mensaje de emergencia", profile.instructions.ifBlank { "Por preparar" }, Icons.Default.Person, onMessage) }
        item { SummaryRow("Medicamentos", profile.medications.ifBlank { "Sin registros añadidos" }, Icons.Default.Settings, onMedications) }
        item { SummaryRow("Contactos de emergencia", profile.contacts.size.let { if (it == 0) "Por añadir" else "$it contacto(s) guardado(s)" }, Icons.Default.Person, onContacts) }
        item {
            PrimaryAction("Probar pantalla de emergencia", onSimulation)
            Text("La simulación está identificada y no genera alarma ni activa el servicio.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp, start = 4.dp))
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SensorScreen(
    monitoring: Boolean,
    available: Boolean,
    sensitivity: ShakeSensitivity,
    modifier: Modifier,
    onMonitoring: (Boolean) -> Unit,
    onSimulation: () -> Unit,
    onSensitivity: (ShakeSensitivity) -> Unit,
    onAlarmTest: () -> Unit,
    onTutorial: () -> Unit,
) {
    var showDiagnostics by remember { mutableStateOf(false) }
    if (showDiagnostics) {
        DetectorDiagnosticScreen(onBack = { showDiagnostics = false }, sensitivity = sensitivity)
        return
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageTitle("Gesto de activación", "Gestiona el acelerómetro y prueba la interfaz.")
        InfoCard("Acelerómetro", if (available) "Disponible en este dispositivo." else "No se informa un acelerómetro disponible.")
        InfoCard("Gesto experimental", "Dos impulsos vigorosos relacionados dentro de una ventana breve. Comprueba el reconocimiento con Diagnóstico seguro.")
        PrimaryAction("Aprender el doble tajo", onTutorial)
        InfoCard("Calibración", "Los umbrales iniciales son heurísticos. Deben comprobarse con movimientos controlados en este Motorola.")
        OutlinedButton(onClick = { showDiagnostics = true }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Diagnóstico seguro del gesto") }
        Text("Sensibilidad", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        ShakeSensitivity.entries.forEach { option ->
            val detail = when (option) {
                ShakeSensitivity.MORE -> "Puede responder a movimientos menos intensos."
                ShakeSensitivity.BALANCED -> "Ajuste inicial equilibrado; requiere pruebas reales."
                ShakeSensitivity.LESS -> "Exige movimientos más intensos y puede no detectar algunos gestos."
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = sensitivity == option, onClick = { onSensitivity(option) }, role = Role.RadioButton)) {
                RadioButton(selected = sensitivity == option, onClick = null)
                Column {
                    Text(option.displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        Text("La selección no está calibrada clínicamente y no garantiza evitar activaciones accidentales.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(if (monitoring) "Monitoreo activo" else "Monitoreo pausado", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("La notificación persistente muestra el estado del servicio.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Switch(checked = monitoring, enabled = available, onCheckedChange = onMonitoring)
        }
        PrimaryAction("Probar pantalla (simulación)", onSimulation)
        OutlinedButton(onClick = onAlarmTest, modifier = Modifier.fillMaxWidth()) { Text("Probar tono (simulación)") }
        Text("Reproduce un tono local breve; no inicia una emergencia ni activa el monitoreo.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileScreen(profile: MedicalProfile, modifier: Modifier, onSave: (MedicalProfile) -> Unit) {
    var value by remember(profile) { mutableStateOf(profile) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(8.dp))
        PageTitle("Mi perfil", "Elige qué información quieres mostrar a quien te ayude.")
        ProfileField("Nombre", value.name, { value = value.copy(name = it) }, "Tu nombre", singleLine = true)
        ProfileField("Condiciones médicas", value.conditions, { value = value.copy(conditions = it) }, "Solo las que quieras compartir")
        ProfileField("Alergias", value.allergies, { value = value.copy(allergies = it) }, "Escribe tus alergias o deja vacío")
        ProfileField("Acciones que deben evitarse", value.avoid, { value = value.copy(avoid = it) }, "Instrucciones personales")
        ProfileField("Información adicional", value.additionalInfo, { value = value.copy(additionalInfo = it) }, "Opcional")
        Text("Los datos no salen de esta aplicación. Escribe indicaciones revisadas por ti y tu profesional de salud.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        PrimaryAction("Guardar perfil", { onSave(value) })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MessageScreen(profile: MedicalProfile, photoRevision: Int, modifier: Modifier, onPickPhoto: () -> Unit, onTakePhoto: () -> Unit, onRemovePhoto: () -> Unit, onSave: (String) -> Unit, onBack: () -> Unit) {
    var instructions by remember(profile.instructions) { mutableStateOf(profile.instructions) }
    val context = LocalContext.current
    val photo by produceState<Bitmap?>(initialValue = null, profile.emergencyPhotoSet, photoRevision) {
        value = null
        if (profile.emergencyPhotoSet) {
            value = withContext(Dispatchers.IO) { EmergencyPhotoRepository.load(context) }
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        PageTitle("Mensaje de emergencia", "Prepara instrucciones breves para quien te acompañe.")
        ProfileField("Instrucciones personales", instructions, { instructions = it }, "Escribe solo indicaciones que hayas elegido.", minLines = 5)
        photo?.let { loadedPhoto -> Image(loadedPhoto.asImageBitmap(), contentDescription = "Fotografía guardada para la pantalla de emergencia", modifier = Modifier.fillMaxWidth().height(180.dp)) }
        if (profile.emergencyPhotoSet && photo == null) Text("La fotografía guardada no se pudo abrir. Puedes reemplazarla.", color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) { Text(if (profile.emergencyPhotoSet) "Reemplazar fotografía" else "Añadir fotografía (opcional)") }
        OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) { Text("Tomar fotografía") }
        if (profile.emergencyPhotoSet) TextButton(onClick = onRemovePhoto) { Text("Eliminar fotografía") }
        Text("ImpulSOS no genera ni recomienda instrucciones médicas.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        PrimaryAction("Guardar mensaje", { onSave(instructions) })
    }
}

@Composable
private fun ContactsScreen(contacts: List<EmergencyContact>, modifier: Modifier, onPick: () -> Unit, onSave: (List<EmergencyContact>) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var additional by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        PageTitle("Contactos de emergencia", "Se guardan localmente. ImpulSOS no llama ni envía mensajes.")
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Elegir desde contactos del teléfono") }
        Text("O añade los datos manualmente", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it; error = "" }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(phone, { phone = it; error = "" }, label = { Text("Número telefónico") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        OutlinedTextField(relation, { relation = it }, label = { Text("Relación (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(additional, { additional = it }, label = { Text("Información adicional (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        PrimaryAction(if (editingIndex == null) "Guardar contacto" else "Guardar cambios") {
            val cleanName = name.trim()
            val cleanPhone = phone.trim()
            val validationError = EmergencyContactRules.validationError(cleanName, cleanPhone, contacts, editingIndex)
            if (validationError != null) error = validationError else {
                val candidate = EmergencyContact(cleanName, cleanPhone, relation.trim(), additional.trim())
                val updated = contacts.toMutableList()
                if (editingIndex == null) updated += candidate else updated[editingIndex!!] = candidate
                onSave(updated)
                name = ""; phone = ""; relation = ""; additional = ""; editingIndex = null; error = ""
            }
        }
        if (contacts.isEmpty()) InfoCard("Sin contactos", "Añade al menos una persona de confianza. Puedes omitir este paso.")
        contacts.forEachIndexed { index, contact ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(contact.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(contact.phone, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (contact.relation.isNotBlank()) Text(contact.relation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (contact.additionalInfo.isNotBlank()) Text(contact.additionalInfo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingIndex = index; name = contact.name; phone = contact.phone; relation = contact.relation; additional = contact.additionalInfo; error = "" }) { Text("Editar") }
                        TextButton(onClick = { onSave(contacts.filterIndexed { i, _ -> i != index }); if (editingIndex == index) { editingIndex = null; name = ""; phone = "" } }) { Text("Eliminar") }
                    }
                }
            }
        }
        Text("No introduzcas información que no quieras mostrar a quien te asista.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun MedicationsScreen(profile: MedicalProfile, modifier: Modifier, onSave: (MedicalProfile) -> Unit, onBack: () -> Unit) {
    var value by remember(profile) { mutableStateOf(profile) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Volver") }
        PageTitle("Mis medicamentos", "Registra lo que quieres que el testigo pueda identificar.")
        ProfileField("Medicamentos", value.medications, { value = value.copy(medications = it) }, "Nombre e información que quieras compartir", minLines = 3)
        ProfileField("Medicamento de rescate", value.rescueMedication, { value = value.copy(rescueMedication = it) }, "Nombre y dónde está guardado", minLines = 2)
        Text("No añadas dosis ni indicaciones que no hayan sido revisadas por tu profesional de salud.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        PrimaryAction("Guardar medicamentos", { onSave(value) })
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    appearance: AppAppearance,
    onAppearanceChange: (AppAppearance) -> Unit,
    onSimulation: () -> Unit,
    onTutorial: () -> Unit,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        PageTitle("Ajustes", "Preferencias y límites del dispositivo.")
        PrimaryAction("Ver tutorial", onTutorial)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp).selectableGroup()) {
                Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                AppAppearance.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(
                            selected = appearance == option,
                            onClick = { onAppearanceChange(option) },
                            role = Role.RadioButton,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = appearance == option, onClick = null)
                        Text(option.label)
                    }
                }
            }
        }
        InfoCard("Privacidad", "Perfil cifrado y guardado en almacenamiento privado. No se solicita acceso a internet.")
        InfoCard("Pantalla de emergencia", "Android puede mostrar la actividad sobre el bloqueo; esta función no desbloquea el teléfono ni elimina la autenticación.")
        InfoCard("Monitoreo", "El servicio solo escucha el acelerómetro cuando lo activas. Android y el fabricante pueden limitar su ejecución en segundo plano.")
        SpeechSettingsScreen()
        OutlinedButton(onClick = onSimulation, modifier = Modifier.fillMaxWidth()) { Text("Abrir simulación identificada") }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, singleLine: Boolean = false, minLines: Int = 2) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    )
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SummaryRow(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun playAlarmPreview() {
    val tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 70) }.getOrNull() ?: return
    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1_000)
    Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 1_100L)
}
