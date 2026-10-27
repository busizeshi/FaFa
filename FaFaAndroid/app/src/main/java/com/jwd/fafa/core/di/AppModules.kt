package com.jwd.fafa.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jwd.fafa.BuildConfig
import com.jwd.fafa.core.config.FaFaConfig
import com.jwd.fafa.core.data.SessionStore
import com.jwd.fafa.core.network.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "fafa_session")

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fafa_user_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @SessionDataStore
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.sessionDataStore

    @Provides
    @Singleton
    @UserPrefsDataStore
    fun userPrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.userPrefsDataStore
}

internal const val HEADER_TOKEN = "fafa-token"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun okHttpClient(config: FaFaConfig, sessionStore: SessionStore): OkHttpClient {
        val tokenInterceptor = Interceptor { chain ->
            val token = sessionStore.tokenNow()
            val request = if (token != null) {
                chain.request().newBuilder().header(HEADER_TOKEN, token).build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .addInterceptor(tokenInterceptor)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "FaFaAndroid/${BuildConfig.VERSION_NAME}")
                        .build()
                )
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (config.enableHttpLog) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun retrofit(config: FaFaConfig, json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.baseUrl.let { if (it.endsWith("/")) it else "$it/" })
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}
