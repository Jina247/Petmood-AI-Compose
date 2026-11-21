package com.example.session

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

/** Call once at iOS app launch, before anything touches [SessionManager]. */
fun initSessionManager() {
    SessionManager.init(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))
}
