package com.example.session

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

fun SessionManager.init(context: Context) {
    val prefs = context.applicationContext.getSharedPreferences(SessionManager.PREF_NAME, Context.MODE_PRIVATE)
    init(SharedPreferencesSettings(prefs))
}
