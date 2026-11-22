package tn.smartrecruit.ui.screens.recruiter

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.EntrepriseDto
import tn.smartrecruit.data.repository.OfferRepository

data class CompanyForm(
    val nom: String = "",
    val description: String = "",
    val secteur: String = "",
    val siteWeb: String = "",
    val ville: String = "",
    val adresse: String = "",
    val telephone: String = "",
    val emailContact: String = "",
    val logoUrl: String = ""
)

data class CompanyUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val exists: Boolean = false,
    val form: CompanyForm = CompanyForm(),
    val nomError: String? = null,
    val emailError: String? = null,
    val error: String? = null,
    val message: String? = null
)

class CompanyViewModel(private val repo: OfferRepository) : ViewModel() {

    private val _state = MutableStateFlow(CompanyUiState())
    val state: StateFlow<CompanyUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.getMyCompany()
                .onSuccess { c ->
                    _state.update { s -> s.copy(loading = false, exists = c != null, form = c?.toForm() ?: s.form) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun onForm(form: CompanyForm) {
        _state.update { it.copy(form = form, nomError = null, emailError = null, error = null) }
    }

    fun save() {
        val f = _state.value.form
        val nomError = if (f.nom.isBlank()) "Le nom est obligatoire." else null
        val emailError = if (f.emailContact.isNotBlank() &&
            !Patterns.EMAIL_ADDRESS.matcher(f.emailContact.trim()).matches()
        ) "Adresse email invalide." else null
        if (nomError != null || emailError != null) {
            _state.update { it.copy(nomError = nomError, emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            repo.saveMyCompany(f.toDto())
                .onSuccess { c ->
                    _state.update { it.copy(saving = false, exists = true, form = c.toForm(), message = "Entreprise enregistrée.") }
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    fun consumeMessage() { _state.update { it.copy(message = null) } }
}

private fun String.orNull(): String? = trim().ifBlank { null }

private fun EntrepriseDto.toForm() = CompanyForm(
    nom = nom,
    description = description.orEmpty(),
    secteur = secteur.orEmpty(),
    siteWeb = siteWeb.orEmpty(),
    ville = ville.orEmpty(),
    adresse = adresse.orEmpty(),
    telephone = telephone.orEmpty(),
    emailContact = emailContact.orEmpty(),
    logoUrl = logoUrl.orEmpty()
)

private fun CompanyForm.toDto() = EntrepriseDto(
    nom = nom.trim(),
    description = description.orNull(),
    secteur = secteur.orNull(),
    siteWeb = siteWeb.orNull(),
    ville = ville.orNull(),
    adresse = adresse.orNull(),
    telephone = telephone.orNull(),
    emailContact = emailContact.orNull(),
    logoUrl = logoUrl.orNull()
)
