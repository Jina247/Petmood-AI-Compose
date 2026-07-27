package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.api.ScanResponse
import com.example.data.model.ScanResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ScanRepository(private val apiService: ApiService) {
    suspend fun uploadScan(petId: String, file: File): ScanResponse {
        val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return apiService.uploadScan(petId, body)
    }

    suspend fun getScan(petId: String, scanId: String): ScanResult {
        return apiService.getScan(petId, scanId)
    }

    suspend fun getScanHistory(petId: String): List<ScanResult> {
        return apiService.getScanHistory(petId)
    }

    suspend fun getLatestScan(petId: String): ScanResult {
        return apiService.getLatestScan(petId)
    }
}
