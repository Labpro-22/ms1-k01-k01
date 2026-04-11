package com.tubes.nimons360.data.remote

import android.content.Context
import com.tubes.nimons360.data.local.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // base URL
    private const val BASE_URL = "https://mad.labpro.hmif.dev"

    @Volatile
    private var instance: Retrofit? = null

    // mastiin kita hanya membuat satu object Retrofit aja (Singleton)
    fun getInstance(context: Context): Retrofit {
        return instance ?: synchronized(this) {
            instance ?: buildRetrofit(context).also { instance = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        // pake applicationContext buat mencegah memory leak
        val appContext = context.applicationContext

        // inisialisasi TokenManager dan AuthInterceptor
        val tokenManager = TokenManager(appContext)
        val authInterceptor = AuthInterceptor(tokenManager, appContext)

        // pasang interceptor ke dalam OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // waktu tunggu koneksi
            .readTimeout(30, TimeUnit.SECONDS)    // waktu tunggu baca data
            .build()

        // buat Retrofit-nya
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}