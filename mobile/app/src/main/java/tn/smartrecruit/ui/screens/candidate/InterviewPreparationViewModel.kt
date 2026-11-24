package tn.smartrecruit.ui.screens.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tn.smartrecruit.data.remote.dto.InterviewQuestionDto
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.remote.dto.PreparationSessionDto
import tn.smartrecruit.data.repository.InterviewPreparationRepository
import tn.smartrecruit.data.repository.OfferRepository

private const val MAX_ANSWER_LENGTH = 5000

data class InterviewPreparationUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val sessions: List<PreparationSessionDto> = emptyList(),
    val offers: List<OffreDto> = emptyList(),
    val offersError: String? = null,
    val currentSession: PreparationSessionDto? = null,
    val questionIndex: Int = 0,
    val answer: String = "",
    val lastEvaluatedQuestion: InterviewQuestionDto? = null,
    val starting: Boolean = false,
    val saving: Boolean = false,
    val finishing: Boolean = false,
    val error: String? = null
)

class InterviewPreparationViewModel(
    private val repo: InterviewPreparationRepository,
    private val offerRepo: OfferRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewPreparationUiState())
    val state: StateFlow<InterviewPreparationUiState> = _state.asStateFlow()

    init {
        load(refreshing = false)
    }

    fun refresh() = load(refreshing = true)

    /** History and the optional offer list. A failing offer list does not block a general session. */
    private fun load(refreshing: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = !refreshing && it.sessions.isEmpty(),
                    refreshing = refreshing,
                    error = null
                )
            }
            repo.history()
                .onSuccess { sessions ->
                    _state.update { it.copy(sessions = sessions, loading = false, refreshing = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, refreshing = false, error = e.message) }
                }
            loadOffers()
        }
    }

    private suspend fun loadOffers() {
        offerRepo.getPublicOffers(size = 100)
            .onSuccess { page -> _state.update { it.copy(offers = page.content, offersError = null) } }
            .onFailure { e -> _state.update { it.copy(offersError = e.message) } }
    }

    private suspend fun loadHistorySilently() {
        repo.history().onSuccess { sessions -> _state.update { it.copy(sessions = sessions) } }
    }

    fun start(offreId: String?) {
        if (_state.value.starting) return
        viewModelScope.launch {
            _state.update { it.copy(starting = true, error = null) }
            repo.start(offreId)
                .onSuccess { session ->
                    showSession(session)
                    _state.update { it.copy(starting = false) }
                    loadHistorySilently()
                }
                .onFailure { e -> _state.update { it.copy(starting = false, error = e.message) } }
        }
    }

    fun openSession(id: String) {
        if (_state.value.loading || _state.value.starting) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.get(id)
                .onSuccess { session ->
                    showSession(session)
                    _state.update { it.copy(loading = false) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun onAnswerChange(value: String) {
        _state.update { it.copy(answer = value.take(MAX_ANSWER_LENGTH)) }
    }

    fun submitAnswer() {
        val session = _state.value.currentSession ?: return
        val question = session.questions.getOrNull(_state.value.questionIndex) ?: return
        val answer = _state.value.answer.trim()
        if (answer.isBlank() || _state.value.saving || _state.value.finishing) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            repo.submitAnswer(session.id, question.id, answer)
                .onSuccess { updated ->
                    showSession(updated)
                    _state.update { it.copy(saving = false) }
                    loadHistorySilently()
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    fun finish() {
        val session = _state.value.currentSession ?: return
        if (_state.value.saving || _state.value.finishing) return

        viewModelScope.launch {
            _state.update { it.copy(finishing = true, error = null) }
            repo.finish(session.id)
                .onSuccess { updated ->
                    showSession(updated)
                    _state.update { it.copy(finishing = false) }
                    loadHistorySilently()
                }
                .onFailure { e -> _state.update { it.copy(finishing = false, error = e.message) } }
        }
    }

    fun backToHome() {
        _state.update {
            it.copy(
                currentSession = null,
                questionIndex = 0,
                answer = "",
                lastEvaluatedQuestion = null,
                error = null
            )
        }
    }

    /** Resume at the first unanswered question, like the web candidate app. */
    private fun showSession(session: PreparationSessionDto) {
        val firstUnanswered = session.questions.indexOfFirst { it.evaluation == null }
        val index = if (firstUnanswered < 0) session.questions.size else firstUnanswered
        val lastEvaluated = session.questions.take(index).lastOrNull { it.evaluation != null }
        _state.update {
            it.copy(
                currentSession = session,
                questionIndex = index,
                answer = "",
                lastEvaluatedQuestion = lastEvaluated,
                error = null
            )
        }
    }
}
