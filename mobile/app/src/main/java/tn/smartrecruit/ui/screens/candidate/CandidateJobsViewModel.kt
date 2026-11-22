package tn.smartrecruit.ui.screens.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.OffreDto
import tn.smartrecruit.data.repository.OfferRepository

enum class JobSource(val label: String) {
    SITE("Offres du site"),
    API("Offres de l'API"),
    ALL("Toutes les offres")
}

data class CandidateJobsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val offers: List<OffreDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val searchQuery: String = "",
    val locationQuery: String = "",
    val selectedContractType: String? = null,
    val jobSource: JobSource = JobSource.ALL,
    val error: String? = null,
    val message: String? = null
)

class CandidateJobsViewModel(private val repo: OfferRepository) : ViewModel() {

    private val _state = MutableStateFlow(CandidateJobsUiState())
    val state: StateFlow<CandidateJobsUiState> = _state.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadOffers()
    }

    fun loadOffers(pull: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = !pull && it.offers.isEmpty(),
                    refreshing = pull,
                    error = null
                )
            }

            when (_state.value.jobSource) {
                JobSource.SITE -> {
                    repo.getPublicOffers(
                        titre = _state.value.searchQuery,
                        typeContrat = _state.value.selectedContractType,
                        localisation = _state.value.locationQuery,
                        page = 0
                    ).onSuccess { pageDto ->
                        _state.update {
                            it.copy(
                                loading = false,
                                refreshing = false,
                                offers = pageDto.content,
                                page = pageDto.page,
                                totalPages = pageDto.totalPages
                            )
                        }
                    }.onFailure { e ->
                        _state.update { s ->
                            if (s.offers.isEmpty()) s.copy(loading = false, refreshing = false, error = e.message)
                            else s.copy(loading = false, refreshing = false, message = e.message)
                        }
                    }
                }

                JobSource.API -> {
                    repo.getExternalOffers(
                        q = _state.value.searchQuery,
                        location = _state.value.locationQuery,
                        page = 1
                    ).onSuccess { externalList ->
                        _state.update {
                            it.copy(
                                loading = false,
                                refreshing = false,
                                offers = externalList,
                                page = 0,
                                totalPages = 1
                            )
                        }
                    }.onFailure { e ->
                        _state.update { s ->
                            if (s.offers.isEmpty()) s.copy(loading = false, refreshing = false, error = e.message)
                            else s.copy(loading = false, refreshing = false, message = e.message)
                        }
                    }
                }

                JobSource.ALL -> {
                    val siteRes = repo.getPublicOffers(
                        titre = _state.value.searchQuery,
                        typeContrat = _state.value.selectedContractType,
                        localisation = _state.value.locationQuery,
                        page = 0
                    )
                    val externalRes = repo.getExternalOffers(
                        q = _state.value.searchQuery,
                        location = _state.value.locationQuery,
                        page = 1
                    )
                    val siteList = siteRes.getOrDefault(null)?.content.orEmpty()
                    val extList = externalRes.getOrDefault(emptyList())
                    val combined = siteList + extList

                    if (combined.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                loading = false,
                                refreshing = false,
                                offers = combined,
                                page = 0,
                                totalPages = 1
                            )
                        }
                    } else {
                        val err = siteRes.exceptionOrNull()?.message ?: externalRes.exceptionOrNull()?.message
                        _state.update { s ->
                            if (s.offers.isEmpty()) s.copy(loading = false, refreshing = false, error = err)
                            else s.copy(loading = false, refreshing = false, message = err)
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.refreshing || s.loadingMore || s.page + 1 >= s.totalPages) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            repo.getPublicOffers(
                titre = s.searchQuery,
                typeContrat = s.selectedContractType,
                localisation = s.locationQuery,
                page = s.page + 1
            ).onSuccess { pageDto ->
                _state.update {
                    it.copy(
                        loadingMore = false,
                        offers = it.offers + pageDto.content,
                        page = pageDto.page,
                        totalPages = pageDto.totalPages
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loadingMore = false, message = e.message) }
            }
        }
    }

    fun toggleJobSource() {
        val nextSource = when (_state.value.jobSource) {
            JobSource.ALL -> JobSource.SITE
            JobSource.SITE -> JobSource.API
            JobSource.API -> JobSource.ALL
        }
        _state.update {
            it.copy(
                jobSource = nextSource,
                offers = emptyList(),
                page = 0,
                message = "Source des offres : ${nextSource.label}"
            )
        }
        loadOffers()
    }

    fun setJobSource(source: JobSource) {
        if (_state.value.jobSource == source) return
        _state.update {
            it.copy(
                jobSource = source,
                offers = emptyList(),
                page = 0,
                message = "Source des offres : ${source.label}"
            )
        }
        loadOffers()
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        debounceReload()
    }

    fun onLocationQueryChange(query: String) {
        _state.update { it.copy(locationQuery = query) }
        debounceReload()
    }

    fun onContractTypeSelect(contractType: String?) {
        if (_state.value.selectedContractType == contractType) return
        _state.update { it.copy(selectedContractType = contractType, offers = emptyList(), page = 0) }
        loadOffers()
    }

    private fun debounceReload() {
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(400)
            _state.update { it.copy(offers = emptyList(), page = 0) }
            loadOffers()
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
