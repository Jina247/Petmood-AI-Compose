package com.example.data.model

import com.squareup.moshi.Json

data class PetProfile(
    val id: String = "",
    val name: String = "",
    @Json(name = "pet_type")
    val petType: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.unknown,
    @Json(name = "owner_id")
    val ownerId: String = "",
    val photoUri: String? = null
)

enum class Gender {
    @Json(name = "male") male,
    @Json(name = "female") female,
    @Json(name = "unknown") unknown
}