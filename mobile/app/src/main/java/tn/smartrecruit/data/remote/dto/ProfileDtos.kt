package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors ProfileResponse of auth-service. Fields that do not apply to the role are null. */
data class ProfileDto(
    val id: String? = null,
    @SerializedName("nom") val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val telephone: String? = null,
    val localisation: String? = null,
    val poste: String? = null,
    val entrepriseNom: String? = null,
    val entrepriseDescription: String? = null,
    val entrepriseSiteWeb: String? = null
)

data class UpdateProfileRequest(
    @SerializedName("nom") val fullName: String,
    val telephone: String?,
    val localisation: String?,
    val poste: String?
)

data class ChangePasswordRequest(
    @SerializedName("ancienMotDePasse") val oldPassword: String,
    @SerializedName("nouveauMotDePasse") val newPassword: String
)
