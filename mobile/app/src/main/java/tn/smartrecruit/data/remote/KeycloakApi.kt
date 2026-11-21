package tn.smartrecruit.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import tn.smartrecruit.data.remote.dto.KeycloakTokenResponse

/** Keycloak token endpoint of the realm "smartrecruit" (port 8180). */
interface KeycloakApi {
    @FormUrlEncoded
    @POST("realms/smartrecruit/protocol/openid-connect/token")
    suspend fun token(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("scope") scope: String
    ): KeycloakTokenResponse
}
