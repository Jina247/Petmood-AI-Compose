package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.model.ScanResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ScanRepository(private val apiService: ApiService) {

    suspend fun uploadScan(petId: String, file: File, mimeType: String): Result<ScanResult> {
        return try {
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            Result.success(apiService.uploadScan(petId, body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScan(petId: String, scanId: String): Result<ScanResult> {
        return try {
            Result.success(apiService.getScan(petId, scanId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScanHistory(petId: String): Result<List<ScanResult>> {
        return try {
            Result.success(apiService.getScanHistory(petId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestScan(petId: String): Result<ScanResult> {
        return try {
            Result.success(apiService.getLatestScan(petId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
