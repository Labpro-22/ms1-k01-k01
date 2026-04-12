package com.tubes.nimons360.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes.nimons360.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val token = body.token ?: ""
                        val message = body.message ?: "Login Berhasil"
                        // Simpan token sebelum mengubah state ke Success
                        repository.saveToken(token)
                        _loginState.value = LoginUiState.Success(message)
                    } else {
                        _loginState.value = LoginUiState.Error("Response body kosong")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login gagal"
                    _loginState.value = LoginUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }
}
