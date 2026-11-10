# Auditoría y plan de la versión 2

Fecha: 2026-11-06. Fuente: código local y documento Penpot “Aplicacion (copia)”.

## Base auditada

- Kotlin 1.9.10, Jetpack Compose, módulo Android `:app`.
- Gradle Wrapper 8.0, AGP 8.1.4, Java 17; Compose Compiler 1.5.3, Material 3 1.1.2.
- `minSdk 26`, `compileSdk 34`, `targetSdk 29`; dispositivo previsto Motorola one, Android 10/API 29.
- Perfil de salud cifrado AES/GCM con llave Android Keystore, en directorio no respaldado.
- Servicio foreground iniciado por el usuario, acelerómetro, selector de sensibilidad, notificación, alarma de tono y Activity de emergencia con gesto de pulsación prolongada. La simulación está rotulada y separada de la emergencia real.
- El manifiesto no declara permiso de red, contactos, fotos, llamadas ni SMS. No se observó cliente HTTP ni módulo de IA.
- `MainActivity.kt` concentra actualmente las pantallas Compose y navegación; el perfil y el detector ya tienen módulos separados. No hay `README.md` previo.

## Funciones auditadas

| Área | Estado observado antes de V2 |
| --- | --- |
| Inicio | Inicio de configuración y página Home existentes; sin bienvenida con “Toca para comenzar”. |
| Tutorial | No existe flujo tutorial repetible. |
| Perfil | Campos de salud cifrados localmente; prueba anterior con dato ficticio ya borrado. |
| Instrucciones | Texto local; sin foto. |
| Contactos | Campo simple de contacto de confianza; sin agenda ni administración de varios contactos. |
| Accesibilidad | Compose ofrece semántica base y la emergencia tiene acción personalizada; no hay auditoría TalkBack integral registrada. |
| Voz | Tono de alarma y control de prueba; no hay TTS configurable. |
| Animación | No hay sistema de transición/microinteracciones documentado; priorizar baja carga y movimiento reducido. |
| IA | No integrada. Mantener la función crítica independiente y offline. |

## Penpot frente a los nuevos requisitos

Penpot se consultó de nuevo mediante MCP. El documento tiene 13 páginas, diseño en Roboto y componentes/tokens existentes. Sus páginas cubren portada, sistema visual, onboarding, inicio, perfil, medicamentos, mensaje, asistente IA, contactos, emergencia, sensor, ajustes y flujos/estados. La interfaz implementada toma la paleta existente y varias pantallas de esas páginas.

Las pantallas de bienvenida explícita con CTA “Toca para comenzar”, tutorial permanente, foto de instrucciones y controles de configuración TTS descritos por V2 no se identificaron como tableros dedicados en el inventario disponible; se tratarán como nuevos flujos provisionales, manteniendo los tokens. No se modificó Penpot.

## Plan y riesgos

1. Bienvenida de una sola vez y tutorial que siempre se pueda abrir desde Ajustes.
2. Guardado privado de fotografía redimensionada mediante Photo Picker y selección de contacto puntual mediante UI del sistema, además de contactos manuales. La prueba de selección de ambos queda pendiente.
3. Preferencias TTS locales y voz opcional en la Activity de emergencia sin bloquear la pantalla.
4. Mejoras de semántica, escalado y movimiento reducido; validar TalkBack manualmente donde sea posible.
5. Documentar IA como no integrada hasta tener un caso de uso no clínico, política de datos, consentimiento, costos y despliegue revisados.

El algoritmo de sacudida no se reajusta dentro de esta fase sin datos de hardware. Los estándares mencionados en los documentos de entrada son referencias de diseño/riesgo, no evidencia de cumplimiento ni certificación. La detección, el funcionamiento bajo bloqueo y la eficacia de alarma requieren evaluación de producto/seguridad especializada antes de cualquier uso como dispositivo médico.

La evaluación actual de IA, TTS, Loquendo y referencias regulatorias está en [EVALUACION_IA_Y_REFERENCIAS.md](EVALUACION_IA_Y_REFERENCIAS.md). No se integra IA ni se envían datos.
