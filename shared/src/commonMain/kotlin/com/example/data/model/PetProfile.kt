package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PetProfile(
    val id: String = "",
    val name: String = "",
    @SerialName("pet_type")
    val petType: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.unknown,
    @SerialName("owner_id")
    val ownerId: String = "",
    val photoUri: String? = null
)

@Serializable
enum class Gender {
    @SerialName("male") male,
    @SerialName("female") female,
    @SerialName("unknown") unknown
}
