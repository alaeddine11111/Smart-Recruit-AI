package tn.smartrecruit.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import tn.smartrecruit.data.remote.dto.AnalysisDto
import tn.smartrecruit.data.remote.dto.CandidateProfileDto
import tn.smartrecruit.data.remote.dto.CvInfoDto
import tn.smartrecruit.data.remote.dto.MatchingOfferDto
import tn.smartrecruit.data.remote.dto.MatchingResultDto

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
}