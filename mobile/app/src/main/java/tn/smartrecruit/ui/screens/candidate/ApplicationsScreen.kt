package tn.smartrecruit.ui.screens.candidate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.data.remote.dto.CandidatureDto
import tn.smartrecruit.data.remote.dto.CandidatureExterneDto
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.screens.recruiter.shortDate
import tn.smartrecruit.ui.theme.TextMuted
import tn.smartrecruit.ui.theme.TextPrimary

/** Candidate Applications tab: the offers the candidate applied to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    onOfferClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: ApplicationsViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Mes candidatures",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    "Retrouvez les offres auxquelles vous avez postulé et le suivi de vos candidatures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            if (state.loading && state.applications.isEmpty() && state.externalApplications.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                state.error?.let { message ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ErrorText(message)
                            OutlinedButton(onClick = vm::refresh) { Text("Réessayer") }
                        }
                    }
                }

                item {
                    Text(
                        "Candidatures envoyées",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                if (state.applications.isEmpty()) {
                    item {
                        Text(
                            "Vous n'avez pas encore postulé à d'offres.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                } else {
                    items(state.applications, key = { it.id }) { application ->
                        ApplicationCard(application = application, onClick = onOfferClick)
                    }
                }

                if (state.externalApplications.isNotEmpty()) {
                    item {
                        Text(
                            "Offres externes consultées",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    items(state.externalApplications, key = { it.id }) { application ->
                        ExternalApplicationCard(application = application)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(application: CandidatureDto, onClick: (String) -> Unit) {
    val offer = application.offre
    Card(
        onClick = { offer?.let { onClick(application.offreId) } },
        enabled = offer != null,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    offer?.titre ?: "Offre indisponible",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                ApplicationStatusBadge(application.statut)
            }
            val meta = listOfNotNull(
                offer?.entrepriseNom?.ifBlank { null },
                offer?.localisation?.ifBlank { null }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(
                "Candidature du ${shortDate(application.dateCandidature) ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun ExternalApplicationCard(application: CandidatureExterneDto) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    application.titre,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                ApplicationStatusBadge(application.statut)
            }
            val meta = listOfNotNull(
                application.entreprise?.ifBlank { null },
                application.source?.ifBlank { null }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(
                "Lien ouvert le ${shortDate(application.dateClic) ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(application.urlExterne)))
                }
            }) {
                Text("Ouvrir l'annonce")
            }
        }
    }
}

@Composable
private fun ApplicationStatusBadge(status: String?) {
    val label = when (status) {
        "ACCEPTED" -> "Acceptée"
        "REJECTED" -> "Refusée"
        "PENDING" -> "En attente"
        "REDIRECTED" -> "Lien ouvert"
        else -> "Envoyée"
    }
    val background = when (status) {
        "ACCEPTED" -> MaterialTheme.colorScheme.primary
        "REJECTED" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val foreground = when (status) {
        "ACCEPTED" -> MaterialTheme.colorScheme.onPrimary
        "REJECTED" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
