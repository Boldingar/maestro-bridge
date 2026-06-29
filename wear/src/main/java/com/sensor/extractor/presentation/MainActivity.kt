package com.sensor.extractor.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * MainActivity
 *
 * Simple View-based UI (no Compose, no XML layouts).
 * Shows a status label and a single Start / Stop toggle button.
 *
 * ADB monitoring:
 * adb logcat -s PW2_MainActivity
 * adb logcat -s PW2_SensorService
 * adb logcat -s PW2_EDA
 * adb logcat -s PW2_PPG
 * adb logcat -s PW2_TEMP
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PW2_MainActivity"
    }

    private var isLogging = false

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button

    // ──────────────────────────────────────────────────────────────────────────
    // Permission Launchers (Split into Two Steps)
    // ──────────────────────────────────────────────────────────────────────────

    // STEP 2: Launcher for Background Sensors (Only called after Basic is granted)
    private val backgroundPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.i(TAG, "Background sensors GRANTED.")
            } else {
                Log.w(TAG, "Background sensors DENIED. App may stop logging when screen turns off.")
            }
            // We start the service either way, as foreground is granted.
            startSensorService()
        }

    // STEP 1: Launcher for Foreground Sensors, Notifications, and Hidden OEM Sensors
    private val basicPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach { (perm, granted) ->
                Log.d(TAG, "$perm → ${if (granted) "GRANTED" else "DENIED"}")
            }

            // We base our continuation mostly on BODY_SENSORS. The proprietary
            // permissions might auto-deny in the UI, requiring an ADB grant.
            val bodySensorsGranted = results[Manifest.permission.BODY_SENSORS] ?:
            (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED)

            if (bodySensorsGranted) {
                // Foreground is granted, now ask for Background
                checkAndRequestBackgroundPermission()
            } else {
                Log.w(TAG, "Core permissions denied — cannot start logging.")
                updateUi(logging = false)
            }
        }

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Layout
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        statusText = TextView(this).apply {
            text = "○ IDLE"
            setTextColor(Color.GRAY)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 6.dp }
        }

        val sensorLabel = TextView(this).apply {
            text = "EDA · PPG · TEMP"
            setTextColor(Color.DKGRAY)
            textSize = 10f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 14.dp }
        }

        toggleButton = Button(this).apply {
            text = "START"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(140.dp, 44.dp)
            setOnClickListener { onToggleClicked() }
        }

        root.addView(statusText)
        root.addView(sensorLabel)
        root.addView(toggleButton)
        return root
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Button handler
    // ──────────────────────────────────────────────────────────────────────────

    private fun onToggleClicked() {
        if (!isLogging) {
            checkAndRequestBasicPermissions()
        } else {
            stopSensorService()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI state
    // ──────────────────────────────────────────────────────────────────────────

    private fun updateUi(logging: Boolean) {
        isLogging = logging
        if (logging) {
            statusText.text = "● LOGGING"
            statusText.setTextColor(Color.parseColor("#4CAF50"))
            toggleButton.text = "STOP"
            toggleButton.setBackgroundColor(Color.parseColor("#7F0000"))
        } else {
            statusText.text = "○ IDLE"
            statusText.setTextColor(Color.GRAY)
            toggleButton.text = "START"
            toggleButton.setBackgroundColor(Color.parseColor("#1B5E20"))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Permissions Logic
    // ──────────────────────────────────────────────────────────────────────────

    private fun checkAndRequestBasicPermissions() {
        // Add the proprietary permissions to our request list alongside standard ones
        val required = mutableListOf(
            Manifest.permission.BODY_SENSORS,
            "com.google.pixelwatch.permission.READ_PRIVATE_SENSORS",
            "android.permission.health.READ_SKIN_TEMPERATURE"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            // Basic permissions already granted, move to step 2
            checkAndRequestBackgroundPermission()
        } else {
            Log.d(TAG, "Requesting basic permissions: $missing")
            basicPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkAndRequestBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting background permission")
                backgroundPermissionLauncher.launch(Manifest.permission.BODY_SENSORS_BACKGROUND)
                return
            }
        }
        // If we reach here, we either have all permissions or are on an older Android version
        startSensorService()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Service control
    // ──────────────────────────────────────────────────────────────────────────

    private fun startSensorService() {
        Log.i(TAG, "Starting SensorService.")
        startForegroundService(Intent(this, SensorService::class.java))
        updateUi(logging = true)
    }

    private fun stopSensorService() {
        Log.i(TAG, "Stopping SensorService.")
        stopService(Intent(this, SensorService::class.java))
        updateUi(logging = false)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private val Int.dp: Int get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}