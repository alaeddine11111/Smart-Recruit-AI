package tn.smartrecruit.ui.screens.recruiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.NoCompanyException
import tn.smartrecruit.data.remote.dto.OffreRequestDto
import tn.smartrecruit.data.repository.OfferRepository

data class OfferFormState(
    val loading: Boolean = false,        // loading an existing offer (edit mode)
    val saving: Boolean = false,
    val titre: String = "",
    val description: String = "",
    val localisation: String = "",
    val competences: List<String> = emptyList(),
    val competenceInput: String = "",
    val typeContrat: String = "CDI",
    val salaireMin: String = "",
    val salaireMax: String = "",
    val statut: String = "DRAFT",
    val errors: Map<String, String> = emptyMap(),   // field name -> message
    val error: String? = null,
    val noCompany: Boolean = false,                 // the recruiter must create the company first
    val saved: Boolean = false
)

class OfferFormViewModel(private val repo: OfferRepository) : ViewModel() {

    private val _state = MutableStateFlow(OfferFormState())
    val state: StateFlow<OfferFormState> = _state.asStateFlow()

    private var offerId: String? = null
    private var started = false

    /** id == null: new offer. Otherwise loads the offer to edit. Safe to call on every recomposition. */
    fun start(id: String?) {
        if (started) return
        started = true
        offerId = id
        if (id == null) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repo.getMyOffer(id)
                .onSuccess { o ->
                    _state.update {
                        it.copy(
                            loading = false, titre = o.titre, description = o.description,
                            localisation = o.localisation, competences = o.competences,
                            typeContrat = o.typeContrat, statut = o.statut,
                            salaireMin = numberToText(o.salaireMin), salaireMax = numberToText(o.salaireMax)
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    /** Applies a change to the form and clears the displayed errors. */
    fun edit(change: OfferFormState.() -> OfferFormState) {
        _state.update { it.change().copy(errors = emptyMap(), error = null, noCompany = false) }
    }

    fun addCompetence() {
        val parts = splitSkills(_state.value.competenceInput)
        if (parts.isEmpty()) return
        _state.update { s ->
            s.copy(
                competences = (s.competences + parts).distinctBy { it.lowercase() },
                competenceInput = "",
                errors = s.errors - "competences"
            )
        }
    }

    fun removeCompetence(skill: String) {
        _state.update { it.copy(competences = it.competences - skill) }
    }

    fun save() {
        val s = _state.value
        // a skill typed in the field but not added yet is kept
        val skills = (s.competences + splitSkills(s.competenceInput)).distinctBy { it.lowercase() }
        val min = s.salaireMin.trim().replace(',', '.').toDoubleOrNull()
        val max = s.salaireMax.trim().replace(',', '.').toDoubleOrNull()

        val errs = mutableMapOf<String, String>()
        if (s.titre.isBlank()) errs["titre"] = "Le titre est obligatoire."
        if (s.description.isBlank()) errs["description"] = "La description est obligatoire."
        if (s.localisation.isBlank()) errs["localisation"] = "La localisation est obligatoire."
        if (skills.isEmpty()) errs["competences"] = "Ajoutez au moins une compétence."
        if (s.salaireMin.isNotBlank() && min == null) errs["salaireMin"] = "Nombre invalide."
        if (s.salaireMax.isNotBlank() && max == null) errs["salaireMax"] = "Nombre invalide."
        if (min != null && max != null && min > max) errs["salaireMax"] = "Le maximum doit être supérieur au minimum."
        if (errs.isNotEmpty()) {
            _state.update { it.copy(errors = errs) }
            return
        }

        val body = OffreRequestDto(
            titre = s.titre.trim(),
            description = s.description.trim(),
            localisation = s.localisation.trim(),
            competences = skills,
            typeContrat = s.typeContrat,
            salaireMin = min,
            salaireMax = max,
            devise = "TND",
            statut = s.statut
        )
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, noCompany = false) }
            val id = offerId
            val result = if (id != null) repo.updateOffer(id, body) else repo.createOffer(body)
            result
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, noCompany = e is NoCompanyException, error = e.message) }
                }
        }
    }

    private fun splitSkills(text: String): List<String> =
        text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
