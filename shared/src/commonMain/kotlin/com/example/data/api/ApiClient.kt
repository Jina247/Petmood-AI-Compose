package com.example.data.api

import com.example.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val BASE_URL = "https://petmoodai-au.azurewebsites.net/"

/**
 * The JSON setup for every request and response. `ignoreUnknownKeys`: the backend sends fields
 * the models don't declare (e.g. `photo_paths`). `coerceInputValues`: a null for a non-null field
 * falls back to its default, as Moshi did. Nulls are still sent explicitly (logout clears the
 * FCM token by sending `"fcm_token": null`).
 */
val PetMoodJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** A non-2xx response. [body] is for logs only; never show it to owners. */
class ApiException(val code: Int, val body: String?) : Exception("Server returned HTTP $code") {
    /**
     * The backend's own message for this error (FastAPI's `{"detail": "..."}`), written for
     * people, e.g. "Incorrect email or password". Null for validation errors, whose `detail`
     * is a list of field problems.
     */
    val detail: String? by lazy {
        runCatching {
            (PetMoodJson.parseToJsonElement(body ?: "").jsonObject["detail"] as? JsonPrimitive)
                ?.takeIf { it.isString }?.content
        }.getOrNull()
    }
}

private const val SESSION_EXPIRED_DEBOUNCE_MS = 2_000L

@OptIn(ExperimentalAtomicApi::class)
private val lastSessionExpiredAt = AtomicLong(0L)

/**
 * Builds the [ApiService]. Each platform passes [onSessionExpired] to send the user back to
 * login after a 401 (the session is already cleared when it runs). Several requests failing at
 * once only trigger it once.
 */
@OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)
fun createApiService(onSessionExpired: () -> Unit): ApiService {
    val bearerAuth = createClientPlugin("BearerAuth") {
        onRequest { request, _ ->
            val token = SessionManager.getToken()
            if (token.isNotEmpty()) request.headers[HttpHeaders.Authorization] = "Bearer $token"
        }
    }

    val client = HttpClient {
        expectSuccess = true
        install(bearerAuth)
        install(ContentNegotiation) { json(PetMoodJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000 // per read, like OkHttp's readTimeout; no total cap so long uploads work
        }
        defaultRequest { url(BASE_URL) }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                val response = (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
                val code = response.status.value
                if (code == 401 && SessionManager.isLoggedIn()) {
                    val now = Clock.System.now().toEpochMilliseconds()
                    val last = lastSessionExpiredAt.load()
                    if (now - last > SESSION_EXPIRED_DEBOUNCE_MS && lastSessionExpiredAt.compareAndSet(last, now)) {
                        SessionManager.clearSession()
                        onSessionExpired()
                    }
                }
                throw ApiException(code, runCatching { response.bodyAsText() }.getOrNull())
            }
        }
    }
    return ApiService(client)
}
