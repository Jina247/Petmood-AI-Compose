package com.example.data.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://petmoodai-au.azurewebsites.net"

/**
 * Builds the single Retrofit [ApiService] instance. Extracted from
 * MainActivity.onCreate()'s inline setup so PetMoodFirebaseMessagingService —
 * instantiated independently by the OS, not able to reach MainActivity's local
 * vals — can build its own AuthRepository to resync an FCM token from
 * onNewToken(). MainActivity uses this too, so there's one construction path.
 */
fun buildApiService(context: Context): ApiService {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context.applicationContext))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ApiService::class.java)
}
