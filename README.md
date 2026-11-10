# ImpulSOS

Aplicación Android nativa que mantiene en el teléfono información médica e instrucciones elegidas por la persona y permite activar una pantalla local de emergencia mediante un gesto de sacudida cuando el usuario ha iniciado el monitoreo.

> ImpulSOS es un prototipo de asistencia. No sustituye los servicios de emergencia, la evaluación clínica ni un dispositivo médico validado. La detección actual es heurística y no ha sido validada clínicamente.

## Funciones actuales

- Perfil médico cifrado localmente con Android Keystore.
- Inicio y parada explícitos del servicio foreground que escucha el acelerómetro.
- Tres preajustes de sensibilidad con umbrales heurísticos.
- Notificación persistente, alarma sonora local y pantalla de emergencia.
- Pantalla de simulación identificada, que no activa servicio ni alarma.
- Bienvenida inicial de una sola vez y tutorial repetible desde Ajustes.
- Instrucciones con selección opcional mediante Android Photo Picker o captura de resolución completa delegada a la cámara del sistema mediante `ActivityResultContracts.TakePicture` y `FileProvider` no exportado, limitado a una ruta de caché privada. Ambos recorridos muestran vista previa y confirmación; la copia final se orienta según EXIF, reduce y cifra con AES/GCM antes de guardarse en `noBackupFilesDir`. Cancelar conserva la foto anterior y limpia la captura temporal.
- Contactos manuales y selección puntual desde la pantalla de contactos de Android. No hay permiso general READ_CONTACTS.
- Lectura automática opcional de instrucciones mediante TextToSpeech del dispositivo, con preferencias de voz/idioma disponibles, velocidad, tono, orden y campos leídos.
- La Activity de emergencia muestra todos los contactos guardados y la foto si existe; TTS tiene acciones de detener y repetir.
- Validación básica de contactos y semántica de selección accesible. La auditoría manual de TalkBack sigue pendiente.
- Consultar [auditoría V2](docs/AUDITORIA_V2.md), [plan de pruebas](docs/PLAN_DE_PRUEBAS.md) y [evaluación de IA/estándares](docs/EVALUACION_IA_Y_REFERENCIAS.md) para separar implementación y pruebas pendientes.
- Desde Sensores se puede abrir **Diagnóstico seguro**: sensor físico sin acciones de emergencia y una señal sintética independiente; ambos muestran componentes XYZ, magnitud, impulsos e intervalo.
- El tutorial visual tiene diez pasos y puede repetirse desde Ajustes.

## Arquitectura

- `app/src/main/java/com/impulsos/app/MainActivity.kt`: navegación y pantallas Compose.
- `data/`: modelo y persistencia cifrada del perfil.
- `domain/ShakeDetectorLogic.kt`: lógica heurística independiente del sensor Android.
- `sensor/MonitoringService.kt`: servicio foreground y alarma.
- `emergency/EmergencyActivity.kt`: pantalla sobre bloqueo, información y desactivación por pulsación prolongada.
- `ui/`: tema visual.
- `docs/`: diseño, requisitos, riesgos y validaciones.

## Tecnologías y requisitos

- Kotlin 1.9.10, Jetpack Compose (UI/animation 1.5.3), Material 3 1.1.2.
- Gradle Wrapper 8.0, Android Gradle Plugin 8.1.4, Java 17.
- SDK Android con plataforma 34 y Platform Tools/ADB.
- `minSdk 26`; validación primaria en Motorola one físico Android 10/API 29. No usar emulador.

## Preparación y compilación en PowerShell

Configura Java 17 y el SDK instalado. Los siguientes valores reflejan la estación de desarrollo auditada y podrían ser distintos en otro equipo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Para instalar en el Motorola con depuración USB autorizada:

```powershell
adb devices
.\gradlew.bat installDebug
adb shell monkey -p com.impulsos.app 1
```

No hay dependencia de Gradle global ni de emuladores. Los datos de perfil no se deben usar para pruebas reales.

## Privacidad, red y permisos

La versión base no solicita `INTERNET`, ubicación, contactos, almacenamiento, llamada ni SMS. El perfil se cifra con AES/GCM; `allowBackup` está desactivado. El monitoreo solo se inicia tras acción del usuario y Android/fabricante pueden detenerlo o limitarlo. La alarma y datos de emergencia son locales; no se envían comunicaciones automáticas.

El selector de documentos importa una copia local; no se guarda permiso URI persistente ni se usa permiso de galería. El selector de contactos devuelve un URI elegido por la persona; su acceso y concesión deben verificarse en cada proveedor Android. No se declara acceso general a toda la agenda y no se solicita READ_CONTACTS.

## Voz, accesibilidad e IA

TalkBack de Android es el lector de pantalla compatible; no se implementa uno propio. La lectura TTS de instrucciones es una función separada y opcional. La pantalla muestra si las voces instaladas requieren red; disponibilidad y uso offline dependen del motor del teléfono. No se ha completado una auditoría TalkBack.

No hay integración de IA. La IA no debe detectar emergencias, producir recomendaciones clínicas ni recibir datos de salud. La evaluación comparativa, limitaciones y fuentes están en docs/EVALUACION_IA_Y_REFERENCIAS.md; no hay claves en el cliente.

## Diseño y seguridad

La identidad visual sigue Penpot: violeta `#5553D7`, lima `#BEF54D`, fondo `#F5F6FA`, rojo `#C62D42`, Roboto. Los diseños originales no se modifican sin autorización. Las referencias ISO/IEC/WCAG/APCA no implican certificación ni cumplimiento regulatorio. Consulte `AGENTS.md` para las reglas permanentes.

## Pruebas, límites y trabajo pendiente

Los resultados de compilación, pruebas unitarias y teléfono deben anotarse en [Plan de pruebas](docs/PLAN_DE_PRUEBAS.md). Se ejecutaron pruebas de unidad y flujos de bienvenida/tutorial/contacto manual/TTS en el Motorola. Foto seleccionada, agenda, TalkBack, lectura automática durante emergencia, detección física, full-screen intent en bloqueo, Doze, voz audible, latencia, contraste medido y batería siguen pendientes. No anunciar una función como terminada hasta probar su comportamiento real.

La instalación V2 se verifica comparando hash/versión del APK, `adb install -r` con salida `Success` y `lastUpdateTime`. Si el comando se queda esperando, revisar Package Manager; la transferencia del archivo por sí sola no prueba que la instalación terminó.

Inventarios recientes: [Penpot](docs/INVENTARIO_PENPOT.md), [Skills](docs/INVENTARIO_SKILLS.md). Las pruebas de señal simulada no cuentan como calibración física. El diagnóstico no activa la alarma ni la Activity de emergencia. La versión recién compilada está instalada y abre en el Motorola (versionCode 1, versionName 0.1.0; `lastUpdateTime` 2026-11-07 14:31:39). En la validación actual, el Photo Picker de Android 10 está abierto para importar `/sdcard/Download/medicamento.jpeg`; su interfaz no expone jerarquía a ADB, así que selección, confirmación, persistencia y visualización siguen pendientes de completar desde la pantalla del teléfono.


## Validación física reciente (2026-11-07)

La APK `0.1.0` (versionCode 1) se recompiló, verificó e instaló con `adb install -r` en el Motorola one/API 29; la instalación terminó en `Success` y `lastUpdateTime` cambió a 14:31:39. Se ejecutaron 17 pruebas unitarias (13 del detector y 4 de contactos), todas aprobadas. La selección real de `medicamento.jpeg` fue guardada; el archivo cifrado persistió después de cerrar y reabrir el proceso y su etiqueta accesible apareció en el editor y en la simulación de emergencia. La señal sintética de diagnóstico aceptó seis patrones; esa prueba no valida movimientos físicos. La cámara, el reemplazo/eliminación y la lectura física quedan pendientes; el diagnóstico bloquea el sensor mientras el estado normal aparece activo. Consulta [estado detallado](docs/ESTADO_DEL_PROYECTO.md), [plan de pruebas](docs/PLAN_DE_PRUEBAS.md) y [inventario de Skills](docs/INVENTARIO_SKILLS.md).

## Respuesta de emergencia y carga de fotografía

La información de perfil de `EmergencyActivity` y la foto cifrada se cargan en segundo plano después de componer la pantalla inicial. La lectura de la foto en el editor también usa `Dispatchers.IO`; la foto es opcional y no debe bloquear el primer render de la emergencia. La versión optimizada compila, conserva las 17 pruebas unitarias y quedó instalada (versionCode 1, versionName 0.1.0, `lastUpdateTime` 2026-11-07 15:01:28). Se abrió MainActivity y la EmergencyActivity de simulación; la foto apareció después de cargarla en segundo plano. La captura desde cámara se guardó y persistió en el Motorola; reemplazo por `medicamento.jpeg` está en curso y eliminación sigue pendiente.

## Validación actual tras corregir la foto — 2026-11-07

El editor de emergencia invalida y vuelve a cargar su bitmap al reemplazar o eliminar una foto, para no dejar una imagen antigua en memoria después del borrado. `testDebugUnitTest assembleDebug` pasó con 17 pruebas. La APK actual está instalada en el Motorola one/API 29 (versionCode 1, versionName 0.1.0; `lastUpdateTime` 2026-11-07 15:38:26); la instalación terminó con `adb install --no-streaming -r` → `Success`, sin desinstalar ni borrar los datos.

En esta APK se reabrió el editor con la foto persistida y se probó eliminarla: desapareció el blob cifrado privado y la interfaz cambió inmediatamente al estado sin foto. El Photo Picker quedó cancelado para pausar las pruebas; el perfil de prueba está ahora sin foto. La selección, persistencia tras relanzar y visualización de `medicamento.jpeg` en la simulación se confirmaron en la APK anterior de la misma iteración. La captura de cámara y su persistencia también se confirmaron en una APK anterior; repetir galería, simulación y cámara en la APK instalada a las 15:38 está pendiente.

La investigación de instalación encontró `dex2oat` trabajando sobre otras actualizaciones mientras Package Manager tardaba; el modo no-streaming finalizó correctamente. Esto indica trabajo concurrente, pero no prueba por sí solo la causa del retraso. No se borraron datos, no se desinstaló ImpulSOS y no se activó una emergencia real. Consulta el [estado del proyecto](docs/ESTADO_DEL_PROYECTO.md) y el [plan de pruebas](docs/PLAN_DE_PRUEBAS.md) antes de continuar.

## Continuación del dispositivo — 2026-11-10

La APK 0.1.0 (instalada el 7 de noviembre) seguía presente. Se encontró un blob de foto existente y se comprobó que el editor lo muestra. Después se probó la captura con cámara del sistema en esta APK: abrir cámara desde ImpulSOS, capturar, confirmar en la vista previa y guardar. La copia privada cifrada quedó en `no_backup` (65,110 bytes), el temporal de caché se eliminó y el blob persistió tras cerrar y volver a abrir la app. El editor mostró su descripción accesible de foto guardada.

Al 10 de noviembre, `testDebugUnitTest --rerun-tasks` pasó con 17 pruebas (13 detector, 4 contactos), sin fallos, errores ni omitidas; `assembleDebug` quedó actualizado. La APK conserva el hash `26DD36C94D5468F1D1BD43C8189E0833D8B9F45C2AFA5DD6F591BBF73C859AF7` y sigue instalada en el Motorola.

En la APK actual se repitió la captura de cámara, se confirmó y se guardó cifrada; persistió tras relanzar y apareció en simulación. Después se reimportó `medicamento.jpeg` con el Photo Picker real, se guardó cifrada, persistió tras relanzar y apareció también en la simulación. Cancelar la cámara externa con Atrás conservó la foto previa y limpió el temporal. La comprobación de borrar la foto se había superado antes en esta APK, incluida la actualización inmediata del editor. El trabajo restante es la calibración física más amplia del detector y las pruebas de accesibilidad, Doze, pantalla bloqueada, audio/latencia y batería. Estado e historial en [ESTADO_DEL_PROYECTO.md](docs/ESTADO_DEL_PROYECTO.md) y [PLAN_DE_PRUEBAS.md](docs/PLAN_DE_PRUEBAS.md).


