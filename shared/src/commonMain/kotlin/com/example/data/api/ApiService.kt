package com.example.data.api

import com.example.data.model.PetProfile
import com.example.data.model.ScanResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val name: String,
    val email: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "bearer"
)

@Serializable
data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val email: String
)

@Serializable
data class PetCreate(
    val name: String,
    val age: Int,
    val pet_type: String,
    val gender: String
)

@Serializable
data class FcmTokenRequest(
    @SerialName("fcm_token")
    val fcmToken: String? // sent as an explicit null on logout to clear it server-side
)

/** A file on disk to send as one multipart part. Streamed, never read fully into memory. */
data class UploadFile(val path: String, val fileName: String, val mimeType: String)

/**
 * Every backend endpoint. Paths are relative to the base URL and match the old Retrofit
 * interface exactly — keep the trailing slash on `pets/`, or FastAPI answers with a 307
 * redirect. Build instances with [createApiService].
 */
class ApiService internal constructor(private val client: HttpClient) {

    suspend fun login(request: LoginRequest): LoginResponse =
        client.post("auth/login") { json(request) }.body()

    suspend fun register(request: RegisterRequest): RegisterResponse =
        client.post("auth/register") { json(request) }.body()

    suspend fun getCurrentUserInfo(): RegisterResponse = client.get("auth/users/me").body()

    suspend fun getPets(): List<PetProfile> = client.get("pets/").body()

    suspend fun getPet(petId: String): PetProfile = client.get("pets/$petId").body()

    suspend fun createPet(pet: PetCreate): PetProfile =
        client.post("pets/") { json(pet) }.body()

    suspend fun updatePet(petId: String, pet: PetCreate): PetProfile =
        client.patch("pets/$petId") { json(pet) }.body()

    suspend fun deletePet(petId: String) {
        client.delete("pets/$petId")
    }

    suspend fun uploadScan(
        petId: String,
        video: UploadFile,
        photos: List<UploadFile>,
        description: String?
    ): ScanResult = client.submitFormWithBinaryData(
        url = "pets/$petId/scans",
        formData = formData {
            appendFile("file", video)
            photos.forEach { appendFile("photos", it) }
            if (description != null) append("description", description)
        }
    ).body()

    suspend fun getScan(petId: String, scanId: String): ScanResult =
        client.get("pets/$petId/scans/$scanId").body()

    suspend fun getScanHistory(petId: String): List<ScanResult> =
        client.get("pets/$petId/scans").body()

    suspend fun getLatestScan(petId: String): ScanResult =
        client.get("pets/$petId/latest-scan").body()

    suspend fun updateFcmToken(request: FcmTokenRequest) {
        client.patch("auth/fcm-token") { json(request) }
    }

    private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    // Each part needs its own filename and Content-Type: the backend checks the type against
    // an allowlist and rejects the upload (400) without it.
    private fun io.ktor.client.request.forms.FormBuilder.appendFile(key: String, file: UploadFile) {
        val path = Path(file.path)
        val size = SystemFileSystem.metadataOrNull(path)?.size
        append(
            key,
            InputProvider(size) { SystemFileSystem.source(path).buffered() },
            Headers.build {
                append(HttpHeaders.ContentType, file.mimeType)
                append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
            }
        )
    }
}
