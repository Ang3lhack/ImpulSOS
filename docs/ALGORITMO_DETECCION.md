# Algoritmo de detección — ImpulSOS

## Estado

Versión inicial heurística en `ShakeDetectorLogic`. No es una técnica validada clínicamente y no garantiza que se detecte una emergencia o que se eviten todos los falsos positivos.

## Procesamiento

1. Lee solo el acelerómetro integrado con `SensorManager`.
2. Estima gravedad por eje mediante un filtro pasa-bajas (`alpha = 0.8`) y resta esa estimación para obtener el movimiento dinámico.
3. Calcula la magnitud del vector dinámico.
4. Abre una ventana temporal al superar el umbral dinámico inicial de **2.6 m/s²**.
5. Evalúa una ventana aproximada de **3 s**, con un máximo de **420 ms** de interrupción tolerada, ocupación activa mínima del **42 %** y al menos **18 alternancias** del eje dominante.
6. Solo cuenta alternancias con semiperiodo entre **80 y 360 ms**, lo que reduce respuesta a vibración de alta frecuencia y a patrones lentos.
7. Reinicia la ventana si el movimiento pierde continuidad o si no cumple las condiciones.

La persona puede elegir tres presets sin números técnicos: **Más sensible** (2.2 m/s²), **Equilibrada** (2.6 m/s², predeterminada) y **Menos sensible** (3.2 m/s²). La selección se guarda localmente y, si el monitoreo está activo, se aplica reiniciando el estado del detector.

Estos parámetros son valores iniciales de ingeniería. El ensayo unitario usa datos sintéticos; todavía requieren pruebas físicas controladas de reposo, caminar, correr, transporte, vibración y sacudida deliberada en el Motorola. No deben interpretarse como umbrales científicamente validados.

## Falsos positivos y límites

El filtro temporal descarta una sacudida aislada, una caída con impacto único, cadencias lentas y vibraciones rápidas simuladas. Un movimiento real puede parecerse a una sacudida, y el resultado depende del montaje, orientación, tasa de muestreo, hardware, funda y forma de sujetar el teléfono. No se afirma que caminar, correr o transporte queden descartados en todos los casos.

El servicio usa `START_NOT_STICKY`; requiere consentimiento explícito al activar el monitoreo y muestra una notificación continua. Android, Doze y políticas del fabricante pueden detenerlo.

## Latencia

Se registra `trigger_to_notification_ms` desde la validación hasta publicar la notificación de emergencia. Esto no mide el tiempo de encendido de pantalla ni demuestra el objetivo total menor a un segundo. Esa medición debe realizarse en el dispositivo físico con Logcat.
