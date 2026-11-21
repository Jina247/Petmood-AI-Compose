package com.example.session

import com.russhwolf.settings.Settings

/**
 * JWT, user info and FCM token state. Backed by [Settings]: on Android that's the same
 * `petmood_prefs` SharedPreferences file (and keys) as before, so updating keeps people logged
 * in; on iOS it's NSUserDefaults. [init] must run before first use.
 */
object SessionManager {
    const val PREF_NAME = "petmood_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_FCM_SYNCED = "fcm_synced"

    private lateinit var settings: Settings

    fun init(settings: Settings) {
        this.settings = settings
    }

    fun saveToken(token: String) {
        settings.putString(KEY_TOKEN, token)
    }

    fun saveFcmToken(fcmToken: String) {
        settings.putString(KEY_FCM_TOKEN, fcmToken)
        settings.putBoolean(KEY_FCM_SYNCED, false)
    }

    fun saveUserInfo(email: String, name: String) {
        settings.putString(KEY_EMAIL, email)
        settings.putString(KEY_NAME, name)
    }

    fun getUserName(): String = settings.getString(KEY_NAME, "")

    fun getUserEmail(): String = settings.getString(KEY_EMAIL, "")

    fun getToken(): String = settings.getString(KEY_TOKEN, "")

    fun getFcmToken(): String? = settings.getStringOrNull(KEY_FCM_TOKEN)

    fun syncFcmToken(sync: Boolean) {
        settings.putBoolean(KEY_FCM_SYNCED, sync)
    }

    fun isFcmTokenSynced(): Boolean = settings.getBoolean(KEY_FCM_SYNCED, false)

    fun clearToken() {
        settings.remove(KEY_TOKEN)
    }

    fun clearSession() {
        settings.clear()
    }

    fun isLoggedIn(): Boolean = getToken().isNotEmpty()
}
