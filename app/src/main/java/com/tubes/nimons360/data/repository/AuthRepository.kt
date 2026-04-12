package com.tubes.nimons360.data.repository

import com.tubes.nimons360.data.local.TokenManager
import com.tubes.nimons360.data.model.LoginRequest
import com.tubes.nimons360.data.model.LoginResponse
import com.tubes.nimons360.data.remote.ApiService
import retrofit2.Response

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return apiService.login(LoginRequest(email, password))
    }

    suspend fun saveToken(token: String) {
        tokenManager.saveToken(token)
    }
}
