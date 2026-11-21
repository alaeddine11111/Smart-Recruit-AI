package tn.smartrecruit.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import tn.smartrecruit.data.remote.CvApi
import tn.smartrecruit.data.remote.dto.AnalysisDto
import tn.smartrecruit.data.remote.dto.CandidateProfileDto
import tn.smartrecruit.data.remote.dto.CvInfoDto
import tn.smartrecruit.data.remote.toUserMessage

private const val MAX_SIZE = 5 * 1024 * 1024 // 5 MB, same limit as the backend

/** CV upload, analysis status and extracted profile (cv-service). */
class CvRepository(
    private val api: CvApi,
    private val resolver: ContentResolver
) {
    /** The current CV, or success(null) if the candidate has none yet (404). */
    suspend fun currentCv(): Result<CvInfoDto?> = try {
        Result.success(api.currentCv())
    } catch (e: HttpException) {
        if (e.code() == 404) Result.success(null) else Result.failure(Exception(e.toUserMessage()))
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun upload(uri: Uri): Result<CvInfoDto> = try {
        val (name, bytes) = withContext(Dispatchers.IO) { readPdf(uri) }
        val body = bytes.toRequestBody("application/pdf".toMediaType())
        val part = MultipartBody.Part.createFormData("file", name, body)
        Result.success(api.uploadCv(part))
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    /** The analysis of the last CV, or success(null) if there is none (404). */
    suspend fun analysis(): Result<AnalysisDto?> = try {
        Result.success(api.analysis())
    } catch (e: HttpException) {
        if (e.code() == 404) Result.success(null) else Result.failure(Exception(e.toUserMessage()))
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun profile(): Result<CandidateProfileDto> = try {
        Result.success(api.profile())
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun saveProfile(profile: CandidateProfileDto): Result<CandidateProfileDto> = try {
        Result.success(api.saveProfile(profile))
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun validateProfile(): Result<CandidateProfileDto> = try {
        Result.success(api.validateProfile())
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    /** Reads the picked file and checks it. Throws IllegalArgumentException with a readable message. */
    private fun readPdf(uri: Uri): Pair<String, ByteArray> {
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "cv.pdf"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Impossible de lire ce fichier.")
        require(bytes.isNotEmpty() && bytes.size <= MAX_SIZE) { "Le fichier est vide ou dépasse 5 Mo." }
        require(String(bytes.copyOfRange(0, 4), Charsets.ISO_8859_1) == "%PDF") { "Le CV doit être un fichier PDF." }
        return name to bytes
    }
}