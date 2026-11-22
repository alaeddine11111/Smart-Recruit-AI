package tn.smartrecruit.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import tn.smartrecruit.data.remote.dto.EntrepriseDto
import tn.smartrecruit.data.remote.dto.ExternalOffreDto
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.remote.dto.OffreRequestDto
import tn.smartrecruit.data.remote.dto.PageDto

/**
 * Recruiter & Candidate offer endpoints (port 8084, ApiConfig.OFFERS_URL).
 * Paths have no leading slash because the base URL ends with "/".
 */
interface OfferApi {

    // --- recruiter company ---
    @GET("api/recruteur/entreprise")
    suspend fun getMyCompany(): EntrepriseDto

    @PUT("api/recruteur/entreprise")
    suspend fun saveMyCompany(@Body body: EntrepriseDto): EntrepriseDto

    // --- recruiter offers ---
    @GET("api/recruteur/offres")
    suspend fun getMyOffers(
        @Query("statut") statut: String? = null,
        @Query("titre") titre: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PageDto<OffreDto>

    @GET("api/recruteur/offres/{id}")
    suspend fun getMyOffer(@Path("id") id: String): OffreDto

    @POST("api/recruteur/offres")
    suspend fun createOffer(@Body body: OffreRequestDto): OffreDto

    @PUT("api/recruteur/offres/{id}")
    suspend fun updateOffer(@Path("id") id: String, @Body body: OffreRequestDto): OffreDto

    @PATCH("api/recruteur/offres/{id}/statut")
    suspend fun changeStatus(@Path("id") id: String, @Query("value") value: String): OffreDto

    @DELETE("api/recruteur/offres/{id}")
    suspend fun deleteOffer(@Path("id") id: String): Response<Unit>

    // --- public / candidate site offers ---
    @GET("api/offres")
    suspend fun getPublicOffers(
        @Query("titre") titre: String? = null,
        @Query("typeContrat") typeContrat: String? = null,
        @Query("localisation") localisation: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PageDto<OffreDto>

    @GET("api/offres/{id}")
    suspend fun getPublicOffer(@Path("id") id: String): OffreDto

    // --- external API offers ---
    @GET("api/offres-externes")
    suspend fun getExternalOffers(
        @Query("q") q: String = "",
        @Query("location") location: String = "",
        @Query("page") page: Int = 1
    ): List<ExternalOffreDto>
}
