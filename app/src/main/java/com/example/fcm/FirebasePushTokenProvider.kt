package com.example.fcm

import com.example.data.repository.PushTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebasePushTokenProvider : PushTokenProvider {
    override suspend fun fetchToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) cont.resume(task.result)
            else cont.resumeWithException(task.exception ?: IllegalStateException("No FCM token"))
        }
    }
}
