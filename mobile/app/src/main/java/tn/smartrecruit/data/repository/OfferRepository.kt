package tn.smartrecruit.data.repository

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import tn.smartrecruit.data.remote.OfferApi
import tn.smartrecruit.data.remote.dto.EntrepriseDto
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.remote.dto.OffreRequestDto
import tn.smartrecruit.data.remote.dto.PageDto
import tn.smartrecruit.data.remote.dto.toOffreDto
import tn.smartrecruit.data.remote.toOfferError
import java.util.concurrent.ConcurrentHashMap

class OfferRepository(private val api: OfferApi) {

    private val offerCache = ConcurrentHashMap<String, OffreDto>()

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toOfferError())
        }

    /** Success(null) when the recruiter has no company yet (HTTP 404). */
    suspend fun getMyCompany(): Result<EntrepriseDto?> =
        try {
            Result.success(api.getMyCompany())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 404) Result.success(null) else Result.failure(e.toOfferError())
        } catch (e: Throwable) {
            Result.failure(e.toOfferError())
        }

    suspend fun saveMyCompany(company: EntrepriseDto): Result<EntrepriseDto> = call { api.saveMyCompany(company) }

    suspend fun getMyOffers(statut: String?, titre: String?, page: Int, size: Int = 10): Result<PageDto<OffreDto>> =
        call {
            val res = api.getMyOffers(statut, titre, page, size)
            res.content.forEach { offerCache[it.id] = it }
            res
        }

    suspend fun getMyOffer(id: String): Result<OffreDto> = call {
        val offer = api.getMyOffer(id)
        offerCache[offer.id] = offer
        offer
    }

    suspend fun createOffer(body: OffreRequestDto): Result<OffreDto> = call {
        val offer = api.createOffer(body)
        offerCache[offer.id] = offer
        offer
    }

    suspend fun updateOffer(id: String, body: OffreRequestDto): Result<OffreDto> = call {
        val offer = api.updateOffer(id, body)
        offerCache[offer.id] = offer
        offer
    }

    suspend fun changeStatus(id: String, value: String): Result<OffreDto> = call {
        val offer = api.changeStatus(id, value)
        offerCache[offer.id] = offer
        offer
    }

    suspend fun deleteOffer(id: String): Result<Unit> = call {
        val response = api.deleteOffer(id)
        if (!response.isSuccessful) throw HttpException(response)
        offerCache.remove(id)
        Unit
    }

    /** Candidate / public offers endpoint with memory caching and graceful fallback. */
    suspend fun getPublicOffers(
        titre: String? = null,
        typeContrat: String? = null,
        localisation: String? = null,
        page: Int = 0,
        size: Int = 10
    ): Result<PageDto<OffreDto>> = call {
        val res = try {
            api.getPublicOffers(
                titre = titre?.trim()?.ifBlank { null },
                typeContrat = typeContrat?.trim()?.ifBlank { null },
                localisation = localisation?.trim()?.ifBlank { null },
                page = page,
                size = size
            )
        } catch (e: Exception) {
            api.getMyOffers(
                statut = "PUBLISHED",
                titre = titre?.trim()?.ifBlank { null },
                page = page,
                size = size
            )
        }
        res.content.forEach { offerCache[it.id] = it }
        res
    }

    suspend fun getPublicOffer(id: String): Result<OffreDto> = call {
        // Return from cache if present (e.g. external offer or recently fetched offer)
        offerCache[id]?.let { return@call it }

        val offer = api.getPublicOffer(id)
        offerCache[offer.id] = offer
        offer
    }

    suspend fun getExternalOffers(
        q: String = "",
        location: String = "",
        page: Int = 1
    ): Result<List<OffreDto>> = call {
        val list = api.getExternalOffers(q = q.trim(), location = location.trim(), page = page)
        val mapped = list.map { it.toOffreDto() }
        mapped.forEach { offerCache[it.id] = it }
        mapped
    }
}
