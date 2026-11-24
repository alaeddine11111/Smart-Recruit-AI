package tn.smartrecruit.ui.screens.recruiter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.theme.TextMuted
import tn.smartrecruit.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferListScreen(
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: OfferListViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var toDelete by remember { mutableStateOf<OffreDto?>(null) }

    // Reload each time the screen appears (first time, and after coming back from the form)
    LaunchedEffect(Unit) { vm.reload() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    // Load the next page when the last items become visible
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Mes offres",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.statut == null,
                        onClick = { vm.setFilter(null) },
                        label = { Text("Toutes") }
                    )
                }
                items(OFFER_STATUSES) { s ->
                    FilterChip(
                        selected = state.statut == s,
                        onClick = { vm.setFilter(s) },
                        label = { Text(statusLabel(s)) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { vm.reload(pull = true) },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    state.error != null && state.offers.isEmpty() -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ErrorText(state.error)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { vm.reload() }) { Text("Réessayer") }
                    }

                    state.offers.isEmpty() -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (state.statut == null) "Vous n'avez pas encore d'offre. Appuyez sur « Nouvelle offre »."
                            else "Aucune offre avec ce statut.",
                            color = TextMuted
                        )
                    }

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.offers, key = { it.id }) { offer ->
                            OfferCard(
                                offer = offer,
                                busy = state.busyId == offer.id,
                                onEdit = { onEdit(offer.id) },
                                onToggle = {
                                    vm.changeStatus(offer, if (offer.statut == "PUBLISHED") "CLOSED" else "PUBLISHED")
                                },
                                onDelete = { toDelete = offer }
                            )
                        }
                        if (state.loadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nouvelle offre") }
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    toDelete?.let { offer ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer l'offre ?") },
            text = { Text("« ${offer.titre} » sera supprimée définitivement.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(offer); toDelete = null }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun OfferCard(
    offer: OffreDto,
    busy: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(offer.titre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusBadge(offer.statut)
            }
            Text("${offer.localisation} · ${offer.typeContrat}", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            formatSalary(offer.salaireMin, offer.salaireMax, offer.devise)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (offer.competences.isNotEmpty()) {
                Text(
                    offer.competences.take(5).joinToString(" · "),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            shortDate(offer.dateCreation)?.let {
                Text("Créée le $it", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onToggle) {
                        Text(
                            when (offer.statut) {
                                "PUBLISHED" -> "Clôturer"
                                "CLOSED" -> "Rouvrir"
                                else -> "Publier"
                            }
                        )
                    }
                    TextButton(onClick = onEdit) { Text("Modifier") }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Supprimer") }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "PUBLISHED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "CLOSED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            statusLabel(status),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
