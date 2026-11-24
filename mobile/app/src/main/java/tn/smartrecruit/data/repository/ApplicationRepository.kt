package tn.smartrecruit.data.repository

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import tn.smartrecruit.data.remote.OfferApi
import tn.smartrecruit.data.remote.dto.CandidatureDto
import tn.smartrecruit.data.remote.dto.CandidatureExterneDto
import tn.smartrecruit.data.remote.dto.TrackExternalApplicationRequestDto
import tn.smartrecruit.data.remote.toOfferError

/** Candidate applications (offer-service /api/candidatures). */
class ApplicationRepository(private val api: OfferApi) {

    suspend fun myApplications(): Result<List<CandidatureDto>> = call { api.myApplications() }

    suspend fun myExternalApplications(): Result<List<CandidatureExterneDto>> = call { api.myExternalApplications() }

    suspend fun apply(offreId: String): Result<CandidatureDto> = call { api.apply(offreId) }

    suspend fun trackExternal(request: TrackExternalApplicationRequestDto): Result<CandidatureExterneDto> =
        call { api.trackExternalApplication(request) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(Exception(detailMessage(e) ?: e.toOfferError().message ?: "Erreur inattendue.", e))
    } catch (e: Exception) {
        Result.failure(e.toOfferError())
    }

    /** Spring ProblemDetail uses "detail"; keep it (especially the 409 "already applied" message). */
    private fun detailMessage(error: HttpException): String? = runCatching {
        JsonParser.parseString(error.response()?.errorBody()?.string()).asJsonObject
            .get("detail")?.asString
    }.getOrNull()
}
