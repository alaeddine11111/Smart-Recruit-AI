package tn.smartrecruit.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    // 10.0.2.2 = the PC from the Android emulator.
    // On a real phone: use the PC's LAN IP (e.g. http://192.168.1.20).
    private const val HOST = "http://192.168.100.3"
    const val AUTH_URL = "$HOST:8081/"
    const val CV_URL = "$HOST:8082/"
    const val OFFERS_URL = "$HOST:8084/"
    const val KEYCLOAK_URL = "$HOST:8180/"
}

/** Builds the Retrofit clients. One per microservice, same OkHttp client (JWT interceptor). */
class ApiClient(private val tokenProvider: () -> String?) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath
            val isPublic = path.contains("/api/auth/login") || path.contains("/api/auth/register") ||
                    path.contains("/api/auth/keycloak") || path.contains("/openid-connect/")
            val token = tokenProvider()
            val request = if (token != null && !isPublic) {
                original.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else original
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy { retrofit(ApiConfig.AUTH_URL).create(AuthApi::class.java) }
    val keycloakApi: KeycloakApi by lazy { retrofit(ApiConfig.KEYCLOAK_URL).create(KeycloakApi::class.java) }
    // Next steps: val offerApi, val cvApi
}
