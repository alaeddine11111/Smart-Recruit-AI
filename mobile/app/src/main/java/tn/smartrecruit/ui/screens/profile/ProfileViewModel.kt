package tn.smartrecruit.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.smartrecruit.data.remote.dto.ProfileDto
import tn.smartrecruit.data.remote.dto.UpdateProfileRequest
import tn.smartrecruit.data.repository.ProfileRepository

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: ProfileDto? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val passwordSaving: Boolean = false,
    val passwordError: String? = null
)

class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.load()
                .onSuccess { p -> _state.update { it.copy(loading = false, profile = p) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun save(name: String, phone: String, location: String, position: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Le nom est obligatoire.", message = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, message = null) }
            val request = UpdateProfileRequest(
                fullName = name.trim(),
                telephone = phone.trim().ifBlank { null },
                localisation = location.trim().ifBlank { null },
                poste = position.trim().ifBlank { null }
            )
            repo.update(request)
                .onSuccess { p ->
                    _state.update { it.copy(saving = false, profile = p, message = "Profil enregistré.") }
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(passwordSaving = true, passwordError = null) }
            repo.changePassword(oldPassword, newPassword)
                .onSuccess {
                    _state.update { it.copy(passwordSaving = false, message = "Mot de passe modifié.", error = null) }
                    onSuccess()
                }
                .onFailure { e -> _state.update { it.copy(passwordSaving = false, passwordError = e.message) } }
        }
    }

    fun clearPasswordError() {
        _state.update { it.copy(passwordError = null) }
    }
}
