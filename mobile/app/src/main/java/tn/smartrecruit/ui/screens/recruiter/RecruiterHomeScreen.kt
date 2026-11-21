package tn.smartrecruit.ui.screens.recruiter

import androidx.compose.runtime.Composable
import tn.smartrecruit.ui.screens.profile.ProfileScreen

/**
 * Recruiter space. For now it shows the profile (account + company).
 * The offers and applications screens belong to the teammates and will be added around it
 * (a bottom bar like CandidateMainScreen).
 */
@Composable
fun RecruiterHomeScreen(onLoggedOut: () -> Unit) {
    ProfileScreen(onLoggedOut = onLoggedOut)
}
