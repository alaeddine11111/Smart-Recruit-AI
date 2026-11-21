package tn.smartrecruit.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tn.smartrecruit.data.model.Role
import tn.smartrecruit.data.remote.dto.RegisterCandidateRequest
import tn.smartrecruit.data.remote.dto.RegisterRecruiterRequest
import tn.smartrecruit.data.repository.AuthRepository

// ---------- Login ----------
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    data class Success(val role: Role) : LoginUiState
}

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("Veuillez remplir tous les champs.")
            return
        }
        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            repo.login(email, password)
                .onSuccess { _state.value = LoginUiState.Success(it) }
                .onFailure { _state.value = LoginUiState.Error(it.message ?: "Erreur inconnue") }
        }
    }
}

// ---------- Register (candidate or recruiter request) ----------
sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Error(val message: String) : RegisterUiState
    data object Success : RegisterUiState
}

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun registerCandidate(request: RegisterCandidateRequest) {
        if (!isValid(request.fullName, request.email, request.password)) return
        submit { repo.registerCandidate(request) }
    }

    fun registerRecruiter(request: RegisterRecruiterRequest) {
        if (!isValid(request.fullName, request.email, request.password)) return
        if (request.company.isBlank() || request.position.isBlank()) {
            _state.value = RegisterUiState.Error("Entreprise et poste sont obligatoires.")
            return
        }
        submit { repo.registerRecruiter(request) }
    }

    private fun isValid(name: String, email: String, password: String): Boolean {
        val error = when {
            name.isBlank() || email.isBlank() || password.isBlank() -> "Veuillez remplir tous les champs obligatoires."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Email invalide."
            password.length < 8 -> "Le mot de passe doit contenir au moins 8 caractères."
            else -> null
        }
        if (error != null) _state.value = RegisterUiState.Error(error)
        return error == null
    }

    private fun submit(call: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.value = RegisterUiState.Loading
            call()
                .onSuccess { _state.value = RegisterUiState.Success }
                .onFailure { _state.value = RegisterUiState.Error(it.message ?: "Erreur inconnue") }
        }
    }
}

// ---------- Session (splash decision, user name, logout) ----------
class SessionViewModel(private val repo: AuthRepository) : ViewModel() {
    /** null = still loading from disk */
    val role: StateFlow<Role?> = repo.role
        .map { Role.from(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userName: StateFlow<String?> = repo.userName
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }
}
