package tn.smartrecruit.data.remote.dto

import com.google.gson.annotations.SerializedName

/** AI feedback for one answer (cv-service /api/preparations). */
data class AnswerEvaluationDto(
    @SerializedName("score") val score: Int = 0,
    @SerializedName("pertinence") val pertinence: String? = null,
    @SerializedName("exactitude") val exactitude: String? = null,
    @SerializedName("pointsManquants") val pointsManquants: List<String> = emptyList(),
    @SerializedName("retour") val retour: String? = null
)

/** One generated question, with the candidate's answer and its evaluation when available. */
data class InterviewQuestionDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("question") val question: String,
    @SerializedName("reponse") val reponse: String? = null,
    @SerializedName("evaluation") val evaluation: AnswerEvaluationDto? = null
)

/** Final AI review of a finished preparation session. */
data class FinalEvaluationDto(
    @SerializedName("forces") val forces: List<String> = emptyList(),
    @SerializedName("faiblesses") val faiblesses: List<String> = emptyList(),
    @SerializedName("axesAmelioration") val axesAmelioration: List<String> = emptyList(),
    @SerializedName("conseils") val conseils: String? = null
)

/** A preparation session as returned by cv-service. */
data class PreparationSessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("candidatId") val candidatId: String? = null,
    @SerializedName("offreId") val offreId: String? = null,
    @SerializedName("offreTitre") val offreTitre: String,
    @SerializedName("dateCreation") val dateCreation: String? = null,
    @SerializedName("dateFin") val dateFin: String? = null,
    @SerializedName("questions") val questions: List<InterviewQuestionDto> = emptyList(),
    @SerializedName("scoreGlobal") val scoreGlobal: Int? = null,
    @SerializedName("evaluationFinale") val evaluationFinale: FinalEvaluationDto? = null,
    @SerializedName("statut") val statut: String
)

/** Body of POST /api/preparations (null means a general session). */
data class StartPreparationRequestDto(
    @SerializedName("offreId") val offreId: String?
)

/** Body of PUT /api/preparations/{id}/reponses. */
data class SubmitAnswerRequestDto(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("reponse") val reponse: String
)
