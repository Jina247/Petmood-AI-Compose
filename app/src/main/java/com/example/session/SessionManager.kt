package com.example.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionManager {
    private const val PREF_NAME = "petmood_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_FCM_SYNCED = "fcm_synced"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }
    fun saveFcmToken(fcmToken: String) {
        prefs.edit {
            putString(KEY_FCM_TOKEN, fcmToken);
            putBoolean(KEY_FCM_SYNCED, false)
        }
    }
    fun saveUserInfo(email: String, name: String) {
        prefs.edit {
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
        }
    }
    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }
    fun getUserEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }

    fun getToken(): String {
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun syncFcmToken(sync: Boolean) {
        prefs.edit{ putBoolean(KEY_FCM_SYNCED, sync)}
    }

    fun isFcmTokenSynced(): Boolean {
        return prefs.getBoolean(KEY_FCM_SYNCED, false)
    }

    fun authHeader(): String = "Bearer ${getToken()}"

    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }
    fun clearSession() {
        prefs.edit { clear() }
    }
    fun isLoggedIn(): Boolean = getToken().isNotEmpty()
}