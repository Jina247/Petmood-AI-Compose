package com.example.data.api

import android.content.Context
import android.content.Intent
import com.example.MainActivity

/**
 * Builds the Android [ApiService] on top of the shared client. Used by MainActivity and
 * PetMoodFirebaseMessagingService (which the OS creates on its own), so there's one setup path.
 * On an expired session (401), restarts MainActivity at login with the "session expired" toast.
 */
fun buildApiService(context: Context): ApiService {
    val appContext = context.applicationContext
    return createApiService(onSessionExpired = {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_SESSION_EXPIRED, true)
        }
        appContext.startActivity(intent)
    })
}
