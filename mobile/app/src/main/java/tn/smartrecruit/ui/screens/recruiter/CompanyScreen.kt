package tn.smartrecruit.ui.screens.recruiter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
fun CompanyScreen(
    modifier: Modifier = Modifier,
    vm: CompanyViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Box(modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            val f = state.form
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (state.exists) "Mon entreprise" else "Créer mon entreprise",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (!state.exists) {
                    Text(
                        "Créez le profil de votre entreprise pour pouvoir publier des offres.",
                        color = TextMuted
                    )
                }

                AppTextField(f.nom, { vm.onForm(f.copy(nom = it)) }, "Nom de l'entreprise *")
                ErrorText(state.nomError)
                AppTextField(
                    f.description, { vm.onForm(f.copy(description = it)) }, "Description",
                    singleLine = false, minLines = 3
                )
                AppTextField(f.secteur, { vm.onForm(f.copy(secteur = it)) }, "Secteur d'activité")
                AppTextField(f.ville, { vm.onForm(f.copy(ville = it)) }, "Ville")
                AppTextField(f.adresse, { vm.onForm(f.copy(adresse = it)) }, "Adresse")
                AppTextField(
                    f.telephone, { vm.onForm(f.copy(telephone = it)) }, "Téléphone",
                    keyboardType = KeyboardType.Phone
                )
                AppTextField(
                    f.emailContact, { vm.onForm(f.copy(emailContact = it)) }, "Email de contact",
                    keyboardType = KeyboardType.Email
                )
                ErrorText(state.emailError)
                AppTextField(
                    f.siteWeb, { vm.onForm(f.copy(siteWeb = it)) }, "Site web",
                    keyboardType = KeyboardType.Uri
                )
                AppTextField(
                    f.logoUrl, { vm.onForm(f.copy(logoUrl = it)) }, "Logo (URL)",
                    keyboardType = KeyboardType.Uri
                )

                ErrorText(state.error)
                PrimaryButton(
                    text = if (state.exists) "Enregistrer" else "Créer l'entreprise",
                    onClick = vm::save,
                    loading = state.saving
                )
                if (state.error != null && !state.exists) {
                    OutlinedButton(onClick = vm::load) { Text("Réessayer") }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
