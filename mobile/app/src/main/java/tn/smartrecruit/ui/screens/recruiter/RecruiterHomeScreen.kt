package tn.smartrecruit.ui.screens.recruiter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.screens.auth.SessionViewModel
import tn.smartrecruit.ui.screens.profile.ProfileScreen
import tn.smartrecruit.ui.theme.TextMuted
import tn.smartrecruit.ui.theme.TextPrimary

/** Tabs in the recruiter bottom bar. */
enum class RecruiterTab(val route: String, val label: String, val icon: ImageVector) {
    OFFERS("offers", "Offres", Icons.Outlined.Work),
    COMPANY("company", "Entreprise", Icons.Outlined.Business),
    APPLICATIONS("applications", "Candidatures", Icons.Outlined.Group),
    PROFILE("profile", "Profil", Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecruiterHomeScreen(
    onLoggedOut: () -> Unit,
    darkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {}
) {
    val session: SessionViewModel = viewModel(factory = appViewModelFactory())
    val inner = rememberNavController()
    val backStack by inner.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var menuOpen by remember { mutableStateOf(false) }

    fun goTo(tab: RecruiterTab) = inner.navigate(tab.route) {
        popUpTo(inner.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SmartRecruit AI") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (darkMode) "Activer le mode clair" else "Activer le mode sombre"
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Mon entreprise") },
                            onClick = { menuOpen = false; goTo(RecruiterTab.COMPANY) }
                        )
                        DropdownMenuItem(
                            text = { Text("Mon profil") },
                            onClick = { menuOpen = false; goTo(RecruiterTab.PROFILE) }
                        )
                        DropdownMenuItem(
                            text = { Text("Déconnexion") },
                            onClick = {
                                menuOpen = false
                                session.logout(onLoggedOut)
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                RecruiterTab.entries.forEach { tab ->
                    val isSelected = currentRoute == tab.route ||
                            (tab == RecruiterTab.OFFERS && currentRoute?.startsWith("offer_form") == true)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { goTo(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = inner,
            startDestination = RecruiterTab.OFFERS.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(RecruiterTab.OFFERS.route) {
                OfferListScreen(
                    onNew = { inner.navigate("offer_form") },
                    onEdit = { offerId -> inner.navigate("offer_form/$offerId") }
                )
            }
            composable("offer_form") {
                OfferFormScreen(
                    offerId = null,
                    onDone = { inner.popBackStack() },
                    onNeedCompany = { goTo(RecruiterTab.COMPANY) }
                )
            }
            composable(
                route = "offer_form/{offerId}",
                arguments = listOf(navArgument("offerId") { type = NavType.StringType })
            ) { entry ->
                val offerId = entry.arguments?.getString("offerId")
                OfferFormScreen(
                    offerId = offerId,
                    onDone = { inner.popBackStack() },
                    onNeedCompany = { goTo(RecruiterTab.COMPANY) }
                )
            }
            composable(RecruiterTab.COMPANY.route) {
                CompanyScreen()
            }
            composable(RecruiterTab.APPLICATIONS.route) {
                Placeholder(
                    title = "Candidatures reçues",
                    subtitle = "Consultez les candidatures soumises à vos offres d'emploi (étape suivante)."
                )
            }
            composable(RecruiterTab.PROFILE.route) {
                ProfileScreen(onLoggedOut = onLoggedOut)
            }
        }
    }
}

@Composable
private fun Placeholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(subtitle, color = TextMuted)
    }
}
