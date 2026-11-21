package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.api.UploadFile
import com.example.data.model.ScanResult

class ScanRepository(private val apiService: ApiService) {

    suspend fun uploadScan(
        petId: String,
        video: UploadFile,
        photos: List<UploadFile> = emptyList(), // optional supporting photos
        description: String? = null
    ): Result<ScanResult> {
        return try {
            Result.success(
                apiService.uploadScan(petId, video, photos, description?.takeIf { it.isNotBlank() })
            )
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
