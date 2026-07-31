package com.example.data.model

import com.squareup.moshi.Json

data class ScanResult(
    val id: String = "",
    val status: String = "",                    // "processing" or "complete"
    @Json(name = "mood_result")
    val mood: String? = null,                   // renamed from moodResult to match UI
    val confidence: Double? = null,             // 0.92 not 92
    val summary: String? = null,                   // Added for UI
    @Json(name = "pet_id")
    val petId: String = "",
    val petName: String = "",                   // Added for UI
    @Json(name = "created_at")
    val timestamp: String = ""                  // renamed from createdAt to match UI
)
