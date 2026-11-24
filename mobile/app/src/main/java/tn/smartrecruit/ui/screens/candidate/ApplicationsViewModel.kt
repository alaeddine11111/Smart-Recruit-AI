package tn.smartrecruit.ui.screens.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.CandidatureDto
import tn.smartrecruit.data.remote.dto.CandidatureExterneDto
import tn.smartrecruit.data.repository.ApplicationRepository

data class ApplicationsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val applications: List<CandidatureDto> = emptyList(),
    val externalApplications: List<CandidatureExterneDto> = emptyList(),
    val error: String? = null
)

class ApplicationsViewModel(private val repo: ApplicationRepository) : ViewModel() {

    private val _state = MutableStateFlow(ApplicationsUiState())
    val state: StateFlow<ApplicationsUiState> = _state.asStateFlow()

    init {
        load(refreshing = false)
    }

    fun refresh() = load(refreshing = true)

    private fun load(refreshing: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = !refreshing && it.applications.isEmpty() && it.externalApplications.isEmpty(),
                    refreshing = refreshing,
                    error = null
                )
            }

            repo.myApplications()
                .onSuccess { list -> _state.update { it.copy(applications = list) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }

            repo.myExternalApplications()
                .onSuccess { list -> _state.update { it.copy(externalApplications = list) } }
                .onFailure { e ->
                    _state.update { it.copy(error = it.error ?: e.message) }
                }

            _state.update { it.copy(loading = false, refreshing = false) }
        }
    }
}
