package tn.smartrecruit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tn.smartrecruit.data.model.Role
import tn.smartrecruit.ui.screens.auth.ChooseRoleScreen
import tn.smartrecruit.ui.screens.auth.LoginScreen
import tn.smartrecruit.ui.screens.auth.PendingApprovalScreen
import tn.smartrecruit.ui.screens.auth.RegisterScreen
import tn.smartrecruit.ui.screens.auth.SplashScreen
import tn.smartrecruit.ui.screens.auth.WelcomeScreen
import tn.smartrecruit.ui.screens.candidate.CandidateMainScreen
import tn.smartrecruit.ui.screens.recruiter.RecruiterHomeScreen

private fun homeFor(role: Role) = if (role == Role.RECRUTEUR) Routes.RECRUITER_HOME else Routes.CANDIDATE_HOME

private fun NavHostController.clearAndGo(route: String) =
    navigate(route) { popUpTo(graph.id) { inclusive = true } }

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(onFinished = { role ->
                nav.clearAndGo(if (role == Role.CANDIDAT || role == Role.RECRUTEUR) homeFor(role) else Routes.WELCOME)
            })
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreateAccount = { nav.navigate(Routes.CHOOSE_ROLE) },
                onLogin = { nav.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.CHOOSE_ROLE) {
            ChooseRoleScreen(
                onContinue = { role -> nav.navigate(Routes.register(role)) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { role -> nav.clearAndGo(homeFor(role)) },
                onCreateAccount = { nav.navigate(Routes.CHOOSE_ROLE) }
            )
        }

        composable(
            route = Routes.REGISTER,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { entry ->
            RegisterScreen(
                role = entry.arguments?.getString("role") ?: "candidate",
                onCandidateRegistered = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) }
                },
                onRecruiterRequested = {
                    nav.navigate(Routes.PENDING) { popUpTo(Routes.WELCOME) }
                },
                onLogin = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) } }
            )
        }

        composable(Routes.PENDING) {
            PendingApprovalScreen(onBackToLogin = {
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) }
            })
        }

        composable(Routes.CANDIDATE_HOME) {
            CandidateMainScreen(onLoggedOut = { nav.clearAndGo(Routes.WELCOME) })
        }

        composable(Routes.RECRUITER_HOME) {
            RecruiterHomeScreen(onLoggedOut = { nav.clearAndGo(Routes.WELCOME) })
        }
    }
}
