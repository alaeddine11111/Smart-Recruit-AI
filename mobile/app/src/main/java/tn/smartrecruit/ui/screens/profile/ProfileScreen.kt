package tn.smartrecruit.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.data.model.Role
import tn.smartrecruit.data.remote.dto.ProfileDto
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.AppTextField
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.components.PrimaryButton
import tn.smartrecruit.ui.screens.auth.SessionViewModel
import tn.smartrecruit.ui.theme.TextMuted

/** Profile tab, shared by candidates and recruiters (fields depend on the role). */
@Composable
fun ProfileScreen(onLoggedOut: () -> Unit) {
    val vm: ProfileViewModel = viewModel(factory = appViewModelFactory())
    val session: SessionViewModel = viewModel(factory = appViewModelFactory())
    val state by vm.state.collectAsStateWithLifecycle()
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }

    val profile = state.profile
    when {
        state.loading && profile == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        profile == null -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ErrorText(state.error ?: "Profil indisponible.")
            OutlinedButton(onClick = vm::load, modifier = Modifier.padding(top = 12.dp)) { Text("Réessayer") }
            TextButton(onClick = { session.logout(onLoggedOut) }) { Text("Déconnexion") }
        }

        else -> ProfileContent(
            profile = profile,
            state = state,
            onSave = vm::save,
            onChangePassword = { showPasswordDialog = true },
            onLogout = { session.logout(onLoggedOut) }
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            saving = state.passwordSaving,
            serverError = state.passwordError,
            onDismiss = {
                vm.clearPasswordError()
                showPasswordDialog = false
            },
            onSubmit = { old, new ->
                vm.changePassword(old, new) { showPasswordDialog = false }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    profile: ProfileDto,
    state: ProfileUiState,
    onSave: (name: String, phone: String, location: String, position: String) -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    val role = Role.from(profile.role)
    var name by rememberSaveable(profile.id) { mutableStateOf(profile.fullName.orEmpty()) }
    var phone by rememberSaveable(profile.id) { mutableStateOf(profile.telephone.orEmpty()) }
    var location by rememberSaveable(profile.id) { mutableStateOf(profile.localisation.orEmpty()) }
    var position by rememberSaveable(profile.id) { mutableStateOf(profile.poste.orEmpty()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // header: avatar + name + role
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.fullName?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(profile.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (role == Role.RECRUTEUR) "Recruteur" else "Candidat",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        InfoRow("Email", profile.email.orEmpty())

        AppTextField(name, { name = it }, "Nom complet")
        if (role == Role.RECRUTEUR) {
            AppTextField(position, { position = it }, "Votre poste")
        } else {
            AppTextField(phone, { phone = it }, "Téléphone", keyboardType = KeyboardType.Phone)
            AppTextField(location, { location = it }, "Localisation")
        }

        if (role == Role.RECRUTEUR) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Entreprise", style = MaterialTheme.typography.titleMedium)
                    InfoRow("Nom", profile.entrepriseNom.orEmpty())
                    InfoRow("Site web", profile.entrepriseSiteWeb.orEmpty())
                    InfoRow("Présentation", profile.entrepriseDescription.orEmpty())
                }
            }
        }

        ErrorText(state.error)
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        PrimaryButton("Enregistrer", onClick = { onSave(name, phone, location, position) }, loading = state.saving)
        OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Text("Changer le mot de passe")
        }
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Déconnexion") }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(value)
    }
}

@Composable
private fun PasswordDialog(
    saving: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onSubmit: (old: String, new: String) -> Unit
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Changer le mot de passe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(old, { old = it }, "Mot de passe actuel", isPassword = true)
                AppTextField(new, { new = it }, "Nouveau mot de passe", isPassword = true)
                AppTextField(confirm, { confirm = it }, "Confirmer le nouveau mot de passe", isPassword = true)
                ErrorText(localError ?: serverError)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    localError = when {
                        old.isBlank() || new.isBlank() -> "Remplissez tous les champs."
                        new.length < 8 -> "Au moins 8 caractères."
                        new != confirm -> "Les mots de passe ne correspondent pas."
                        else -> null
                    }
                    if (localError == null) onSubmit(old, new)
                }
            ) { Text(if (saving) "..." else "Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
