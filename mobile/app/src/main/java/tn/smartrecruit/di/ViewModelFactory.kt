package tn.smartrecruit.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tn.smartrecruit.SmartRecruitApp
import tn.smartrecruit.ui.screens.auth.LoginViewModel
import tn.smartrecruit.ui.screens.auth.RegisterViewModel
import tn.smartrecruit.ui.screens.auth.SessionViewModel
import tn.smartrecruit.ui.screens.candidate.CandidateJobsViewModel
import tn.smartrecruit.ui.screens.candidate.JobDetailViewModel
import tn.smartrecruit.ui.screens.cv.CvViewModel
import tn.smartrecruit.ui.screens.profile.ProfileViewModel
import tn.smartrecruit.ui.screens.recruiter.CompanyViewModel
import tn.smartrecruit.ui.screens.recruiter.OfferFormViewModel
import tn.smartrecruit.ui.screens.recruiter.OfferListViewModel
import tn.smartrecruit.ui.screens.candidate.RecommendationsViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(container.authRepository)
        modelClass.isAssignableFrom(RegisterViewModel::class.java) -> RegisterViewModel(container.authRepository)
        modelClass.isAssignableFrom(SessionViewModel::class.java) -> SessionViewModel(container.authRepository)
        modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(container.profileRepository)
        modelClass.isAssignableFrom(CvViewModel::class.java) -> CvViewModel(container.cvRepository)
        modelClass.isAssignableFrom(CompanyViewModel::class.java) -> CompanyViewModel(container.offerRepository)
        modelClass.isAssignableFrom(OfferListViewModel::class.java) -> OfferListViewModel(container.offerRepository)
        modelClass.isAssignableFrom(OfferFormViewModel::class.java) -> OfferFormViewModel(container.offerRepository)
        modelClass.isAssignableFrom(CandidateJobsViewModel::class.java) -> CandidateJobsViewModel(container.offerRepository)
        modelClass.isAssignableFrom(JobDetailViewModel::class.java) -> JobDetailViewModel(container.offerRepository)
        modelClass.isAssignableFrom(RecommendationsViewModel::class.java) -> RecommendationsViewModel(container.matchingRepository)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}

/** Use in screens:  val vm: LoginViewModel = viewModel(factory = appViewModelFactory()) */
@Composable
fun appViewModelFactory(): ViewModelFactory {
    val context: Context = LocalContext.current.applicationContext
    return remember { ViewModelFactory((context as SmartRecruitApp).container) }
}
