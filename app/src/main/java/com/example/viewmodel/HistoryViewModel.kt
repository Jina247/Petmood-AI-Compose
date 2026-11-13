package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScanResult
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface HistoryUIState {
    object Loading : HistoryUIState
    object Empty : HistoryUIState
    data class Error(val message: String) : HistoryUIState
    data class Success(val scanHistoryList: List<ScanResult>) : HistoryUIState
}

/**
 * History and latest scan, loaded by explicit [refresh] calls (pet change, screen entry,
 * pull-to-refresh, scan finished). Not a one-shot flow behind stateIn(WhileSubscribed): Home
 * collects this permanently, so that pattern would never re-fetch (same bug petProfile had).
 */
class HistoryViewModel(private val scanRepository: ScanRepository) : ViewModel() {
    private var petId: String? = null
    private var refreshJob: Job? = null

    private val _uiState = MutableStateFlow<HistoryUIState>(HistoryUIState.Loading)
    val uiState: StateFlow<HistoryUIState> = _uiState.asStateFlow()

    private val _scanHistory = MutableStateFlow<List<ScanResult>?>(null)
    val scanHistory: StateFlow<List<ScanResult>?> = _scanHistory.asStateFlow()

    private val _latestScan = MutableStateFlow<ScanResult?>(null)
    val latestScan: StateFlow<ScanResult?> = _latestScan.asStateFlow()

    /** True while a refresh runs on top of data already shown (drives pull-to-refresh). */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun setPetId(id: String) {
        if (id == petId) return
        petId = id
        _scanHistory.value = null
        _latestScan.value = null
        refresh()
    }

    fun refresh() {
        val id = petId ?: return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasData = _scanHistory.value != null
            if (hasData) _isRefreshing.value = true else _uiState.value = HistoryUIState.Loading

            val history = async { scanRepository.getScanHistory(id) }
            val latest = async { scanRepository.getLatestScan(id) }
            val historyResult = history.await()
            val latestResult = latest.await()

            historyResult.onSuccess { list ->
                _scanHistory.value = list
                _uiState.value = if (list.isEmpty()) HistoryUIState.Empty else HistoryUIState.Success(list)
            }.onFailure {
                // Keep showing what we already had rather than replacing it with an error.
                if (!hasData) {
                    _uiState.value = HistoryUIState.Error("Couldn't load your scan history. Check your connection and try again.")
                }
            }
            latestResult.onSuccess { _latestScan.value = it }
            _isRefreshing.value = false
        }
    }
}
