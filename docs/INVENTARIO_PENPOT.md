# Inventario de Penpot

Archivo conectado por MCP: `Aplicacion (copia)`. Inspección de lectura; no se modificó el diseño. Consulta actual: 13 páginas, 2,194 formas y 7 componentes en la biblioteca local. Hay tokens de color en el conjunto `ImpulSOS` y tipografía Roboto en los tableros. Los tableros usan formas vectoriales; no se encontraron fills de imagen/bitmaps en las páginas consultadas.

| Página | Estructura y propósito consultado | Estado de adaptación en Android |
|---|---|---|
| 00 — Cover & Project Overview | Portada, identidad ImpulSOS y propósito de asistencia local. | Isotipo, nombre y mensaje de bienvenida ya están en la app. |
| 01 — Design System | Paleta violeta/lima/rojo, fondo y superficie, tipografía Roboto, botones, campos, tarjetas y alerta. Biblioteca local con 7 componentes. | Tokens existentes aplicados en tema y componentes Compose; no se exportaron imágenes. |
| 02 — Onboarding | 14 tableros: splash, introducción, privacidad, offline, perfil, gesto, capacidades y seis vistas de tutorial. | Onboarding/tour de 10 pasos ahora con pictogramas tipográficos grandes, progreso y navegación; estilo adaptado. No se reutilizó ilustración separada porque la página organiza recursos como formas dentro de tableros y no como imagen exportable independiente. |
| 03 — Home & Monitoring | Inicio, monitoreo activo/pausado, permisos, prueba y sensibilidad. | Home y controles de monitoreo/sensibilidad existentes; diagnóstico seguro agregado como flujo separado. |
| 04 — Medical Profile | Perfil, datos personales, salud, padecimientos, alergias, advertencias y resumen. | Perfil local cifrado con campos editables. Medicamentos siguen en pantalla separada por límite del modelo actual. |
| 05 — Medications | Lista, captura y detalle de medicamentos/dispositivo. | Lista/campos locales ya implementados; no se agregaron nuevos flujos clínicos. |
| 06 — Emergency Message | Editor, vista previa, orden de instrucciones, prioridad y acciones a evitar. | Editor de instrucciones y foto opcional; picker ahora muestra preview/confirmar. Cámara del sistema con preview/confirmar. |
| 07 — AI Assistant | Propuesta, comparación, permiso, sin conexión y error. | No se implementa IA; el asistente no forma parte del núcleo offline ni debe generar contenido clínico. |
| 08 — Emergency Contacts | Contactos, alta, selección del dispositivo y eliminación. | Contactos manuales y picker del sistema con acceso puntual. |
| 09 — Emergency Mode | Emergencia inmediata, detalle de ayuda, alarma, contactos, estados de cierre y simulación. | Pantalla de emergencia y simulación separadas; foto local se muestra si existe. |
| 10 — Sensor | Gesto, sensibilidad, calibración y estado del sensor. | Tres sensibilidades y detector heurístico; nueva herramienta diagnóstica independiente evita alarmas. Calibración real aún requiere pruebas físicas. |
| 11 — Settings | Ajustes, sonido, brillo, accesibilidad, privacidad, texto, idioma e IA. | Ajustes existentes, tutorial repetible y voz local. Funciones de apariencia que requerirían cambiar configuración del sistema no se inventaron. |
| 12 — States & User Flows | Estados vacíos/errores, permisos, imagen no disponible, privacidad y mapa de navegación. | Se conservaron estados de error/ausencia en pantallas relevantes; falta recorrer exhaustivamente todos los flujos en el teléfono. |

## Componentes, recursos y estilos

- Lectura MCP de la biblioteca: 7 componentes locales, colección de colores/typographies vacía, conjunto de tokens `ImpulSOS` con 12 nombres de token color. La exportación del tablero `01 — Sistema visual | ImpulSOS` mostró paleta y muestras de botones/campos/tarjetas/alerta.
- Revisión de las 13 páginas: sin recursos tipo `image` o fill de imagen; los recursos gráficos presentes están formados por shapes vectoriales. Los pictogramas añadidos al tutorial son símbolos tipográficos, no assets Penpot reexportados.
- La inspección visual se hizo exportando PNG representativo de cada página y consultando jerarquías de los tableros. Esta revisión no valida el comportamiento interactivo del prototipo ni sustituye pruebas TalkBack/Motorola.
- Tokens observados: violeta primario `#5553D7`, violeta profundo `#38369C`, lima `#BEF54D`, fondo `#F5F6FA`, texto `#191A32`, emergencia `#C62D42`, éxito `#178353`; Roboto y tamaños visibles de 10–56 px en distintos roles.

## Reconsulta MCP — 2026-11-07

La sesión MCP responde en vivo: archivo `Aplicacion (copia)`, página actual `02 — Onboarding`; `currentFile.pages` enumeró 13 páginas y sus identificadores, coincidentes con la tabla anterior. Esta consulta confirmó conexión y disponibilidad de metadatos de página; la exportación visual de las 13 páginas y la inspección estructural documentadas arriba corresponden a la consulta previa. El MCP no implica que se modificara el documento.
