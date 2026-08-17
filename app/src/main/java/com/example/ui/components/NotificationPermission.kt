package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

/**
 * Current POST_NOTIFICATIONS permission status plus a way to (re)request it.
 * Mirrors CameraPreview.kt's CameraPermissionState/rememberCameraPermissionState —
 * same shape, kept as a separate file since this permission isn't tied to one screen.
 */
data class NotificationPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current

    // NotificationManagerCompat.areNotificationsEnabled() rather than
    // ContextCompat.checkSelfPermission(POST_NOTIFICATIONS) — correctly reflects
    // notification-enabled state across API levels, including pre-33 where
    // there's no runtime permission but the user can still disable notifications
    // via system settings.
    var hasPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    return remember(hasPermission, launcher) {
        NotificationPermissionState(
            hasPermission = hasPermission,
            requestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // Pre-33: no runtime permission to request — notifications work
                // as long as the user hasn't disabled them in system settings.
            }
        )
    }
}
