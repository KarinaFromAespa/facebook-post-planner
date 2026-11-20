package com.facebookpostplanner.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.facebookpostplanner.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore("planner_settings")
private val BACKEND_URL_KEY = stringPreferencesKey("backend_url")

class SettingsStore(private val context: Context) {
    val backendUrl: Flow<String> = context.dataStore.data.map {
        it[BACKEND_URL_KEY] ?: BuildConfig.BACKEND_URL
    }
    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { it[BACKEND_URL_KEY] = url.trim().trimEnd('/') }
    }
    fun currentBackendUrl(): String = runBlocking { backendUrl.first() }
}

object ApiFactory {
    fun create(baseUrl: String): PlannerApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlannerApi::class.java)
    }
}
