package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MatchingOfferDto(
    @SerializedName("id") val id: String,
    @SerializedName("titre") val title: String,
    @SerializedName("source") val source: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("competencesRequises") val requiredSkills: List<String>,
    @SerializedName("entreprise") val company: String? = null,
    @SerializedName("localisation") val location: String? = null,
    @SerializedName("description") val description: String? = null
)

data class MatchingResultDto(
    @SerializedName("offre") val offer: MatchingOfferDto,
    @SerializedName("score") val score: Int,
    @SerializedName("competencesCorrespondantes") val matchingSkills: List<String>,
    @SerializedName("competencesManquantes") val missingSkills: List<String>,
    @SerializedName("explication") val explanation: String
)