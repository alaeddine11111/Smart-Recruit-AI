package tn.smartrecruit.ui.screens.cv

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.AnalysisDto
import tn.smartrecruit.data.remote.dto.CandidateProfileDto
import tn.smartrecruit.data.remote.dto.CvInfoDto
import tn.smartrecruit.data.remote.dto.SkillDto
import tn.smartrecruit.data.repository.CvRepository

data class CvUiState(
    val loading: Boolean = true,
    val cv: CvInfoDto? = null,
    val uploading: Boolean = false,
    val analysis: AnalysisDto? = null,
    val profile: CandidateProfileDto? = null,
    val selectedSkills: Set<String> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class CvViewModel(private val repo: CvRepository) : ViewModel() {

    private val _state = MutableStateFlow(CvUiState())
    val state: StateFlow<CvUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init { loadExisting() }

    /** On opening the screen: show the current CV and the result of its last analysis. */
    private fun loadExisting() {
        viewModelScope.launch {
            repo.currentCv()
                .onSuccess { cv ->
                    _state.update { it.copy(loading = false, cv = cv) }
                    if (cv != null) watchAnalysis()
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun onFileChosen(uri: Uri) {
        viewModelScope.launch {
            pollJob?.cancel()
            _state.update {
                it.copy(uploading = true, error = null, message = null, analysis = null, profile = null)
            }
            repo.upload(uri)
                .onSuccess { cv ->
                    _state.update { it.copy(uploading = false, cv = cv) }
                    watchAnalysis()
                }
                .onFailure { e -> _state.update { it.copy(uploading = false, error = e.message) } }
        }
    }

    /** Asks the analysis status every 2 s while it is running (same as the web app). */
    private fun watchAnalysis() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val result = repo.analysis()
                val analysis = result.getOrNull()
                if (analysis == null) {
                    result.exceptionOrNull()?.let { e -> _state.update { it.copy(error = e.message) } }
                    break
                }
                _state.update { it.copy(analysis = analysis) }
                if (analysis.status != "EN_COURS") {
                    if (analysis.status == "TERMINEE") loadProfile()
                    break
                }
                delay(2000)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            repo.profile()
                .onSuccess { p ->
                    // start from the validated skills, otherwise from what the AI detected
                    val initial = p.validatedSkills.orEmpty()
                        .ifEmpty { p.detectedSkills.orEmpty() }
                        .ifEmpty { p.skills.orEmpty() }
                    _state.update {
                        it.copy(profile = p, selectedSkills = initial.mapNotNull { s -> s.name }.toSet())
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun toggleSkill(name: String) {
        _state.update {
            val next = if (name in it.selectedSkills) it.selectedSkills - name else it.selectedSkills + name
            it.copy(selectedSkills = next, message = null)
        }
    }

    /** Saves the checked skills, then validates the profile (needed for matching). */
    fun validate(allSkills: List<SkillDto>) {
        val profile = _state.value.profile ?: return
        val chosen = allSkills.filter { it.name in _state.value.selectedSkills }
        if (chosen.isEmpty()) {
            _state.update { it.copy(error = "Cochez au moins une compétence.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, message = null) }
            val saved = repo.saveProfile(profile.copy(validatedSkills = chosen))
            if (saved.isFailure) {
                _state.update { it.copy(saving = false, error = saved.exceptionOrNull()?.message) }
                return@launch
            }
            repo.validateProfile()
                .onSuccess { p ->
                    _state.update {
                        it.copy(saving = false, profile = p, message = "Profil validé : il sera utilisé pour les recommandations.")
                    }
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }
}