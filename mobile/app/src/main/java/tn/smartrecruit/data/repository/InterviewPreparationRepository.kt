package tn.smartrecruit.data.repository

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import tn.smartrecruit.data.remote.CvApi
import tn.smartrecruit.data.remote.dto.PreparationSessionDto
import tn.smartrecruit.data.remote.dto.StartPreparationRequestDto
import tn.smartrecruit.data.remote.dto.SubmitAnswerRequestDto
import tn.smartrecruit.data.remote.toUserMessage
import java.net.SocketTimeoutException

/** Interview preparation sessions (cv-service /api/preparations). */
class InterviewPreparationRepository(private val api: CvApi) {

    suspend fun history(): Result<List<PreparationSessionDto>> = call { api.preparationHistory() }

    suspend fun get(id: String): Result<PreparationSessionDto> = call { api.preparation(id) }

    suspend fun start(offreId: String?): Result<PreparationSessionDto> =
        call { api.startPreparation(StartPreparationRequestDto(offreId?.trim()?.ifBlank { null })) }

    suspend fun submitAnswer(sessionId: String, questionId: String, answer: String): Result<PreparationSessionDto> =
        call { api.submitPreparationAnswer(sessionId, SubmitAnswerRequestDto(questionId, answer.trim())) }

    suspend fun finish(sessionId: String): Result<PreparationSessionDto> = call { api.finishPreparation(sessionId) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(Exception(messageFor(e)))
    } catch (e: SocketTimeoutException) {
        Result.failure(Exception("Le service IA met trop de temps à répondre. Réessayez dans un instant."))
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    /** The backend reasons are in French; keep them instead of the generic auth/409 messages. */
    private fun messageFor(error: HttpException): String {
        val serverMessage = runCatching {
            JsonParser.parseString(error.response()?.errorBody()?.string()).asJsonObject
                .get("message")?.asString
        }.getOrNull()

        return when (error.code()) {
            400 -> serverMessage ?: "Complétez votre profil et validez au moins une compétence avant de démarrer la préparation."
            401 -> serverMessage ?: "Session expirée, veuillez vous reconnecter."
            403 -> serverMessage ?: "La préparation aux entretiens est réservée aux candidats."
            404 -> serverMessage ?: "Session introuvable."
            409 -> serverMessage ?: "Cette session est déjà terminée."
            502, 503 -> serverMessage ?: "Le service IA est momentanément indisponible. Votre progression enregistrée est conservée."
            else -> serverMessage ?: error.toUserMessage()
        }
    }
}
