package com.tubes.nimons360.data.remote

import com.tubes.nimons360.data.model.LoginRequest
import com.tubes.nimons360.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ApiService {

    // Auth & Profile
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/api/me")
    suspend fun getProfile(): Response<Any> // Ganti dengan ProfileResponse

    @PATCH("/api/me")
    suspend fun updateProfile(@Body request: Any): Response<Any> // Ganti dengan UpdateProfileRequest


    // @GET("/api/families")
    // suspend fun getFamilies(): Response<Any>

    // @POST("/api/families")
    // suspend fun createFamily(@Body request: Any): Response<Any>

}