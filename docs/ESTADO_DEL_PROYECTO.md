# Estado del proyecto

## Registro previo del detector — 2026-11-10, revisión 0.1.2

La secuencia física sin USB de 0.1.1 no se reconoció: cero patrones, 19 ms entre muestras. No se conoce la causa porque Detener borraba el estado del rechazo. Se instaló 0.1.2 (code 3) para conservar intensidad máxima y 12 eventos/rechazos; no se cambiaron parámetros de detección en esta revisión. 23 pruebas unitarias aprobadas y assembleDebug correcto. Firma verificada; instalación Success; `lastUpdateTime=2026-11-10 08:20:15`; SHA-256 `8F63507F018C478B900FA89426B8D6EF40BDE78281E5825618154F5BEAFACFBA`, 7,974,376 bytes.

En teléfono, la señal sintética reconoció cuatro patrones; los intervalos aceptados conservados fueron 240–260 ms, máxima dinámica 4.38 m/s². Al detener, el resumen permaneció y se imprimió identificado como sintético. Sin servicio MonitoringService activo. No cuenta como calibración física. Se preparó diagnóstico físico detenido y se solicitó repetir un gesto sin USB. La activación y el consumo aún requieren validación física; no se afirma solución definitiva ni ahorro medido.

El estado de detector y validación más reciente aparece en «Detector chop-chop — revisión 0.1.3» al final de este documento; este registro describe la compilación anterior.

## Verificado (2026-11-07)

- Skills: 55 Skills del proyecto y 53 host, inventariadas por origen. Las seis pertinentes aplicadas se describen en `docs/INVENTARIO_SKILLS.md`. Sin snapshot previo no se etiqueta ninguna como recién instalada.
- Penpot MCP: conectado; archivo `Aplicacion (copia)`, página actual `02 — Onboarding`; `currentFile.pages` expone las 13 páginas. El inventario enumera sus propósitos, estilos y diferencias de adaptación. No se editó el diseño original.
- Código: flujo de imagen orienta según EXIF, reanuda la ruta temporal de cámara, restringe el URI a caché con `FileProvider` no exportado y cifra la copia final AES/GCM en `noBackupFilesDir`. El diagnóstico sondea el estado del servicio y no llama a acciones de emergencia.
- Automatización: `testDebugUnitTest assembleDebug` exitoso; 17 pruebas, cero fallos/errores/omitidas.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, 8,105,808 bytes, SHA-256 `36372F324AEFC4F6F3ADAC8A4E89CCBE79FACAD6C75588119585C5B68F337456`; firma v2 verificada; `com.impulsos.app`, code 1, name 0.1.0.
- Instalación: `adb install -r` → `Success`; `lastUpdateTime` 2026-11-07 14:31:39. Se abrió MainActivity; no se observó FATAL EXCEPTION de la app.
- Paquete Manager: el instalador esperó mientras `installd`/`dex2oat` trabajaban; no se listaron sesiones activas y `/data` tenía 9 GB libres. Esperar en un único intento terminó con éxito, sin desinstalación ni borrado. Evidencia sugerente, no prueba concluyente de la causa interna.
- Foto: selección real de `/sdcard/Download/medicamento.jpeg` (19,937 bytes), confirmación de vista previa, guardado cifrado (blob privado de 23,263 bytes), persistencia tras cerrar/reabrir proceso y etiquetas accesibles de imagen presentes en editor y simulación de emergencia.
- Diagnóstico sintético: seis patrones aceptados, último en 1,421 ms; ningún servicio de monitoreo ni emergencia activo durante esa prueba. No equivale a validación física.

## Pendiente

- Prueba con cámara del sistema, cancelación de cámara, reemplazo y eliminación en hardware.
- Lectura física deliberada/cotidiana en modo seguro; el botón está deshabilitado por el estado persistido de monitoreo. Se pidió pausar el monitoreo desde UI; no se cambió estado de producción desde ADB.
- Gestos controlados, caminar/correr, falsos positivos/negativos; TalkBack, escala de fuente, Doze, pantalla bloqueada, TTS/volumen y batería.
- El detector sigue siendo heurístico, no validado clínicamente. No se activó emergencia real.

Lectura física de base: el listener arrancó tras pausar el monitoreo normal y reportó intervalo recibido de 18–19 ms, magnitud dinámica en reposo de 0.01–0.07 m/s² y cero impulsos. El contador del modo físico alcanzó 2 patrones aceptados antes de detenerlo; el intervalo del patrón/latencia relativa no se registró y falta comparar movimientos cotidianos.



## Build optimizada de emergencia — 2026-11-07, 15:01 local

- Tras las modificaciones produceState/Dispatchers.IO, 	estDebugUnitTest assembleDebug finalizó correctamente con 17 pruebas aprobadas.
- APK de 8,108,224 bytes, SHA-256 86E5A535AD15E2790B611E020A64110C4004C6BAA12C52D44D1C617D0A1B9017; instalación db install -r finalizó Success. El paquete informa lastUpdateTime=2026-11-07 15:01:28, code 1, name 0.1.0.
- Se abrió MainActivity y la EmergencyActivity de simulación; la etiqueta de la foto apareció luego de carga asíncrona, la simulación se cerró con Atrás y no hubo MonitoringService ni excepción fatal detectada.
- El blob de la galería permaneció sin cambios durante la actualización. Captura/confirmación de cámara sigue pendiente.


**Actualización cámara (15:19):** el usuario capturó y guardó una imagen desde la cámara del sistema. El blob cifrado cambió a 136,758 bytes, el temporal se eliminó y la imagen persistió tras cerrar/reabrir; se mostró en el editor y en simulación de emergencia. Prueba de reemplazo por `medicamento.jpeg` en curso; eliminación pendiente.

## Pausa de pruebas — estado vigente (2026-11-07, 15:39–15:43)

Las notas de estado anteriores registran mediciones históricas. La siguiente lista prevalece para retomar el trabajo:

- Corregido en `MainActivity.kt`: el editor de mensaje ahora invalida y recarga el bitmap cuando se reemplaza o elimina la foto. Antes, tras el borrado podía quedar una imagen antigua en memoria mientras el perfil ya indicaba que no había foto.
- `testDebugUnitTest assembleDebug`: `BUILD SUCCESSFUL`, 17 pruebas (13 detector + 4 contactos), sin fallos, errores ni omitidas.
- APK actual: `app/build/outputs/apk/debug/app-debug.apk`, 8,108,878 bytes, SHA-256 `26DD36C94D5468F1D1BD43C8189E0833D8B9F45C2AFA5DD6F591BBF73C859AF7`; paquete `com.impulsos.app`, versionCode 1, versionName 0.1.0, firma v2 verificada.
- Instalación actualizada conservando datos: `adb install --no-streaming -r` terminó en `Success`; `lastUpdateTime=2026-11-07 15:38:26`. Durante la espera no-streaming, ADB confirmó que los 8.1 MB ya se habían copiado y `dex2oat` procesaba otras actualizaciones. La sesión terminó sin sesiones activas; el intento no-streaming concluyó correctamente. No se desinstaló la app ni se borraron sus datos.
- La versión instalada abrió `MainActivity`; no apareció `FATAL EXCEPTION`. Después del relanzamiento el blob de la foto seguía presente y el editor mostró la descripción accesible, además de las acciones Reemplazar/Eliminar.
- Reemplazo: en el Photo Picker real se eligió `medicamento.jpeg`, se confirmó la vista previa y Guardar; el blob cifrado volvió a 23,263 bytes (15:31). Tras cerrar y reabrir la app, el editor mostró la foto. La simulación se abrió desde la acción visible de la app y mostró `SIMULACIÓN · NO ES UNA EMERGENCIA REAL` y la descripción accesible de la fotografía; no se ejecutó el flujo de emergencia real.
- Borrado con la APK corregida: se eliminó la foto desde el editor; `emergency-photo.bin` desapareció y la interfaz pasó inmediatamente a «Añadir fotografía (opcional)», sin dejar visible el bitmap anterior.
- Para pausar, se canceló el Photo Picker sin selección ni guardado. Estado final de prueba: ImpulSOS quedó en el editor de emergencia y el perfil de prueba está sin fotografía cifrada (`no_backup` conserva solo `medical-profile.bin`).
- La cámara del sistema sí se probó y persistió en una APK anterior de esta iteración (15:19); su recorrido aún no se ha repetido en la APK instalada a las 15:38.

### Pendiente para retomar

- Reimportar `medicamento.jpeg` con la APK actual mediante Photo Picker, guardar y confirmar persistencia tras cerrar/reabrir.
- Con esa imagen, repetir la comprobación de la fotografía en simulación de emergencia usando la versión instalada a las 15:38.
- Repetir captura y cancelación de cámara en la APK actual; verificar reemplazo por cámara si el tiempo lo permite.
- Continuar pruebas físicas seguras del detector solo cuando el usuario pueda: intervalos/latencia, movimientos cotidianos, falsos positivos y falsos negativos. La sesión anterior contó 2 patrones físicos aceptados, pero no guardó el intervalo exacto ni midió el retardo; no equivale a una batería de validación.
- TalkBack, escalado de fuente, permisos/cancelaciones, Doze, pantalla bloqueada, audio/TTS, latencia, consumo de batería y regresión funcional completa siguen pendientes.

El Photo Picker quedó cancelado. El monitoreo normal permanece pausado por la acción previa del usuario; no se reactivó durante esta pausa. No se activó una emergencia real.

## Continuación — 2026-11-10

- Motorola one respondió como `device`; la APK instalada seguía en versionCode 1, versionName 0.1.0, `lastUpdateTime=2026-11-07 15:38:26`.
- Al iniciar esta sesión, el almacenamiento privado ya contenía `emergency-photo.bin` (23,269 bytes; fecha 2026-11-09 10:21) y `medical-profile.bin`. La causa/origen del guardado que ocurrió entre sesiones no se infiere. La imagen cargó en el editor con la descripción accesible de foto guardada.
- La simulación identificada mostró `SIMULACIÓN · NO ES UNA EMERGENCIA REAL` y «Fotografía de referencia elegida por la persona». No se observó `MonitoringService` ejecutándose ni se activó alarma real.
- Se repitió cámara en la APK instalada: abrir desde el editor → cámara Motorola → captura real → vista previa de ImpulSOS → confirmar Guardar. El archivo privado cambió a 65,110 bytes a las 07:18 (hora del teléfono); temporal de 2,464,233 bytes se borró del directorio `cache/impulsos-camera` tras guardar.
- `am force-stop` y relanzamiento conservaron el blob cifrado de 65,110 bytes; el editor volvió a mostrar la descripción accesible de foto guardada y los controles Reemplazar/Eliminar. No se encontró `FATAL EXCEPTION` en el log filtrado.
- Al intentar volver a la simulación, Android reportó temporalmente la ausencia de servicios `input`/`activity`; reaparecieron a los pocos segundos. El teléfono se desconectó de ADB durante el intento posterior, por lo que no se completó la comprobación de la foto de cámara en la simulación en esta sesión.
- ADB terminó con el dispositivo desconectado. No se insistió en reconexión ni se tocaron ajustes del teléfono.

**Pendiente para continuar:** completar el protocolo físico del detector (métricas de intervalo/latencia y actividades cotidianas), TalkBack, escalado, Doze, pantalla bloqueada, voz/audio, consumo de batería y regresión funcional. Pruebas de foto/cámara para esta APK se detallan a continuación.

Actualización al cierre del 2026-11-10: se forzó de verdad `testDebugUnitTest --rerun-tasks`; terminó `BUILD SUCCESSFUL` en 1m15s. Reportes: 13 pruebas de detector + 4 de contactos, 0 fallos, 0 errores y 0 omitidas. `assembleDebug` de esta sesión terminó `UP-TO-DATE`; el hash del APK sigue `26DD36C94D5468F1D1BD43C8189E0833D8B9F45C2AFA5DD6F591BBF73C859AF7` (8,108,878 bytes). La ejecución forzada recompiló Kotlin y mostró solo el warning conocido de `systemUiVisibility` obsoleto en `EmergencyActivity.kt:208`.

### Continuación de pruebas físicas — cierre 2026-11-10

- ADB reconectó tras el corte transitorio. La captura de cámara descrita arriba se verificó también en la simulación segura: `SIMULACIÓN · NO ES UNA EMERGENCIA REAL`, foto presente, sin servicio `MonitoringService` activo.
- Se repitió la importación explícita de `/sdcard/Download/medicamento.jpeg` mediante Photo Picker en la APK actual; el usuario eligió el archivo y se confirmó Guardar desde la vista previa de ImpulSOS. El blob cifrado quedó en `no_backup/emergency-photo.bin` (23,269 bytes, 07:27 hora del teléfono), modo `0600`; la caché temporal de cámara estaba vacía.
- Tras `am force-stop` y relanzamiento, el blob persistió. El editor mostró la descripción accesible «Fotografía guardada para la pantalla de emergencia» y acción Reemplazar. La simulación volvió a mostrar el encabezado inequívoco de práctica y la descripción «Fotografía de referencia elegida por la persona»; no se activó alarma ni emergencia real.
- Cancelación real de cámara en APK actual: con `medicamento.jpeg` guardada, se abrió la cámara externa y se volvió atrás sin capturar. La temporal vacía desapareció, el blob anterior permaneció en 23,269 bytes y el editor siguió indicando foto guardada. Se comprobó por tanto conservación al cancelar.
- Una captura de cámara real y el reemplazo de `medicamento.jpeg` han sido probados en APK actual; el borrado se había comprobado previamente en esta misma APK y devolvió el editor al estado vacío sin bitmap anterior.
- ADB estaba conectado al cierre. No se registraron pruebas físicas nuevas del gesto en esta continuación; el protocolo amplio de movimientos cotidianos/latencia continúa pendiente.

### Detector chop-chop — revisión 0.1.3, continuación 2026-11-10

- Se instaló la APK `com.impulsos.app`, versionCode 4 / versionName 0.1.3, manteniendo los datos; `lastUpdateTime=2026-11-10 08:33:37`. APK de 7,975,604 bytes, SHA-256 `B9B55D9E87C612ADA090C1D49E2DD9443624E80340B2A2039E6C6610E615BF7E`. `MainActivity` abierta sin excepción fatal observada.
- La prueba física previa de 0.1.2 mostró un impulso a 75 ms y el candidato a 205 ms rechazado con separación 130 ms. La 0.1.3 cambia el umbral relativo del valle, no los umbrales de intensidad ni los 200 ms mínimos. La nueva sesión sí registró patrones físicos, pero hubo movimientos adicionales y no permite concluir sensibilidad ni falsos positivos.
- Suite unitaria: 27/27 aprobada; 21 del detector, 4 de contactos y 2 de sesión diagnóstica. Señal sintética en Motorola: 4 patrones aceptados, intervalos de 240 y 260 ms, rebotes de 120 y 140 ms, pico dinámico 4.38 m/s². Sin `MonitoringService` activo.
- Diagnóstico físico 0.1.3: intervalo de muestras 19 ms; máximo dinámico 98.79 m/s²; dos patrones aceptados con intervalos 579 y 225 ms; dos candidatos descartados por dirección (318 y 504 ms). Entrega sensor-a-callback de 73–74 ms para las aceptaciones observadas; no es latencia total de emergencia.
- El usuario informó que hubo otros movimientos en la sesión. No es posible adjudicar las aceptaciones exclusivamente a los dos tajos ni estimar falsos positivos/negativos. Se detuvo el listener antes de reconectar; `MonitoringService` permaneció ausente y no hubo alarma/emergencia real.
- Para cerrar la sesión se abrió y detuvo el lector físico sin hacer nuevos gestos: última lectura 18 ms entre muestras, dinámica máxima 0.38 m/s², cero patrones y fuente inactiva. No aparece servicio de monitoreo. Después ImpulSOS se cerró con `am force-stop` sin borrar datos; el teléfono estaba reconectado por ADB.
- No se hizo otro build o instalación tras la prueba. Los documentos vigentes son README, este estado y `docs/PLAN_DE_PRUEBAS.md`; para retomar: iniciar con las limitaciones de atribución física y batería, ejecutar una prueba estructurada solo cuando el usuario tenga tiempo y mantener Diagnóstico seguro (nunca emergencia real).
- Cierre de entorno: servidor ADB apagado con `adb kill-server`; daemon Gradle detenido con `gradlew --stop`; verificación final sin procesos ADB, emulador ni Gradle/Java. En el último intento de limpieza ADB respondió `no devices/emulators found`; por ello no se comprobó si sigue en el teléfono el volcado temporal de jerarquía `/sdcard/window.xml` generado durante la inspección de la pantalla de diagnóstico. No contiene una lectura médica.
- `adb` no inicia desde el sandbox por fallo local de `fdevent socketpair`; ejecutar ADB en una única sesión local permitió verificar el dispositivo y su pantalla. No se cambió ninguna configuración del teléfono.
- Consumo de batería, variación en actividades cotidianas, falsos positivos/negativos y latencia completa hasta emergencia siguen sin medir. No hubo alarma ni emergencia real.

### Historial de Diagnóstico seguro — 0.1.4, 2026-11-11

Se compiló e instaló la versión 0.1.4 (código 5) en el Motorola. Pasaron 30 pruebas unitarias. El diagnóstico conserva en la pantalla hasta ocho resúmenes de lecturas físicas detenidas manualmente y permite copiarlos juntos. En el teléfono se verificaron la pantalla de historial vacío, la fuente inactiva y la ausencia del servicio de monitoreo. Queda pendiente el bloque físico de cuatro ensayos y la comprobación de su copia. Ver el procedimiento y los metadatos de la APK en `docs/PLAN_DE_PRUEBAS.md`.

### Ajuste del detector — 0.1.5, 2026-11-11

El historial físico de 0.1.4 registró `1, 1, 3, 4, 1, 0, 0, 2` patrones en los ocho ensayos. La 0.1.5 exige dos crestas confirmadas, usa un piso experimental de 28 m/s² en modo equilibrado y mide el intervalo entre sus máximos. Pasaron 34 pruebas unitarias. ADB confirmó la instalación de 0.1.5 (código 6), apertura correcta y Diagnóstico seguro detenido. Falta repetir los ensayos dirigidos con 0.1.5 y medir el consumo. Los datos del bloque anterior quedaron en `docs/PLAN_DE_PRUEBAS.md`.

### Verificación de 0.1.5 y guarda de caída de 0.1.6 — 2026-11-11

El bloque de cinco pruebas produjo `0, 1, 0, 0, 0`. Se reconoció el doble tajo moderado y se rechazaron las tres actividades cotidianas. El tajo natural se perdió en una sesión que registró bloqueo por posible caída. La 0.1.6 exige persistencia de la baja aceleración antes de descartar el gesto y conserva 800 ms de protección tras confirmar. Pasaron 36 pruebas y ADB confirmó la instalación de 0.1.6 (código 7).

El historial físico de 0.1.6 confirmó `1, 1, 0` para dos dobles tajos naturales y manipulación habitual. Los intervalos fueron 489 y 471 ms. Diagnóstico seguro quedó abierto y detenido, con el servicio de monitoreo ausente. Se conserva esta versión del detector para medir batería, comportamiento con la pantalla bloqueada y latencia de apertura mediante una ruta segura.

### Guía paso a paso y apariencia — 0.1.7

La guía conserva diez pasos e incorpora la demostración reproducible del doble tajo, la entrada directa desde Sensor, la práctica sin alarma y un acceso a la simulación de emergencia. Ajustes permite elegir Sistema, Claro u Oscuro. En el Motorola se recorrieron los diez pasos y se comprobó la persistencia del modo oscuro tras volver a abrir la aplicación. La práctica guiada abrió con el sensor detenido. Su reconocimiento físico, TalkBack y la verificación en el dispositivo con animaciones desactivadas siguen pendientes.
