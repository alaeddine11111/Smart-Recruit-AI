package tn.smartrecruit.data.repository

import tn.smartrecruit.data.local.TokenStore
import tn.smartrecruit.data.remote.AuthApi
import tn.smartrecruit.data.remote.dto.ChangePasswordRequest
import tn.smartrecruit.data.remote.dto.ProfileDto
import tn.smartrecruit.data.remote.dto.UpdateProfileRequest
import tn.smartrecruit.data.remote.toUserMessage

/** User profile (account information held by auth-service). */
class ProfileRepository(
    private val api: AuthApi,
    private val store: TokenStore
) {
    suspend fun load(): Result<ProfileDto> = try {
        Result.success(api.getProfile())
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun update(request: UpdateProfileRequest): Result<ProfileDto> = try {
        val profile = api.updateProfile(request)
        profile.fullName?.let { store.saveName(it) }
        Result.success(profile)
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = try {
        api.changePassword(ChangePasswordRequest(oldPassword, newPassword))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toUserMessage()))
    }
}
