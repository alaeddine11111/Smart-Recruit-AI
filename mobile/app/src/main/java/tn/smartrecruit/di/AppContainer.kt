package tn.smartrecruit.di

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import tn.smartrecruit.data.local.TokenStore
import tn.smartrecruit.data.remote.ApiClient
import tn.smartrecruit.data.repository.AuthRepository
import tn.smartrecruit.data.repository.CvRepository
import tn.smartrecruit.data.repository.OfferRepository
import tn.smartrecruit.data.repository.ProfileRepository

/** Manual dependency injection (course: "Injection manuelle : un conteneur d'application"). */
class AppContainer(context: Context) {
    val tokenStore = TokenStore(context)

    private val apiClient = ApiClient { runBlocking { tokenStore.token.first() } }

    val authRepository: AuthRepository by lazy {
        AuthRepository(apiClient.authApi, apiClient.keycloakApi, tokenStore)
    }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(apiClient.authApi, tokenStore) }


    val cvRepository: CvRepository by lazy {
        CvRepository(apiClient.cvApi, context.applicationContext.contentResolver)
    }

    val offerRepository by lazy { OfferRepository(apiClient.offerApi) }
}
