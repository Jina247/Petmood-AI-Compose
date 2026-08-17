package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.api.FcmTokenRequest
import com.example.data.api.LoginRequest
import com.example.data.api.RegisterRequest
import com.example.session.SessionManager

class AuthRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            SessionManager.saveToken(response.accessToken)  // save token
            SessionManager.syncFcmToken(false)  // fresh login => always resync
            syncFcmTokenIfNeeded()
            val userInfo = api.getCurrentUserInfo()
            SessionManager.saveUserInfo(userInfo.email, userInfo.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, name: String, password: String): Result<Unit> {
        return try {
            api.register(RegisterRequest(email, name, password))
            val loginResponse = api.login(LoginRequest(email, password))
            SessionManager.saveToken(loginResponse.accessToken)
            SessionManager.syncFcmToken(false)
            syncFcmTokenIfNeeded()
            SessionManager.saveUserInfo(email, name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            api.updateFcmToken(FcmTokenRequest(fcmToken = null))
        }
        catch (e: Exception)
        { /* best-effort */ }
        SessionManager.clearToken()
        SessionManager.clearSession()
    }

    /**
     * Sends the device's current FCM token (if any) to the backend, but only if
     * it's not already synced for this session. Called right after login/register
     * (once a bearer token exists for AuthInterceptor to attach) and from
     * onNewToken() for mid-session token rotation while already logged in — the
     * single reusable "attempt sync" entry point so that not-synced/isLoggedIn
     * guard logic doesn't get duplicated at each call site.
     */
    suspend fun syncFcmTokenIfNeeded() {
        if (!SessionManager.isLoggedIn() || SessionManager.isFcmTokenSynced()) return
        val token = SessionManager.getFcmToken() ?: return  // no token yet — onNewToken will trigger this later
        try {
            api.updateFcmToken(FcmTokenRequest(fcmToken = token))
            SessionManager.syncFcmToken(true)
        } catch (e: Exception) {
            // leave unsynced — next opportunity (relaunch, next token rotation) retries
        }
    }

    fun getCurrentUserEmail(): String {
        return SessionManager.getUserEmail()
    }

    fun getCurrentUserName(): String {
        return SessionManager.getUserName()
    }

    fun editUserName(): String {
        return ""
    }

    fun editUserEmail(): String {
        return ""
    }

    fun editUserPassword(): String {
        return ""
    }
}