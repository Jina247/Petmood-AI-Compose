package com.example

import com.example.data.model.ScanResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies ScanResult deserializes correctly for both the current response shape (video-required
 * scan, optionally carrying photos/description and always eligible for suggestions) and an older
 * response shape missing those newer fields, using the exact same Moshi setup (reflection-based
 * KotlinJsonAdapterFactory) as MainActivity's Retrofit client.
 */
class ScanResultMoshiTest {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(ScanResult::class.java)

    @Test
    fun `parses a completed scan response with description and suggestions`() {
        // Captured verbatim from a live POST /pets/{id}/scans (video + description) -> GET poll response.
        val json = """
            {
                "id": "219a20fd-66ca-4103-85e2-38ce2ba3a290",
                "status": "complete",
                "mood_result": "anxious",
                "confidence": 0.35,
                "created_at": "2026-08-14T16:24:32",
                "pet_id": "5577eb4c-e828-428a-8865-c2c925a4141f",
                "summary": "The pet appears cautious, consistent with the owner's reported behavior.",
                "error_message": null,
                "description": "hides under the bed when guests arrive",
                "suggestions": [
                    "Provide a designated safe retreat zone with familiar bedding, food, and water where guests are not permitted to enter.",
                    "Instruct visitors to ignore the pet completely upon arrival rather than trying to approach, reach for, or coax them out."
                ]
            }
        """.trimIndent()

        val result = adapter.fromJson(json)!!

        assertEquals("complete", result.status)
        assertEquals("anxious", result.mood)
        assertEquals(0.35, result.confidence!!, 0.0001)
        assertEquals("hides under the bed when guests arrive", result.description)
        assertEquals(2, result.suggestions?.size)
        assertTrue(result.suggestions!!.all { it.isNotBlank() })
    }

    @Test
    fun `parses an older scan response missing description and suggestions`() {
        val json = """
            {
                "id": "old-scan-id",
                "status": "complete",
                "mood_result": "playful",
                "confidence": 0.87,
                "created_at": "2026-01-01T00:00:00",
                "pet_id": "some-pet-id",
                "summary": "Video summary here.",
                "error_message": null
            }
        """.trimIndent()

        val result = adapter.fromJson(json)!!

        assertEquals("playful", result.mood)
        assertNull(result.description)
        assertNull(result.suggestions)
    }
}
