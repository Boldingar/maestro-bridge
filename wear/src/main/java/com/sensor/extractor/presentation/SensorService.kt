package com.sensor.extractor.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * SensorService
 *
 * Logs official Heart Rate, Skin Temp, and the Google-specific Extended HR sensor
 * at a continuous 3-second interval, and sends the string over Bluetooth.
 */
class SensorService : Service(), SensorEventListener {

    companion object {
        private const val TAG            = "PW2_SensorService"
        private const val TAG_CONTINUOUS = "PW2_CONTINUOUS"

        private const val SENSOR_ID_HR       = Sensor.TYPE_HEART_RATE // Type 21
        private const val SENSOR_ID_TEMP     = 65555
        private const val SENSOR_ID_HR_EXT   = 131086 // Extended HR Data

        private const val SAMPLING_RATE_MS = 3000L

        private const val CHANNEL_ID      = "biometric_logging_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG   = "SensorLogging::WakeLock"
    }

    private lateinit var sensorManager: SensorManager
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var sensorHandlerThread: HandlerThread
    private lateinit var sensorHandler: Handler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Variables to hold the most recent sensor values
    private var latestHr: Float = 0f
    private var latestTemp: Float = 0f
    private var latestHrExt: String = "[]" // Stored as string to handle potential arrays
    private var tickCount = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Continuous Biometrics Active")
            .setContentText("Logging HR, Temp, & HR-Ext every 3s")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)

        serviceScope.launch {
            initializeSensorsAndWakeLock()
            startContinuousLoggingLoop()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeSensorsAndWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).also {
            it.acquire()
        }

        sensorHandlerThread = HandlerThread("SensorCallbackThread").also { it.start() }
        sensorHandler = Handler(sensorHandlerThread.looper)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        registerSensor(SENSOR_ID_HR,     "Standard HR (21)")
        registerSensor(SENSOR_ID_TEMP,   "Skin Temp (65555)")
        registerSensor(SENSOR_ID_HR_EXT, "Extended HR (131086)")
    }

    private fun registerSensor(sensorId: Int, label: String) {
        val sensor = sensorManager.getDefaultSensor(sensorId)
        if (sensor != null) {
            val ok = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, sensorHandler)
            Log.i(TAG, "Registered $label: $ok")
        } else {
            Log.w(TAG, "$label not found on this device")
        }
    }

    private fun startContinuousLoggingLoop() {
        serviceScope.launch {
            Log.i(TAG_CONTINUOUS, "=== STARTING CONTINUOUS LOGGING EVERY ${SAMPLING_RATE_MS}ms ===")

            while (isActive) {
                tickCount++
                val tsMs = System.currentTimeMillis()

                // Create the payload string
                val payload = "TICK=#$tickCount | ts_ms=$tsMs | BPM=${"%.1f".format(latestHr)} | TEMP_C=${"%.3f".format(latestTemp)} | HR_EXT=$latestHrExt"

                // Print it locally to logcat (just like before)
                Log.i(TAG_CONTINUOUS, payload)

                // Beam it to the phone
                sendDataToPhone(payload)

                delay(SAMPLING_RATE_MS)
            }
        }
    }

    private fun sendDataToPhone(payload: String) {
        val messageClient = Wearable.getMessageClient(this)
        val nodeClient = Wearable.getNodeClient(this)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/biometrics_data", payload.toByteArray())
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            SENSOR_ID_HR -> {
                latestHr = event.values[0]
            }
            SENSOR_ID_TEMP -> {
                latestTemp = event.values[0]
            }
            SENSOR_ID_HR_EXT -> {
                latestHrExt = event.values.joinToString(prefix = "[", postfix = "]") { "%.2f".format(it) }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Biometric Sensor Logging", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun cleanup() {
        try { sensorManager.unregisterListener(this) } catch (e: Exception) {}
        wakeLock?.let { if (it.isHeld) it.release() }
        if (::sensorHandlerThread.isInitialized) sensorHandlerThread.quitSafely()
        serviceScope.cancel()
        Log.i(TAG, "Stopped. Total recorded ticks: $tickCount")
    }
}