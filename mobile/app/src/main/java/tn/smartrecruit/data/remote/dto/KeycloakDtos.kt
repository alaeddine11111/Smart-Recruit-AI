package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

data class KeycloakTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long = 0
)

/** Body of POST /api/auth/keycloak (auth-service). */
data class KeycloakLoginRequest(
    @SerializedName("accessToken") val accessToken: String
)
