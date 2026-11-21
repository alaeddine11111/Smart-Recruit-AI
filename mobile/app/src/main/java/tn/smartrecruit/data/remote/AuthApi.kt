package tn.smartrecruit.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import tn.smartrecruit.data.remote.dto.AuthResponse
import tn.smartrecruit.data.remote.dto.ChangePasswordRequest
import tn.smartrecruit.data.remote.dto.KeycloakLoginRequest
import tn.smartrecruit.data.remote.dto.LoginRequest
import tn.smartrecruit.data.remote.dto.ProfileDto
import tn.smartrecruit.data.remote.dto.RegisterCandidateRequest
import tn.smartrecruit.data.remote.dto.RegisterRecruiterRequest
import tn.smartrecruit.data.remote.dto.UpdateProfileRequest
import tn.smartrecruit.data.remote.dto.UserDto

/** Retrofit interface describing auth-service (port 8081). */
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    /** Mobile login: exchanges a Keycloak access token for the SmartRecruit JWT. */
    @POST("api/auth/keycloak")
    suspend fun loginWithKeycloak(@Body request: KeycloakLoginRequest): AuthResponse

    @POST("api/auth/register/candidat")
    suspend fun registerCandidate(@Body request: RegisterCandidateRequest)

    @POST("api/auth/register/recruteur")
    suspend fun registerRecruiter(@Body request: RegisterRecruiterRequest)

    @GET("api/auth/me")
    suspend fun me(): UserDto

    // ---- profile ----
    @GET("api/auth/profil")
    suspend fun getProfile(): ProfileDto

    @PUT("api/auth/profil")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ProfileDto

    @PUT("api/auth/mot-de-passe")
    suspend fun changePassword(@Body request: ChangePasswordRequest)
}
