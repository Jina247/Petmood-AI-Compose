package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Gender
import com.example.data.model.PetProfile
import com.example.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PetUiState {
    object Idle : PetUiState
    object Loading : PetUiState
    data class Success(val pets: List<PetProfile>) : PetUiState
    data class Error(val message: String) : PetUiState
}


class PetViewModel(private val repository: PetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<PetUiState>(PetUiState.Idle)
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    private val _isLoadingPets = MutableStateFlow(false)
    val isLoadingPets: StateFlow<Boolean> = _isLoadingPets.asStateFlow()
    private val _selectedPet = MutableStateFlow<PetProfile?>(null)
    val selectedPet: StateFlow<PetProfile?> = _selectedPet.asStateFlow()

    // input fields for pet profile form
    private val _inputPetName = MutableStateFlow("")
    val inputPetName: StateFlow<String> = _inputPetName.asStateFlow()

    private val _inputPetAge = MutableStateFlow<String>("")
    val inputPetAge: StateFlow<String> = _inputPetAge.asStateFlow()

    private val _inputPetType = MutableStateFlow("Cat")
    val inputPetType: StateFlow<String> = _inputPetType.asStateFlow()

    private val _inputPetPhotoUri = MutableStateFlow<String?>(null)
    val inputPetPhotoUri: StateFlow<String?> = _inputPetPhotoUri.asStateFlow()

    private val _inputPetGender = MutableStateFlow(Gender.unknown)
    val inputPetGender: StateFlow<Gender> = _inputPetGender.asStateFlow()

    fun resetState() {
        _uiState.value = PetUiState.Idle
    }
    // Validation logic moved to ViewModel
    val isSaveEnabled: StateFlow<Boolean> = combine(
        _inputPetName,
        _inputPetAge,
        _uiState
    ) { name, age, state ->
        name.trim().isNotEmpty() && age.trim().isNotEmpty() && state !is PetUiState.Loading
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onNameChange(newName: String) {
        _inputPetName.value = newName
    }

    fun onAgeChange(newAge: String) {
        // Basic numeric filtering can be done here or in the UI
        _inputPetAge.value = newAge
    }

    fun onTypeChange(newType: String) {
        _inputPetType.value = newType
    }

    fun onGenderChange(newGender: Gender) {
        _inputPetGender.value = newGender
    }
    fun onPhotoUriChange(newUri: String?) {
        _inputPetPhotoUri.value = newUri
    }

    fun loadPets() {
        viewModelScope.launch {
            _uiState.value = PetUiState.Idle
            _uiState.value = PetUiState.Loading
            try {
                val pets = repository.getPets()
                _uiState.value = PetUiState.Success(pets)
                if (pets.isNotEmpty()) {
                    _selectedPet.value = pets.first()
                    populateForm(pets.first()) // fill form with existing pet data
                }
            } catch (e: Exception) {
                _uiState.value = PetUiState.Error(e.message ?: "Failed to load pets")
            }
        }
    }

    fun selectPet(pet: PetProfile) {
        _selectedPet.value = pet
        populateForm(pet)
    }

    fun clearForm() {
        _selectedPet.value = null
        _inputPetName.value = ""
        _inputPetAge.value = ""
        _inputPetType.value = "Cat"
        _inputPetGender.value = Gender.unknown
        _inputPetPhotoUri.value = null
    }

    // fill form fields when editing existing pet
    private fun populateForm(pet: PetProfile) {
        _inputPetName.value = pet.name
        _inputPetAge.value = pet.age.toString()
        _inputPetType.value = pet.petType
        _inputPetGender.value = pet.gender
    }

    fun checkHasPets(
        onHasPets: () -> Unit,
        onNoPets: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val pets = repository.getPets()
                if (pets.isNotEmpty()) {
                    _selectedPet.value = pets.first()
                    populateForm(pets.first())
                    onHasPets()
                } else {
                    onNoPets()
                }
            } catch (e: Exception) {
                onNoPets()
            }
        }
    }

//    fun getPet() {
//        val petId = selectedPet.value?.id ?: return
//        viewModelScope.launch {
//            try {
//            val pet = repository.getPet(petId)
//            _selectedPet.value = pet
//        } catch (e: Exception) {
//            _uiState.value = PetUiState.Error(e.message ?: "Failed to load pet")
//            }
//        }
//    }

    // Save pet profile in registration page
    fun savePetProfile() {
        val name = _inputPetName.value.trim()
        val ageText = _inputPetAge.value.trim()
        val type = _inputPetType.value
        val gender = _inputPetGender.value.name.lowercase()

        // basic validation
        if (name.isEmpty() || ageText.isEmpty()) {
            _uiState.value = PetUiState.Error("Name and age are required")
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age < 0 || age > 20) {
            _uiState.value = PetUiState.Error("Please enter a valid age (0-20)")
            return
        }
        println("PetMood Debug → name=$name, age=$age, type=$type, gender=$gender")
        viewModelScope.launch {
            _uiState.value = PetUiState.Loading
            try {
                val existing = _selectedPet.value
                if (existing != null) {
                    // pet exists → update it
                    val updated = repository.updatePet(
                        petId = existing.id ,
                        name = name,
                        age = age,
                        petType = type,
                        gender = gender
                    )
                    _selectedPet.value = updated
                } else {
                    // no pet yet → create new one
                    val created = repository.createPet(
                        name = name,
                        age = age,
                        petType = type,
                        gender = gender
                    )
                    _selectedPet.value = created
                }
                _uiState.value = PetUiState.Success(listOf(_selectedPet.value!!))
            } catch (e: Exception) {
                _uiState.value = PetUiState.Error(e.message ?: "Failed to save pet")
            } finally {
                _isLoadingPets.value = false
            }
        }
    }
}
