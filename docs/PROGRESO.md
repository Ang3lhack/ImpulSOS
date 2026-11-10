# Progreso

## 2026-11-06

- Auditoría inicial completada; se leyó `AGENTS.md` y el prompt maestro adjunto.
- Inventario consultado de las 13 páginas, los 7 componentes y los tokens de Penpot; isotipo exportado.
- Proyecto Android creado desde cero con Kotlin/Compose, Gradle Wrapper 8.0, AGP 8.1.4, minSdk 26, compileSdk 34 y targetSdk 29.
- Temurin 17.0.21 seleccionado por proyecto. `JAVA_HOME` sigue sin definirse globalmente; `java` del PATH continúa siendo Java 26.
- Implementación inicial del perfil cifrado local, pantallas, servicio de monitoreo, detector heurístico y pantalla de emergencia.
- Compilación debug exitosa con `assembleDebug` y luego `installDebug` en el Motorola.
- Pruebas unitarias: 11/11 exitosas, incluyendo patrón de 60 ms y selección de los preajustes de sensibilidad.
- Motorola: onboarding, inicio, perfil y sensor abiertos; existencia de acelerómetro confirmada.
- Verificado servicio foreground/notificación al activar y ausencia del servicio después de detenerlo.
- Verificada persistencia cifrada con dato sintético, tras reinicio de la app; el dato se borró al terminar.
- Verificada simulación no real, cancelación de pulsación a 2 s y finalización a 5.4 s.
- El tono local de prueba se activó sin error de runtime; su nivel audible no se midió.
- Sensibilidad: se muestran tres opciones en lenguaje sencillo; se seleccionó “Más sensible”, se cambió de pestaña y se confirmó visualmente que persistió. El monitoreo quedó apagado.
- Última versión recompilada con `assembleDebug` e instalada en el Motorola mediante `installDebug`; `MainActivity` quedó abierta y no se observaron errores `AndroidRuntime` en el log consultado.
- V2: bienvenida única, tutorial repetible, administración manual de contactos, importación cifrada de foto, preferencias TTS y lectura opcional en emergencia implementados.
- `EmergencyContactRulesTest` añadió cuatro pruebas; en conjunto `testDebugUnitTest` pasó 15/15.
- Motorola: bienvenida mostrada y no repetida tras reinicio; tutorial abierto desde Ajustes y navegación probada; contacto sintético guardado, persistido y eliminado; ajustes TTS abiertos y prueba genérica solicitada.
- No se probó selección real desde agenda. La selección de foto se canceló por privacidad antes de elegir; se retiró el archivo temporal. TalkBack, foto persistida y voz automática de emergencia quedan pendientes.
- Pendiente: estímulo físico real del sensor, FSI con pantalla bloqueada, latencia, brillo perceptible, batería y calibración con movimientos cotidianos en el Motorola.
