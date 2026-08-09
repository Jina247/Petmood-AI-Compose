package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScanResult
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

sealed interface HistoryUIState {
    object Loading : HistoryUIState
    object Empty : HistoryUIState
    data class Success(val scanHistoryList: List<ScanResult>) : HistoryUIState
}
class HistoryViewModel(private val scanRepository: ScanRepository) : ViewModel() {
    private val _petId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow<HistoryUIState>(HistoryUIState.Loading)
    val uiState : StateFlow<HistoryUIState> = _uiState.asStateFlow()
    fun setPetId(id: String) {
        _petId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val scanHistory = _petId.flatMapLatest { id ->
        flow {
            if (id == null) {
                emit(null)
                _uiState.value = HistoryUIState.Empty
            } else {
                val result = scanRepository.getScanHistory(id)
                if (result.isSuccess) {
                    val results = result.getOrThrow()
                    emit(results)
                    _uiState.value = HistoryUIState.Success(results)
                } else {
                    emit(null)
                    _uiState.value = HistoryUIState.Empty
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val latestScan: StateFlow<ScanResult?> = _petId.flatMapLatest { id ->
        flow {
            if (id == null) {
                emit(null)
            } else {
                val result = scanRepository.getLatestScan(id)
                emit(result.getOrNull())
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}
