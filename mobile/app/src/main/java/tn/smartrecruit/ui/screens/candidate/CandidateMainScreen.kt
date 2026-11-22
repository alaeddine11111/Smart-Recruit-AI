package tn.smartrecruit.ui.screens.candidate

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import tn.smartrecruit.ui.screens.cv.CvScreen
import tn.smartrecruit.ui.screens.profile.ProfileScreen
import tn.smartrecruit.ui.theme.TextMuted

/** The 5 bottom tabs of the candidate app (enum with properties). */
enum class CandidateTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    JOBS("jobs", "Jobs", Icons.Outlined.Work),
    APPLICATIONS("applications", "Applications", Icons.Outlined.Description),
    INTERVIEW("interview", "Interview", Icons.Outlined.Mic),
    PROFILE("profile", "Profile", Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidateMainScreen(onLoggedOut: () -> Unit) {
    val session: SessionViewModel = viewModel(factory = appViewModelFactory())
    val userName by session.userName.collectAsStateWithLifecycle()
    val jobsViewModel: CandidateJobsViewModel = viewModel(factory = appViewModelFactory())
    val jobsState by jobsViewModel.state.collectAsStateWithLifecycle()

    val inner = rememberNavController()
    val backStack by inner.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var menuOpen by remember { mutableStateOf(false) }

    fun goTo(tab: CandidateTab) = inner.navigate(tab.route) {
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
                    IconButton(onClick = { /* TODO notifications */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Mon CV") }, onClick = { menuOpen = false; inner.navigate("cv") })
                        DropdownMenuItem(text = { Text("Mon profil") }, onClick = { menuOpen = false; goTo(CandidateTab.PROFILE) })
                        DropdownMenuItem(text = { Text("Déconnexion") }, onClick = {
                            menuOpen = false
                            session.logout(onLoggedOut)
                        })
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                CandidateTab.entries.forEach { tab ->
                    val isSelected = currentRoute == tab.route ||
                            (tab == CandidateTab.JOBS && currentRoute?.startsWith("job_detail") == true)
                    val labelText = if (tab == CandidateTab.JOBS && jobsState.jobSource != JobSource.ALL) {
                        "Jobs (${if (jobsState.jobSource == JobSource.SITE) "Site" else "API"})"
                    } else tab.label

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { goTo(tab) },
                        icon = {
                            if (tab == CandidateTab.JOBS) {
                                Box(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { goTo(tab) },
                                            onLongPress = {
                                                goTo(tab)
                                                jobsViewModel.toggleJobSource()
                                            }
                                        )
                                    }
                                ) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(labelText) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(inner, startDestination = CandidateTab.HOME.route, modifier = Modifier.padding(padding)) {
            composable(CandidateTab.HOME.route) { Placeholder("Hello, ${userName ?: ""}", "Profile progress, recommended jobs and recent application come next.") }
            composable(CandidateTab.JOBS.route) {
                CandidateJobsScreen(
                    onOfferClick = { offerId -> inner.navigate("job_detail/$offerId") },
                    vm = jobsViewModel
                )
            }
            composable(
                route = "job_detail/{offerId}",
                arguments = listOf(navArgument("offerId") { type = NavType.StringType })
            ) { entry ->
                val offerId = entry.arguments?.getString("offerId") ?: ""
                JobDetailScreen(
                    offerId = offerId,
                    onBack = { inner.popBackStack() }
                )
            }
            composable(CandidateTab.APPLICATIONS.route) { Placeholder("Applications", "My applications and their status (next step).") }
            composable(CandidateTab.INTERVIEW.route) { Placeholder("Interview", "AI interview preparation (later).") }
            composable(CandidateTab.PROFILE.route) { ProfileScreen(onLoggedOut = onLoggedOut) }
            composable("cv") { CvScreen() }
        }
    }
}

@Composable
private fun Placeholder(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, color = TextMuted)
    }
}
