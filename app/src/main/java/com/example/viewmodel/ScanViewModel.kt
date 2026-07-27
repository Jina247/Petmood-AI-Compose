package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScanResult
import com.example.data.repository.PetRepository
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Analysing : ScanUiState
    data class Success(val result: ScanResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val petRepository: PetRepository)
    : ViewModel() {

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    // Active screen result
    private val _currentScanResult = MutableStateFlow<ScanResult?>(null)
    val currentScanResult: StateFlow<ScanResult?> = _currentScanResult.asStateFlow()

    // Profile input states (Setup / Editing)
    private val _inputPetName = MutableStateFlow("")
    val inputPetName: StateFlow<String> = _inputPetName.asStateFlow()

    private val _inputPetAge = MutableStateFlow("")
    val inputPetAge: StateFlow<String> = _inputPetAge.asStateFlow()

    private val _inputPetType = MutableStateFlow("Cat")
    val inputPetType: StateFlow<String> = _inputPetType.asStateFlow()

    private val _inputPetPhotoUri = MutableStateFlow<String?>(null)
    val inputPetPhotoUri: StateFlow<String?> = _inputPetPhotoUri.asStateFlow()

    private val _inputPetGender = MutableStateFlow("unknown")
    val inputPetGender: StateFlow<String> = _inputPetGender.asStateFlow()

    // Save profile status
    private val _profileSaveSuccess = MutableStateFlow(false)
    val profileSaveSuccess = _profileSaveSuccess.asStateFlow()
    
    val petProfile = flow {
        try {
            val pets = petRepository.getPets()
            emit(pets.firstOrNull())
        } catch (e: Exception) {
            emit(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun resetProfileSaveState() {
        _profileSaveSuccess.value = false
    }

    // Selected pet for scan
    private val _currentPetId = MutableStateFlow<String?>(null)
    val currentPetId: StateFlow<String?> = _currentPetId.asStateFlow()

    fun setPetId(petId: String) {
        _currentPetId.value = petId
    }

    fun startAnalysis(fakeVideo: File) {
        val petId = _currentPetId.value
        if (petId == null) {
            _scanUiState.value = ScanUiState.Error("Please select a pet first")
            return
        }

        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Analysing
            _currentScanResult.value = null
            
            val result = try {
                val response = scanRepository.uploadScan(petId, fakeVideo)
                val mappedResult = ScanResult(
                    id = response.id,
                    status = response.status,
                    mood = response.moodResult,
                    confidence = response.confidence,
                    petId = response.petId,
                    timestamp = response.createdAt
                )
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _currentScanResult.value = data
                _scanUiState.value = ScanUiState.Success(data)
            } else {
                _scanUiState.value = ScanUiState.Error(
                    result.exceptionOrNull()?.message ?: "Hmm, we couldn't read your pet's mood. Try again in better lighting!"
                )
            }
        }
    }

    fun clearActiveResult() {
        _currentScanResult.value = null
        _scanUiState.value = ScanUiState.Idle
    }

    fun showScanDetails(scan: ScanResult) {
        _currentScanResult.value = scan
        _scanUiState.value = ScanUiState.Success(scan)
    }
}
