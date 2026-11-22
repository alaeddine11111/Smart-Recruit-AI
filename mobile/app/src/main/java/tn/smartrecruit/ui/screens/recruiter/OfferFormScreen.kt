package tn.smartrecruit.ui.screens.recruiter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.AppTextField
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.components.PrimaryButton
import tn.smartrecruit.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OfferFormScreen(
    offerId: String?,                 // null = new offer
    onDone: () -> Unit,               // after saving or cancelling
    onNeedCompany: () -> Unit,        // the recruiter has no company yet
    modifier: Modifier = Modifier,
    vm: OfferFormViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(offerId) { vm.start(offerId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (offerId == null) "Nouvelle offre" else "Modifier l'offre",
            style = MaterialTheme.typography.headlineSmall
        )

        AppTextField(state.titre, { v -> vm.edit { copy(titre = v) } }, "Titre du poste *")
        ErrorText(state.errors["titre"])

        AppTextField(
            state.description, { v -> vm.edit { copy(description = v) } }, "Description *",
            singleLine = false, minLines = 5
        )
        ErrorText(state.errors["description"])

        AppTextField(
            state.localisation, { v -> vm.edit { copy(localisation = v) } }, "Localisation *",
            placeholder = "Tunis, Bizerte, Télétravail…"
        )
        ErrorText(state.errors["localisation"])

        ContractDropdown(state.typeContrat) { v -> vm.edit { copy(typeContrat = v) } }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                state.salaireMin, { v -> vm.edit { copy(salaireMin = v) } }, "Salaire min (TND)",
                modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal
            )
            AppTextField(
                state.salaireMax, { v -> vm.edit { copy(salaireMax = v) } }, "Salaire max (TND)",
                modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal
            )
        }
        ErrorText(state.errors["salaireMin"])
        ErrorText(state.errors["salaireMax"])

        // --- skills as chips ---
        Text("Compétences requises *", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                state.competenceInput, { v -> vm.edit { copy(competenceInput = v) } }, "Compétence",
                modifier = Modifier.weight(1f), placeholder = "java, spring boot…"
            )
            FilledTonalIconButton(onClick = vm::addCompetence) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter la compétence")
            }
        }
        if (state.competences.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.competences.forEach { skill ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeCompetence(skill) },
                        label = { Text(skill) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Retirer $skill", modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
        ErrorText(state.errors["competences"])

        // --- status ---
        Text("Statut", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OFFER_STATUSES.forEach { s ->
                FilterChip(
                    selected = state.statut == s,
                    onClick = { vm.edit { copy(statut = s) } },
                    label = { Text(statusLabel(s)) }
                )
            }
        }
        Text("Seules les offres « Publiée » sont visibles par les candidats.", color = TextMuted, style = MaterialTheme.typography.bodySmall)

        if (state.noCompany) {
            ErrorText(state.error)
            OutlinedButton(onClick = onNeedCompany, modifier = Modifier.fillMaxWidth()) {
                Text("Créer mon entreprise")
            }
        } else {
            ErrorText(state.error)
        }

        PrimaryButton(
            text = if (offerId == null) "Créer l'offre" else "Enregistrer",
            onClick = vm::save,
            loading = state.saving,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Annuler") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type de contrat *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CONTRACT_TYPES.forEach { type ->
                DropdownMenuItem(text = { Text(type) }, onClick = { onSelect(type); expanded = false })
            }
        }
    }
}
