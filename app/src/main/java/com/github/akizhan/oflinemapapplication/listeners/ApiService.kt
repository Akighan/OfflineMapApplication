package com.github.akizhan.oflinemapapplication.listeners

import com.github.akizhan.oflinemapapplication.entiies.User
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {
    //TODO: нужно вставить endpoint'ы

    @GET()
    fun getUsers(@Header("X-Access-Token") token:String): Call<List<User>>

    @POST()
    fun addUser(user:User, @Header("X-Access-Token") token:String)

    @POST()
    fun init(userName:String):Call<String>

    companion object Factory {
        private const val BASE_URL = ""

        fun create(): ApiService {
            val okHttpClient = OkHttpClient()
                .newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .build()

            return retrofit.create(ApiService::class.java);
        }
    }
}