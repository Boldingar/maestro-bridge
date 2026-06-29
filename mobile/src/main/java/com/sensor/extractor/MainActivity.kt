package com.sensor.extractor // Ensure this matches your package!

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvBpm: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvHrExt: TextView

    // This listens for the broadcast sent by our DataReceiverService
    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val payload = intent?.getStringExtra(DataReceiverService.EXTRA_PAYLOAD) ?: return
            updateUI(payload)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Link our code variables to the XML layout text boxes
        tvStatus = findViewById(R.id.tvStatus)
        tvBpm = findViewById(R.id.tvBpm)
        tvTemp = findViewById(R.id.tvTemp)
        tvHrExt = findViewById(R.id.tvHrExt)
    }

    override fun onStart() {
        super.onStart()
        // Register the receiver when the app is on screen
        val filter = IntentFilter(DataReceiverService.ACTION_BIOMETRIC_DATA)

        ContextCompat.registerReceiver(
            this,
            dataReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the app is in the background to save battery
        unregisterReceiver(dataReceiver)
    }

    private fun updateUI(payload: String) {
        tvStatus.text = "Receiving live data..."

        // We split the string using our " | " separator
        // Payload example: TICK=#1 | ts_ms=1772764853920 | BPM=73.0 | TEMP_C=34.016 | HR_EXT=[...]
        val parts = payload.split(" | ")

        for (part in parts) {
            when {
                part.startsWith("BPM=") -> tvBpm.text = part.replace("=", ": ")
                part.startsWith("TEMP_C=") -> tvTemp.text = "Skin Temp: " + part.substringAfter("TEMP_C=") + " °C"
                part.startsWith("HR_EXT=") -> tvHrExt.text = part
            }
        }
    }
}