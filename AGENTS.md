# AGENTS.md — PROYECTO IMPULSOS

## Identidad del proyecto

Nombre: ImpulSOS.

Aplicación Android nativa de asistencia ante emergencias médicas.

## Herramientas

- Codex  como agente principal de programación.
- Penpot MCP como fuente de diseño e identidad visual.
- Android SDK Command-line Tools.
- Android SDK Platform-Tools.
- ADB para conexión con dispositivos físicos.
- Gradle Wrapper para compilación.
- Kotlin y Jetpack Compose, si son compatibles con la arquitectura elegida.

## Entorno de desarrollo

La computadora tiene recursos limitados.

Está prohibido depender de Android Emulator para las tareas habituales de desarrollo y validación.

El dispositivo físico Motorola conectado por USB será el dispositivo principal de pruebas.

No descargar imágenes de sistemas virtuales ni crear AVD.

Evitar ejecutar procesos innecesarios simultáneamente.

## Flujo obligatorio

1. Inspeccionar el proyecto antes de modificarlo.
2. Consultar Penpot MCP para recuperar los diseños.
3. Mantener la identidad visual existente.
4. Crear componentes reutilizables.
5. Implementar las funciones progresivamente.
6. Ejecutar compilaciones incrementales.
7. Corregir los errores de compilación.
8. Instalar la aplicación en el Motorola mediante ADB.
9. Realizar pruebas en el dispositivo físico.
10. Documentar resultados y problemas pendientes.

## Compilación

Utilizar Gradle Wrapper.

Comando principal:

`.\gradlew.bat assembleDebug`

Para instalar en el dispositivo conectado:

`.\gradlew.bat installDebug`

No instalar Gradle globalmente si el proyecto ya incluye su Wrapper.

## Penpot MCP

Antes de realizar modificaciones:

- Inspeccionar las páginas.
- Identificar componentes.
- Recuperar estilos.
- Analizar la identidad visual.
- Documentar las estructuras existentes.

No destruir diseños originales.

Conservar una referencia antes de realizar cambios.

## Seguridad

- No inventar protocolos médicos.
- No modificar arbitrariamente la lógica del acelerómetro.
- No simular estados técnicos inexistentes.
- No almacenar información médica real en repositorios.
- No compartir claves MCP.
- No realizar operaciones destructivas sin autorización.

## Criterio de finalización

Una función no se considera terminada únicamente porque el código esté escrito.

Debe compilar correctamente y, cuando corresponda, probarse en el Motorola.

Documentar qué se probó realmente y qué permanece pendiente.

## Arquitectura y tecnologías actuales

- Aplicación Android nativa en Kotlin y Jetpack Compose.
- Módulo `:app`; lógica de dominio en `domain`, datos en `data`, servicio en `sensor`, interfaz y actividad de emergencia en `ui`/`emergency`.
- Gradle Wrapper 8.0, Android Gradle Plugin 8.1.4, Kotlin 1.9.10, Java 17, Compose Compiler 1.5.3 y Material 3 1.1.2.
- `minSdk 26`, `compileSdk 34`, `targetSdk 29`; validar todos los cambios en Android 10/API 29.
- No reemplazar arquitectura ni dependencias sin necesidad comprobable. Revisar `docs/ARQUITECTURA.md` y mantenerlo actualizado al cambiar límites entre capas.

## Identidad visual y experiencia de uso

- Mantener tokens de Penpot: violeta `#5553D7`, lima `#BEF54D`, fondo `#F5F6FA`, rojo emergencia `#C62D42`, tipografía Roboto.
- Usar navegación predecible, frases breves, jerarquía clara y botones táctiles amplios (objetivo de diseño mínimo 52 dp en controles críticos).
- Evitar párrafos densos, truncar datos médicos, depender del color para comunicar estados o esconder información crítica tras menús.
- Diseñar estados vacíos, carga, éxito, error y operación; indicar qué ocurrió y cómo resolver errores.
- La emergencia tiene prioridad; no hacer que animaciones, red, TTS ni accesos opcionales retrasen la vista médica.
- Respetar escalado de texto y reducir o eliminar movimiento cuando el usuario lo solicite o el sistema indique animaciones reducidas.
- Medir contraste antes de declarar cumplimiento WCAG o APCA. No presentar metas editoriales de APCA como norma universal sin fuente y contexto verificados.

## Seguridad, contenido médico y privacidad

- No inventar protocolos, diagnósticos, tratamientos ni dosis. Diferenciar los datos del usuario del texto de la aplicación; no afirmar sustitución de servicios de emergencia.
- Mantener el núcleo (sensor, emergencia, perfil e instrucciones) local y operativo sin Internet. No añadir permiso de red salvo requisito aprobado y documentado.
- No enviar datos médicos ni contactos a servicios externos. No integrar IA al núcleo ni permitir que decida emergencias o genere indicaciones clínicas.
- Toda transferencia externa de datos requeriría consentimiento informado explícito, revisión de privacidad/seguridad y arquitectura documentada; nunca incluir secretos API en el cliente o repositorio.
- Mantener el perfil cifrado con Android Keystore, en almacenamiento privado y no respaldado. Evitar datos reales de salud en pruebas, capturas, logs y repositorio.
- No solicitar permisos de contactos, archivos, ubicación, llamadas o SMS si un selector del sistema o alternativa local sirve. Nunca llamar o enviar SMS automáticamente.
- Revisar los límites del SO y del fabricante: servicio foreground no garantiza ejecución permanente; notificaciones, pantalla de bloqueo, TTS, sensores y políticas de energía dependen del estado y configuración del dispositivo.

## Accesibilidad y lectura por voz

- Compatibilidad con TalkBack de Android: etiquetas útiles, controles semánticos nativos, orden de lectura lógico, controles operables, mensajes de error anunciables y texto escalable.
- No crear un lector de pantalla propio ni ocultar semántica que necesitan los servicios de accesibilidad.
- TTS de emergencia es opcional y separado de TalkBack. Mostrar la pantalla de emergencia primero, no bloquearla esperando el motor y permitir detener/repetir la voz.
- Identificar las voces que requieren red y no ofrecerlas como opción offline. Liberar motores al terminar su ciclo de vida y manejar falta de idioma/voz sin impedir la función visual.

## Penpot y cambios de diseño

- Consultar Penpot antes de implementar una pantalla basada en diseño. Registrar páginas/componentes usados y diferencias en `docs/INVENTARIO_PENPOT.md`.
- No afirmar que existe un diseño si no se encontró. Los diseños originales se conservan; solicitar aprobación antes de modificar el documento de Penpot.
- Nuevas pantallas sin referencia de Penpot deben identificarse como diseño de producto provisional, coherente con tokens existentes.

## Pruebas y revisión de riesgos

- Para cambios funcionales, ejecutar pruebas unitarias/integración relevantes, `assembleDebug`, instalar en el Motorola físico y registrar qué validaciones se completaron. No usar emuladores ni crear AVD.
- No activar alarmas reales durante pruebas; usar exclusivamente rutas de simulación identificadas.
- TalkBack, escalado, Doze, pantalla bloqueada, volumen, latencia y detección física requieren pruebas explícitas en dispositivo; no inferir éxito por compilar.
- Revisar aplicabilidad de ISO 14971, IEC 62304, IEC 62366-1, ISO 9241-210, WCAG/APCA e IEC 60601-1-8 como referencias. No declarar certificación, homologación ni cumplimiento normativo; eso requiere análisis especializado.
- Documentar riesgos, decisiones relevantes, limitaciones de Android y resultados en `docs/PLAN_DE_PRUEBAS.md`, `docs/ESTADO_DEL_PROYECTO.md` y `README.md`.

## Comandos

Desde PowerShell, con Java 17 en `JAVA_HOME` y el Motorola autorizado por USB:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

No instalar Gradle global si el Wrapper está presente. No cambiar configuración global del teléfono sin explicar el propósito y restaurar el estado original al terminar una prueba.

## Diagnóstico seguro de movimiento

- El recorrido de producción (`MonitoringService`) y `DetectorDiagnosticScreen` deben permanecer separados. El diagnóstico no puede iniciar `EmergencyActivity`, crear notificaciones de emergencia ni reproducir alarma.
- Antes de instalar una APK, ejecutar pruebas unitarias y compilación. Verificar código/versión/hash del artefacto, resultado explícito de `adb install`, `lastUpdateTime` y apertura sin excepción.
- Para medir el sensor, registrar timestamps de `SensorEvent`; el periodo pedido al sistema no garantiza la frecuencia real recibida.
- La simulación del detector se etiqueta como señal sintética. No cuenta como prueba física ni como calibración.
- No activar el monitoreo normal para pruebas de movimiento salvo que una ruta segura esté demostrada sin alarma.

## Fotografías locales

- Preferir Photo Picker y cámara del sistema para evitar permisos globales de almacenamiento/cámara cuando el flujo de captura externo sea suficiente.
- Mostrar vista previa y pedir confirmación antes de sustituir la foto existente. Cancelar debe conservar la foto previamente guardada.
- Guardar una copia redimensionada y cifrada bajo `noBackupFilesDir`; borrar la imagen previa solo tras el guardado confirmado.
- No leer, descifrar ni decodificar la imagen desde el hilo principal. Cargarla con un efecto Compose ligado al ciclo de vida y `Dispatchers.IO`; la pantalla de emergencia se compone primero y la foto aparece después si está disponible.
- Cargar el perfil cifrado de emergencia en `Dispatchers.IO` tras componer la primera vista. TTS opcional puede inicializarse después de cargar el perfil; no debe bloquear la pantalla.
- La captura completa delegada al sistema usa `TakePicture` con URI privado; si se usa `TakePicturePreview`, documentar su resolución limitada. Revisar ruta FileProvider, permisos temporales y ciclo de vida al volver de la cámara.
- No dejar copias de prueba ni datos médicos fuera del almacenamiento privado; limpiar los artefactos sintéticos al finalizar.

## Skills y Penpot

- Inventariar Skills detectadas en `.agents/skills/` y registrar las relevantes utilizadas en `docs/INVENTARIO_SKILLS.md`. Leer completas las Skills aplicables y consultar referencias citadas que afecten la decisión técnica.
- Registrar cada página y componentes de Penpot inspeccionados en `docs/INVENTARIO_PENPOT.md`; distinguir lectura/exportación de una verificación real en dispositivo.
- Pictogramas de Compose deben tener descripción TalkBack cuando aporten información y ser decorativos (`contentDescription = null`) cuando el texto adyacente ya comunique lo mismo.

## Reglas ampliadas de cámara y fotografía

- Para captura completa delegada a cámara externa, usar `ActivityResultContracts.TakePicture` con un URI de `FileProvider` no exportado y ruta de caché estrictamente limitada. No pedir `CAMERA` ni almacenamiento amplio si el contrato de cámara y Photo Picker cubren el caso.
- Mantener identificable/restaurable la ruta temporal mientras la cámara externa está activa; limpiar temporales en cancelación, error y confirmación. La foto existente solo se reemplaza tras confirmar que la nueva copia cifrada fue guardada.
- Orientar las imágenes según EXIF tanto en vista previa como antes de guardar. Redimensionar antes de conservar y guardar solo el blob cifrado en almacenamiento privado no respaldado.
- Si el árbol de accesibilidad del selector del sistema no está disponible, no seleccionar elementos por coordenadas supuestas cuando pueda haber fotos personales; pedir al usuario que elija el archivo de prueba indicado y continuar al volver a la app.

## Inventario de Skills host

- El inventario de Skills debe separar `.agents/skills/` del proyecto y las Skills instaladas en rutas host (`.codex/skills`, plugins y runtimes). No declarar “nueva” sin una instantánea temporal de comparación.
- Registrar `utilizada` solo para las Skills cuyas instrucciones se leyeron y cuyo criterio se aplicó en una tarea verificable. Listar no equivale a utilizar.
