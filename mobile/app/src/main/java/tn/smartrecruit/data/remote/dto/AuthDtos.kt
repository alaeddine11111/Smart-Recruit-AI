package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

/*
 * ALL JSON field names of auth-service live in this file.
 * If the backend uses other names, change ONLY the @SerializedName values here.
 */

data class LoginRequest(
    val email: String,
    @SerializedName("motDePasse") val password: String
)

data class RegisterCandidateRequest(
    @SerializedName("nom") val fullName: String,
    val email: String,
    @SerializedName("motDePasse") val password: String,
    @SerializedName("telephone") val phone: String,
    @SerializedName("localisation") val city: String
)

data class RegisterRecruiterRequest(
    @SerializedName("nom") val fullName: String,
    val email: String,
    @SerializedName("motDePasse") val password: String,
    @SerializedName("poste") val position: String,
    @SerializedName("entrepriseNom") val company: String,
    @SerializedName("entrepriseDescription") val companyDescription: String,
    @SerializedName("entrepriseSiteWeb") val website: String
)

data class UserDto(
    val id: String? = null,
    @SerializedName("nom") val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerializedName("dateCreation") val createdAt: String? = null
)

data class AuthResponse(
    val token: String,
    val tokenType: String? = null,
    val expiresInMs: Long = 0,
    val user: UserDto? = null
)
