package com.example.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.api.buildApiService
import com.example.data.repository.AuthRepository
import com.example.session.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "scan_results"
private const val NOTIFICATION_ID = 1001

/**
 * Registered in AndroidManifest.xml under the MESSAGING_EVENT intent-filter.
 * Instantiated independently by the OS — not reachable from MainActivity's
 * locally-built repositories, so it builds its own ApiService/AuthRepository
 * via buildApiService() (see ApiServiceFactory.kt).
 */
class PetMoodFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SessionManager.saveFcmToken(token)  // also resets the synced flag
        if (SessionManager.isLoggedIn()) {
            CoroutineScope(Dispatchers.IO).launch {
                val authRepository = AuthRepository(buildApiService(applicationContext))
                authRepository.syncFcmTokenIfNeeded()
            }
        }
        // If not logged in, the token is cached and login()/register() will
        // pick it up and sync it via syncFcmTokenIfNeeded() themselves.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        // Sent alongside the notification text by the backend's send_scan_notification() —
        // see MainActivity.EXTRA_DEEPLINK_SCAN_ID for how the client picks this up.
        val scanId = message.data["scan_id"]

        ensureNotificationChannel()

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (scanId != null) putExtra(MainActivity.EXTRA_DEEPLINK_SCAN_ID, scanId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted on API 33+ — nothing to show without it.
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scan Results",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
