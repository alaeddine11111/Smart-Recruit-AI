package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors CvInfo of cv-service. */
data class CvInfoDto(
    @SerializedName("nomFichier") val fileName: String? = null,
    @SerializedName("taille") val size: Long = 0,
    @SerializedName("dateDepot") val uploadedAt: String? = null
)

/** Mirrors AnalyseInfo. status: EN_COURS, TERMINEE or ECHEC. */
data class AnalysisDto(
    @SerializedName("statut") val status: String? = null,
    @SerializedName("messageErreur") val errorMessage: String? = null,
    @SerializedName("texteOCR") val rawText: String? = null,
    @SerializedName("ocrUtilise") val ocrUsed: Boolean = false
)

data class SkillDto(
    @SerializedName("nom") val name: String? = null,
    @SerializedName("categorie") val category: String? = null
)

data class ExperienceDto(
    @SerializedName("poste") val position: String? = null,
    @SerializedName("entreprise") val company: String? = null,
    @SerializedName("dateDebut") val startDate: String? = null,
    @SerializedName("dateFin") val endDate: String? = null,
    val description: String? = null
)

data class EducationDto(
    @SerializedName("diplome") val degree: String? = null,
    @SerializedName("etablissement") val school: String? = null,
    @SerializedName("dateDebut") val startDate: String? = null,
    @SerializedName("dateFin") val endDate: String? = null
)

data class LanguageDto(
    @SerializedName("nom") val name: String? = null,
    @SerializedName("niveau") val level: String? = null
)

data class CustomSectionDto(
    val id: String? = null,
    @SerializedName("titre") val title: String? = null,
    @SerializedName("elements") val items: List<String>? = null
)

/** Mirrors Profil of cv-service (data extracted from the CV). */
data class CandidateProfileDto(
    val id: String? = null,
    @SerializedName("candidatId") val candidateId: String? = null,
    @SerializedName("titre") val title: String? = null,
    @SerializedName("resume") val summary: String? = null,
    @SerializedName("telephone") val phone: String? = null,
    @SerializedName("localisation") val location: String? = null,
    val experiences: List<ExperienceDto>? = null,
    @SerializedName("formations") val education: List<EducationDto>? = null,
    @SerializedName("competences") val skills: List<SkillDto>? = null,
    @SerializedName("competencesDetectees") val detectedSkills: List<SkillDto>? = null,
    @SerializedName("competencesValidees") val validatedSkills: List<SkillDto>? = null,
    @SerializedName("langues") val languages: List<LanguageDto>? = null,
    @SerializedName("sections") val sections: List<CustomSectionDto>? = null,
    @SerializedName("valide") val validated: Boolean = false,
    @SerializedName("misAJourLe") val updatedAt: String? = null
)

/** Every skill name known for this profile (detected, legacy and validated), without duplicates. */
fun CandidateProfileDto.skillNames(): List<String> =
    (detectedSkills.orEmpty() + skills.orEmpty() + validatedSkills.orEmpty())
        .mapNotNull { it.name?.takeIf(String::isNotBlank) }
        .distinct()