# Compatibilidad Android

- `minSdk`: 26 (Android 8.0).
- `compileSdk`: 34, instalado en este entorno.
- `targetSdk`: 29 para la primera validación en el Motorola Android 10/API 29.
- Kotlin 1.9.10, Android Gradle Plugin 8.1.4 y Gradle Wrapper 8.0.
- JDK de compilación configurado por proyecto: Temurin 17.0.21.
- Dispositivo conectado confirmado por ADB: Motorola one, Android 10, API 29.

## Consideraciones

El monitoreo solo se registra tras una acción explícita y se ejecuta como servicio de primer plano con notificación. `START_NOT_STICKY` evita prometer disponibilidad permanente. Android 10 puede permitir presentar la actividad mediante notificación de pantalla completa de alta prioridad, pero el usuario, el modo No molestar, los canales de notificación y las políticas del fabricante pueden impedir o silenciarlo. Mostrar sobre el bloqueo no desbloquea el dispositivo.

La alarma usa el flujo de audio de alarma a volumen de sistema; la app no puede garantizar volumen máximo ni superar No molestar. El brillo y la ejecución en Doze dependen del sistema. La primera versión no pide ignorar optimización de batería, no solicita superposición ni programa reinicio tras boot.

El entorno tenía Java 26 en el `PATH`, `JAVA_HOME` vacío y Temurin 17 instalado. `gradle.properties` selecciona Temurin 17 localmente sin cambiar variables globales.
