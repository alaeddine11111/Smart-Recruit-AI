package tn.smartrecruit.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import tn.smartrecruit.data.remote.dto.AnalysisDto
import tn.smartrecruit.data.remote.dto.CandidateProfileDto
import tn.smartrecruit.data.remote.dto.CvInfoDto
import tn.smartrecruit.data.remote.dto.MatchingOfferDto
import tn.smartrecruit.data.remote.dto.MatchingResultDto
import tn.smartrecruit.data.remote.dto.PreparationSessionDto
import tn.smartrecruit.data.remote.dto.StartPreparationRequestDto
import tn.smartrecruit.data.remote.dto.SubmitAnswerRequestDto

/** Retrofit interface describing cv-service (port 8082). */
/**Retrofit is an Android library that allows your application to communicate with a backend through HTTP requests.*/
interface CvApi {
    @GET("api/cv")
    suspend fun currentCv(): CvInfoDto            // 404 if the candidate has no CV yet

    @Multipart
    @POST("api/cv")
    suspend fun uploadCv(@Part file: MultipartBody.Part): CvInfoDto

    @GET("api/cv/analyse")
    suspend fun analysis(): AnalysisDto           // 404 if no analysis yet

    @GET("api/profil")
    suspend fun profile(): CandidateProfileDto

    @PUT("api/profil")
    suspend fun saveProfile(@Body profile: CandidateProfileDto): CandidateProfileDto

    @POST("api/profil/valider")
    suspend fun validateProfile(): CandidateProfileDto

    @POST("api/matching")
    suspend fun evaluateMatching(
        @Body offers: List<MatchingOfferDto>
    ): List<MatchingResultDto>

    @GET("api/preparations")
    suspend fun preparationHistory(): List<PreparationSessionDto>

    @GET("api/preparations/{id}")
    suspend fun preparation(@Path("id") id: String): PreparationSessionDto

    @POST("api/preparations")
    suspend fun startPreparation(@Body request: StartPreparationRequestDto): PreparationSessionDto

    @PUT("api/preparations/{id}/reponses")
    suspend fun submitPreparationAnswer(
        @Path("id") id: String,
        @Body request: SubmitAnswerRequestDto
    ): PreparationSessionDto

    @POST("api/preparations/{id}/terminer")
    suspend fun finishPreparation(@Path("id") id: String): PreparationSessionDto
}