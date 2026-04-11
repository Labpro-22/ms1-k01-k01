package com.tubes.nimons360.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

// perlu buat Data Class buat Request dan Response ini nanti
// (misal: LoginRequest, LoginResponse, ProfileResponse)

interface ApiService {

    // Auth & Profile
    @POST("/api/login")
    suspend fun login(@Body request: Any): Response<Any> // Ganti 'Any' dengan LoginRequest & LoginResponse

    @GET("/api/me")
    suspend fun getProfile(): Response<Any> // Ganti dengan ProfileResponse

    @PATCH("/api/me")
    suspend fun updateProfile(@Body request: Any): Response<Any> // Ganti dengan UpdateProfileRequest


    // @GET("/api/families")
    // suspend fun getFamilies(): Response<Any>

    // @POST("/api/families")
    // suspend fun createFamily(@Body request: Any): Response<Any>

}