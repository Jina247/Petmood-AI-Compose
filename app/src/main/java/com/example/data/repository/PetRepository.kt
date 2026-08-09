package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.api.PetCreate
import com.example.data.model.PetProfile

class PetRepository(private val apiService: ApiService) {

    suspend fun getPets(): List<PetProfile> {
        return apiService.getPets()
    }

    suspend fun getPet(petId: String): PetProfile {
        return apiService.getPet(petId)
    }

    suspend fun createPet(name: String, age: Int, petType: String, gender: String): PetProfile {
        return try {
            apiService.createPet(
                PetCreate(name = name, age = age, pet_type = petType, gender = gender)
            )
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            println("PetMood Error → $errorBody")
            throw e
        }
    }

    suspend fun updatePet(petId: String, name: String, age: Int, petType: String, gender: String): PetProfile {
        return apiService.updatePet(
            petId,
            PetCreate(name = name, age = age, pet_type = petType, gender = gender)
        )
    }

    suspend fun deletePet(petId: String) {
        apiService.deletePet(petId)
    }
}