package tn.smartrecruit.ui.screens.cv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.data.remote.dto.SkillDto
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.components.PrimaryButton
import tn.smartrecruit.ui.theme.GreenLight
import tn.smartrecruit.ui.theme.TextMuted

private fun formatSize(bytes: Long): String =
    if (bytes < 1024 * 1024) "${maxOf(1, bytes / 1024)} Ko" else "%.1f Mo".format(bytes / 1024.0 / 1024.0)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CvScreen() {
    val vm: CvViewModel = viewModel(factory = appViewModelFactory())
    val state by vm.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.onFileChosen(uri)
    }
    val analysing = state.analysis?.status == "EN_COURS"
    val profile = state.profile
    val allSkills: List<SkillDto> = profile?.let {
        (it.detectedSkills.orEmpty() + it.skills.orEmpty() + it.validatedSkills.orEmpty())
            .filter { s -> !s.name.isNullOrBlank() }
            .distinctBy { s -> s.name }
    }.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Mon CV", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Un PDF suffit. L'IA en extrait vos compétences, puis vous validez celles qui sont justes.", color = TextMuted)
        }

        state.cv?.let { cv ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = GreenLight), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("CV actuel", color = TextMuted)
                        Text(cv.fileName ?: "cv.pdf", style = MaterialTheme.typography.titleMedium)
                        Text("${formatSize(cv.size)}, déposé le ${cv.uploadedAt?.take(10) ?: "-"}", color = TextMuted)
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = if (state.cv != null) "Remplacer mon CV" else "Choisir un PDF",
                onClick = { picker.launch(arrayOf("application/pdf")) },
                loading = state.uploading,
                enabled = !analysing
            )
        }
        item { ErrorText(state.error) }

        if (analysing) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Analyse en cours… cela peut prendre une minute.", color = TextMuted)
                }
            }
        }
        if (state.analysis?.status == "ECHEC") {
            item { ErrorText(state.analysis?.errorMessage ?: "L'analyse du CV a échoué.") }
        }

        if (profile != null && state.analysis?.status == "TERMINEE") {
            item {
                Text("Données extraites", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.analysis?.ocrUsed == true) "CV scanné : OCR utilisé" else "Texte lu directement dans le PDF",
                    color = TextMuted
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Titre : ${profile.title?.ifBlank { "-" } ?: "-"}")
                    Text("Téléphone : ${profile.phone?.ifBlank { "-" } ?: "-"}")
                    Text("Localisation : ${profile.location?.ifBlank { "-" } ?: "-"}")
                    if (!profile.summary.isNullOrBlank()) Text("Résumé : ${profile.summary}")
                }
            }

            item { Text("Compétences (cochez les bonnes)", style = MaterialTheme.typography.titleMedium) }
            item {
                if (allSkills.isEmpty()) Text("Aucune compétence détectée.", color = TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allSkills.forEach { skill ->
                        val name = skill.name.orEmpty()
                        FilterChip(
                            selected = name in state.selectedSkills,
                            onClick = { vm.toggleSkill(name) },
                            label = { Text(name) }
                        )
                    }
                }
            }

            item { Text("Expériences", style = MaterialTheme.typography.titleMedium) }
            val experiences = profile.experiences.orEmpty()
            if (experiences.isEmpty()) item { Text("Aucune détectée.", color = TextMuted) }
            items(experiences.size) { i ->
                val e = experiences[i]
                Column {
                    Text("${e.position.orEmpty()} - ${e.company.orEmpty()}", style = MaterialTheme.typography.titleSmall)
                    Text("${e.startDate.orEmpty()} → ${e.endDate.orEmpty()}", color = TextMuted)
                    if (!e.description.isNullOrBlank()) Text(e.description)
                }
            }

            item { Text("Formations", style = MaterialTheme.typography.titleMedium) }
            val education = profile.education.orEmpty()
            if (education.isEmpty()) item { Text("Aucune détectée.", color = TextMuted) }
            items(education.size) { i ->
                val f = education[i]
                Column {
                    Text(f.degree.orEmpty(), style = MaterialTheme.typography.titleSmall)
                    Text("${f.school.orEmpty()} (${f.startDate.orEmpty()} → ${f.endDate.orEmpty()})", color = TextMuted)
                }
            }

            item { Text("Langues", style = MaterialTheme.typography.titleMedium) }
            val languages = profile.languages.orEmpty()
            if (languages.isEmpty()) item { Text("Aucune détectée.", color = TextMuted) }
            items(languages.size) { i ->
                val l = languages[i]
                Text("${l.name.orEmpty()} ${l.level.orEmpty()}".trim())
            }

            item {
                PrimaryButton(
                    text = if (profile.validated) "Profil validé ✓ (revalider)" else "Valider mon profil",
                    onClick = { vm.validate(allSkills) },
                    loading = state.saving,
                    enabled = state.selectedSkills.isNotEmpty()
                )
            }
            state.message?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.primary) } }
        }
    }
}