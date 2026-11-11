package com.impulsos.app.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.impulsos.app.R
import com.impulsos.app.domain.ShakeDetectorLogic
import com.impulsos.app.domain.ShakeSensitivity
import com.impulsos.app.emergency.EmergencyActivity

class MonitoringService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var detector = ShakeDetectorLogic()
    private var alarm: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastTriggerAt: Long? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = MotionSensorPolicy.sensor(sensorManager)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMonitoring()
            ACTION_DISARM -> disarmEmergency()
            ACTION_SET_SENSITIVITY -> detector = newDetector()
            else -> startMonitoring()
        }
        // A user can always restart monitoring from the app; do not claim permanent background uptime.
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        val sensor = accelerometer
        if (sensor == null) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
            stopSelf()
            return
        }
        startForeground(NOTIFICATION_ID, monitoringNotification())
        detector = newDetector()
        sensorManager.unregisterListener(this)
        // Request a steadier cadence for peak timestamps; Android may still batch/throttle it.
        val registered = sensorManager.registerListener(this, sensor, MotionSensorPolicy.SAMPLE_PERIOD_US, MotionSensorPolicy.latency(sensor))
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, registered).apply()
        if (!registered) stopMonitoring()
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        detector.reset()
        handler.removeCallbacksAndMessages(null)
        alarm?.stopTone()
        alarm?.release()
        alarm = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun newDetector(): ShakeDetectorLogic {
        val selected = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SENSITIVITY, null)
        return ShakeDetectorLogic(ShakeSensitivity.fromStorage(selected).detectorConfig())
    }

    private fun disarmEmergency() {
        handler.removeCallbacksAndMessages(null)
        alarm?.stopTone()
        alarm?.release()
        alarm = null
        getSystemService(NotificationManager::class.java).cancel(EMERGENCY_NOTIFICATION_ID)
        detector.reset()
        if (isActive(this)) startMonitoring() else stopMonitoring()
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        alarm?.stopTone()
        alarm?.release()
        alarm = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val detected = detector.addSample(
            event.values[0], event.values[1], event.values[2], event.timestamp / 1_000_000L,
        )
        if (detected) activateEmergency()
    }

    private fun activateEmergency() {
        val now = SystemClock.elapsedRealtime()
        if (lastTriggerAt?.let { now - it < TRIGGER_COOLDOWN_MILLIS } == true) return
        lastTriggerAt = now
        detector.reset()
        sensorManager.unregisterListener(this)
        val requestAt = SystemClock.elapsedRealtime()
        val pendingIntent = emergencyPendingIntent()
        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("ImpulSOS · Emergencia médica")
            .setContentText("Toca para mostrar la información de emergencia.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(EMERGENCY_NOTIFICATION_ID, notification)
        startAlarmLoop()
        val postedAt = SystemClock.elapsedRealtime()
        android.util.Log.i(TAG, "trigger_to_notification_ms=${postedAt - requestAt}")
    }

    private fun startAlarmLoop() {
        alarm?.release()
        alarm = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        val beep = object : Runnable {
            override fun run() {
                alarm?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
                handler.postDelayed(this, 1_300L)
            }
        }
        handler.post(beep)
    }

    private fun emergencyPendingIntent(): PendingIntent {
        val intent = Intent(this, EmergencyActivity::class.java)
            .putExtra(EmergencyActivity.EXTRA_SIMULATION, false)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this, EMERGENCY_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun monitoringNotification(): Notification = NotificationCompat.Builder(this, MONITOR_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("ImpulSOS está monitoreando")
        .setContentText("El acelerómetro está activo. Toca para revisar el estado.")
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, com.impulsos.app.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(MONITOR_CHANNEL, "Monitoreo activo", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Indica que ImpulSOS está usando el acelerómetro." },
        )
        manager.createNotificationChannel(
            NotificationChannel(EMERGENCY_CHANNEL, "Emergencia médica", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = "Alerta audiovisual local cuando se valida el gesto de emergencia."
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.impulsos.app.action.START_MONITORING"
        const val ACTION_STOP = "com.impulsos.app.action.STOP_MONITORING"
        const val ACTION_DISARM = "com.impulsos.app.action.DISARM"
        const val ACTION_SET_SENSITIVITY = "com.impulsos.app.action.SET_SENSITIVITY"
        const val PREFS = "monitoring-state"
        const val KEY_ACTIVE = "active"
        const val KEY_SENSITIVITY = "sensitivity"
        private const val MONITOR_CHANNEL = "monitoring"
        private const val EMERGENCY_CHANNEL = "emergency"
        private const val NOTIFICATION_ID = 4101
        private const val EMERGENCY_NOTIFICATION_ID = 4102
        private const val EMERGENCY_REQUEST_CODE = 4103
        private const val TRIGGER_COOLDOWN_MILLIS = 30_000L
        private const val TAG = "ImpulSOS"

        fun isActive(context: Context): Boolean = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, false)
    }
}
