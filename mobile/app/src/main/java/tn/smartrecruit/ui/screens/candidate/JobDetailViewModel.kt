package tn.smartrecruit.ui.screens.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.repository.OfferRepository

data class JobDetailUiState(
    val loading: Boolean = true,
    val offer: OffreDto? = null,
    val applying: Boolean = false,
    val applied: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class JobDetailViewModel(private val repo: OfferRepository) : ViewModel() {

    private val _state = MutableStateFlow(JobDetailUiState())
    val state: StateFlow<JobDetailUiState> = _state.asStateFlow()

    private var offerId: String? = null

    fun load(id: String) {
        if (offerId == id && _state.value.offer != null) return
        offerId = id
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.getPublicOffer(id)
                .onSuccess { o ->
                    _state.update { it.copy(loading = false, offer = o) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    fun apply() {
        val o = _state.value.offer ?: return
        viewModelScope.launch {
            _state.update { it.copy(applying = true, message = null) }
            // Simulation or call to application endpoint when backend application endpoint is linked
            _state.update {
                it.copy(
                    applying = false,
                    applied = true,
                    message = "Votre candidature pour « ${o.titre} » a été enregistrée avec succès !"
                )
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
