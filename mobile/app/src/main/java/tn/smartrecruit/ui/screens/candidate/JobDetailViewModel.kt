package tn.smartrecruit.ui.screens.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.remote.dto.TrackExternalApplicationRequestDto
import tn.smartrecruit.data.repository.ApplicationRepository
import tn.smartrecruit.data.repository.OfferRepository

data class JobDetailUiState(
    val loading: Boolean = true,
    val offer: OffreDto? = null,
    val applying: Boolean = false,
    val applied: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class JobDetailViewModel(
    private val repo: OfferRepository,
    private val applications: ApplicationRepository
) : ViewModel() {

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
                    applications.myApplications().onSuccess { mine ->
                        if (mine.any { it.offreId == id }) _state.update { it.copy(applied = true) }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    fun apply() {
        val o = _state.value.offer ?: return
        if (_state.value.applying || _state.value.applied) return
        viewModelScope.launch {
            _state.update { it.copy(applying = true, message = null) }
            applications.apply(o.id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            applying = false,
                            applied = true,
                            message = "Votre candidature pour « ${o.titre} » a été enregistrée avec succès !"
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(applying = false, message = e.message ?: "Impossible d'envoyer la candidature.")
                    }
                }
        }
    }

    /** Keep the web app's external application history when the original announcement is opened. */
    fun trackExternalClick() {
        val offer = _state.value.offer ?: return
        val url = offer.urlExterne?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            applications.trackExternal(
                TrackExternalApplicationRequestDto(
                    offreId = offer.id,
                    titre = offer.titre,
                    entreprise = offer.entrepriseNom,
                    urlExterne = url,
                    source = "Externe"
                )
            )
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
