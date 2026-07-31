package com.example.data.repository

import com.example.data.api.LoginRequest
import com.example.data.api.RegisterRequest
import com.example.data.api.RetrofitInstance
import com.example.session.SessionManager

class AuthRepository {

    private val api = RetrofitInstance.api

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            SessionManager.saveToken(response.accessToken)  // save token
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
            SessionManager.saveUserInfo(email, name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        SessionManager.clearToken()
        SessionManager.clearSession()
    }

    fun getCurrentUserEmail(): String {
        return SessionManager.getUserEmail()
    }

    fun getCurrentUserName(): String {
        return SessionManager.getUserName()
    }
}