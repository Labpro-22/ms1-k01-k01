package com.tubes.nimons360.data.remote

import android.content.Context
import android.content.Intent
import com.tubes.nimons360.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class `AuthInterceptor`(
    private val tokenManager: TokenManager,
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // ambil token secara sinkron menggunakan runBlocking
        // karena interceptor Retrofit berjalan di background thread, bukan UI thread
        val token = runBlocking { tokenManager.getToken().first() }

        // kalo token ada, masukkan ke header Authorization
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // request
        val response = chain.proceed(requestBuilder.build())

        // handle kalo token expired atau tidak valid
        if (response.code == 401 || response.code == 409) {

            // hapus token yang udah tidak valid
            runBlocking { tokenManager.clearToken() }

            // force redirect ke LoginActivity
            val intent = Intent().apply {
                setClassName(context, "com.tubes.nimons360.ui.auth.LoginActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}