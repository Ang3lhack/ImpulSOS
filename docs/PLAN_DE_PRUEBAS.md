# Plan de pruebas

## Pruebas automatizadas del dominio

`ShakeDetectorLogicTest` usa secuencias sintéticas de aceleración para reposo, caminata, carrera, sacudida sostenida, impulso aislado, caída/impacto, vibración de alta frecuencia, pausa, reinicio de ventana y umbrales de los preajustes de sensibilidad.

## Pruebas en Motorola one (API 29)

Registrar resultado/limitaciones reales de cada una; no extrapolar las pruebas unitarias.

- Instalar y abrir; onboarding y navegación.
- Guardar, cerrar, abrir y editar perfil; confirmar que el archivo es cifrado y queda en almacenamiento privado.
- Iniciar/detener monitoreo; verificar notificación, listener y sensor liberado.
- Caminar, correr, movimiento cotidiano, transporte, vibración, golpe aislado y sacudida deliberada, en ensayos controlados.
- Confirmar activación, notificación, tono, pantalla encendida/sobre bloqueo y presentación de información local.
- Confirmar que soltar antes de cinco segundos cancela y completar cinco segundos finaliza.
- Probar sin conexión, reiniciar la aplicación y revisar logs para asegurar que no contienen datos médicos.
- Medir latencia de trigger a notificación/sonido/solicitud de pantalla y observar consumo básico.

## Estado actual

> Este bloque registra el estado previo al 14:31 del 2026-11-07 y es histórico. El registro posterior al final del documento supersede sus estados de prueba e instalación donde difieran.


### Iteración de diagnóstico/foto (2026-11-07)

- Detector: las pruebas existentes de detector (12) y contactos (4) pasan, 16/16 total. Se agregó el caso `deliberateShakeCanTriggerWithinTheRecognitionWindow`; no se agregaron casos de señal física porque deben medirse en el Motorola.
- Compilación: `testDebugUnitTest assembleDebug` termina `BUILD SUCCESSFUL`; APK debug generada.
- Diagnóstico seguro: implementado con `SensorEventListener` local y un recognizer independiente; lectura física y señal sintética no llaman al servicio de monitoreo, alarma ni EmergencyActivity. La UI no se ha validado en el teléfono todavía.
- Cámara/foto: Photo Picker muestra preview antes de guardar; cámara usa `TakePicture` con FileProvider privado, preview y confirmación. Ambos convergen al repositorio cifrado. No se ha validado el ciclo en dispositivo.
- Penpot: MCP consultó 13 páginas, estructura de tableros y exportó una vista PNG representativa por página en una inspección previa; inventario en `INVENTARIO_PENPOT.md`.
- Skills: 55 Skills locales documentadas en `INVENTARIO_SKILLS.md`; se revisaron las aplicables a Compose, cámara/URI, seguridad de permisos, pruebas y documentación. Skills de profiling requieren trazas físicas todavía no registradas.
- ADB/install de este APK: `adb devices` identifica Motorola one/API 29; la APK se transfiere a `/data/local/tmp`, pero `adb install --no-streaming -r` y `pm install -r` quedan esperando sin devolver estado ni actualizar `lastUpdateTime` (sigue en 2026-11-07 13:42:44). La versión nueva NO se considera instalada ni abierta.
- Foto de prueba: `/sdcard/Download/medicamento.jpeg` existe (19,937 bytes), pero el flujo nuevo no se ejecutó por instalación bloqueada. No se seleccionó ni copió al proyecto.

### Pendiente de la nueva iteración

- Resolver la espera de Package Manager en el Motorola sin borrar datos de la aplicación.
- Ejecutar la instalación y abrir la APK nueva; inspeccionar logs posteriores al arranque.
- Probar diagnóstico desde Ajustes/Sensor, registrar rango de timestamps reales, movimientos deliberados y cotidianos sin alarma. La tasa de muestreo que se solicita no es garantía de la recibida.
- Importar `medicamento.jpeg` con Photo Picker real, confirmar vista previa, verificar cifrado/persistencia tras relanzar y vista en emergencia de simulación; comprobar reemplazo/eliminación.
- Capturar desde cámara, confirmar imagen y verificar cancelación/reemplazo. ADB install bloqueado impide validar el flujo actual.
- TalkBack, escalado, contrastes, full-screen intent, Doze, batería y falsos positivos/negativos físicos siguen sin validación.

### Ejecutado

- `testDebugUnitTest`: **15 pruebas, 0 fallos, 0 errores, 0 omitidas**. Incluye los 11 casos del detector y 4 validaciones de contactos.
- `assembleDebug`: compilación exitosa con Gradle Wrapper 8.0/Java 17.
- `installDebug`: APK V2 instalada en Motorola one Android 10/API 29.
- Motorola one Android 10/API 29: instalación con `installDebug` y apertura de `MainActivity` confirmadas.
- Inicio, formulario de perfil y pantalla de sensor recorridos mediante ADB. Una captura temprana de jerarquía devolvió nodo raíz vacío; las jerarquías posteriores de ajustes, tutorial y contactos sí se pudieron consultar. El acelerómetro se reporta disponible.
- Bienvenida: se vio el CTA “Toca para comenzar” y se llegó a Inicio; al forzar cierre y abrir de nuevo no volvió a aparecer la bienvenida. No se borraron los datos existentes para probar el estado de primera instalación.
- Tutorial: abierto desde Ajustes; se recorrió el primer paso, avanzó al segundo y se salió. Los demás pasos están implementados, pero no se recorrieron todos en el dispositivo.
- Perfil temporal con el único valor ficticio `PRUEBA`: guardado, persistido tras cerrar/reabrir y no visible en el archivo cifrado. Después se vació y se confirmó que ya no reaparece.
- Monitoreo: el servicio apareció en `dumpsys activity services` como foreground con notificación persistente; tras detenerlo, ADB confirmó que no quedó servicio activo.
- Simulación: texto visible “SIMULACIÓN · NO ES UNA EMERGENCIA REAL”; la pantalla quedó abierta al soltar a los 2 s y volvió a Inicio al mantener 5,4 s.
- Tono de prueba: se activó el control de simulación y no apareció error de ejecución. La audibilidad/volumen no se midió objetivamente.
- Sensibilidad: la pantalla muestra las tres opciones, se seleccionó “Más sensible” y se confirmó por captura que continuó seleccionada al cambiar de pestaña. El servicio permaneció detenido. La calibración física de los niveles no se ha realizado.
- Contacto manual: se guardó un contacto sintético, se verificó persistencia tras cerrar/reabrir y se eliminó; se confirmó el estado vacío posterior. No quedó registro sintético.
- Ajustes TTS: se comprobó que el teléfono informa su motor/idioma y aparecen los controles. Se pulsó la prueba genérica sin error AndroidRuntime; no se midió audibilidad.
- Privacidad durante pruebas: el perfil ya existente en el teléfono contenía datos personales. No se modificó; se borraron capturas y jerarquías UI temporales que lo mostraban. Se eliminó el archivo geométrico de prueba de Descargas.

### Pendiente

- No se ejecutó un patrón físico de sacudida; no se han medido falsos positivos/negativos con caminar, correr, transporte, vibración o movimiento deliberado.
- No se probó el disparo real desde el servicio, el full-screen intent bajo bloqueo, ni el comportamiento con PIN/biometría.
- Brillo máximo solicitado, audibilidad, latencia total, consumo de batería, Doze y uso sin conectividad requieren medición adicional en el teléfono.
- No se introdujeron datos médicos reales. El valor ficticio de persistencia quedó borrado.
- No se seleccionó la foto sintética. La prueba inicial con OpenDocument abrió un panel general de ubicaciones; se sustituyó por AndroidX Photo Picker, que se abrió correctamente en el Motorola. Su jerarquía accesible no identificó el nombre del archivo sintético, así que se canceló sin elegir entre las miniaturas. Se retiró el archivo de prueba; selección, cifrado, vista previa y persistencia quedan pendientes.
- No se eligió un contacto desde la agenda ni se verificó el URI devuelto por el proveedor del teléfono; se evitó leer o modificar contactos personales. La selección manual sí se probó.
- No se activó TalkBack ni se modificó tamaño de fuente o animaciones globales del teléfono. La semántica Compose no equivale a una prueba con TalkBack.
- No se habilitó lectura automática ni se generó una emergencia real de prueba; la lectura de instrucciones en EmergencyActivity y el funcionamiento offline de una voz local siguen pendientes.

## Registro adicional de estabilidad — 2026-11-07 (estado posterior)

Las notas previas de esta página son registros históricos de la hora en que se realizaron y quedaron superadas en cuanto a conteo de pruebas e instalación.

- Ejecutado `testDebugUnitTest assembleDebug`: **17 pruebas, cero fallos, cero errores, cero omitidas**; compilación `BUILD SUCCESSFUL`.
- La APK final de esta revisión verificó firma v2 y se instaló con `adb install -r` → `Success`. Datos de paquete en el dispositivo: `com.impulsos.app`, versionCode 1, versionName 0.1.0, `lastUpdateTime=2026-11-07 14:31:39`. La APK mide 8,105,808 bytes; SHA-256 `36372F324AEFC4F6F3ADAC8A4E89CCBE79FACAD6C75588119585C5B68F337456`.
- ADB permaneció `device` para serial `ZY323D6MSV`. La aplicación se abrió; PID reportado. La lectura de logs filtrada no detectó `FATAL EXCEPTION` de la app. No se abrió el modo de diagnóstico tras esta recompilación.
- Investigación de espera de Package Manager: no se detectó prompt en pantalla en los intentos observados; durante la instalación activa se vio `installd` y un `dex2oat` ejecutándose, no había sesiones activas listadas y quedaban 9.0 GB en `/data`. La instalación completó después de esperar. Es evidencia compatible con trabajo de optimización/reconciliación del dispositivo, no prueba concluyente de una causa interna única.
- El Photo Picker real está abierto para importar `/sdcard/Download/medicamento.jpeg` (19,937 bytes). El árbol de UI del selector devolvió raíz nula en este dispositivo, por lo que no se usaron coordenadas ciegas para elegir una foto. Selección, cifrado, persistencia y emergencia permanecen pendientes de completar tras acción del usuario en pantalla.
- No se activó monitoreo normal, alarma, EmergencyActivity por gesto ni una emergencia real. No se ha ejecutado gesto físico, cámara ni la comprobación de reemplazo/eliminación.

### Evidencia de foto y diagnóstico — 2026-11-07, 14:35–14:38 local

- En el Motorola se seleccionó `medicamento.jpeg` desde el Photo Picker real. ImpulSOS mostró el diálogo de vista previa; se confirmó Guardar.
- El editor cambió a los estados «Reemplazar fotografía» y «Eliminar fotografía». El blob `no_backup/emergency-photo.bin` quedó en el sandbox privado de la app con modo `0600` y 23,263 bytes.
- Se cerró el proceso (`am force-stop`, sin limpiar datos) y se abrió de nuevo. El blob persistió; el editor emitió la etiqueta accesible «Fotografía guardada para la pantalla de emergencia».
- Se abrió la simulación identificada y se confirmó la etiqueta accesible de la foto «Fotografía de referencia elegida por la persona». El encabezado visible fue `SIMULACIÓN · NO ES UNA EMERGENCIA REAL`. `MonitoringService` no aparecía activo. Atrás cerró la simulación.
- Diagnóstico sintético: la señal mostrada como «última prueba sintética» fue aceptada seis veces; el último patrón se aceptó a 1,421 ms. La verificación de actividad no encontró `MonitoringService` ejecutándose ni una EmergencyActivity reanudada.
- Diagnóstico físico: no se completa. El botón «Leer sensor» aparece deshabilitado por el estado guardado de monitoreo activo. ADB no mostró el proceso/servicio corriendo, lo que apunta a estado persistido obsoleto. No se cambió ese ajuste porque pausarlo alteraría el estado de producción del usuario; se pidió al usuario pausarlo en pantalla para esta prueba. Una lectura mostrada previamente como fuente sintética no se contabiliza como lectura física.
- Cámara, reemplazo y eliminación no probados aún en hardware. La cámara debe apuntarse a un objeto no sensible y la captura requiere acción física de usuario.
- La interfaz del Photo Picker devolvió raíz UI nula a ADB; el archivo se eligió con interacción explícita del usuario, no por coordenadas supuestas.


### Lectura física de base — 2026-11-07

- Tras pausar el monitoreo normal desde UI, el control «Leer sensor» quedó habilitado y la pantalla indicó «acelerómetro físico».
- En reposo sobre el escritorio se observaron X=0.01, Y=-0.17, Z=9.80 m/s², magnitud dinámica 0.07 m/s², 0 impulsos y 19 ms entre muestras (solicitud de registro: 20,000 µs). En la lectura posterior estable: magnitud 0.01, 0 impulsos y 18 ms entre muestras.
- Durante la sesión física, el contador visible alcanzó 2 patrones aceptados antes de detener el listener. El último snapshot posterior al reset conservó `Patrones aceptados: 2`; no se capturó el intervalo exacto de ese patrón ni el tiempo de reconocimiento relativo al inicio del gesto. No se realizó una batería de movimientos cotidianos ni se midieron falsos positivos/negativos.


### Build de emergencia con cargas diferidas — 2026-11-07, 15:01

- `testDebugUnitTest assembleDebug`: `BUILD SUCCESSFUL`, suite 17/17 (13 detector + 4 contactos), cero fallos/errores/omitidas.
- APK 8,108,224 bytes, SHA-256 `86E5A535AD15E2790B611E020A64110C4004C6BAA12C52D44D1C617D0A1B9017`; paquete 1 / 0.1.0, `adb install -r` → `Success`, `lastUpdateTime=2026-11-07 15:01:28`.
- La pantalla de inicio y editor mostraron la foto cifrada persistida. EmergencyActivity cargó el perfil/foto tras aparecer la pantalla de simulación; la etiqueta accesible de foto fue visible. Se cerró con Atrás; el servicio normal no estaba activo. No se detectó FATAL EXCEPTION de app.
- El intento de instalación demoró mientras `dumpsys package` listó una sesión activa de Google Play para `com.google.android.deskclock` y `installd` ejecutó `dex2oat`; el log mostraba compilación de clases de esa actualización. Quedaban ~9.1 GB. El comando terminó en `Success` sin prompt visible. Es evidencia de trabajo concurrente y posible contención; no prueba concluyente de causa única.
- `/sdcard/Download/medicamento.jpeg` sigue guardada como foto de perfil (blob cifrado, 23,263 bytes). El flujo de cámara desde el sistema no produjo un nuevo blob/temporal confirmado en la última inspección; captura de cámara sigue pendiente de acción de usuario.

### Cámara y persistencia — 2026-11-07, 15:19

- El usuario completó la captura de cámara y la confirmó en ImpulSOS. El blob privado cifrado cambió de 23,263 a 136,758 bytes; fecha 15:19; `cache/impulsos-camera` quedó vacío tras el guardado.
- Se ejecutó `am force-stop` y se volvió a abrir la app sin limpiar datos. El blob persistió y el editor expuso la etiqueta accesible «Fotografía guardada para la pantalla de emergencia» con acciones Reemplazar/Eliminar.
- En simulación de emergencia también apareció la etiqueta «Fotografía de referencia elegida por la persona». La actividad se cerró con Atrás y no hubo `MonitoringService` ni alarma real.
- Prueba de reemplazo iniciada mediante Photo Picker; el usuario está eligiendo de nuevo el archivo de prueba para confirmar el reemplazo. Eliminación aún pendiente.

### Reanudación y pausa — 2026-11-07, 15:31–15:43

- `testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL, 17/17 pruebas aprobadas, cero fallos/errores/omitidas.
- Se corrigió la actualización del bitmap de foto en el editor al reemplazar o eliminar: el estado ahora se invalida usando una revisión de foto y vuelve a cargar desde el almacenamiento cifrado. No se modificó el formato ni las claves de cifrado.
- APK de la corrección: 8,108,878 bytes, SHA-256 `26DD36C94D5468F1D1BD43C8189E0833D8B9F45C2AFA5DD6F591BBF73C859AF7`; firma v2 válida; paquete `com.impulsos.app`, versionCode 1, versionName 0.1.0.
- Instalación conservando datos: `adb install --no-streaming -r` → `Success`; `lastUpdateTime=2026-11-07 15:38:26`. Al esperar, el teléfono registraba `dex2oat` optimizando paquetes de otras apps; no hay evidencia suficiente para atribuir únicamente a eso el retraso. No se detectó cuadro de confirmación en pantalla, no se desinstaló ImpulSOS ni se borraron datos.
- Con APK corregida: se relanzó ImpulSOS y el editor mostró la foto cifrada persistida. Se eliminó desde la UI; el archivo privado desapareció y el control regresó enseguida a «Añadir fotografía (opcional)» sin conservar la vista de la imagen antigua.
- Photo Picker se canceló sin elegir archivo para pausar las pruebas. Estado actual: sin `emergency-photo.bin`; solamente permanece `medical-profile.bin` en `no_backup`.
- El reemplazo real de `medicamento.jpeg`, el relanzamiento y su presencia en la simulación están confirmados a las 15:31 en la versión previa de la app. La APK corregida a las 15:38 aún necesita repetir importación/persistencia/simulación con la imagen de prueba.
- La cámara del sistema fue capturada, confirmada, cifrada y probada tras relanzamiento en una APK anterior (15:19); repetirla en la APK actual y verificar cancelación queda pendiente.
- Las dos sacudidas físicas aceptadas corresponden a una sesión anterior. No se registró el intervalo ni latencia exacta; no se observó una serie de movimientos cotidianos. No inferir tasa de falsos positivos/negativos.
- No se inició ninguna emergencia real. La simulación es independiente del servicio y se cerró con Atrás. Monitoreo normal permanece pausado por instrucción/acción previa del usuario.

**Pendiente al retomar:** importar de nuevo `/sdcard/Download/medicamento.jpeg` por el selector real y guardar; cerrar/reabrir; verificar foto en editor y simulación; repetir cámara/cancelación en la APK actual; continuar protocolo de diagnóstico físico seguro cuando el usuario tenga tiempo. Mantener pendientes TalkBack, escala, Doze, pantalla bloqueada, latencia, voz/audio y batería.

### Continuación — 2026-11-10

- ADB confirmó APK 0.1.0 instalada con `lastUpdateTime=2026-11-07 15:38:26`. La foto cifrada ya existía al comenzar esta sesión (23,269 bytes); se verificó en editor y en simulación identificada. No se determinó cuándo ni por qué se guardó entre sesiones.
- Recorrido de cámara en APK actual: abrió la cámara del sistema desde el editor, se tomó una foto, se mostró la vista previa de ImpulSOS y se confirmó Guardar. Se observó un URI de captura temporal bajo caché privada (`capture-*.jpg`, 2,464,233 bytes) mientras la cámara estaba abierta. Después de guardar, `emergency-photo.bin` tuvo 65,110 bytes y la caché de cámara quedó vacía.
- Se cerró forzadamente y se relanzó la app. El blob de 65,110 bytes persistió; se mostró «Fotografía guardada para la pantalla de emergencia» y acciones de reemplazo/eliminación en el editor. No se observó excepción fatal.
- La prueba de simulación correspondiente a la nueva foto de cámara quedó incompleta: los servicios `input` y `activity` desaparecieron brevemente y se recuperaron; durante el nuevo intento ADB reportó el dispositivo desconectado. Sin reinicio del teléfono, cambio de ajustes ni reintentos continuos.
- La selección de `medicamento.jpeg` y su presentación en editor/emergencia están confirmadas en una sesión previa. Repetir esa misma importación en la APK actual y documentar exactamente el contenido no se completó hoy. La foto actual es la captura reciente de cámara.
- No se ejecutó una nueva suite automatizada hoy (no hubo cambios de código); el resultado más reciente permanece en `testDebugUnitTest assembleDebug`: 17/17 aprobado, 2026-11-07.

**Pendiente:** restablecer conexión ADB USB; comprobar captura actual dentro de la simulación; volver a importar `medicamento.jpeg` por el selector, guardar y relanzar para comprobar persistencia; cancelar cámara y verificar conservación de la foto previa; continuar pruebas físicas seguras del gesto con métricas de intervalo/latencia y actividades cotidianas.

**Cierre automatizado 2026-11-10:** `testDebugUnitTest --rerun-tasks` terminó `BUILD SUCCESSFUL` en 1m15s: detector 13/13 y contactos 4/4, sin fallos, errores ni omitidas. `assembleDebug` ejecutado previamente quedó `UP-TO-DATE`; APK sin cambios: 8,108,878 bytes, SHA-256 `26DD36C94D5468F1D1BD43C8189E0833D8B9F45C2AFA5DD6F591BBF73C859AF7`. Único warning Kotlin: setter `systemUiVisibility` obsoleto (`EmergencyActivity.kt:208`).

Corrección de estado tras reconectar ADB el 2026-11-10: se completó la simulación de la captura de cámara, y se reimportó el archivo `medicamento.jpeg` por Photo Picker en la APK actual. La importación sobrevivió al cierre/reapertura y apareció en simulación. También se probó cancelar la cámara con Atrás: la foto previa permaneció y el archivo temporal se eliminó. Al cierre ya no se requiere reconectar ADB ni repetir esas validaciones de foto; falta el protocolo físico amplio del detector y las comprobaciones de accesibilidad/Doze/audio/batería descritas en la sección siguiente.
