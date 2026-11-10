# Evaluación de IA, TTS y referencias de diseño

Revisión documental: 2026-11-06. No se incorporó ningún servicio remoto, voz o clave.

## IA

| Alternativa | Disponibilidad/costo | Datos y privacidad | Adecuación a ImpulSOS |
| --- | --- | --- | --- |
| OpenAI API | API pública con cobro por modelo/uso; tabla de precios cambia y debe revisarse antes de presupuestar. | La API no usa por defecto sus entradas/salidas para entrenar, pero por defecto puede generar registros de abuso que contengan contenido durante hasta 30 días; algunos endpoints persisten estado. No equivale a procesamiento local ni sustituye una revisión legal/contractual. | No usar con información médica ni en el núcleo. Una eventual prueba de función no clínica necesitaría backend propio, consentimiento granular y política de retención. |
| Gemini Developer API | API pública con tiers gratuitos y pagados; límites y tarifas dependen del modelo y cambian. La documentación de precios indica que los datos de algunos modelos/tier gratuitos pueden usarse para mejorar productos; la tabla distingue condiciones de tiers pagados. | Requiere transmisión a Google. No enviar datos médicos. Revisar condiciones específicas del modelo, región y tier elegido. | No usar con información médica ni en la activación. |
| Modelo local | No se eligió ni se añadió un runtime/modelo. Hardware, tamaño, energía, licencias y calidad tendrían que evaluarse para un caso concreto. | Podría evitar transferencia si se demuestra que todo el procesamiento ocurre localmente; no es garantía por sí sola. | Sin caso de uso no clínico seguro definido, no se integra. |

**Decisión:** no integrar IA en V2. La aplicación no tiene una tarea de IA necesaria; generar, resumir o reescribir instrucciones de emergencia podría cambiar su significado y crear riesgo. No hay clave en Android ni permiso de red. Si en el futuro se propone una utilidad no clínica, debe ser opcional, aislada, con consentimiento previo, datos mínimos no médicos, evaluación de costos/retención y salida revisada por la persona.

Referencias oficiales: [precios de OpenAI API](https://platform.openai.com/pricing/), [controles de datos de OpenAI API](https://developers.openai.com/api/docs/guides/your-data), [precios del Gemini Developer API](https://ai.google.dev/gemini-api/docs/pricing).

## Texto a voz y Loquendo

- Se utiliza la API nativa Android TextToSpeech y motores/idiomas instalados en el teléfono. Android expone voces y la propiedad Voice.isNetworkConnectionRequired() para diferenciar las que requieren conexión.
- La función TTS es opcional y no sustituye TalkBack. La pantalla médica aparece sin esperar inicialización del motor; si falla la voz, la presentación visual sigue disponible.
- Las voces del dispositivo tienen disponibilidad, licencias y funcionamiento offline propios del motor. La aplicación no descarga voces.
- No se verificó en fuente oficial accesible un API Android pública gratuita ni una licencia de distribución de Loquendo para esta aplicación; no se afirma que no exista una oferta comercial. No se descargó ni integró Loquendo. Hace falta contacto/licencia directa del titular para evaluar esa opción.

Referencias: [Android TextToSpeech Engine](https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine), [Android Voice](https://developer.android.com/reference/android/speech/tts/Voice).

## Referencias de diseño y normas

- **ISO 14971:2019** especifica un proceso de gestión de riesgos para dispositivos médicos, incluido software como dispositivo médico. Su aplicación regulatoria depende de clasificación, mercado y uso previsto; esta auditoría no determina esa clasificación.
- **IEC 62304** describe procesos del ciclo de vida de software para dispositivos médicos; **IEC 62366-1** trata ingeniería de usabilidad relacionada con seguridad de dispositivos médicos. Citar estas normas no prueba cumplimiento ni sustituye un sistema de calidad.
- **ISO 9241-210:2019** sigue vigente según la ficha consultada (revisada/confirmada en 2025) y ofrece procesos/recomendaciones de diseño centrado en las personas.
- **IEC 60601-1-8** tiene alcance sobre sistemas de alarma de equipos y sistemas electromédicos. No se traducen sus requisitos de señal a una alarma de teléfono sin análisis de aplicabilidad y acceso a la norma.
- **WCAG 2.2** es una recomendación W3C para accesibilidad de contenido web. Se usa como referencia heurística para contraste y uso del color, no se declara conformidad nativa Android a partir de ello.
- **APCA** puede usarse como métrica complementaria de contraste visual, pero no se acepta la afirmación de la fuente de diseño aportada de que sea un estándar normativo universal en 2026. WCAG 2.2 mantiene criterios de contraste propios; no se declara APCA ni WCAG aprobado sin una medición de todas las pantallas.

Referencias: [ISO 14971](https://www.iso.org/standard/72704.html), [IEC 62304](https://www.iso.org/standard/38421.html), [IEC 62366-1](https://www.iso.org/obp/ui?_escaped_fragment_=iso%3Astd%3Aiec%3A62366%3A-1%3Aed-1%3Av1%3Aen), [ISO 9241-210](https://committee.iso.org/standard/77520.html?browse=tc), [IEC 60601-1-8](https://webstore.iec.ch/en/publication/2598), [WCAG 2.2](https://www.w3.org/TR/WCAG22/), [estado de APCA/WCAG 3](https://git.apcacontrast.com/documentation/README).

Estos documentos sirven como referencias para planear análisis y pruebas. ImpulSOS no está certificado ni homologado, y este informe no es una evaluación regulatoria.
