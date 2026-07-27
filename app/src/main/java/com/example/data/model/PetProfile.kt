package com.example.data.model

import com.google.gson.annotations.SerializedName

data class PetProfile(
    val id: String = "",
    val name: String = "",
    @SerializedName("pet_type")
    val petType: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.unknown,
    @SerializedName("owner_id")
    val ownerId: String = "",
    val photoUri: String = ""
)

enum class Gender {
    @SerializedName("male") male,
    @SerializedName("female") female,
    @SerializedName("unknown") unknown
}