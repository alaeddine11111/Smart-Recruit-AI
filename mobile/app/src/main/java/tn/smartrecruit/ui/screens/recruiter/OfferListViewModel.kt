package tn.smartrecruit.ui.screens.recruiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.repository.OfferRepository

data class OfferListUiState(
    val loading: Boolean = false,        // full-screen spinner (first load / filter change)
    val refreshing: Boolean = false,     // pull to refresh
    val loadingMore: Boolean = false,    // next page
    val offers: List<OffreDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val statut: String? = null,          // null = all
    val busyId: String? = null,          // offer being published/closed/deleted
    val error: String? = null,
    val message: String? = null
)

class OfferListViewModel(private val repo: OfferRepository) : ViewModel() {

    private val _state = MutableStateFlow(OfferListUiState())
    val state: StateFlow<OfferListUiState> = _state.asStateFlow()

    /** Loads the first page. Called when the screen appears (also after coming back from the form). */
    fun reload(pull: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(loading = !pull && it.offers.isEmpty(), refreshing = pull, error = null) }
            repo.getMyOffers(_state.value.statut, null, 0)
                .onSuccess { p ->
                    _state.update {
                        it.copy(loading = false, refreshing = false, offers = p.content, page = p.page, totalPages = p.totalPages)
                    }
                }
                .onFailure { e ->
                    _state.update { s ->
                        if (s.offers.isEmpty()) s.copy(loading = false, refreshing = false, error = e.message)
                        else s.copy(loading = false, refreshing = false, message = e.message)
                    }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.refreshing || s.loadingMore || s.page + 1 >= s.totalPages) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            repo.getMyOffers(s.statut, null, s.page + 1)
                .onSuccess { p ->
                    _state.update {
                        it.copy(loadingMore = false, offers = it.offers + p.content, page = p.page, totalPages = p.totalPages)
                    }
                }
                .onFailure { e -> _state.update { it.copy(loadingMore = false, message = e.message) } }
        }
    }

    fun setFilter(statut: String?) {
        if (statut == _state.value.statut) return
        _state.update { it.copy(statut = statut, offers = emptyList(), page = 0, totalPages = 0) }
        reload()
    }

    fun changeStatus(offer: OffreDto, value: String) {
        viewModelScope.launch {
            _state.update { it.copy(busyId = offer.id) }
            repo.changeStatus(offer.id, value)
                .onSuccess { updated ->
                    _state.update { s ->
                        val list = if (s.statut != null && updated.statut != s.statut) {
                            s.offers.filter { it.id != updated.id }       // no longer matches the filter
                        } else {
                            s.offers.map { if (it.id == updated.id) updated else it }
                        }
                        s.copy(
                            busyId = null, offers = list,
                            message = if (value == "PUBLISHED") "Offre publiée." else "Offre clôturée."
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(busyId = null, message = e.message) } }
        }
    }

    fun delete(offer: OffreDto) {
        viewModelScope.launch {
            _state.update { it.copy(busyId = offer.id) }
            repo.deleteOffer(offer.id)
                .onSuccess {
                    _state.update { s ->
                        s.copy(busyId = null, offers = s.offers.filter { it.id != offer.id }, message = "Offre supprimée.")
                    }
                }
                .onFailure { e -> _state.update { it.copy(busyId = null, message = e.message) } }
        }
    }

    fun consumeMessage() { _state.update { it.copy(message = null) } }
}
