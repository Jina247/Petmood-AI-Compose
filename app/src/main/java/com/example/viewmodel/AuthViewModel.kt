package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val isLoggedIn = SessionManager.isLoggedIn()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Login state
    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    val isLoginPasswordVisible = MutableStateFlow(false)

    // Register state
    val registerName = MutableStateFlow("")
    val registerEmail = MutableStateFlow("")
    val registerPassword = MutableStateFlow("")
    val registerConfirmPassword = MutableStateFlow("")
    val isRegisterPasswordVisible = MutableStateFlow(false)

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun login() {
        val email = loginEmail.value.trim()
        val password = loginPassword.value

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter both email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun register() {
        val name = registerName.value.trim()
        val email = registerEmail.value.trim()
        val password = registerPassword.value
        val confirmPassword = registerConfirmPassword.value

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _uiState.value = AuthUiState.Error("All fields are mandatory")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match!")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(email, name, password)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
        // Reset view state
        loginEmail.value = ""
        loginPassword.value = ""
        registerName.value = ""
        registerEmail.value = ""
        registerPassword.value = ""
        registerConfirmPassword.value = ""
        _uiState.value = AuthUiState.Idle
    }

    fun getUserEmail(): String {
        return authRepository.getCurrentUserEmail()
    }

    fun getUserName(): String {
        return authRepository.getCurrentUserName()
    }
}
