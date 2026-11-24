package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

/** A candidate's application to an internal offer (offer-service /api/candidatures). */
data class CandidatureDto(
    @SerializedName("id") val id: String,
    @SerializedName("offreId") val offreId: String,
    @SerializedName("dateCandidature") val dateCandidature: String? = null,
    @SerializedName("statut") val statut: String? = null,
    @SerializedName("offre") val offre: OffreDto? = null
)

/** History of external offer links opened by the candidate. */
data class CandidatureExterneDto(
    @SerializedName("id") val id: String,
    @SerializedName("offreId") val offreId: String,
    @SerializedName("titre") val titre: String,
    @SerializedName("entreprise") val entreprise: String? = null,
    @SerializedName("urlExterne") val urlExterne: String,
    @SerializedName("source") val source: String? = null,
    @SerializedName("dateClic") val dateClic: String? = null,
    @SerializedName("statut") val statut: String? = null
)

/** Body of POST /api/candidatures/externes. */
data class TrackExternalApplicationRequestDto(
    @SerializedName("offreId") val offreId: String,
    @SerializedName("titre") val titre: String,
    @SerializedName("entreprise") val entreprise: String? = null,
    @SerializedName("urlExterne") val urlExterne: String,
    @SerializedName("source") val source: String
)
