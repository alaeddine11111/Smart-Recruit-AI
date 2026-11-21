package tn.smartrecruit.data.repository

import kotlinx.coroutines.flow.Flow
import tn.smartrecruit.data.local.TokenStore
import tn.smartrecruit.data.model.Role
import tn.smartrecruit.data.remote.AuthApi
import tn.smartrecruit.data.remote.KeycloakApi
import tn.smartrecruit.data.remote.KeycloakConfig
import tn.smartrecruit.data.remote.dto.KeycloakLoginRequest
import tn.smartrecruit.data.remote.dto.LoginRequest
import tn.smartrecruit.data.remote.dto.RegisterCandidateRequest
import tn.smartrecruit.data.remote.dto.RegisterRecruiterRequest
import tn.smartrecruit.data.remote.toUserMessage

/** Single source of truth for authentication (MVVM: data layer). */
class AuthRepository(
    private val api: AuthApi,
    private val keycloakApi: KeycloakApi,
    private val store: TokenStore
) {
    val role: Flow<String?> = store.role
    val userName: Flow<String?> = store.name

    /**
     * Login. With Keycloak: 1) password login on Keycloak, 2) the Keycloak token is exchanged on
     * auth-service for the SmartRecruit JWT, which the other services accept.
     */
    suspend fun login(email: String, password: String): Result<Role> = try {
        val response = if (KeycloakConfig.USE_KEYCLOAK) {
            val keycloakToken = keycloakApi.token(
                grantType = "password",
                clientId = KeycloakConfig.CLIENT_ID,
                username = email.trim(),
                password = password,
                scope = "openid"
            )
            api.loginWithKeycloak(KeycloakLoginRequest(keycloakToken.accessToken))
        } else {
            api.login(LoginRequest(email.trim(), password))
        }

        val role = Role.from(response.user?.role)
        if (role == Role.UNKNOWN) {
            Result.failure(Exception("Rôle introuvable dans la réponse du serveur (reçu : ${response.user?.role})."))
        } else if (role == Role.ADMIN) {
            Result.failure(Exception("Les administrateurs utilisent le site web."))
        } else {
            store.save(response.token, role.name, response.user?.fullName ?: email)
            Result.success(role)
        }
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun registerCandidate(request: RegisterCandidateRequest): Result<Unit> = try {
        api.registerCandidate(request)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun registerRecruiter(request: RegisterRecruiterRequest): Result<Unit> = try {
        api.registerRecruiter(request)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun logout() = store.clear()
}
