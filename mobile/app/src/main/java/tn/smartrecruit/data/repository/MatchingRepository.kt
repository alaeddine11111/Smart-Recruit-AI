package tn.smartrecruit.data.repository

import tn.smartrecruit.data.remote.dto.ExternalOffreDto
import tn.smartrecruit.data.remote.dto.MatchingOfferDto
import tn.smartrecruit.data.remote.dto.MatchingResultDto

class MatchingRepository(
    private val cvRepository: CvRepository,
    private val offerRepository: OfferRepository
) {
    private val knownSkills = listOf(
        "Spring Boot", "Machine Learning", "Power BI", "JavaScript",
        "TypeScript", "PostgreSQL", "TensorFlow", "Kubernetes",
        "Bootstrap", "Tailwind", "Node.js", "Express", "Hibernate",
        "Symfony", "Laravel", "FastAPI", "Java", "Spring", "Python",
        "Angular", "React", "Vue", "PHP", "SQL", "MySQL", "MongoDB",
        "Oracle", "Docker", "GitHub", "GitLab", "Jenkins", "Linux",
        "Azure", "AWS", "Maven", "REST", "HTML", "CSS", "Flutter",
        "Android", "Kotlin", "Swift", "Figma", "Scrum", "Agile",
        "Jira", "Excel", "Django", "Flask", "Firebase", "Selenium",
        "Postman", "MATLAB", "Arduino", "C++", "C#", ".NET", "JPA",
        "Git"
    )

    suspend fun loadRecommendations(): Result<List<MatchingResultDto>> {
        val profileResult = cvRepository.profile()

        val profile = profileResult.getOrElse { error ->
            return Result.failure(error)
        }

        if (!profile.validated) {
            return Result.failure(Exception(
                "Validez d’abord votre profil dans l’écran Mon CV."
            ))
        }

        val candidateSkills = profile.validatedSkills
            .orEmpty()
            .mapNotNull { it.name?.trim()?.takeIf(String::isNotBlank) }
            .distinct()

        if (candidateSkills.isEmpty()) {
            return Result.failure(Exception(
                "Sélectionnez au moins une compétence validée."
            ))
        }

        val internalOffers = mutableListOf<MatchingOfferDto>()
        var page = 0
        var totalPages = 1

        while (page < totalPages) {
            val pageResult = offerRepository.getPublicOffers(
                page = page,
                size = 100
            )

            val pageData = pageResult.getOrElse { error ->
                return Result.failure(error)
            }

            pageData.content.forEach { offer ->
                internalOffers += MatchingOfferDto(
                    id = offer.id,
                    title = offer.titre,
                    source = "INTERNE",
                    url = null,
                    requiredSkills = offer.competences,
                    company = offer.entrepriseNom,
                    location = offer.localisation,
                    description = offer.description
                )
            }

            totalPages = pageData.totalPages
            page++
        }

        val externalOffers = mutableMapOf<String, ExternalOfferSearch>()
        val externalSearchErrors = mutableListOf<String>()

        candidateSkills.forEach { skill ->
            val result = offerRepository.getExternalOfferDtos(
                q = skill,
                location = "",
                page = 1
            )

            result.onSuccess { offers ->
                offers.forEach { offer ->
                    val current = externalOffers[offer.id]
                        ?: ExternalOfferSearch(offer = offer, searchSkills = mutableSetOf())
                    current.searchSkills += skill
                    externalOffers[offer.id] = current
                }
            }.onFailure { error ->
                externalSearchErrors += error.message ?: "Recherche externe impossible"
            }
        }

        val externalMatchingOffers = externalOffers.map { (_, item) ->
            val offer = item.offer
            val text = normalize(
                "${offer.titre} ${offer.description.orEmpty()}"
            )

            val requiredSkills = (knownSkills + candidateSkills)
                .distinctBy(::normalize)
                .filter { skill ->
                    val normalizedSearchSkill = normalize(skill)

                    if (item.searchSkills.any {
                            normalize(it) == normalizedSearchSkill
                        }) {
                        true
                    } else {
                        normalizedSearchSkill.isNotBlank() &&
                            text.contains(normalizedSearchSkill)
                    }
                }

            MatchingOfferDto(
                id = "external:${offer.id}",
                title = offer.titre,
                source = offer.source ?: "EXTERNE",
                url = offer.urlExterne,
                requiredSkills = requiredSkills,
                company = offer.entreprise,
                location = offer.localisation,
                description = offer.description
            )
        }

        val allOffers = internalOffers + externalMatchingOffers

        if (allOffers.isEmpty()) {
            return Result.failure(Exception(
                "Aucune offre interne ou externe n’est disponible."
            ))
        }

        val matchingResult = cvRepository.evaluateMatching(allOffers)

        if (matchingResult.isFailure) {
            val error = matchingResult.exceptionOrNull()
            return Result.failure(error ?: Exception("Impossible de calculer les recommandations."))
        }

        val results = matchingResult.getOrThrow()
            .sortedByDescending { it.score }

        return Result.success(results)
    }

    private data class ExternalOfferSearch(
        val offer: ExternalOffreDto,
        val searchSkills: MutableSet<String>
    )

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}