package tn.smartrecruit.ui.screens.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.MatchingResultDto
import tn.smartrecruit.data.repository.MatchingRepository

data class RecommendationsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val results: List<MatchingResultDto> = emptyList(),
    val error: String? = null
)

class RecommendationsViewModel(
    private val repository: MatchingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RecommendationsUiState())
    val state: StateFlow<RecommendationsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(refreshing: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = !refreshing && it.results.isEmpty(),
                    refreshing = refreshing,
                    error = null
                )
            }

            repository.loadRecommendations()
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            results = results,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            results = emptyList(),
                            error = error.message
                        )
                    }
                }
        }
    }
}