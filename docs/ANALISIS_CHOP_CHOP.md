# Doble tajo y consumo — 2026-11-10

## Fuentes examinadas

Se consultaron los árboles de GitHub y los archivos fuente mediante API/raw. No se incorporó código de terceros ni dependencias de estas aplicaciones; la adaptación modifica nuestro detector existente.

| Referencia | Revisión | Hallazgo y decisión |
| --- | --- | --- |
| [SebastianKuniec/Latarka](https://github.com/SebastianKuniec/Latarka) | `171a695294a22847cc0bb0e02b2fa3fb1e72feed` | `ShakeDetector.kt`: filtro de gravedad con alpha fijo, acumulación de eventos, exclusión de caída libre. `ShakeDetectionService.kt`: SENSOR_DELAY_GAME y PARTIAL_WAKE_LOCK de 30 minutos. Adoptamos explícitamente el rechazo de caída; conservamos nuestro filtro dependiente del tiempo. No incorporamos wake lock permanente ni control de flash. GPL-3.0 según el repositorio. |
| [NPFS42/Kinetic-Torch](https://github.com/NPFS42/Kinetic-Torch) | `92c06a323f81d7b4d4a73d31ac0a23ef5162bbb4` | `ChopDetector.kt`: pico–valle–pico, separación mínima 200 ms para no contar aceleración y frenado como dos tajos, valle relativo 80%, filtro de plano XY. El servicio usa sensor lineal o acelerómetro, proximidad y wake lock de hasta dos horas, con ventanas según pantalla. En el código leído, proximidad bloquea la decisión; no desregistra por sí sola el acelerómetro. Adaptamos valle relativo y distinción del rebote, con parámetros propios y repetición vectorial independiente de orientación. |
| [BespredeL/MacroDroid / ShakeFlashlight](https://github.com/BespredeL/MacroDroid/tree/main/ShakeFlashlight) | `46bd349236f20879d2a68fa9cd57ad0c8a98de7c` | Se leyó README y `Flashlight.macro`. Pantalla encendida/bloqueada y proximidad libre son condiciones activas. La antigua condición global de luz está deshabilitada; el flujo actual evalúa <50 lux dentro de un If y puede apagar la linterna con más luz. El reconocimiento inercial pertenece al motor externo de MacroDroid, no está publicado en esta macro. No adoptamos esas restricciones para emergencias. |

Ninguno de estos repositorios demuestra por sí solo un consumo bajo en nuestro Motorola. Acciones CI y manejo de cámara no validan consumo ni detección de emergencias.

## Problemas identificados y adaptación

- En una prueba física de 0.1.2 se observó un primer impulso a 75 ms y un segundo candidato a 205 ms, separado 130 ms y rechazado por el mínimo de 200 ms. También se encontró que exigir un valle absoluto bajo podía no rearmar tras una cresta fuerte. En 0.1.3 el valle es `max(peakRelease, max(umbral, crestaMedida) * 0.60)`; los umbrales de intensidad de las sensibilidades se conservan (2.8/3.2/4.2 m/s²). La secuencia física debe repetirse antes de concluir que esta adaptación resuelve el caso.
- Se exigen impulsos relacionados: coseno entre vectores >=0.5 (hasta 60 grados). Se evita imponer un eje específico de agarre.
- Separación entre los impulsos válidos: 200–600 ms. Picos a menos de 110 ms reinician el patrón por vibración; los de 110–200 ms se consideran rebote y requieren otro valle. Ventana general 1,200 ms; no hay espera de tres segundos.
- Magnitud bruta inferior a 0.3g invalida la secuencia y suprime impactos durante 800 ms desde la última muestra de caída libre. Son valores experimentales; no se debe probar soltando el teléfono.
- Valores no finitos reinician el detector. Una interrupción de muestras >650 ms reinicializa también el filtro de gravedad, evitando residuos de orientación.
- El primer disparo del servicio ya no se bloquea por comparar el tiempo desde arranque con un cooldown inicial de cero. Se mantiene el cooldown de 30 segundos después de una activación real.
- Un registro de sensor fallido ya no se anuncia como monitoreo activo.

## Consumo y entrega

Motorola one/API29: `dumpsys sensorservice` identifica LSM6DSM, versiones normal y wake-up, FIFO de 10,000 eventos y tasa máxima 200 Hz. La implementación anterior elegía el normal, que no despierta la CPU suspendida.

`MotionSensorPolicy` comparte entre producción y diagnóstico: preferencia por acelerómetro wake-up, muestras solicitadas cada 20,000 µs (50 Hz), entrega agrupada de hasta 100,000 µs si hay FIFO. Si no hay wake-up/FIFO, se usa entrega inmediata y no se promete detección en suspensión. No se añade wake lock, giroscopio, proximidad, luz ni permisos. Batching puede reducir despertares frente a wake-up sin agrupar, pero puede aumentar consumo frente al detector antiguo que dejaba de responder con CPU suspendida. No se ha medido ahorro porcentual ni autonomía.

Referencias de plataforma: [SensorManager](https://developer.android.com/reference/android/hardware/SensorManager), [batching AOSP](https://source.android.com/docs/core/interaction/sensors/batching). La latencia solicitada no garantiza latencia total de Android ni funcionamiento permanente.

El diagnóstico procesa todas las muestras, publica la UI aproximadamente a 10 Hz y retiene intervalo del último patrón y demora desde timestamp de muestra hasta callback. Esta demora no es el tiempo de apertura de emergencia ni una medición de batería. El listener se libera al salir y al pasar a segundo plano. Las señales sintéticas permanecen separadas.

## Validación

27 pruebas unitarias: 21 de detector, 4 de reglas de contacto y 2 de la sesión diagnóstica; todas aprobadas. Se añadieron escenarios de crestas fuertes repetidas, impulso/rebote aislado, movimiento transversal residual y vibración intensa de 20 Hz. En Motorola, la señal sintética mostró cuatro patrones aceptados con intervalos válidos de 240/260 ms y dos rebotes tempranos de 120/140 ms; máximo dinámico 4.38 m/s². La fuente quedó identificada como sintética y `MonitoringService` no estaba activo. La lectura física en reposo reportó 19 ms entre muestras y 0.01 m/s² dinámicos.

La APK 0.1.3 (`versionCode=4`) fue instalada y abierta en el Motorola. No se observó `FATAL EXCEPTION`; hash del artefacto `B9B55D9E87C612ADA090C1D49E2DD9443624E80340B2A2039E6C6610E615BF7E`. La señal sintética aceptó cuatro patrones. En la sesión física el modo seguro registró dos patrones (intervalos 579 y 225 ms), dos rechazos direccionales y demora sensor-a-callback de 73–74 ms. El usuario indicó que hubo otros movimientos durante esa sesión; no se sabe si cada aceptación correspondió al gesto previsto ni se puede puntuar como acierto/falso positivo. No se activó el servicio ni emergencia. La batería A/B sigue pendiente; no se afirma sensibilidad validada ni ahorro cuantificado.

Pendiente: calibración con secuencias físicas etiquetadas y batería A/B en reposo y movimiento. No considerar el detector validado por estas pruebas simuladas. La aplicación permanece offline y la validación en teléfono debe usar Diagnóstico seguro.

## Ajuste experimental a partir del Motorola — 0.1.5

El bloque físico de 0.1.4 mostró tres detecciones al caminar, cuatro al manipular el teléfono y dos durante el ensayo de bolsillo. Sus máximas dinámicas fueron 23.43, 11.96 y 13.02 m/s², respectivamente. El doble tajo moderado no fue aceptado pese a alcanzar 90.17 m/s². El tajo aislado alcanzó 117.72 m/s² y se rechazó. El rango temporal de detecciones cotidianas se solapó con el de los gestos intencionales.

El detector empieza a observar un impulso con el umbral de entrada existente y lo califica al bajar al 60 % de su cresta, con piso de valle de 1.2 m/s². Exige dos crestas confirmadas de al menos 28 m/s² en modo equilibrado, 26 en modo más sensible o 32 en modo menos sensible. Calcula dirección y tiempo en el máximo real de cada impulso. Los candidatos débiles no consumen la ventana del par y los rebotes fuertes próximos renuevan la protección contra vibración sin borrar el primer tajo válido. La confirmación agrega la demora hasta observar el descenso y ahora se registra por separado.

Estos pisos se eligieron a partir de un solo bloque de un dispositivo. Las simulaciones modelan candidatos con amplitudes e intervalos compatibles con los resúmenes. Falta comprobar si el cambio conserva sensibilidad en uso real y reduce detecciones cotidianas. La política del acelerómetro se conserva y el consumo comparativo sigue pendiente. Ver la tabla y el procedimiento en [Plan de pruebas](PLAN_DE_PRUEBAS.md).

## Confirmación temporal de caída — 0.1.6

La 0.1.5 registró cero patrones al caminar, manipular el teléfono y usar el bolsillo. Reconoció el doble tajo moderado y perdió el natural. En el ensayo natural apareció un bloqueo de posible caída. La duración de la baja aceleración no quedó registrada, por lo que no se puede identificar ese bloqueo como causa única.

En 0.1.6 la guarda confirma la caída con al menos tres muestras bajas consecutivas que abarquen 55 ms o más. Se descartan los intervalos entre muestras superiores a 75 ms para confirmar continuidad. Tras la confirmación se conserva el bloqueo de 800 ms. Son parámetros experimentales. El fundamento del umbral es que el acelerómetro puede medir cerca de cero durante la caída libre. [SensorEvent de Android](https://developer.android.com/reference/android/hardware/SensorEvent#values)

La repetición física de 0.1.6 reconoció ambos tajos naturales (489 y 471 ms entre crestas) y la manipulación habitual produjo cero detecciones. Se conserva la configuración para la siguiente fase. Tres ensayos dirigidos no bastan para estimar tasas de error ni permiten atribuir la mejora a la guarda de caída por sí sola.
