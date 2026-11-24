package tn.smartrecruit.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    // 10.0.2.2 = the PC from the Android emulator.
    // On a real phone: use the PC's LAN IP (e.g. http://192.168.1.20).
    private const val HOST = "http://192.168.1.191"
    const val AUTH_URL = "$HOST:8081/"
    const val CV_URL = "$HOST:8082/"
    const val OFFERS_URL = "$HOST:8084/"
    const val KEYCLOAK_URL = "$HOST:8180/"
}

/** Builds the Retrofit clients. One per microservice, same OkHttp client (JWT interceptor). */
class ApiClient(private val tokenProvider: () -> String?) {

    private fun client(): OkHttpClient.Builder = OkHttpClient.Builder()
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

    private val http: OkHttpClient = client().build()

    /**
     * cv-service starts/finishes an interview by calling Groq synchronously (up to 45s per model,
     * plus fallbacks). The default 10s read timeout broke those calls before the server answered.
     */
    private val cvHttp: OkHttpClient = client()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient = http): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy { retrofit(ApiConfig.AUTH_URL).create(AuthApi::class.java) }
    val keycloakApi: KeycloakApi by lazy { retrofit(ApiConfig.KEYCLOAK_URL).create(KeycloakApi::class.java) }
    val cvApi: CvApi by lazy { retrofit(ApiConfig.CV_URL, cvHttp).create(CvApi::class.java) }
    val offerApi: OfferApi by lazy { retrofit(ApiConfig.OFFERS_URL).create(OfferApi::class.java) }
}
