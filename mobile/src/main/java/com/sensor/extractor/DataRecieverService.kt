package com.sensor.extractor // Ensure this matches your package!

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataReceiverService : WearableListenerService() {

    companion object {
        private const val TAG = "PhoneReceiver"
        const val ACTION_BIOMETRIC_DATA = "com.sensor.extractor.BIOMETRIC_DATA"
        const val EXTRA_PAYLOAD = "payload"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        if (messageEvent.path == "/biometrics_data") {
            val dataString = String(messageEvent.data)
            Log.i(TAG, "RECEIVED FROM WATCH: $dataString")

            // Send a local broadcast so the MainActivity can catch it and update the UI
            val intent = Intent(ACTION_BIOMETRIC_DATA).apply {
                putExtra(EXTRA_PAYLOAD, dataString)
                setPackage(packageName)
            }
            sendBroadcast(intent)
        }
    }
}