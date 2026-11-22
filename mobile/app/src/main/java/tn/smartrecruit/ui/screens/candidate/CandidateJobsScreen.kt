package tn.smartrecruit.ui.screens.candidate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.screens.recruiter.CONTRACT_TYPES
import tn.smartrecruit.ui.screens.recruiter.formatSalary
import tn.smartrecruit.ui.screens.recruiter.shortDate
import tn.smartrecruit.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CandidateJobsScreen(
    onOfferClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: CandidateJobsViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Offres d'emploi",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                // Source badge / info
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = vm::toggleJobSource
                ) {
                    Text(
                        state.jobSource.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Search filters
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = vm::onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Poste, mots-clés…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.locationQuery,
                        onValueChange = vm::onLocationQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ville, région…") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true
                    )
                }

                // Source Chips + Contract Type Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.jobSource == JobSource.ALL,
                            onClick = { vm.setJobSource(JobSource.ALL) },
                            label = { Text("Toutes sources") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.jobSource == JobSource.SITE,
                            onClick = { vm.setJobSource(JobSource.SITE) },
                            label = { Text("Sur le site") },
                            leadingIcon = { Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.jobSource == JobSource.API,
                            onClick = { vm.setJobSource(JobSource.API) },
                            label = { Text("API Externe") },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.selectedContractType == null,
                            onClick = { vm.onContractTypeSelect(null) },
                            label = { Text("Tous les contrats") }
                        )
                    }
                    items(CONTRACT_TYPES) { type ->
                        FilterChip(
                            selected = state.selectedContractType == type,
                            onClick = { vm.onContractTypeSelect(type) },
                            label = { Text(type) }
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { vm.loadOffers(pull = true) },
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
                        OutlinedButton(onClick = { vm.loadOffers() }) { Text("Réessayer") }
                    }

                    state.offers.isEmpty() -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Aucune offre disponible pour (${state.jobSource.label}).",
                            color = TextMuted
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = vm::toggleJobSource) { Text("Basculer la source des offres") }
                    }

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.offers, key = { it.id }) { offer ->
                            CandidateJobCard(
                                offer = offer,
                                onClick = { onOfferClick(offer.id) }
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
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CandidateJobCard(
    offer: OffreDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    offer.titre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ContractBadge(offer.typeContrat)
            }

            if (!offer.entrepriseNom.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Text(offer.entrepriseNom, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Text(offer.localisation, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            formatSalary(offer.salaireMin, offer.salaireMax, offer.devise)?.let { salary ->
                Text(salary, style = MaterialTheme.typography.bodySmall)
            }

            if (offer.competences.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    offer.competences.take(4).forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                skill,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                shortDate(offer.datePublication ?: offer.dateCreation)?.let { date ->
                    Text("Publiée le $date", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } ?: Spacer(Modifier.weight(1f))

                Button(onClick = onClick) {
                    Text("Voir l'offre")
                }
            }
        }
    }
}

@Composable
private fun ContractBadge(contractType: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            contractType,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
