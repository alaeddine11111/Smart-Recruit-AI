package tn.smartrecruit.ui.screens.candidate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.smartrecruit.data.remote.dto.AnswerEvaluationDto
import tn.smartrecruit.data.remote.dto.FinalEvaluationDto
import tn.smartrecruit.data.remote.dto.InterviewQuestionDto
import tn.smartrecruit.data.remote.dto.PreparationSessionDto
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.AppTextField
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.components.PrimaryButton
import tn.smartrecruit.ui.screens.recruiter.shortDate
import tn.smartrecruit.ui.theme.TextMuted
import tn.smartrecruit.ui.theme.TextPrimary
import tn.smartrecruit.ui.theme.TextSecondary

/** Interview tab: start a session, answer its questions and review the AI feedback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewPreparationScreen(
    modifier: Modifier = Modifier,
    vm: InterviewPreparationViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var offerMenuOpen by remember { mutableStateOf(false) }
    var selectedOfferId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedOfferLabel = state.offers.firstOrNull { it.id == selectedOfferId }
        ?.let { offer -> listOfNotNull(offer.titre, offer.entrepriseNom?.ifBlank { null }).joinToString(" · ") }
        ?: "Entretien général"

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
                    "Préparation aux entretiens",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    "Des questions générées à partir de votre profil validé et, si vous choisissez une offre, de ses exigences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            if (state.loading && state.currentSession == null && state.sessions.isEmpty()) {
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
                            if (state.currentSession == null && state.sessions.isEmpty()) {
                                OutlinedButton(onClick = vm::refresh) { Text("Réessayer") }
                            }
                        }
                    }
                }

                val session = state.currentSession
                if (session != null) {
                    SessionContent(session = session, state = state, vm = vm)
                } else {
                    StartContent(
                        state = state,
                        selectedOfferLabel = selectedOfferLabel,
                        offerMenuOpen = offerMenuOpen,
                        onOfferMenuChange = { offerMenuOpen = it },
                        onOfferSelected = { selectedOfferId = it },
                        onStart = { vm.start(selectedOfferId) },
                        onOpenSession = vm::openSession
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.StartContent(
    state: InterviewPreparationUiState,
    selectedOfferLabel: String,
    offerMenuOpen: Boolean,
    onOfferMenuChange: (Boolean) -> Unit,
    onOfferSelected: (String?) -> Unit,
    onStart: () -> Unit,
    onOpenSession: (String) -> Unit
) {
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Commencer une préparation",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    "Choisissez une offre pour cibler le poste, ou laissez le champ vide pour un entretien général. Vous devez avoir au moins une compétence validée dans votre profil. La génération des questions peut prendre quelques secondes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                ExposedDropdownMenuBox(
                    expanded = offerMenuOpen,
                    onExpandedChange = onOfferMenuChange
                ) {
                    OutlinedTextField(
                        value = selectedOfferLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Offre d'emploi (facultatif)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offerMenuOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = offerMenuOpen,
                        onDismissRequest = { onOfferMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Entretien général") },
                            onClick = { onOfferSelected(null); onOfferMenuChange(false) }
                        )
                        state.offers.forEach { offer ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        listOfNotNull(
                                            offer.titre,
                                            offer.entrepriseNom?.ifBlank { null }
                                        ).joinToString(" · ")
                                    )
                                },
                                onClick = { onOfferSelected(offer.id); onOfferMenuChange(false) }
                            )
                        }
                    }
                }

                state.offersError?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }

                PrimaryButton(
                    text = "Démarrer un entretien",
                    onClick = onStart,
                    loading = state.starting
                )
            }
        }
    }

    item {
        Text(
            "Mes préparations précédentes",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
    if (state.sessions.isEmpty()) {
        item { Text("Vous n'avez pas encore de session de préparation.", color = TextMuted) }
    } else {
        items(state.sessions, key = { it.id }) { session ->
            SessionHistoryCard(session = session, onClick = { onOpenSession(session.id) })
        }
    }
}

@Composable
private fun SessionHistoryCard(session: PreparationSessionDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
                    session.offreTitre,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                SessionStatusBadge(session.statut)
            }
            Text(
                "${shortDate(session.dateCreation) ?: "-"} · ${session.questions.size} questions",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Text(
                if (session.statut == "TERMINEE") "${session.scoreGlobal ?: 0}/100" else "En cours",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun LazyListScope.SessionContent(
    session: PreparationSessionDto,
    state: InterviewPreparationUiState,
    vm: InterviewPreparationViewModel
) {
    item {
        TextButton(onClick = vm::backToHome, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Toutes mes sessions")
        }
    }

    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (session.offreId == null) "Entretien général" else "Entretien ciblé",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Text(
                    session.offreTitre,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                SessionStatusBadge(session.statut)
            }
        }
    }

    if (session.statut == "TERMINEE" && session.evaluationFinale != null) {
        ResultsContent(session = session, evaluation = session.evaluationFinale)
    } else {
        val answered = session.questions.count { it.evaluation != null }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Progression",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    "$answered / ${session.questions.size} questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        item {
            LinearProgressIndicator(
                progress = {
                    if (session.questions.isEmpty()) 0f
                    else answered / session.questions.size.toFloat()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.lastEvaluatedQuestion?.evaluation?.let { evaluation ->
            item { LastFeedback(evaluation = evaluation) }
        }

        val question = session.questions.getOrNull(state.questionIndex)
        if (question != null) {
            item {
                QuestionCard(
                    question = question,
                    index = state.questionIndex,
                    total = session.questions.size,
                    answer = state.answer,
                    onAnswerChange = vm::onAnswerChange,
                    onSubmit = vm::submitAnswer,
                    saving = state.saving,
                    busy = state.finishing
                )
            }
        } else {
            item {
                Text(
                    "Toutes vos réponses ont été évaluées. Terminez la session pour obtenir votre bilan global.",
                    color = TextMuted
                )
            }
        }

        if (session.questions.isNotEmpty() && answered == session.questions.size) {
            item {
                PrimaryButton(
                    text = "Terminer et voir mon bilan",
                    onClick = vm::finish,
                    loading = state.finishing,
                    enabled = !state.saving && !state.finishing
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: InterviewQuestionDto,
    index: Int,
    total: Int,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    saving: Boolean,
    busy: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        questionTypeLabel(question.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    "Question ${index + 1} sur $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                question.question,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            AppTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = "Votre réponse",
                placeholder = "Prenez le temps de structurer votre réponse…",
                singleLine = false,
                minLines = 6
            )
            Text(
                "${answer.length}/5000",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.align(Alignment.End)
            )
            PrimaryButton(
                text = "Envoyer ma réponse",
                onClick = onSubmit,
                loading = saving,
                enabled = answer.isNotBlank() && !saving && !busy
            )
        }
    }
}

@Composable
private fun LastFeedback(evaluation: AnswerEvaluationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Évaluation de votre dernière réponse : ${evaluation.score}/100",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            FeedbackLine("Pertinence", evaluation.pertinence)
            FeedbackLine("Exactitude", evaluation.exactitude)
            if (evaluation.pointsManquants.isNotEmpty()) {
                Text(
                    "Points à compléter : ${evaluation.pointsManquants.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            FeedbackLine("Conseil", evaluation.retour)
        }
    }
}

@Composable
private fun FeedbackLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Text(
            "$label : $value",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

private fun LazyListScope.ResultsContent(
    session: PreparationSessionDto,
    evaluation: FinalEvaluationDto
) {
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${session.scoreGlobal ?: 0}/100",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text("Score global", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                LinearProgressIndicator(
                    progress = { (session.scoreGlobal ?: 0).coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    evaluation.forces.takeIf { it.isNotEmpty() }?.let {
        item { EvaluationSection("Vos points forts", it) }
    }
    evaluation.faiblesses.takeIf { it.isNotEmpty() }?.let {
        item { EvaluationSection("À travailler", it) }
    }
    evaluation.axesAmelioration.takeIf { it.isNotEmpty() }?.let {
        item { EvaluationSection("Axes d'amélioration", it) }
    }

    if (!evaluation.conseils.isNullOrBlank()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Conseils pour votre entretien",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(evaluation.conseils, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    item {
        Text(
            "Vos réponses",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
    items(session.questions, key = { it.id }) { question ->
        AnswerReviewCard(question = question)
    }
}

@Composable
private fun EvaluationSection(title: String, points: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        points.forEach { point ->
            Text("• $point", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AnswerReviewCard(question: InterviewQuestionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                question.question,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(question.reponse.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            question.evaluation?.let { evaluation ->
                Text(
                    "${evaluation.score}/100 · ${evaluation.retour.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                if (evaluation.pointsManquants.isNotEmpty()) {
                    Text(
                        "Points à compléter : ${evaluation.pointsManquants.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionStatusBadge(status: String) {
    val finished = status == "TERMINEE"
    val background = if (finished) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondaryContainer
    val foreground = if (finished) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSecondaryContainer

    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(
            if (finished) "Terminé" else "En cours",
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun questionTypeLabel(type: String): String = when (type) {
    "TECHNIQUE" -> "Technique"
    "EXPERIENCE" -> "Expérience"
    else -> "Générale"
}
