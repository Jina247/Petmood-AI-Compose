package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(
    val id: String = "",
    val status: String = "",                    // "processing" | "complete" | "failed"
    @SerialName("mood_result")
    val mood: String? = null,                   // renamed from moodResult to match UI
    val confidence: Double? = null,             // 0.92 not 92
    val summary: String? = null,                   // Added for UI
    @SerialName("error_message")
    val errorMessage: String? = null,           // set by backend when status == "failed"
    @SerialName("pet_id")
    val petId: String = "",
    val petName: String = "",                   // Added for UI
    @SerialName("created_at")
    val timestamp: String = "",                 // renamed from createdAt to match UI
    val description: String? = null,            // owner-supplied text, optional
    val suggestions: List<String>? = null       // actionable next steps
)
