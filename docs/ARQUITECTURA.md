# Arquitectura

## Capas

- **UI:** Jetpack Compose para bienvenida de primera apertura, onboarding, tutorial repetible, inicio, perfil, mensaje/foto, medicamentos, contactos, sensor, ajustes y modo de emergencia. La navegación usa un desvanecido corto y progreso animado del tutorial.
- **Dominio:** `ShakeDetectorLogic`, independiente de Android, con configuración y estado temporal propios.
- **Servicios Android:** `MonitoringService` registra y libera el acelerómetro tras una acción explícita. Usa una notificación persistente y una notificación de emergencia de prioridad alta con pantalla completa para solicitar la presentación desde segundo plano.
- **Datos:** `ProfileRepository` cifra perfil, instrucciones y contactos con AES/GCM y una clave Android Keystore. `EmergencyPhotoRepository` importa, redimensiona y cifra la foto con clave AES/GCM separada. Ambos quedan en `noBackupFilesDir`; `allowBackup=false`.
- **Voz:** `TextToSpeech` del sistema se inicializa de forma asíncrona en la Activity de emergencia; la pantalla no espera al motor. Las preferencias locales incluyen voz/idioma, velocidad, tono, orden y campos incluidos.

## Flujo de emergencia

Monitoreo iniciado por la persona → lecturas del acelerómetro → filtro de gravedad y validación temporal → notificación local de emergencia y alarma → `EmergencyActivity` sobre pantalla bloqueada cuando Android lo permite → perfil e instrucciones locales → pulsación sostenida de cinco segundos para finalizar.

Si se configuró TTS, la lectura empieza cuando el motor del sistema está listo; detener o repetir está disponible en pantalla. Si falta idioma/voz o la voz requiere una conexión no disponible, la presentación visual sigue siendo la fuente local.

La notificación de pantalla completa usa las APIs públicas de Android y no elimina la autenticación del teléfono. El servicio no declara reinicio permanente. No se añaden llamadas automáticas, SMS, GPS, red, analítica ni nube.

## Persistencia y privacidad

## Captura inercial 0.1.1

`MotionSensorPolicy` centraliza selección wake-up y entrega con FIFO para `MonitoringService` y diagnóstico; no comparte acciones de emergencia. La lógica pura conserva filtro de gravedad dependiente del tiempo, valida dos impulsos relacionados por dirección y tiempo y rechaza rebotes/vibraciones y caída libre. El diagnóstico limita publicaciones visuales a ~10 Hz, conserva la última métrica aceptada y libera su listener en ON_STOP. No se incorporan bibliotecas externas ni wake lock propio. Detalles y limitaciones: `ANALISIS_CHOP_CHOP.md`.

El perfil no se imprime en logs. La clave nunca se guarda en el repositorio. El modo de simulación muestra una etiqueta persistente y no inicia el servicio ni la alarma. La imagen se selecciona con Photo Picker y se copia cifrada sin conservar un URI externo. La agenda utiliza ACTION_PICK para seleccionar un número; no se solicita permiso general `READ_CONTACTS`.

## Guía y apariencia 0.1.7

`TutorialScreen` conserva el paso durante la recreación y devuelve al origen al salir. `GestureDemonstration` dibuja dos paneles en Compose y reproduce una secuencia local a petición. La práctica usa la rama guiada de `DetectorDiagnosticScreen`, que comparte la política del sensor y la sensibilidad seleccionada con el diagnóstico. La pantalla de práctica requiere una acción explícita para escuchar y libera el listener al salir o pasar a segundo plano.

`MainActivity` guarda la apariencia Sistema, Claro u Oscuro en las preferencias privadas `impulsos-ui`. Los colores normales provienen de `ImpulSOSTheme`. La actividad de emergencia y su simulación conservan la paleta de emergencia.
