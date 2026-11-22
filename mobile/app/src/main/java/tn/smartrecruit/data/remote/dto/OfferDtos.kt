package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Company of the recruiter (offer-service, port 8084). Same shape for GET and PUT. */
data class EntrepriseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("nom") val nom: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("secteur") val secteur: String? = null,
    @SerializedName("siteWeb") val siteWeb: String? = null,
    @SerializedName("ville") val ville: String? = null,
    @SerializedName("adresse") val adresse: String? = null,
    @SerializedName("telephone") val telephone: String? = null,
    @SerializedName("emailContact") val emailContact: String? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null
)

/** Job offer as returned by the API. Dates are ISO-8601 strings. */
data class OffreDto(
    @SerializedName("id") val id: String,
    @SerializedName("titre") val titre: String,
    @SerializedName("description") val description: String,
    @SerializedName("localisation") val localisation: String,
    @SerializedName("competences") val competences: List<String> = emptyList(),
    @SerializedName("typeContrat") val typeContrat: String,      // CDI, CDD, STAGE, FREELANCE, ALTERNANCE
    @SerializedName("salaireMin") val salaireMin: Double? = null,
    @SerializedName("salaireMax") val salaireMax: Double? = null,
    @SerializedName("devise") val devise: String? = null,
    @SerializedName("statut") val statut: String,                // DRAFT, PUBLISHED, CLOSED
    @SerializedName("entrepriseId") val entrepriseId: String? = null,
    @SerializedName("entrepriseNom") val entrepriseNom: String? = null,
    @SerializedName("dateCreation") val dateCreation: String? = null,
    @SerializedName("dateMiseAJour") val dateMiseAJour: String? = null,
    @SerializedName("datePublication") val datePublication: String? = null,
    @SerializedName("urlExterne") val urlExterne: String? = null
)

/** Body of POST /api/recruteur/offres and PUT /api/recruteur/offres/{id}. */
data class OffreRequestDto(
    @SerializedName("titre") val titre: String,
    @SerializedName("description") val description: String,
    @SerializedName("localisation") val localisation: String,
    @SerializedName("competences") val competences: List<String>,
    @SerializedName("typeContrat") val typeContrat: String,
    @SerializedName("salaireMin") val salaireMin: Double? = null,
    @SerializedName("salaireMax") val salaireMax: Double? = null,
    @SerializedName("devise") val devise: String? = "TND",
    @SerializedName("statut") val statut: String
)

/** Spring page wrapper used by the offer lists. */
data class PageDto<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int
)

/** External job offers returned by GET /api/offres-externes */
data class ExternalOffreDto(
    @SerializedName("id") val id: String,
    @SerializedName("titre") val titre: String,
    @SerializedName("entreprise") val entreprise: String? = null,
    @SerializedName("localisation") val localisation: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("salaire") val salaire: String? = null,
    @SerializedName("typeContrat") val typeContrat: String? = null,
    @SerializedName("dateMiseAJour") val dateMiseAJour: String? = null,
    @SerializedName("urlExterne") val urlExterne: String? = null,
    @SerializedName("source") val source: String? = null
)

fun ExternalOffreDto.toOffreDto(): OffreDto = OffreDto(
    id = id,
    titre = titre,
    description = description.takeIf { !it.isNullOrBlank() }
        ?: "Offre d'emploi provenant de $source. Pour en savoir plus et postuler, consultez l'annonce originale.",
    localisation = localisation.takeIf { !it.isNullOrBlank() } ?: "Non spécifiée / Externe",
    typeContrat = typeContrat.takeIf { !it.isNullOrBlank() } ?: "CDI",
    statut = "PUBLISHED",
    entrepriseNom = entreprise.takeIf { !it.isNullOrBlank() } ?: source ?: "Source Externe",
    dateCreation = dateMiseAJour,
    datePublication = dateMiseAJour,
    urlExterne = urlExterne
)
