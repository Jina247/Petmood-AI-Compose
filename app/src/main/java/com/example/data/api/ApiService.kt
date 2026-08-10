package com.example.data.api

import com.example.data.model.PetProfile
import com.example.data.model.ScanResult
import com.squareup.moshi.Json
import okhttp3.MultipartBody
import retrofit2.http.*

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val name: String,
    val email: String,
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "token_type")
    val tokenType: String = "bearer"
)

data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String
)

data class RegisterResponse(
    @Json(name = "user_id")
    val userId: String,
    val name: String,
    val email: String
)

data class PetCreate(
    val name: String,
    val age: Int,
    val pet_type: String,
    val gender: String
)

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("auth/users/me")
    suspend fun getCurrentUserInfo(): RegisterResponse

    @GET("pets/")
    suspend fun getPets(): List<PetProfile>

    @GET("pets/{pet_id}")
    suspend fun getPet(
        @Path("pet_id") petId: String,
    ): PetProfile

    @POST("pets/")
    suspend fun createPet(
        @Body pet: PetCreate
    ): PetProfile

    @PATCH("pets/{pet_id}")
    suspend fun updatePet(
        @Path("pet_id") petId: String,
        @Body pet: PetCreate
    ): PetProfile

    @DELETE("pets/{pet_id}")
    suspend fun deletePet(
        @Path("pet_id") petId: String
    )

    @Multipart
    @POST("pets/{pet_id}/scans")
    suspend fun uploadScan(
        @Path("pet_id") petId: String,
        @Part file: MultipartBody.Part
    ): ScanResult

    @GET("pets/{pet_id}/scans/{scan_id}")
    suspend fun getScan(
        @Path("pet_id") petId: String,
        @Path("scan_id") scanId: String
    ): ScanResult

    @GET("pets/{pet_id}/scans")
    suspend fun getScanHistory(
        @Path("pet_id") petId: String
    ): List<ScanResult>

    @GET("pets/{pet_id}/latest-scan")
    suspend fun getLatestScan(
        @Path("pet_id") petId: String
    ): ScanResult
}
