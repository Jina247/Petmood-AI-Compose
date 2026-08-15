package com.example.data.api

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.MainActivity
import com.example.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    companion object {
        @Volatile
        private var isRedirecting = false
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SessionManager.getToken()
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        if (token.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            // Avoid multiple redirects if multiple requests fail at once
            if (SessionManager.isLoggedIn() && !isRedirecting) {
                synchronized(this) {
                    if (SessionManager.isLoggedIn() && !isRedirecting) {
                        isRedirecting = true
                        SessionManager.clearSession()
                        // redirect to log in
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                        // Reset the flag after some time to allow future redirects if needed
                        // (e.g. after user logs in and then token expires again)
                        Handler(Looper.getMainLooper()).postDelayed({
                            isRedirecting = false
                        }, 2000)
                    }
                }
            }
        }
        return response
    }
}
