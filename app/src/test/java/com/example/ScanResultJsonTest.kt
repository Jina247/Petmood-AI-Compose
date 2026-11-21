package com.example

import com.example.data.api.ApiException
import com.example.data.api.FcmTokenRequest
import com.example.data.api.PetMoodJson
import com.example.data.model.Gender
import com.example.data.model.PetProfile
import com.example.data.model.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the models parse real backend responses with the same JSON setup the API client
 * uses ([PetMoodJson]): the current scan shape, an older one missing newer fields, and fields
 * the models don't declare.
 */
class ScanResultJsonTest {

    private fun parse(json: String): ScanResult = PetMoodJson.decodeFromString(json)

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

        val result = parse(json)

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

        val result = parse(json)

        assertEquals("playful", result.mood)
        assertNull(result.description)
        assertNull(result.suggestions)
    }

    @Test
    fun `ignores fields the model doesn't declare and nulls for defaulted fields`() {
        val json = """
            {
                "id": "s1",
                "status": "processing",
                "pet_id": null,
                "photo_paths": ["/tmp/a.jpg"],
                "updated_at": "2026-01-01T00:00:00"
            }
        """.trimIndent()

        val result = parse(json)

        assertEquals("processing", result.status)
        assertEquals("", result.petId)
    }

    @Test
    fun `parses a pet with its lowercase gender`() {
        val pet = PetMoodJson.decodeFromString<PetProfile>(
            """{"id":"p1","name":"Milo","pet_type":"dog","age":3,"gender":"male","owner_id":"u1"}"""
        )
        assertEquals(Gender.male, pet.gender)
        assertEquals("dog", pet.petType)
    }

    @Test
    fun `logout sends fcm_token as an explicit null`() {
        assertEquals("""{"fcm_token":null}""", PetMoodJson.encodeToString(FcmTokenRequest(null)))
    }

    @Test
    fun `api errors expose the backend's message but not validation lists`() {
        assertEquals("Incorrect email or password", ApiException(401, """{"detail":"Incorrect email or password"}""").detail)
        assertNull(ApiException(422, """{"detail":[{"loc":["body","email"],"msg":"bad"}]}""").detail)
        assertNull(ApiException(500, "Internal Server Error").detail)
    }
}
