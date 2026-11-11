# Plan de pruebas

## Diagnóstico que conserva rechazos — 0.1.2

23 pruebas aprobadas: 17 originales, cuatro del doble tajo y dos de historial limitado/retención tras reposo. Compilación correcta; firma verificada; instalación Success el 2026-11-10 08:20:15, versionCode 3. SHA-256 `8F63507F018C478B900FA89426B8D6EF40BDE78281E5825618154F5BEAFACFBA`.

Verificación en Motorola: señal sintética → cuatro patrones aceptados → Detener conserva máxima 4.38 m/s² e historial con intervalos aceptados 240–260 ms y rebotes 120–140 ms. Fuente identificada como sintética; servicio de monitoreo ausente. Se dejó una sesión física detenida para repetición sin USB. La prueba física anterior quedó rechazada (cero aceptaciones), y no permite estimar una tasa de falsos negativos.

## Gesto doble tajo 0.1.1 — 2026-11-10

Resultado de la secuencia sin USB: usuario confirmó dos tajos y reconexión; la pantalla indicó cero patrones aceptados y periodo de muestra de 19 ms. `MonitoringService` ausente. No se reconoce como éxito; es un intento deliberado no detectado, sin registro suficiente para asignar causa (Detener reiniciaba el estado). Se corrige el diagnóstico en 0.1.2 para conservar máxima dinámica e historial limitado de 12 eventos/rechazos, con tiempos relativos e intervalos. No se cambian umbrales ni algoritmo a partir de este intento sin evidencia.

- Suite: 21 pruebas aprobadas (17 existentes conservadas + 4 nuevas); assembleDebug correcto. La primera iteración falló en vibración rápida y fue corregida sin cambiar esa prueba.
- APK versionCode 2 / versionName 0.1.1; firma verificada; SHA-256 `2BA5486F5511C17F8F296D9E20CACD3A8BE9C86835BD2723FA2545192F0F1B45`; 8,060,751 bytes.
- Instalación `adb install --no-streaming -r`: Success; Motorola registra `lastUpdateTime=2026-11-10 08:01:33`. App abrió y diagnóstico accesible.
- Sensorservice confirmó handle 0x08 wake-up: periodo 20 ms y batching 100 ms. Diagnóstico físico en reposo mostró 19 ms entre muestras, magnitud dinámica 0.01 m/s² y cero patrones aceptados. Esto no es medida de batería ni validación de movimientos cotidianos.
- Servicio MonitoringService ausente durante la prueba segura. Usuario solicitó quitar USB porque estorba: diagnóstico detenido, Fuente inactiva, cero patrones, listo para prueba manual sin cable. Debe pulsar Leer sensor después de desconectar, realizar dos tajos, detener y reconectar conservando la pantalla.
- Pendiente resultado del gesto sin USB, intervalos/entrega y evaluación de falsos positivos; consumo A/B prolongado y pantalla apagada también pendientes. No se activó alarma real.

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
- ADB siguió enumerando el Motorola como `device`. La aplicación se abrió y se comprobó el proceso. La lectura de logs filtrada no detectó `FATAL EXCEPTION` de la app. No se abrió el modo de diagnóstico tras esta recompilación.
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

### Detector chop-chop — iteración 0.1.3, 2026-11-10

- Causa concreta en la sesión física de 0.1.2: primer impulso observado a 75 ms; candidato siguiente a 205 ms (separación 130 ms) rechazado como rebote temprano por estar debajo del mínimo de 200 ms. Las capturas disponibles no permiten afirmar que esa fuera la única causa del gesto no reconocido. Además, el umbral de valle fijo podía impedir rearmar después de un impulso de amplitud elevada.
- Cambio aplicado: el rearmado usa el 60 % de la mayor entre el umbral de gesto y la cresta medida, con piso configurado de 1.2 m/s². Se conservan los umbrales de intensidad, la dirección relacionada, el mínimo de 200 ms, el máximo de 600 ms, la ventana de 1,200 ms, la protección frente a vibración y el rechazo de caída. La ventana temporal no se amplió y no se redujo el mínimo para forzar aceptación.
- Cobertura automatizada actual: 27/27 (21 detector, 4 contactos, 2 sesión diagnóstica); cero fallos/errores/omitidas. APK `0.1.3`, `versionCode=4`, tamaño 7,975,604 bytes, SHA-256 `B9B55D9E87C612ADA090C1D49E2DD9443624E80340B2A2039E6C6610E615BF7E`.
- Instalación previa confirmada por ADB: `adb install --no-streaming -r` → `Success`; `lastUpdateTime=2026-11-10 08:33:37`; `com.impulsos.app`, versión 0.1.3/code 4. `MainActivity` se abrió y no hubo `FATAL EXCEPTION`. En la lectura del diagnóstico no había `MonitoringService` activo.
- Señal sintética en el Motorola: cuatro patrones aceptados; intervalos aceptados 240 y 260 ms; rebotes tempranos 120 y 140 ms; máximo dinámico 4.38 m/s². Esto no equivale a un gesto físico.
- Reposo físico en diagnóstico: acelerómetro wake-up, intervalo recibido observado 19 ms, magnitud dinámica 0.01 m/s², cero impulsos. En la secuencia física 0.1.3 se observaron dos patrones aceptados (intervalos 579 y 225 ms), dos rechazos por dirección (318 y 504 ms) y una cresta máxima de 98.79 m/s². La demora evento-a-callback fue 73–74 ms para las aceptaciones registradas; no mide la latencia hasta la pantalla de emergencia.
- El usuario informó que hubo otros movimientos durante la sesión. No se puede atribuir cada aceptación al gesto deliberado ni calcular falsos positivos/negativos. Se detuvo el listener antes de reconectar; `MonitoringService` permaneció ausente y no se activó alarma ni emergencia.
- Al solicitar cerrar la sesión, se inició un lector físico nuevo y se detuvo de inmediato sin realizar otro gesto: pantalla «Fuente: inactiva», máximo dinámico 0.38 m/s², cero patrones; intervalo final 18 ms. No había `MonitoringService`. Esto confirma listener detenido, no es una prueba adicional del gesto.
- Cierre: Motorola one estaba reconectado y accesible; APK continúa en code 4/name 0.1.3 con `lastUpdateTime=2026-11-10 08:33:37`. No se instaló otro APK, no se borraron datos ni se modificaron ajustes. Después se cerró ImpulSOS con `am force-stop`, se apagó el servidor ADB y se detuvo el daemon Gradle; el listado final no mostró procesos ADB, emulador o Java/Gradle. En el último intento de limpieza ADB informó `no devices/emulators found`, así que el teléfono no estaba disponible y no se verificó la retirada del volcado temporal `/sdcard/window.xml` generado para leer la pantalla de diagnóstico (solo jerarquía de esa pantalla). Quedan pendientes movimientos cotidianos estructurados, medición A/B de batería y latencia hasta pantalla de emergencia.
- ADB desde el proceso sandbox vuelve a fallar al iniciar `fdevent interrupt socketpair: Invalid argument`; en una sesión local autorizada fue posible listar el Motorola y leer la pantalla/registros. No se cambió configuración del teléfono.
- Ahorro de batería, tasa de falsos positivos/negativos y latencia de apertura de emergencia no medidos. El batching solicitado de hasta 100 ms es una política técnica, no una medición de consumo ni garantía de latencia.

### Ensayos físicos separados — 2026-11-11

Motorola one reconectado por USB. ADB confirmó la versión 0.1.3 (`versionCode=4`, actualización del 2026-11-10 a las 08:33:37). Se abrió la app y se confirmó que `MonitoringService` estaba inactivo. Al iniciar Diagnóstico seguro se observaron 19 ms entre muestras, magnitud dinámica instantánea de 0.02 m/s² y cero patrones. Se retiró el volcado temporal `/sdcard/window.xml` de la sesión previa.

La persona controla **Leer sensor** y **Detener** en cada ensayo. Con la lectura detenida, desconecta el USB y coloca o sujeta el Motorola en la posición inicial. Abre Diagnóstico seguro si hace falta, pulsa **Leer sensor** y deja el teléfono quieto unos segundos. Realiza solo la actividad indicada y vuelve a dejarlo quieto. Pulsa **Detener** antes de moverlo para reconectar el USB. ADB se usa para verificar el estado y leer el resultado después de reconectar.

Para el doble tajo, realiza una sola secuencia por sesión. Prueba por separado caminar, manipular el teléfono y sacarlo del bolsillo. La lectura que se inició por ADB antes de desconectar queda fuera del protocolo porque puede incluir la manipulación del cable. Si hubo movimientos adicionales, regístralos junto al resultado.

Anotar actividad, patrones aceptados, intervalos, motivo de los rechazos, magnitud máxima y demora entre la muestra y su entrega a la app. En una sesión de gesto deliberado se espera una aceptación y en una sesión de actividad cotidiana se esperan cero. Separar las sesiones que mezclen actividades antes de calcular resultados por categoría.

Tras el primer aviso de reconexión, `adb devices -l` seguía sin enumerar el Motorola incluso después de reiniciar el servidor. En la reconexión posterior ADB recuperó el dispositivo. Diagnóstico seguro mostraba la fuente inactiva, **3 patrones aceptados**, último intervalo **243 ms**, entrega **69 ms** y **19 ms** entre muestras. `MonitoringService` estaba ausente. Falta confirmar cuántas secuencias deliberadas se hicieron y si hubo otros movimientos. Si se hizo una sola secuencia, hubo más aceptaciones de las esperadas.

Para evitar reconexiones por ensayo, hacer el siguiente bloque con el USB desconectado. La persona inicia y detiene cada lectura y apunta el contador antes de empezar la siguiente, porque **Leer sensor** reinicia el resumen. Para una primera comprobación, registrar dos sesiones con un doble tajo cada una y dos sesiones de actividad cotidiana, una al caminar y otra al manipular el teléfono. El bloque sirve para detectar problemas evidentes y orientar la siguiente ronda. Con esos pocos ensayos no se pueden estimar tasas de error. El resultado se puede comunicar desde la pantalla sin USB.

### Historial de pruebas físicas — 0.1.4

Con el USB desconectado, realizar las cuatro sesiones en la misma pantalla. La persona pulsa **Leer sensor** y **Detener** en cada sesión. Al detener una lectura física con muestras, su resumen queda en el historial. Al terminar el bloque, pulsar **Copiar historial** y pegarlo en el chat o enviarlo como texto. Mantener abierta la pantalla hasta copiar el resultado. El historial conserva ocho resúmenes recientes y las simulaciones quedan fuera.

Comprobación en el Motorola: el contador de pruebas debe pasar de cero a cuatro al completar el bloque, en el orden de las actividades. Leer sensor reinicia solo el contador del ensayo actual. La acción Copiar historial incluye el conteo, la duración, el máximo dinámico, la información del último patrón si existe y los eventos limitados de cada ensayo. Verificar que al abandonar la pantalla se libere el acelerómetro y que `MonitoringService` siga inactivo.

Compilación y suite local correctas con `testDebugUnitTest assembleDebug --offline`. Pasaron 30 pruebas, incluidas tres nuevas sobre resultados independientes, lecturas incompletas y límite de ocho ensayos. APK `com.impulsos.app` 0.1.4 (código 5), 7,982,604 bytes, firma v2 verificada y SHA-256 `548755B9E0B0F21E32216D27DA032923F8285E291726178CD71D096F1E7A828A`.

Instalación en el Motorola confirmada con `Success` y `lastUpdateTime=2026-11-11 09:40:16`. ADB verificó el estado vacío inicial de Historial de pruebas físicas, fuente inactiva, apertura de MainActivity y ausencia de `MonitoringService`. La captura de las ocho filas se confirmó en el bloque siguiente. La copia al portapapeles queda pendiente de verificación.

### Bloque de ocho ensayos físicos y ajuste de crestas — 2026-11-11

Se recuperaron las ocho filas del historial de 0.1.4 y los eventos asociados en el Motorola. El usuario inició y detuvo cada lectura. El diagnóstico quedó inactivo y ADB confirmó que `MonitoringService` estaba ausente. Las filas se asocian al orden de actividades indicado para el bloque.

| Nº | Movimiento | Esperado | Aceptados | Máxima dinámica | Intervalos aceptados |
|---:|---|---:|---:|---:|---|
| 1 | Doble tajo natural | 1 | 1 | 105.15 m/s² | 508 ms |
| 2 | Doble tajo natural repetido | 1 | 1 | 101.62 m/s² | 527 ms |
| 3 | Caminar | 0 | 3 | 23.43 m/s² | 283, 564 y 226 ms |
| 4 | Manipulación habitual | 0 | 4 | 11.96 m/s² | 376, 547, 226 y 584 ms |
| 5 | Doble tajo rápido | 1 | 1 | 105.56 m/s² | 470 ms |
| 6 | Doble tajo moderado | 1 | 0 | 90.17 m/s² | — |
| 7 | Tajo aislado | 0 | 0 | 117.72 m/s² | — |
| 8 | Bolsillo | 0 | 2 | 13.02 m/s² | 320 y 283 ms |

Se reconocieron tres de cuatro intentos intencionales y hubo nueve detecciones durante tres ensayos cotidianos. El tajo aislado se rechazó pese al máximo más alto del bloque. Los intervalos de actividad cotidiana se solapan con los de gestos reconocidos. El ensayo moderado registró rechazos por dirección y reinicios por picos rápidos. Los resúmenes no permiten recuperar las muestras XYZ ni atribuir cada candidato a una fase exacta del tajo.

La versión 0.1.5 exige dos crestas confirmadas en el descenso. Usa magnitud, timestamp y dirección de cada cresta. El piso experimental del modo equilibrado es 28 m/s², por encima de las máximas de caminar, manipulación y bolsillo de este bloque. Los otros preajustes usan 26 y 32 m/s². Los candidatos débiles quedan fuera de la secuencia y los rebotes de menos de 110 ms no reinician el primer impulso válido. Se mantiene la separación de 200 a 600 ms entre crestas. El diagnóstico separa el intervalo entre crestas, la demora de confirmación y la entrega de la muestra.

Dos regresiones sintéticas fallaron con 0.1.4: pares cotidianos alineados y dos tajos con precursores débiles. Las pruebas positivas previas conservan sus comprobaciones de dirección, cadencia, ventana y sensibilidad. Se actualizaron las amplitudes positivas para representar crestas que superen el nuevo piso y las pruebas que dependen del instante de decisión esperan el descenso. La señal integrada de diagnóstico representa dos tajos y dos retornos. Son señales generadas para probar el algoritmo y se conservan como tales en el historial.

Revalidación física pendiente: repetir un doble tajo natural, uno moderado y las tres actividades que generaron detecciones. Iniciar y detener cada ensayo manualmente y copiar los cinco resultados juntos. Medir consumo con un bloque A/B aparte.

`testDebugUnitTest assembleDebug --offline` terminó correctamente con 34 pruebas, sin fallos, errores ni omitidas. APK 0.1.5 (código 6), 8,081,971 bytes, firma v2 verificada, SHA-256 `CF20AE22E8C6446CA5A4E51A0E6EF8A6C02BCD700B221D97DC8A4E5569F6BE9E`. ADB confirmó `Success` al instalar conservando los datos y `lastUpdateTime=2026-11-11 09:55:47`. MainActivity abrió correctamente y Diagnóstico seguro quedó en fuente inactiva, sin servicio de monitoreo ni excepción fatal observada.

### Cinco ensayos dirigidos de 0.1.5 — 2026-11-11

El usuario confirmó que hizo el bloque y reconectó el USB. Se leyeron las cinco filas del historial y se asociaron a los últimos cinco cierres del registro.

| Nº | Movimiento | Esperado | Aceptados | Máxima dinámica |
|---:|---|---:|---:|---:|
| 1 | Doble tajo natural | 1 | 0 | 113.49 m/s² |
| 2 | Doble tajo moderado | 1 | 1 | 98.02 m/s² |
| 3 | Caminar | 0 | 0 | 13.80 m/s² |
| 4 | Manipulación habitual | 0 | 0 | 10.80 m/s² |
| 5 | Bolsillo | 0 | 0 | 19.07 m/s² |

El tajo moderado tuvo 546 ms entre crestas, 38 ms hasta confirmar el descenso y 54 ms de entrega de la muestra de confirmación. Se observaron 19 ms entre muestras en el diagnóstico. El tajo natural registró el descarte por posible caída a los 2,674 ms y solo un impulso calificado después del bloqueo. La versión 0.1.5 activaba el bloqueo con una sola muestra baja y el registro no conserva su duración. El bloqueo es una explicación posible del fallo. Los tres ensayos cotidianos no generaron activaciones en este bloque.

La 0.1.6 exige al menos tres muestras bajas consecutivas que abarquen al menos 55 ms, con separaciones máximas de 75 ms. Una lectura baja pendiente queda fuera del análisis de crestas y conserva la secuencia. Tras confirmar la caída, se descarta la secuencia y se mantiene el bloqueo de 800 ms. Se mantienen los pisos de cresta, la dirección y la ventana de 200–600 ms. La señal simulada de un tajo con una o varias muestras bajas breves falló antes del cambio. La segunda regresión comprueba que una caída sostenida seguida de picos fuertes sigue bloqueada. No se dejan caer teléfonos durante la validación.

La repetición física dirigida de 0.1.6 se registró en el bloque siguiente. Batería y latencia de apertura de emergencia siguen pendientes.

`testDebugUnitTest assembleDebug --offline` terminó con 36 pruebas aprobadas, sin fallos, errores ni omitidas. APK 0.1.6 (código 7), 7,986,980 bytes, firma v2 verificada y SHA-256 `01D8D5A48999FB4D5FC79C15DD36F2A30F66C47ACE5858ECD817CE56797B8AB9`. ADB confirmó `Success` y `lastUpdateTime=2026-11-11 10:06:16`. ImpulSOS abrió correctamente y quedó en Diagnóstico seguro con fuente inactiva, servicio de monitoreo ausente y sin excepción fatal observada.

### Tres ensayos físicos de 0.1.6 — 2026-11-11

ADB confirmó la versión 0.1.6 (código 7) y leyó el historial al reconectar. El resultado fue **1, 1, 0**, igual al esperado. La fuente quedó inactiva y el servicio de monitoreo estaba ausente.

| Ensayo | Patrones aceptados | Máxima dinámica | Intervalo entre crestas | Confirmación | Entrega |
|---|---:|---:|---:|---:|---:|
| Doble tajo natural 1 | 1 | 100.27 m/s² | 489 ms | 37 ms | 83 ms |
| Doble tajo natural 2 | 1 | 108.31 m/s² | 471 ms | 57 ms | 51 ms |
| Manipulación habitual | 0 | 12.84 m/s² | — | — | — |

Confirmación mide la espera desde la segunda cresta hasta la muestra que confirma el descenso. Entrega mide desde esa muestra hasta su procesamiento por la app. La suma fue 120 y 108 ms, respectivamente, dentro de Diagnóstico seguro. El bloque no midió la latencia hasta mostrar la pantalla de emergencia, el consumo ni el comportamiento con la pantalla bloqueada. Se conserva el algoritmo 0.1.6 como referencia para esas pruebas.

### Tutorial, práctica y apariencia — 0.1.7

- Desde Sensor, se abrió directamente el paso 8. Dos capturas del Motorola mostraron el diagrama inicial y el desplazamiento del teléfono durante la demostración. El control cambió a «Repetir demostración».
- Desde Ajustes, se recorrieron los diez pasos. En el paso 9 se abrió la simulación identificada y se volvió a la guía con Atrás. Terminar regresó a Ajustes. El servicio de monitoreo permaneció ausente.
- Se seleccionó Oscuro y se revisaron capturas de Ajustes y el paso 8. Oscuro seguía seleccionado después de cerrar el proceso y relanzar. Al finalizar se restauró Usar tema del sistema.
- Se calcularon pares de contraste de la paleta. El texto secundario sobre el fondo claro dio 6.84:1, el texto secundario sobre la superficie oscura 9.75:1 y el texto del botón primario dio 5.83:1 en claro y 7.97:1 en oscuro. Estas mediciones cubren esos pares concretos.
- Se abrió Práctica del doble tajo con el sensor detenido y se comprobó que se puede volver a la guía. La persona inicia y detiene la práctica. Queda por verificar el reconocimiento en esa pantalla, TalkBack y el comportamiento con animaciones del sistema desactivadas.

`testDebugUnitTest assembleDebug --offline` terminó correctamente con 36 pruebas aprobadas. La APK final 0.1.7 (código 8) tiene firma v2 verificada, 8,142,134 bytes y SHA-256 `0E7A459CAFF45CE2CB9214B710FDA6C8FF6C488B4F48FA5BFA38729C6088CE81`. ADB confirmó `Success` y `lastUpdateTime=2026-11-11 10:26:39`. Se revisó el recordatorio del ritmo en práctica y se dejó la demostración abierta, con el acelerómetro detenido.

### Evidencia visual y paquete de entrega

En el Motorola one se capturaron seis vistas para el README. Se utilizó el mismo código 0.1.7 en un paquete temporal con datos separados. El perfil contiene el nombre ficticio «Persona Demo» y la pantalla de emergencia está identificada como simulación. La APK de entrega conserva el paquete `com.impulsos.app` y la huella de la compilación comprobada.
