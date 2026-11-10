# Requisitos funcionales y alcance

Fuente: `# PROMPT MAESTRO DEFINITIVO — IMPULSOS`, adjunto por la persona usuaria.

## Requisitos

- **RF01 — Perfil médico:** registrar, ver, editar y persistir localmente nombre, condiciones, alergias, medicamentos, medicamento de rescate, acciones a evitar, instrucciones y datos adicionales. No se precarga información médica ficticia.
- **RF02 — Segundo plano:** servicio de primer plano solo tras activación explícita, notificación persistente, ciclo de vida controlado, sensor liberado al detener y manejo de hardware no disponible.
- **RF03 — Detección:** `ShakeDetectorLogic` independiente de UI; magnitud, filtrado de gravedad, umbral configurable, ventana temporal de aproximadamente tres segundos, persistencia e interrupciones.
- **RF04 — Falsos positivos:** reducir activaciones por cadencias lentas, evento aislado, caída, vibración y movimiento accidental; nunca afirmar eliminación total. Incluye tres presets de sensibilidad sin exponer parámetros técnicos; calibrar en el Motorola.
- **RF05 — Pantalla:** solicitar encendido y mostrar sobre bloqueo mediante APIs oficiales; nunca eludir PIN, patrón, contraseña o biometría.
- **RF06 — Iluminación:** solicitar brillo máximo solo en la ventana de emergencia y restaurarlo al terminar; no alterar permanentemente la configuración global.
- **RF07 — Alarma:** alarma local persistente durante el evento, sin exceder restricciones del sistema ni duplicar recursos. Sonido original/local.
- **RF08 — Información:** emergencia visible con identidad opcional, condiciones, alergias, medicamentos, instrucciones, acciones a evitar e información adicional; sin recomendaciones médicas inventadas.
- **RF09 — Cierre:** mantener pulsado cinco segundos, progreso visual y cancelación al soltar antes; detener la alarma y restaurar la ventana. Mantener el monitoreo si es seguro.
- **RNF01–05:** ciclo crítico offline, flujo sin menús en emergencia, consumo responsable, privacidad local/cifrada, sin datos médicos en logs y medir latencia antes de declarar el objetivo menor a un segundo.

## Límites explícitos

Android nativo, Kotlin, minSdk 26, operación local; sin Internet, servidor, nube, analítica, GPS, SMS automáticos, llamadas automáticas ni wearables. Solo acelerómetro, pantalla y altavoces.

## Estado de la primera implementación

Implementados en código: perfil local cifrado, mensaje/instrucciones, medicamentos, onboarding, inicio, controles de monitoreo, servicio de primer plano, detector heurístico con tres presets, actividad de emergencia, alarma local, prueba corta de tono, pantalla sobre bloqueo solicitada, brillo temporal y cierre por cinco segundos. Pendientes de aceptación física: calibración con movimientos, medición de latencia y batería, audibilidad real, pantalla de bloqueo y Doze.

No implementados todavía: selector de contactos del teléfono, carga de foto del dispositivo, tutorial completo, accesibilidad/idioma configurable y vistas de ajuste detallado. No se implementa la función de IA de Penpot por requerir conectividad.
